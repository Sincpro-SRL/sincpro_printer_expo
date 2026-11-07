# Copilot instructions — @sincpro/printer-expo

Read these before changing code:

- `docs/ARCHITECTURE.md`: layers, the TypeScript ↔ native print job contract, the session model.
- `docs/BIXOLON_NOTES.md`: verified Bixolon SDK behaviour (return values, timeout units, threading).
- `docs/DEVELOPMENT.md`: where each kind of change goes, example apps, releases.

Key rules:

- Printed layout is computed in TypeScript (`src/domain/receipt.ts`); native code only draws primitives.
- Only `entrypoints/` (Kotlin and Swift) may import Expo; the other native layers stay plain so the
  example apps in `example_platforms/` can compile them directly.
- Every Bixolon SDK call goes through `PrinterService.enqueue` (one SDK thread/queue per platform).
- Never bump the version by hand; never commit generated `build/` outputs.
