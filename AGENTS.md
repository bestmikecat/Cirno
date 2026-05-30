# AGENTS.md

## Project

Cirno is an Xposed module for Android 12+ that freezes background apps through cgroup v2 to reduce CPU usage. Requires Linux kernel >= 5.0.

## Quick Rules

- Communicate in the user's language.
- Write only the minimum code needed to solve the problem. No premature abstraction. No designing flexibility nobody asked for.
- Only change what the task requires. Do not optimize nearby code. Do not refactor things that aren't broken.
- For significant changes/refactors, provide a short implementation plan before coding.
- After each feature, run `./gradlew :app:assembleDebug`.
- Use **hook side** and **ui side** consistently.

## Key Commands

- `./gradlew build` (CI-equivalent full build)
- `./gradlew :app:assembleDebug` (debug APK)
- `./gradlew :app:assembleRelease` (release APK, minified)
- `./gradlew :app:lint` (lint)

## Architecture & Data Flow

- **hook side**: Java; **ui side**: Kotlin + Jetpack Compose.
- Entry: `nep.timeline.cirno.HookInit` (`app/src/main/assets/xposed_init`).
- Only hooks the `android` system process; all other packages are skipped.
- When loading itself, sets `GlobalVars.isModuleActive = true` for module-active detection.
- Hooks are registered in one flow via `master/AndroidHooks.java`.
- Hook framework:
  - Extend `framework/MethodHook` and override `getTargetClass()`, `getTargetMethod()`, `getTargetParam()`, and `getTargetHook()`.
  - Use `framework/AbstractMethodHook` as the XC_MethodHook callback base.
  - Use `getMinVersion()` to gate hooks by SDK version.
- Freezing writes `/sys/fs/cgroup/.../cgroup.freeze` via `FreezerService` + `FrozenRW`.
- Runtime state and config are Binder-first:
  - **ui side** does not fetch data directly.
  - **ui side** requests reads/updates through Binder exposed by **hook side**.
- Config files are JSON under `/data/system/Cirno/`, managed on **hook side** via regular `File` APIs in `ConfigManagerJson` (Gson).

### Binder Discovery Mechanism

- Hook side publishes binders via sticky broadcast `"Cirno-Binder"` through `MonitorBinderHub.publish()`.
- UI side receives via `binder.BinderService.register(context)`, caches IBinders, wraps them via `provide.ConfigBinder/ApplicationBinder/FrozenStateBinder`.

### cgroup v2 Path Layout

- `FrozenRW` supports two cgroup v2 layouts detected at class-load time:
  - **Default**: `/sys/fs/cgroup/uid_{uid}/pid_{pid}/cgroup.freeze`
  - **Isolated** (when `/sys/fs/cgroup/uid_1000/cgroup.freeze` does not exist): system apps under `system/`, normal apps under `apps/`.

### Vendor-Specific Binder Hooks

- **Xiaomi** (`MilletBinderTransHook`) and **OPPO/OnePlus** (`HansKernelUnfreezeHook`) intercept synchronous binder transactions and trigger temporary unfreeze.
- Both self-unhook when ReKernel netlink is active (`BinderService.received == true`), as ReKernel takes over binder monitoring at the kernel level.

### ReKernel Netlink Integration

- ReKernel communicates via netlink socket (protocol unit 22-26, configurable via `GlobalSettings.netlinkUnit`).
- `services.BinderService` (netlink client) listens for kernel-level binder/network events and triggers temporary unfreeze.
- `utils.SystemChecker` detects device vendor by probing for vendor-specific classes (Xiaomi/OPPO/Samsung/Huawei/Vivo/Nubia).

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

## Testing

- Only scaffold tests exist (`ExampleUnitTest`, `ExampleInstrumentedTest`).
- There is no full test suite requirement in this repo.
- Practical validation is build-first (`./gradlew :app:assembleDebug`) plus device/framework verification in an Xposed environment (for example LSPosed).

## Git Commit Style

- Format: `<action>(<scope>): <summary>`
- Actions: `feat`, `fix`, `refact`, `build`, `docs`, `chores`, `ci`, `ui`
- Scopes: specific to the changed area (e.g. `hook`, `config`, `binder`, `network`, `freezer`, `appRecord`, `freezerHandler`, `configManager`). Use space separation for multiple scopes.
- Compound words use camelCase (e.g. `appRecord`, `freezerHandler`, `configManager`)
- Keep subject line ≤ 72 characters, sentence case, no trailing period
- All commits must be GPG signed (`git commit -S`) if a signing key is available

## AGENTS Maintenance Policy

Update this file whenever behavior-impacting project facts change, including:

- Build/verification commands
- Toolchain versions or SDK levels
- Repository structure or ownership boundaries
- Hook side / ui side data-flow boundaries
- Binder contract workflow requirements
- CI/release responsibilities
- Any hard constraint that affects implementation correctness
