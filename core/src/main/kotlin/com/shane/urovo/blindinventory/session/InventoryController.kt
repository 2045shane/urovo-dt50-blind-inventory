package com.shane.urovo.blindinventory.session

import com.shane.urovo.blindinventory.rfid.InitListener
import com.shane.urovo.blindinventory.rfid.RfidSdk

/**
 * 把读卡回调收进 [InventorySession]。启停成对调用 SDK 与会话，避免一边在读、一边已经暂停。
 */
class InventoryController(
    private val session: InventorySession,
    private val sdk: RfidSdk,
    private val onChange: () -> Unit = {},
) {
    fun init(listener: InitListener) {
        sdk.setInventoryCallback { tag ->
            if (session.onTag(tag.epcId, tag.rssi)) {
                onChange()
            }
        }
        sdk.init(listener)
    }

    fun start(): Boolean {
        if (!sdk.startInventory()) return false
        session.start()
        onChange()
        return true
    }

    fun stop() {
        sdk.stopInventory()
        session.stop()
        onChange()
    }

    /** 停止读卡并清空本场。这是唯一的「新会话」入口。 */
    fun clear() {
        sdk.stopInventory()
        session.clear()
        onChange()
    }

    fun release() {
        sdk.release()
        session.stop()
    }
}
