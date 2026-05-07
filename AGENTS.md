# AGENTS.md

## Project

Cirno is an Xposed module for Android 12+ that freezes background apps through cgroup v2 to reduce CPU usage. Requires Linux kernel >= 5.0.

## Quick Rules

- For significant changes/refactors, provide a short implementation plan before coding.
- After each feature, run `./gradlew :app:assembleDebug`.
- Use **hook side** and **ui side** consistently.

## Key Commands

- `./gradlew build` (CI-equivalent full build)
- `./gradlew :app:assembleDebug` (debug APK)
- `./gradlew :app:assembleRelease` (release APK, minified)
- `./gradlew :app:lint` (lint)

## Repository Structure

- Root module: `:app`
- Build config: `build.gradle.kts`, `settings.gradle.kts`, `gradle/wrapper/`
- Hook entry/orchestration:
  - `app/src/main/java/nep/timeline/cirno/HookInit.java`
  - `app/src/main/java/nep/timeline/cirno/master/AndroidHooks.java`
- Hook framework:
  - `app/src/main/java/nep/timeline/cirno/framework/MethodHook.java`
  - `app/src/main/java/nep/timeline/cirno/framework/AbstractMethodHook.java`
- Freezer:
  - `app/src/main/java/nep/timeline/cirno/services/FreezerService.java`
  - `app/src/main/java/nep/timeline/cirno/utils/FrozenRW.java`
- Config manager:
  - `app/src/main/java/nep/timeline/cirno/configs/ConfigManagerJson.java`
- Xposed declarations:
  - `app/src/main/assets/xposed_init`
  - `app/src/main/res/values/array.xml`
- CI:
  - `.github/workflows/android.yml`
  - `.github/workflows/release.yml`

## Architecture & Data Flow

- **hook side**: Java; **ui side**: Kotlin + Jetpack Compose.
- Entry: `nep.timeline.cirno.HookInit` (`app/src/main/assets/xposed_init`).
- Only hooks the `android` system process; all other packages are skipped.
- When loading itself, sets `GlobalVars.isModuleActive = true` for module-active detection.
- Hooks are registered in one flow via `master/AndroidHooks.java`.
- Hook framework details:
  - Extend `framework/MethodHook` and override `getTargetClass()`, `getTargetMethod()`, `getTargetParam()`, and `getTargetHook()`.
  - Use `framework/AbstractMethodHook` as the XC_MethodHook callback base.
  - Use `getMinVersion()` to gate hooks by SDK version.
- Freezing writes `/sys/fs/cgroup/.../cgroup.freeze` via `FreezerService` + `FrozenRW`.
- Runtime state and config are Binder-first:
  - **ui side** does not fetch data directly.
  - **ui side** requests reads/updates through Binder exposed by **hook side**.
- Config files are JSON under `/data/system/Cirno/`, managed on **hook side** via regular `File` APIs in `ConfigManagerJson` (Gson).

## Critical Constraints

- Lombok is a compile-only dependency with `annotationProcessor` (not kapt). Do not convert Lombok-based Java code to Kotlin unless Lombok usage is removed or replaced correctly.
- UI toolkit: Miuix KMP (`top.yukonga.miuix.kmp`) + `miuix-blur` (not Material-style by default).
- Xposed scope is only `android` and `nep.timeline.cirno` (`app/src/main/res/values/array.xml`).
- Keep release ProGuard rules preserving `nep.timeline.cirno.**` and required `GlobalVars` fields.
- **ui side must not** directly access `/sys/fs/cgroup`.
- **ui side must not** directly read/write `/data/system/Cirno`.
- Do not bypass `ConfigManagerJson` for config writes.
  - `setXXXX` APIs (existing/future) can be entrypoints.
  - Final mutation must still go through `ConfigManagerJson`.
- For any new state/config field, update Binder contract on **hook side** first, then wire **ui side**.

## Workflows

- Feature changes:
  1. For significant work, provide a plan first.
  2. If state/config contract changes are needed, implement **hook side** Binder changes first.
  3. Wire **ui side** to Binder.
  4. Run `./gradlew :app:assembleDebug`.
- Config changes:
  1. Keep mutation centralized in `ConfigManagerJson`.
  2. Expose updates through Binder-facing APIs.
  3. Keep **ui side** Binder-only for config access.

## Performance Constraints

- Avoid blocking Binder operations on the UI main thread when possible.
- Prefer coalesced/batched Binder reads over chatty repeated calls.
- Avoid high-frequency polling in **ui side**; prefer event-driven updates when available.
- Keep Binder payloads limited to required fields.

## Toolchain

- JDK 25 (Temurin), AGP 9.1.0, Kotlin 2.3.20, Gradle 9.4.1
- compileSdk = 37, targetSdk = 36, minSdk = 31
- Java source/target compatibility = 25

## CI & Release Responsibilities

- `android.yml`: runs on `legacy` push/PR (path-filtered) and manual dispatch; executes `./gradlew build`, signs release APK, and publishes CI artifacts.
- `release.yml`: runs on GitHub Release published; executes `./gradlew build`, signs release APK, and uploads Release assets.

## Testing

- Only scaffold tests exist (`ExampleUnitTest`, `ExampleInstrumentedTest`).
- There is no full test suite requirement in this repo.
- Practical validation is build-first (`./gradlew :app:assembleDebug`) plus device/framework verification in an Xposed environment (for example LSPosed).

## Git Commit Style

- Format: `<scope>: <summary>`
- Scopes: `hook`, `ui`, `config`, `binder`, `build`, `docs`, `fix`, `chores`, `ci`
- Special-case scopes: `revert`, `merge`

## AGENTS Maintenance Policy

Update this file whenever behavior-impacting project facts change, including:

- Build/verification commands
- Toolchain versions or SDK levels
- Repository structure or ownership boundaries
- Hook side / ui side data-flow boundaries
- Binder contract workflow requirements
- CI/release responsibilities
- Any hard constraint that affects implementation correctness
