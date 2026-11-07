# Development

## Requirements

- Node.js 22.13+, 24.3+ or 26 (Expo SDK 58 tooling)
- Android Studio, JDK 17
- **Xcode 26.4+** to build Expo SDK 58 apps for iOS. The native iOS example app builds with older
  Xcode versions.

## Setup

```bash
make init          # pre-commit + npm install
make build         # src/ → build/, plugin/src/ → plugin/build/
make test
```

## Where to change what

| Change                                                    | Where                                                                                                |
| --------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| Layout of printed elements (positions, wrapping, columns) | `src/domain/receipt.ts`, `src/domain/metrics.ts` + tests in `src/__tests__/`                         |
| Public JS API                                             | `src/services/printer_service.ts`, exports in `src/index.ts`                                         |
| Native contract (new command or field)                    | `src/domain/print_job.ts`, plus `entrypoints/PrinterRecords` and `domain/PrintJob` on both platforms |
| Connection / session behaviour                            | `services/PrinterService.kt` / `.swift`                                                              |
| Bixolon SDK calls                                         | `adapters/BixolonClient.kt` / `.swift`                                                               |
| Info.plist keys for apps                                  | `plugin/src/index.ts`                                                                                |
| Android permissions                                       | `android/src/main/AndroidManifest.xml`                                                               |

Native changes are exercised with the example apps (`docs/TESTING.md`). They compile the module
sources directly: open them, change code under `android/` or `ios/`, and run again.

## Trying the module in an app before publishing

```bash
make pack                      # creates sincpro-printer-expo-<version>.tgz
# in the app: "@sincpro/printer-expo": "file:../path/to/sincpro-printer-expo-<version>.tgz"
```

## Updating the Bixolon SDKs

1. Android: replace the jars in `android/libs/` and the `.so` files in `android/src/main/jniLibs/`.
2. iOS: replace `ios/Frameworks/frmLabelPrinterSDK.xcframework`.
3. Put the new manuals in `docs/manuals/` and update `docs/BIXOLON_NOTES.md`.
4. Build both example apps (Android release included) and run the hardware checklist.

## Releases

The version is set only by the release workflow (`make update-version VERSION=x.y.z` from
`03-release_draft`). Never edit it by hand. Breaking changes are marked in the commit (`feat!:`
plus a `BREAKING CHANGE:` footer). `make publish` builds and publishes to npm.
