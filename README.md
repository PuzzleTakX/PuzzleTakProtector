# PuzzleTakProtector Library

PuzzleTakProtector is a library designed to help with security checks in Android applications. It offers a series of checks for various security issues such as root access, debugging, emulator checks, and more.

## How to use it?

To use the library, you can call the following methods:

```java
PuzzleTakProtectorLib.checkIsRoot();
PuzzleTakProtectorLib.checkIsDebug();
PuzzleTakProtectorLib.checkIsPortUsing();
PuzzleTakProtectorLib.checkXposedExistAndDisableIt();
PuzzleTakProtectorLib.checkIsBeingTracedByC();
PuzzleTakProtectorLib.checkIsRunningInVirtualApk();
PuzzleTakProtectorLib.checkIsRunningInEmulator();

## More Functions

For more functions, please refer to the following classes:

- `SecurityCheckUtil.class`
- `EmulatorCheckUtil.class`
- `VirtualApkCheckUtil.class`
- `AccessibilityServicesCheckUtil.class`

## Proguard

No need for additional Proguard rules. The library is compatible with Proguard out of the box.

## Compatibility

- **Minimum Android SDK**: Requires a minimum API level of 16.
- **CPU Architecture**: Supports both x86 & ARM architectures.


# Test

| Phone      | SDK         | ROM             |
| ---------- | ----------- | --------------- |
| RedMi 3s   | Android 6.0 | Google ENG      |
| Huawei P9  | Android 7.0 | EMUI 5.1 (Root) |
| Mix 2      | Android 8.0 | MIUI 9 Stable   |
| OnePlus 5T | Android 8.1 | H2OS 5.1 Stable |
| Android 10 | Tested      | Various ROMs    |
| Android 11 | Tested      | Various ROMs    |
| Android 12 | Tested      | Various ROMs    |
| Android 13 | Tested      | Various ROMs    |
| Android 14 | Tested      | Various ROMs    |
| Android 15 | Tested      | Various ROMs    |

