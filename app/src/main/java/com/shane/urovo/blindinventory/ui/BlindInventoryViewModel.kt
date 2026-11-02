package com.shane.urovo.blindinventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shane.urovo.blindinventory.export.CsvExport
import com.shane.urovo.blindinventory.rfid.MockRfidSdk
import com.shane.urovo.blindinventory.session.InventorySession
import com.shane.urovo.blindinventory.session.InventoryController
import com.shane.urovo.blindinventory.session.SessionPhase
import com.shane.urovo.blindinventory.session.TagRecord
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val phase: SessionPhase = SessionPhase.IDLE,
    val initOk: Boolean = false,
    val initMessage: String = "",
    val uniqueCount: Int = 0,
    val totalReads: Int = 0,
    val recent: List<TagRecord> = emptyList(),
    val banner: String? = null,
)

data class CsvPayload(
    val fileName: String,
    val csv: String,
    val uniqueCount: Int,
)

class BlindInventoryViewModel(
    private val session: InventorySession,
    private val sdk: MockRfidSdk,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private val controller = InventoryController(session, sdk) { publish() }

    private var dripJob: Job? = null
    private var manualIndex: Int = 0

    init {
        controller.init { success, message ->
            publish(initOk = success, initMessage = message)
        }
    }

    fun toggleScan() {
        if (session.phase == SessionPhase.SCANNING) {
            stop()
        } else {
            start()
        }
    }

    fun simulateOne() {
        if (session.phase != SessionPhase.SCANNING) {
            publish(banner = SIMULATE_NEED_START)
            return
        }
        val epc = MockRfidSdk.MANUAL_POOL[manualIndex % MockRfidSdk.MANUAL_POOL.size]
        manualIndex += 1
        val accepted = sdk.inject(epc, rssi = -48)
        if (!accepted) {
            publish(banner = SIMULATE_NEED_START)
        } else {
            publish(banner = null)
        }
    }

    fun clear() {
        dripJob?.cancel()
        dripJob = null
        manualIndex = 0
        controller.clear()
        publish(banner = CLEARED)
    }

    fun buildExport(now: Instant = Instant.now()): CsvPayload? {
        val records = session.records()
        if (records.isEmpty()) return null
        return CsvPayload(
            fileName = CsvExport.fileName(now, zone),
            csv = CsvExport.render(records, zone),
            uniqueCount = records.size,
        )
    }

    override fun onCleared() {
        dripJob?.cancel()
        controller.release()
    }

    private fun start() {
        if (!_state.value.initOk) {
            publish(banner = _state.value.initMessage.ifBlank { "初始化未成功，无法开始盘点" })
            return
        }
        if (!controller.start()) {
            publish(banner = "无法开始盘点")
            return
        }
        dripJob?.cancel()
        dripJob = viewModelScope.launch {
            while (isActive) {
                delay(DRIP_MS)
                if (session.phase == SessionPhase.SCANNING) {
                    sdk.dripOnce()
                }
            }
        }
        publish(banner = null)
    }

    private fun stop() {
        dripJob?.cancel()
        dripJob = null
        controller.stop()
        publish(banner = null)
    }

    private fun publish(
        initOk: Boolean = _state.value.initOk,
        initMessage: String = _state.value.initMessage,
        banner: String? = _state.value.banner,
    ) {
        _state.value = UiState(
            phase = session.phase,
            initOk = initOk,
            initMessage = initMessage,
            uniqueCount = session.uniqueCount(),
            totalReads = session.totalReads(),
            recent = session.recent(RECENT_LIMIT),
            banner = banner,
        )
    }

    companion object {
        const val DRIP_MS: Long = 900L
        const val RECENT_LIMIT: Int = 8
        const val SIMULATE_NEED_START: String = "请先开始盘点，再模拟扫到标签"
        const val CLEARED: String = "本场已清空"

        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (!modelClass.isAssignableFrom(BlindInventoryViewModel::class.java)) {
                    throw IllegalArgumentException("未知 ViewModel：${modelClass.name}")
                }
                @Suppress("UNCHECKED_CAST")
                return BlindInventoryViewModel(
                    session = InventorySession(),
                    sdk = MockRfidSdk(),
                ) as T
            }
        }
    }
}
