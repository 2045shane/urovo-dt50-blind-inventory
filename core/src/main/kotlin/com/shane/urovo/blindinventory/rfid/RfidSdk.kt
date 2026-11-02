package com.shane.urovo.blindinventory.rfid

/**
 * 盲扫读卡面。形状对齐日后优博讯 `RFIDSDKManager` / `RfidManager`：
 * `init` / `startInventory` / `stopInventory` / `release`，回调给出 `epcId` 与可选 RSSI。
 *
 * 本阶段只有 [MockRfidSdk]。真机 AAR（U7/U8）到位后新增 `UrovoRfidSdk` 实现本接口，
 * 在界面工厂里替换 Mock，不要把客户 APK 里的 so/jar 拷进仓库。替换步骤见仓库 `FEATURE_MAP.md`。
 *
 * 映射（实现时对照官方文档，不要从旧包反编译抄代码）：
 * - [init] → `RFIDSDKManager.getInstance().init(context, listener)`
 * - [startInventory] → `getRfidManager().startInventory()`，把 SDK 的成败收成 Boolean
 * - [stopInventory] → `getRfidManager().stopInventory()`
 * - [release] → `RFIDSDKManager.getInstance().release()`
 * - 盘点回调里的 epcId / rssi → [InventoryTag]
 */
interface RfidSdk {
    fun init(listener: InitListener)

    /** 未初始化或硬件拒绝启动时返回 false，不抛给界面。 */
    fun startInventory(): Boolean

    fun stopInventory()

    fun release()

    fun setInventoryCallback(callback: InventoryCallback?)
}

fun interface InitListener {
    fun onInitResult(success: Boolean, message: String)
}

fun interface InventoryCallback {
    fun onInventoryTag(tag: InventoryTag)
}

data class InventoryTag(
    val epcId: String,
    val rssi: Int? = null,
)
