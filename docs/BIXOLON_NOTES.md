# Bixolon SDK notes

These are facts checked against the SDK binaries, not only the manuals; the manuals are wrong or
silent on several of them. Manuals: `docs/manuals/`.

## Versions

| Platform | SDK                                      | Files                                                               |
| -------- | ---------------------------------------- | ------------------------------------------------------------------- |
| Android  | Label SDK **2.1.2**, libcommon **1.4.5** | `android/libs/*.jar`, `android/src/main/jniLibs/*/libbxl_common.so` |
| iOS      | Label Printer SDK **1.1.16**             | `ios/Frameworks/frmLabelPrinterSDK.xcframework`                     |

From Android 2.1.1 to 2.1.2: no API change. It adds the XC3/XC5 models and code page CP874, and
ships an updated native device database. The vendor README inside the zip lists version numbers
that don't match its own files (V2.2.5 / 1.2.5). `Bixolon_pdf.aar` is no longer used, because PDFs
are rasterized with the platform renderer.

## Android (verified in bytecode)

- **`connect` result.** It returns the model name on success and `""` on any failure. It never
  returns null, and never anything containing "fail". 1.x checked for `"fail"`, so every failed
  connect looked successful.
- **Overloads and timeout.** Every overload ends in `connect(address, interfaceType, port, timeoutMs)`.
  - Interface types: 0 Bluetooth classic, 1 network, 2 USB, 3 BLE. The manual's "1 = BLE" is wrong.
  - `connect(address)` passes timeout 0, which means wait forever.
  - The timeout is in **milliseconds** (`Future.get(timeout, MILLISECONDS)`).
- **Error codes.** Use `getLastError()` after a failure: 1000 connect failed, 1007 unsupported model.
  The SDK disconnects by itself when the model name read after connecting isn't in its label list.
- **Printer-side drops are never reported** through the Handler, because no listener is wired
  internally. Only `isConnected()` reflects a drop.
- **Not thread-safe.** Process-wide singletons and static state. Use one instance, from one thread.
- **Handler.** The Looper argument of the constructor is stored and never used. Messages go to the
  Handler's own looper. `MESSAGE_OUTPUT_COMPLETE` is never sent.
- **Codes.** Speed codes are 0..6 (`SPEED_25IPS`…`SPEED_80IPS`). 1.x sent 25/50/70/80, which was
  always rejected. Draw and set calls return 0 on success, 50 when not connected or the write
  failed, and 1004 for an invalid parameter.
- **R8.** `libbxl_common.so` registers natives by class name, so `android/proguard-rules.pro` keeps
  `com.bixolon.**`. It is shipped as consumer rules. Expo SDK 58 enables R8 in release builds.

## iOS (verified in the binary and headers)

- **Connection types.**
  - Bluetooth classic uses ExternalAccessory: MFi models only, protocol `com.bixolon.protocol`.
  - BLE uses CoreBluetooth and needs no MFi.
  - `connectWithAddress:port:` connects over TCP for dotted IPs and over BLE for a MAC address. The
    SDK scans for the BLE printer while connecting.
- **Timeout units.**
  - `connectWithTarget:timeout:` takes **milliseconds**.
  - `disconnectWithTimeout:` takes seconds.
  - `lookupPrinterWithCount:interval:` takes intervals in seconds. The official sample passes
    1000, which is a bug.
- **Callbacks.** Delegate callbacks and receivers (status, print completion) arrive on the main
  queue, while the calls themselves block the caller. Never wait for a receiver on the main thread.
  The delegate property is `weak`, and every delegate method is `@required`.
- **Paired list.** `getPairedDevices` only lists MFi printers whose classic link is currently up.
  Use discovery (`bluetooth.scan()`) to find BLE printers.
- **Device fonts.** CENTER alignment is not supported by the SDK, which is one more reason the
  layout computes x positions in TypeScript.
- **No full/partial cut.** `setAutoCutter` has no full/partial selector.
- **Framework.** It is dynamic and needs iOS 15.0+ (the module requires 16.4 for Expo SDK 58). It
  has an arm64 simulator slice. Do not import `BluetoothModuleInfo.h`: it references a header the
  framework doesn't ship.
- **App Store.** Shipping an app that declares `com.bixolon.protocol` (classic Bluetooth) requires
  Bixolon to register the bundle ID in Apple's MFi program. BLE-only apps don't need it.
