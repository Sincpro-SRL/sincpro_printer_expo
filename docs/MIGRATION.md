# Migrating from 1.x to 2.0

The public API (`bluetooth`, `connection`, `config`, `print`) keeps its methods and options. Most
apps only need the Expo upgrade and the config plugin.

## Required

1. **Expo SDK 58.** The module now requires `expo >= 58`, so upgrade the app first
   (`npx expo install expo@^58 --fix`). iOS builds need Xcode 26.4+.
2. **Config plugin.** Add `"@sincpro/printer-expo"` to `app.json` → `expo.plugins`. Without it, iOS
   apps can't use Bluetooth printers.

## Behaviour changes

- `isConnected()` now reflects the real link. A printer that was switched off reports
  `false`, where 1.x kept reporting `true` forever.
- `connectBluetooth` rejects when the connection fails. In 1.x a failed connect resolved, and
  the state flipped to disconnected right after. It also honours `timeoutMs`.
- `PrinterStatus.connectionState` no longer has `ERROR`. A failed status read keeps the
  connection state and sets `hasError` + `errorMessage`. New flags: `isCutterJammed`,
  `isRibbonEnd`, `hasMediaError`.
- `getDpi()` returns the real dpi instead of an internal code.
- Rejections carry `error.code` (`PrinterErrorCode`); see `isPrinterError()`.
- Printed layout: see the "Printed output vs 1.x" table in [TESTING.md](TESTING.md).

## New

- `connection.addListener()`, `connection.getState()` / `getConnection()` and the
  `useConnectionState()` hook, so apps can react to `link_lost` instead of polling.
- `bluetooth.scan()` for iOS discovery.
- `BluetoothDevice.transport` (`classic | ble | unknown`).
