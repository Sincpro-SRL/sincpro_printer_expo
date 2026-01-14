# Bixolon SDK - Complete Analysis

## Problem Context

**Symptom:** Printer retracts paper instead of feeding forward after printing
**Root cause being investigated:** LABEL vs PAGE mode, media configuration, orientation

---

## SDK Flow Analysis (from Java Sample)

### 1. Connection Flow

```java
// From sample code
mBixolonLabelPrinter = new BixolonLabelPrinter(context, handler, Looper.getMainLooper());
mBixolonLabelPrinter.connect(address);  // Bluetooth
```

**Our implementation:** ✅ CORRECT - Same flow

---

### 2. Configuration Flow (CRITICAL)

```java
// Sample shows these configurations are OPTIONAL and set via dialogs:
mBixolonLabelPrinter.setOrientation(ORIENTATION_TOP_TO_BOTTOM);  // Default: TOP_TO_BOTTOM
mBixolonLabelPrinter.setMargin(horizontalMargin, verticalMargin); // Default: 0, 0
mBixolonLabelPrinter.setDensity(density);  // Default: 10
mBixolonLabelPrinter.setSpeed(speed);      // Default: 50
mBixolonLabelPrinter.setOffset(offset);    // For BLACK_MARK media
```

**Key insight:** Sample DOES NOT call these on every print - they're set ONCE via settings dialog.

**Our implementation:** ❌ PROBLEM - We're calling `configure()` in test app, but SDK should work OUT-OF-BOX with defaults

---

### 3. Print Transaction Flow

#### Sample Pattern (for every print job):

```java
// Step 1: Begin transaction
mBixolonLabelPrinter.beginTransactionPrint();

// Step 2: Set media configuration (INSIDE transaction)
// For LABEL mode (GAP/BLACK_MARK):
mBixolonLabelPrinter.setLength(
    1218,  // heightDots (label length)
    24,    // gapDots
    BixolonLabelPrinter.MEDIA_TYPE_GAP,  // or MEDIA_TYPE_BLACK_MARK
    0      // offset
);

// For CONTINUOUS/PAGE mode:
// NO setLength() call! Just beginTransactionPrint()

// Step 3: Draw content
mBixolonLabelPrinter.drawText(text, x, y, fontSize, ...);
mBixolonLabelPrinter.drawQrCode(data, x, y, ...);
mBixolonLabelPrinter.drawBitmap(bitmap, x, y, ...);

// Step 4: Print
mBixolonLabelPrinter.print(copies, 1);

// Step 5: End transaction
mBixolonLabelPrinter.endTransactionPrint();
```

#### Our Pattern:

```kotlin
// BixolonPrinterAdapter.beginTransaction()
p.setWidth(media.widthDots)  // ⚠️ Sample does NOT call this!
p.beginTransactionPrint()

// Only for GAP/BLACK_MARK:
if (media.type != MediaType.CONTINUOUS) {
    p.setLength(media.heightDots, media.gapDots, mediaCode, 0)
}
```

**CRITICAL FINDING:** We're calling `setWidth()` BEFORE `beginTransactionPrint()`, but sample NEVER calls `setWidth()`!

---

## setWidth() vs setLength() - What do they do?

### setWidth(widthDots)

**Purpose:** Configure paper width (in dots)
**When to use:** When printer needs to know paper width for calculations
**Sample usage:** ❌ NEVER CALLED in sample code

### setLength(heightDots, gapDots, mediaType, offset)

**Purpose:** Configure LABEL dimensions for label printers
**When to use:** ONLY for LABEL mode (GAP/BLACK_MARK media)
**Sample usage:** ✅ Called inside transaction for label printing

**Parameters:**

- `heightDots`: Label height (not including gap)
- `gapDots`: Gap between labels
- `mediaType`: GAP=0, CONTINUOUS=1, BLACK_MARK=2
- `offset`: Offset for BLACK_MARK sensor

---

## LABEL Mode vs PAGE Mode

### LABEL Mode (Label Printer)

**Characteristics:**

- Uses GAP or BLACK_MARK sensors
- Automatically detects label boundaries
- Stops at each label
- Requires `setLength()` configuration

**Usage:**

```java
beginTransactionPrint();
setLength(labelHeight, gap, MEDIA_TYPE_GAP, 0);
drawContent();
print(1, 1);  // Prints 1 label
endTransactionPrint();
```

**Media types:**

- `MEDIA_TYPE_GAP` = 0 - Labels with gap between them
- `MEDIA_TYPE_BLACK_MARK` = 2 - Labels with black mark separator

### PAGE Mode (Receipt Printer / Continuous Paper)

