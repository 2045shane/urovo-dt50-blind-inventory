# 盲扫盘点（Urovo DT50）

方案 A 的 Phase 1 / U1b：工业风演示壳 + 模拟 RFID。不包含优博讯 URFID AAR，也不连接任何后台。

Cut：`RFID-DT50-BLIND-CUT-2026-11-02-v1`

## 构建

需要 JDK 17+ 与 Android SDK 35（build-tools 35）。

```bash
export ANDROID_HOME="$HOME/android-sdk"   # 或写入 local.properties 的 sdk.dir
./gradlew :core:test
./gradlew :app:assembleDebug
```

调试包：`app/build/outputs/apk/debug/app-debug.apk`

安装到模拟器或 DT50：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

应用不声明 `INTERNET`。盘点与导出都不需要登录或网络。

## 模拟 RFID 怎么用

冷启动会 `init` 模拟模块（成功提示「模拟 RFID 已就绪，未连接真机模块」）。

1. 点 **开始盘点**。界面大约每 0.9 秒从 8 个固定 EPC 里抽一张，所以唯一数会先上升，随后停住、次数继续涨（这就是去重）。
2. **模拟扫到** 另塞 3 个 `…DEMO…` EPC，轮流重复，方便单步看 count。
3. **停止盘点** 后不再增加。再点 **开始盘点** 会**继续本场**，不新建会话。
4. 只有 **清空本场** 会停扫并清空，下一场从零开始。
5. **导出 CSV** 把当前唯一标签交给系统分享面板。空场会提示没有可导出的标签。

停止期间的模拟读数会被丢掉。

## CSV

UTF-8，无 BOM。表头恰好是：

```text
epc,count,first_seen,last_seen
```

时间是设备本地时区的 ISO-8601，秒精度，模式 `yyyy-MM-dd'T'HH:mm:ssXXX`。上海时区形如 `2026-11-02T13:00:00+08:00`；零时区写成 `Z`。文件名 `blind-inventory-yyyyMMdd-HHmmss.csv`（同一秒重名加 `-2`）。行序是首次读到的顺序；屏幕是最近读到在前，并且最多 8 条，文件含全部唯一 EPC。

## 换成真机 SDK

见 [FEATURE_MAP.md](FEATURE_MAP.md) 的「Mock → 真机」。不要把客户 APK 里的 so/jar/代码拷进本仓库。扳机键码仍是未知项 U3，这一阶段只用屏幕按钮。
