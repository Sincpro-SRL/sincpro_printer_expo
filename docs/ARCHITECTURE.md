# Architecture

`@sincpro/printer-expo` is an Expo module that drives Bixolon label printers (SPP-L310 / L410 and
family) on Android and iOS. It follows the Sincpro layering (`domain/`, `adapters/`,
`infrastructure/`, `services/`, `entrypoints/`) on all three sides: TypeScript, Kotlin and Swift.

## Repository layout

```
src/                      TypeScript — public API and the shared print layout
  domain/                 printer types, media, firmware metrics, receipt layout, print job contract
  adapters/               typed binding to the native module (requireNativeModule)
  services/               public API: bluetooth, connection, config, print, useConnectionState
android/                  Kotlin — Expo module for Android
  libs/                   Bixolon Label SDK 2.1.2 jars
  src/main/jniLibs/       libbxl_common.so per ABI
  src/main/java/sincpro/expo/printer/
    entrypoints/          Expo DSL + JS records (the only Expo-dependent layer)
    services/             PrinterService: session, SDK thread, connection state
    adapters/             BixolonClient (SDK wrapper), AndroidBluetooth (paired list, link watch)
    domain/               plain data: connection, status, config, print job
    infrastructure/       bitmap decoding, PDF rasterizing
ios/                      Swift — Expo module for iOS (same layers as Android)
  Frameworks/             frmLabelPrinterSDK.xcframework (Bixolon iOS SDK 1.1.16, dynamic)
plugin/                   Expo config plugin (iOS Info.plist keys)
example_platforms/
  android/                native Android test app (Android Studio)
  ios/                    native iOS test app (Xcode)
docs/                     this documentation and the Bixolon manuals
```

## Design rules

**Layout lives in TypeScript, native code is a thin driver.** Positions, alignment, line heights,
wrapping, columns and QR/barcode sizes are computed once in `src/domain/receipt.ts` and sent to
native as a `NativePrintJob` (`src/domain/print_job.ts`): a media description plus primitive
commands (`text`, `qr`, `barcode`, `image`, `pdf`) with dot coordinates. Android and iOS only draw
those primitives inside one print transaction, so both platforms print the same output and the
layout is covered by Jest tests.

Font and barcode codes in the job are Bixolon firmware codes, identical in both SDKs. Device fonts
are rendered by the printer firmware, so their metrics (`src/domain/metrics.ts`) are the same on
both platforms.

**Only `entrypoints/` knows Expo.** `domain/`, `adapters/`, `services/` and `infrastructure/` are
plain Kotlin/Swift. That is what lets the native example apps compile the module sources directly
(no AAR or framework copies): what you test in `example_platforms/` is exactly what ships.

## Native session model

`PrinterService` (one per platform) owns the printer session:

- **One SDK thread / serial queue.** The Bixolon SDKs block and are not thread-safe. Every SDK call
  goes through `enqueue`, which runs tasks one at a time. Overlapping connects in 1.x left orphan
  RFCOMM sockets that kept the printer busy; serializing removes that class of bug.
- **Connection state is owned by the service** and published as `onConnectionChange`
  (`state`, `address`, `reason`). It is verified against the SDK link on every query
  (`isConnected`, `getConnection`, before printing).
- **Connect** reuses the session when already connected to the same printer. Otherwise it drops
  any previous session, connects with the requested timeout, reads the dpi and applies the stored
  configuration.
- **Printer-side drops** (power off, out of range):
  - Android: the SDK never reports these, so `AndroidBluetooth` watches the system
    `ACL_DISCONNECTED` broadcast for the connected MAC, then confirms with the SDK link.
  - iOS: the SDK reports them through `didBeBrokenConnection`.
  - Both publish `DISCONNECTED` with reason `link_lost`.

```
DISCONNECTED ──connect──▶ CONNECTING ──ok──▶ CONNECTED ──disconnect──▶ DISCONNECTED (user_disconnect)
      ▲                       │                   │
      └──── connect_failed ───┘                   └──link drop──▶ DISCONNECTED (link_lost)
```

## Errors

A single error type per layer: native throws `PrinterException` / `PrinterError` with a code, and
the entrypoint maps it to an Expo coded error. In JS every rejection has `error.code` in
`PrinterErrorCode` (`NOT_CONNECTED`, `CONNECT_TIMEOUT`, `CONNECT_FAILED`, `UNSUPPORTED_MODEL`,
`PRINT_FAILED`, `INVALID_ARGUMENT`, `UNSUPPORTED_PLATFORM`, …); `isPrinterError()` narrows it.

## Configuration

`config.set()` stores the configuration and applies it immediately when connected, and on every
later connect. Missing fields fall back to the 1.x defaults (margins 0, density medium,
top-to-bottom, cutter off). The one exception is `speed`: it is only sent when the app sets it,
because 1.x never managed to change the speed and printers kept their own setting. Applying the
configuration is best effort: a setting the model rejects is logged and the rest still apply.
