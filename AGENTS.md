# AGENTS.md

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

## Git Commit Style

- Format: `<action>(<scope>): <summary>`
- Actions: `feat`, `fix`, `refact`, `build`, `docs`, `chores`, `ci`, `ui`
- Scopes: specific to the changed area (e.g. `hook`, `config`, `binder`, `network`, `freezer`, `appRecord`, `freezerHandler`, `configManager`). Use `,` separation for multiple scopes.
- Compound words use camelCase (e.g. `appRecord`, `freezerHandler`, `configManager`)
- Keep subject line ≤ 72 characters, sentence case, no trailing period
- All commits must be GPG signed (`git commit -S`) if a signing key is available
