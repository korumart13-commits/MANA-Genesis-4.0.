# MANA Genesis 4.0 — универсальное Android-приложение и отдельный MANA OS/AOSP track

**Главный источник всех требований:** [MANA Genesis 4.0 — MASTER FILE (RU)](MANA_GENESIS_4.0_MASTER_FILE_RU.md).  
Все задачи, изменения кода, сборки и отчёты должны соответствовать этому документу. Если старый документ противоречит ему, считать старую формулировку устаревшей и согласованно обновить её.

## Цель текущего релиза

Один продукт MANA с единым центральным MANA Brain, локальным офлайн-движком, памятью, голосом, аватаром, модулями и безопасным саморазвитием. Базовая цель переносимого Android-приложения — **Android 10 / API 29+**, ARM64 как основной ABI; фактические возможности определяются по устройству и подтверждаются матрицей тестов.

**realme RMX5566RU / RE645F, Android 16 / API 36, Snapdragon 685 / SM6225, ARM64** — первый целевой тестовый профиль, а не единственная поддерживаемая модель и не доказательство универсальной совместимости. Полноценные ROM/OTA создаются отдельно для каждой квалифицированной аппаратной ревизии.

После штатной установки APK владелец нажимает одну кнопку **«ЗАПУСТИТЬ MANA»**. Она запускает доступную приложению автоматическую подготовку, сохраняет состояние и возобновляет незавершённые шаги. Системные разрешения, лицензии, аккаунты, платежи, разблокировка загрузчика и прошивка остаются под контролем владельца; обычный APK не обходит защиту Android.

## Архитектура и рабочие документы

- [Главный файл требований MANA Genesis 4.0](MANA_GENESIS_4.0_MASTER_FILE_RU.md)
- [Независимый контроль полноты](docs/MANA_COMPLETENESS_GUARD_RU.md)
- [Последний автономный инженерный аудит](docs/MANA_AUTONOMOUS_AUDIT_LOG.md)
- [Архитектура универсального приложения и правило одного нажатия](docs/MANA_ASI_UNIVERSAL_ARCHITECTURE_RU.md)
- [Реализационное задание](ai-collaboration/inbox/CLAUDE_MANA_ASI_UNIVERSAL_IMPLEMENTATION_TASK.md)
- [Локальная LiteRT-LM модель](launcher/LOCAL_MODEL_SETUP.md)
- [GitHub Actions engineering loop](docs/MANA_GITHUB_ACTIONS_AUTOMATION_2026-11-27.md)
- [Интеграция Android Engine/Binder](docs/MANA_ENGINE_SERVICE_INTEGRATION_2026-11-27.md)
- [Профиль целевого устройства](device/RMX5566RU_TARGET_PROFILE.md)
- [Аудит исходников и аппаратных блокеров](device/RMX5566RU_SOURCE_DISCOVERY_2026-11-27.md)
- [Анализ read-only runtime audit](device/RMX5566RU_RUNTIME_AUDIT_ANALYSIS_2026-11-27.md)
- [Статус выполнения проекта](docs/PROJECT_EXECUTION_STATUS_2026-11-27.md)

## Статус и честные границы

В репозитории есть Kotlin/JVM Brain foundation, Android launcher/app source, OS core, AIDL/Binder scaffolding, LiteRT-LM runtime integration, encrypted persistence/recovery code, model integrity checks, one-action bootstrap entry и GitHub Actions engineering-loop implementation.

Наличие исходников и зелёной части CI не означает, что APK установлен и проверен на физическом телефоне, локальная модель реально генерирует ответы на RMX5566RU или все модули завершены. Полная Android 16 AOSP/Soong интеграция, device-specific ROM, OTA и физически проверенный rollback также не завершены. Точный актуальный статус и доказательства записываются в журнале аудита выше.

## Правила безопасности

Не создавать вирус, stealth persistence, exploit chain, скрытую эскалацию привилегий или обход Verified Boot. Не прошивать неподтверждённые образы. Не менять `main` и не сливать PR без отдельного согласия владельца. Разделять SOURCE, CI, Gradle APK, emulator, physical device, AOSP ROM и OTA/rollback доказательства.

Полный перечень функций, архитектура, критерии приёмки и известные блокеры находятся в главном файле выше.
