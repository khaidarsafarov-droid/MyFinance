## Phase 3.X — [Название шага]

**Цель:** Разбить `[GodFile].kt` ([N] строк) на модули ≤600 строк (ideal ≤350).

### Что сделано

| До | После | Строки (до→после) |
|---|---|---|
| `[GodFile].kt` | `[NewDir]/[File1].kt` | [N]→[X] |
| | `[NewDir]/[File2].kt` | →[Y] |
| | `[NewDir]/[File3].kt` | →[Z] |

### Архитектура

- **Hilt:** Ручной `UserComponent` (не `@DefineComponent`). Модули подключаются через `*.create()` в `UserComponent.create()`.
- **Scope:** `@UserScope` для репо/VM, `@Singleton` для координаторов.
- **Facade:** Старый класс оставлен `@Deprecated` (если есть внешние вызовы).

### Проверки

- [ ] `./gradlew :app:checkKotlinFileSize` — green (нет новых >600)
- [ ] `./gradlew :app:testDebugUnitTest` — все проходят
- [ ] `./gradlew :app:assembleDebug` — без ошибок
- [ ] Hilt compile-time check — `hiltJavaCompileDebug` ок

### Тесты

- [ ] Unit: [название теста] — проверяет [что]
- [ ] Instrumented (если UI): [название] — проверяет [что]

### Риски / Rollback

- **Риск:** [кратко]
- **Rollback:** `git revert` этого PR; facade (если есть) продолжит работать.

### Связанные шаги

- Blocks: [следующий шаг, если есть]
- Blocked by: [предыдущий шаг]
