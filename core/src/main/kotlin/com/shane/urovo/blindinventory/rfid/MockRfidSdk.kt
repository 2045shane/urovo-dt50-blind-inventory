package com.shane.urovo.blindinventory.rfid

import kotlin.random.Random

/**
 * 不接触硬件的读卡实现。只有处于盘点中时，[inject] / [dripOnce] 才会把标签交给回调。
 * 界面在「盘点中」定时调用 [dripOnce]，并用「模拟扫到」调用 [inject]。
 */
class MockRfidSdk(
    private val pool: List<String> = DEFAULT_POOL,
    private val random: Random = Random.Default,
) : RfidSdk {
    var initialized: Boolean = false
        private set

    var inventorying: Boolean = false
        private set

    private var callback: InventoryCallback? = null
    private var failInit: Boolean = false
    private var failInitMessage: String = "RFID 初始化失败"

    fun failNextInit(fail: Boolean = true, message: String = "RFID 初始化失败") {
        failInit = fail
        failInitMessage = message
    }

    override fun init(listener: InitListener) {
        if (failInit) {
            initialized = false
            inventorying = false
            listener.onInitResult(false, failInitMessage)
            return
        }
        initialized = true
        listener.onInitResult(true, "模拟 RFID 已就绪，未连接真机模块")
    }

    override fun startInventory(): Boolean {
        if (!initialized) return false
        inventorying = true
        return true
    }

    override fun stopInventory() {
        inventorying = false
    }

    override fun release() {
        inventorying = false
        initialized = false
        callback = null
    }

    override fun setInventoryCallback(callback: InventoryCallback?) {
        this.callback = callback
    }

    /** @return 已交给回调则为 true；未在盘点中则丢弃并返回 false。 */
    fun inject(epcId: String, rssi: Int? = -58): Boolean {
        if (!inventorying) return false
        callback?.onInventoryTag(InventoryTag(epcId = epcId, rssi = rssi))
        return true
    }

    fun dripOnce(): Boolean {
        if (!inventorying || pool.isEmpty()) return false
        val epc = pool[random.nextInt(pool.size)]
        val rssi = -35 - random.nextInt(40)
        return inject(epc, rssi)
    }

    companion object {
        val DEFAULT_POOL: List<String> = listOf(
            "E28011606000020900000001",
            "E28011606000020900000002",
            "E28011606000020900000003",
            "E28011606000020900000004",
            "E28011606000020900000005",
            "E28011606000020900000006",
            "E28011606000020900000007",
            "E28011606000020900000008",
        )

        /** 「模拟扫到」轮换的三条，不在自动滴入池里，方便看出按钮单独生效。 */
        val MANUAL_POOL: List<String> = listOf(
            "E28011700000DEMO000001",
            "E28011700000DEMO000002",
            "E28011700000DEMO000003",
        )
    }
}
