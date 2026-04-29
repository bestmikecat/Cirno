# AGENTS.md

## Project

Cirno is an Xposed framework module for Android 12+ that freezes background apps via cgroup v2 to reduce CPU usage. Requires cgroup v2 (Linux kernel >= 5.0).

## Build & Run

```bash
./gradlew build          # full build (what CI runs)
./gradlew :app:assembleDebug    # debug APK only
./gradlew :app:assembleRelease  # release APK (minified, ProGuard applied)
```

No `lint` or `typecheck` tasks are configured beyond what `build` includes.

## Architecture

- **Single Gradle module**: `:app` (namespace `nep.timeline.cirno`)
- **Mixed language**: Hook/backend logic is Java; UI layer (activities, screens) is Kotlin with Jetpack Compose
- **Xposed entrypoint**: `nep.timeline.cirno.HookInit` (declared in `app/src/main/assets/xposed_init`)
  - Only hooks the `android` system process — all other packages are skipped
  - When loading itself, sets `GlobalVars.isModuleActive = true` for module-active detection
- **Hook orchestration**: `master/AndroidHooks.java` registers all hooks in a single `start()` call
- **Hook framework**: Extend `framework/MethodHook` (abstract class) — override `getTargetClass()`, `getTargetMethod()`, `getTargetParam()`, `getTargetHook()`. Use `framework/AbstractMethodHook` as the XC_MethodHook callback base. Use `getMinVersion()` to gate hooks by SDK version.
- **Freezing mechanism**: `services/FreezerService.java` writes to `/sys/fs/cgroup/.../cgroup.freeze` via `utils/FrozenRW.java`
- **Config**: JSON files in `/data/system/Cirno/` (read via libsu `SuFile` in hook context, plain `File` in app context). Managed through `configs/ConfigManagerJson.java` with Gson.

## Key Conventions

- **Lombok** is a compile-only dependency with `annotationProcessor` (not kapt). Java files use Lombok annotations — don't convert them to Kotlin without removing Lombok.
- **UI toolkit**: Miuix KMP (`top.yukonga.miuix.kmp`) + Haze (blur). Not Material Design.
- **Xposed scope**: `android` and `nep.timeline.cirno` only (see `res/values/array.xml`)
- **ProGuard**: Release builds use minification. Keep rules preserve all classes under `nep.timeline.cirno.**` and specific `GlobalVars` fields — do not rename these.

## Toolchain

- JDK 25 (Temurin), AGP 9.1.0, Kotlin 2.3.20, Gradle 9.3.1
- compileSdk / targetSdk = 36, minSdk = 31
- Java source/target compatibility = 25

## CI

The `android.yml` workflow triggers on the **`legacy`** branch (not `main`/`master`). Builds with `./gradlew build`, signs release APK, uploads both debug and release artifacts.

## Testing

Only scaffold tests exist (`ExampleUnitTest`, `ExampleInstrumentedTest`). No real test suite — verify by building and installing on device with an Xposed framework (LSPosed etc.).
