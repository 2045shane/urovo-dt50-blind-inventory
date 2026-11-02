# Feature Map

Cut：`RFID-DT50-BLIND-CUT-2026-11-02-v1`  
阶段：Phase 1 / U1b（AAR 前的演示壳）。方案 A only。  
SoT：Notion binding cut（2026-11-02）。本文件与该 cut 的 Feature Map 对齐；U2–U5 不写成必须功能。

## 与 cut 对齐

| Feature | v1（cut） | Phase 1（本仓库现在） |
| --- | --- | --- |
| RFID init / start / stop / release | ✅ | ✅ 接口 + Mock。没有 URFID AAR |
| 扳机或按钮启停 | ✅ | 按钮 ✅。扳机键码是 U3（未知），本阶段不猜、不接 |
| EPC 去重 + count | ✅ | ✅ 同一场按 EPC 去重，count 递增 |
| 会话 UI（计数 / 最近列表 / 清空） | ✅ | ✅ 单屏 |
| CSV 导出 + 分享 | ✅ | ✅ UTF-8 + 系统分享 |
| 离线可用 | ✅ | ✅ 无登录、无 `INTERNET` |
| 演示级 UI（大按钮 / 醒目计数 / 清晰导出） | ✅ | ✅ 中文工业风深色壳 |
| 中金后台 / API 上报 | ❌ | ❌ |
| 地图 / 微信登录 | ❌ | ❌ |
| EPC→SKU 映射 / 写库存 | ❌ | ❌ |

范围外（不要加进本 cut）：方案 B 库存主数据、条码主流程、多厂商 SDK、BLE 外挂枪、功率/区域高级调参、HTTP API、标签写/锁/杀。

## 屏幕

只有一个界面：`MainActivity` → `BlindInventoryScreen`。

| 区域 | 行为 |
| --- | --- |
| 顶栏 | 「盲扫盘点」、方案 A、离线不上传、状态（未开始 / 盘点中 / 已暂停） |
| 计数 | 大号唯一标签数；其下是累计读取次数 |
| 提示 | 写死 A4：停止后再开始 = 继续本场；只有清空才新开一场 |
| 初始化 | 成功：小字「模拟 RFID 已就绪…」。失败：红字中文，开始按钮不可用 |
| 最近读取 | 最近 8 条，EPC + 时间 + 可选 RSSI + `×count`。空场有说明 |
| 开始 / 停止 | 底部主按钮。盘点中为红「停止盘点」，否则为黄绿「开始盘点」 |
| 导出 CSV | 主按钮正下方的琥珀色按钮，空场点击会说明没有标签 |
| 模拟扫到 | 仅演示。未在盘点中则提示先开始 |
| 清空本场 | 确认后停止并清空 |

包名：`com.shane.urovo.blindinventory`。不要用 `com.hdwl.*`。

## 模块

| 模块 | 内容 | 依赖 Android |
| --- | --- | --- |
| `:core` | `RfidSdk`、`MockRfidSdk`、`InventorySession`、`InventoryController`、`CsvExport`、`ExportFiles` | 否。单元测试在这里 |
| `:app` | Compose 界面、`BlindInventoryViewModel`（自动滴入）、`CsvShare`（FileProvider） | 是 |

界面状态在 ViewModel。`:core` 不引用 Android，所以 `./gradlew :core:test` 不需要模拟器。

## RFID 接口

`com.shane.urovo.blindinventory.rfid.RfidSdk`

| 方法 | 含义 |
| --- | --- |
| `init(InitListener)` | 初始化。回调 `onInitResult(success, message)`，文案给界面 |
| `startInventory(): Boolean` | 开始盘点。失败返回 false |
| `stopInventory()` | 停止 |
| `release()` | 释放 |
| `setInventoryCallback` | `InventoryTag(epcId, rssi: Int?)` |

`InventoryController` 把回调写进 `InventorySession`：没在 `SCANNING` 就丢弃。

### A4（写死）

**停止后再开始 = 继续同一场。** 不去重重置，不新建会话。  
**只有「清空本场」新开一场**（先 `stopInventory`，再清空表，相位回到 `IDLE`）。  
停止或未开始时的标签（含模拟）不增加 count。  
导出是当前表的快照：不停止、不清空。

### Mock → 真机

U7/U8 之前不要提交 AAR / `.so`。客户 APK 里的库和代码不要拷过来。

1. 把官方 URFIDLibrary AAR 放到 `app/libs/`（该目录已 gitignore）。
2. 新增 `UrovoRfidSdk : RfidSdk`（放 `:app`，因为它需要 `Context`）：
   - `init` → `RFIDSDKManager.getInstance().init(context, listener)`
   - `startInventory` → `getRfidManager().startInventory()`，把成败收成 Boolean
   - `stopInventory` → `stopInventory()`
   - `release` → `RFIDSDKManager.getInstance().release()`
   - 盘点回调的 epcId / rssi → `InventoryTag`
3. 在 `BlindInventoryViewModel.factory()` 里用 `UrovoRfidSdk` 换掉 `MockRfidSdk`。
4. 去掉或藏起「模拟扫到」和自动滴入（那是 Mock 的 `inject` / `dripOnce`，不在 `RfidSdk` 上）。
5. 扳机仍等 U3 确认后再接，不要沿用旧包键码当验收条件。

官方文档入口：<https://urovosdkcenter.github.io/docs-site/zh/rfid/>。以文档和实机 Demo 为准，不要以旧 APK 反编译为准。

## CSV

实现：`CsvExport`。分享：`CsvShare` 写到 `cache/exports/`，经 `FileProvider`（`${applicationId}.fileprovider`）走 `ACTION_SEND` / `text/csv`。

| 项 | 值 |
| --- | --- |
| 编码 | UTF-8，无 BOM |
| 表头 | `epc,count,first_seen,last_seen` |
| 时间 | 设备本地 ISO-8601，秒精度，`yyyy-MM-dd'T'HH:mm:ssXXX`（零时区为 `Z`） |
| 行序 | 首次读到的顺序 |
| 文件名 | `blind-inventory-yyyyMMdd-HHmmss.csv`，同一秒冲突则 `-2`、`-3` |
| RSSI | 只在屏幕上，不进 CSV |

含逗号、引号或换行的 EPC 按 RFC 4180 加引号。模拟 EPC 都是十六进制，正常不会触发。