**Characteristics:**

- Continuous paper roll (no labels)
- No automatic stopping
- Paper feeds based on content height
- NO `setLength()` call needed

**Usage:**

```java
beginTransactionPrint();
// NO setLength() call!
drawContent();
print(1, 1);  // Feeds paper based on content
endTransactionPrint();
```

**Media type:**

- `MEDIA_TYPE_CONTINUOUS` = 1

---

## Our Current Configuration

### MediaConfig.kt

```kotlin
companion object {
    fun continuous80mm() = MediaConfig(640, 200, MediaType.CONTINUOUS, 0)
    fun continuous58mm() = MediaConfig(464, 200, MediaType.CONTINUOUS, 0)
}
```

**Analysis:**

- ✅ `widthDots = 640` = 80mm at 203dpi (640/8 = 80mm)
- ⚠️ `heightDots = 200` - This is IGNORED for CONTINUOUS mode!
- ✅ `type = CONTINUOUS` - Correct for receipt printing
- ✅ `gapDots = 0` - Correct for continuous

**Expected behavior:** Paper should feed based on content, not fixed 200 dots

---

## Hypothesis: Why Paper Retracts

### Possible causes:

1. **❌ setWidth() interference**

   - We call `setWidth(640)` before `beginTransactionPrint()`
   - Sample NEVER calls this
   - **Action:** Remove `setWidth()` call

2. **❌ Wrong orientation default**

   - Printer might default to `BOTTOM_TO_TOP` (66)
   - **Action:** Ensure `ORIENTATION_TOP_TO_BOTTOM` (84) is set

3. **❌ Height calculation error**

   - `heightDots = 200` might confuse printer in CONTINUOUS mode
   - **Action:** Change to `heightDots = 0` for continuous

4. **❌ Missing initial configuration**
   - Printer needs one-time config after connect
   - **Action:** Call `configure()` once after connect (not before every print)

---

## Sample Code Evidence

### What sample DOES call:

1. ✅ `beginTransactionPrint()` - Always
2. ✅ `setLength()` - ONLY for GAP/BLACK_MARK
3. ✅ `drawXXX()` - Drawing methods
4. ✅ `print(copies, 1)` - Always
5. ✅ `endTransactionPrint()` - Always

### What sample DOES NOT call:

1. ❌ `setWidth()` - NEVER
2. ❌ `configure()` before every print - Settings are GLOBAL
3. ❌ `setLength()` for CONTINUOUS mode

---

## Recommended Fixes

### Fix 1: Remove setWidth() call

**Current code:**

```kotlin
override suspend fun beginTransaction(media: MediaConfig): Result<Unit> {
    p.setWidth(media.widthDots)  // ❌ REMOVE THIS
    p.beginTransactionPrint()
    // ...
}
```

**Fixed code:**

```kotlin
override suspend fun beginTransaction(media: MediaConfig): Result<Unit> {
    p.beginTransactionPrint()
    // Only for LABEL mode:
    if (media.type != MediaType.CONTINUOUS) {
        p.setLength(media.heightDots, media.gapDots, mediaCode, 0)
    }
}
```

### Fix 2: Configure once after connect (optional)

**Current:** Test app manually calls `configure()` after connect
**Better:** Adapter auto-configures on first connection

```kotlin
override suspend fun connect(config: ConnectionConfig): Result<Unit> {
    // ... connect logic ...
    if (result success) {
        // Auto-configure with defaults
        p.setOrientation(BixolonLabelPrinter.ORIENTATION_TOP_TO_BOTTOM)
        p.setMargin(0, 0)
        p.setDensity(10)
        p.setSpeed(50)
    }
}
```

### Fix 3: Fix MediaConfig height for continuous

**Current:**

```kotlin
fun continuous80mm() = MediaConfig(640, 200, MediaType.CONTINUOUS, 0)
```

**Fixed:**

```kotlin
fun continuous80mm() = MediaConfig(640, 0, MediaType.CONTINUOUS, 0)
//                                         ^ height=0 for continuous
```

---

## Testing Plan

1. **Test 1:** Remove `setWidth()` call, keep everything else
2. **Test 2:** Change `heightDots` from 200 to 0 for continuous
3. **Test 3:** Add auto-configure on connect (if needed)
4. **Test 4:** Verify orientation is set to TOP_TO_BOTTOM

---

## Next Steps

1. ✅ Document SDK behavior (this file)
2. ⏳ Remove `setWidth()` call from `beginTransaction()`
3. ⏳ Test if paper feeds correctly
4. ⏳ If still retracts, check orientation setting
5. ⏳ If still fails, analyze printer firmware defaults
