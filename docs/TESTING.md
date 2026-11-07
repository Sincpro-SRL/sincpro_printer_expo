# Testing

## Automated

```bash
make test            # Jest: layout, media, image header parsing (src/__tests__)
make verify-format   # prettier + eslint + tsc (module and plugin)
```

## Example apps (real printer)

Both apps compile the module's native layers directly. They have the same test catalogue as the
1.x Android test app: connectivity, text, QR, barcode, image, PDF, receipt, errors and config.

|         | Open                                 | Run on a USB device                                                                   |
| ------- | ------------------------------------ | ------------------------------------------------------------------------------------- |
| Android | `make open-android` (Android Studio) | `make example-android` (debug) · `make example-android-release` (R8, like production) |
| iOS     | `make open-ios` (Xcode)              | Pick the iPhone in Xcode and press Run                                                |

To run on a physical device:

- **iOS:**
  - In Xcode → _Signing & Capabilities_, select a Team (a free personal team works for testing).
  - On the iPhone, enable _Developer Mode_.
  - Bluetooth only works on a physical device, not in the simulator.
- **Android:** enable _USB debugging_. `adb devices` must list the phone.

## Hardware checklist (SPP-L310 / L410)

Run each item on Android **and** iOS. The `conn_*` cases in the example apps cover most of them.

**Connection: the reported bug**

- [ ] Connect → status shows CONNECTED and a test print works.
- [ ] Tap Connect again while connected (`conn_05`) → stays connected, no reconnect.
- [ ] Double connect (`conn_06` / `conn_07` on iOS) → ends CONNECTED and prints. This was the
      orphan-socket bug.
- [ ] Turn the printer off while connected (`conn_08`) → DISCONNECTED / `link_lost` within a few
      seconds.
- [ ] Walk out of range and back → DISCONNECTED, then a reconnect works.
- [ ] Connect with the printer off or to a wrong MAC (`conn_09`) → fails within the timeout, with
      no endless "Conectando...".
- [ ] Android release build (`make example-android-release`) → connects and prints, so the R8
      rules work.

**Printing**

- [ ] Text, font sizes (no overlap between rows), bold, alignment.
- [ ] Spanish characters `áéíóú ñ ¿¡`. The code page was left as in 1.x; report if they print
      wrong.
- [ ] QR, CODE128, CODE39, EAN13.
- [ ] Image (logo) and PDF page; copies.
- [ ] Status with the cover open and without paper.

**iOS specific**

- [ ] Find out how your printer connects: does it appear in `Get Paired Devices` (MFi, classic) or
      only in `Discover Printers` (BLE)?
- [ ] The MAC shown on iOS equals the one Android stores, so a saved printer reconnects on both.

## Printed output vs 1.x

The layout keeps the 1.x spacing (20-dot top margin, 10-dot side padding, 30-dot rows, same fonts
and 100-dot tail on continuous paper). These changes are bug fixes; check them on paper:

| Area                                   | 1.x                                           | 2.0                                                         |
| -------------------------------------- | --------------------------------------------- | ----------------------------------------------------------- |
| Large / xlarge rows                    | Fixed 30-dot rows, so taller fonts overlapped | Row height = font height (40 / 76)                          |
| Long text                              | Cut off past the paper edge                   | Wrapped on word boundaries                                  |
| `keyValue`                             | `key: value`                                  | Key left, value right on one row, as the 1.x docs described |
| `columns`                              | Fixed 48/64 characters, overflowed 72/80 mm   | Characters computed from the paper width                    |
| `separator` default                    | 48 characters                                 | Fills the paper width                                       |
| QR / barcode / image default alignment | Left (options parsing bug)                    | Center, as the 1.x defaults intended                        |
| Images wider than the paper            | Overflowed                                    | Scaled down to the paper width                              |
| PDF background                         | Transparent                                   | White                                                       |
| `speed`                                | Always rejected by the SDK                    | Applied when set; left untouched when not set               |
| `getDpi()`                             | Internal code                                 | Real dpi (203 / 300 / 600)                                  |
