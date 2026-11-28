# MANA Genesis 4.0 — автономный инженерный аудит

Дата: 2026-11-28  
Ветка: `chatgpt/mana-asi-rmx5566-one-action-setup`  
PR: [#22](https://github.com/korumart13-commits/MANA-Genesis-4.0./pull/22)  
Проверенный кодовый снимок после исправлений: `fc6795a20847da9dc48e4b176f767e508bbdb0d2`  
Последний документальный снимок перед созданием этого журнала: `57318e9c80aa1b7f6c3fbdd457c09c80cdcf9035`

## Что проверено

Перед аудитом прочитаны `MANA_GENESIS_4.0_MASTER_FILE_RU.md` и `docs/MANA_COMPLETENESS_GUARD_RU.md`. Проверены PR #22, полный список изменённых файлов, ключевые Android/Brain/Core исходники, инженерный workflow, связанные тесты и актуальные GitHub Actions. `main` не изменялся, PR не объединялся и не закрывался.

## Исправления этого аудита

- **Model bootstrap:** добавлена очередь общих callback-ов для параллельных вызовов Engine/UI/Bridge. Отсутствие проверенной модели теперь возвращается как явный результат, а не как тихий выход из фонового потока. Это позволяет UI показать владельцу условия Gemma и запросить отдельное согласие. Добавлены JVM unit-тесты очереди.
- **Завершение загрузки модели:** MainActivity теперь слушает системное событие DownloadManager и автоматически пытается активировать модель после завершения загрузки; событие фильтруется по ID именно MANA-загрузки, повторная загрузка блокируется флагом.
- **Сохранность отчёта:** отчёт one-action setup публикуется через atomic replace с безопасным fallback; старый отчёт больше не удаляется заранее перед попыткой переименования.
- **Честный статус:** экран после bootstrap теперь говорит о завершении базовой подготовки, а не о полной готовности MANA; отдельно указывает, что модель, разрешения и внешние интеграции могут требовать действий владельца.
- **CI hardening:** job, который применяет и исполняет предлагаемый patch в Gradle/tests, оставлен с `contents: read`. `id-token: write` и `attestations: write` перенесены в отдельный post-test job, который только скачивает готовый артефакт и не исполняет изменённый код. Документация обновлена.
- **Сохранение требований:** Google ecosystem services (Calendar, Maps, Search, Drive/files и продуктивные сервисы через официальные API/OAuth) добавлены в master и completeness guard отдельно от Google Gemini AI provider.
- **README:** устранено противоречие, будто MANA поддерживает только RMX5566; теперь указаны Android 10/API 29+ как цель приложения и RMX5566RU/RE645F как первый тестовый профиль.

Основные commits исправлений: `4e74dabe`, `2db8b62b`, `c9e526e8`, `1fa42225`, `fd4e17de`, `2c28f136`, `34e1c129`, `6a8a935b`, `788cabc3`, `8a1c47b7`, `bd0b067c`, `b32c03d2`, `fc6795a2`, `57318e9c`.

## CI evidence

Для промежуточного кодового снимка `788cabc3da96db75019c216cde9152ffdb0483b6` завершились успешно:
- [MANA Brain Engine — JVM verification](https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/46125053080)
- [MANA llama.cpp JNI — Android ARM64 build](https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/46125053032)
- [Soong and Binder integration contract](https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/46125053067)
- [MANA Engineering CI](https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/46125053060)

Для проверки hardening workflow:
- Первичный run `46124755303` завершился ошибкой только на записи machine-readable diagnostics и загрузке этого отчёта. Причина была в shell escaping в YAML.
- После исправления escaping [push run `46124835779`](https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/46124835779) прошёл полностью, включая отдельный OIDC provenance-attestation job.
- [PR run `46125268933`](https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/46125268933) прошёл validation, JVM tests и JAR build; attestation был корректно пропущен для pull_request.

Для более позднего кодового снимка `fc6795a20847da9dc48e4b176f767e508bbdb0d2` на момент последней проверки:
- [JNI ARM64 build `46125402757`](https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/46125402757) — **success**.
- [OS Core JVM `46125402705`](https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/46125402705) — **success**.
- [Microworld Core JVM `46125402742`](https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/46125402742) — **success**.
- [Launcher Gradle `46125398366`](https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/46125398366) — **in progress**, сейчас собирает unsigned release APK для последующей AOSP platform signing.
- [Android API 36 smoke `46125398394`](https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/46125398394) — **in progress** на шаге emulator install/launch; API 29 и 33 ещё queued.
- Brain Engine `46125402770`, Engineering CI `46125402745` и emulator install/launch matrix `46125398361` — **queued**. Дублирующие PR runs для части workflow были cancelled при одновременных push/PR запусках, это не считается тестовым провалом.
- После создания audit log запуски на документальном head `982b5b6366e7d765a1df4e63da132c294fb5d367` были queued; после обновления CI-раздела новые запуски для head `21bdfad1b4ab73e680813fade62bbb15fc4bd57d` тоже начались: OS Core JVM `46125765603` — **in progress**, остальные соответствующие runs пока **queued**.
- Успех промежуточного SHA не выдаётся за успех последнего снимка; актуальные queued/in-progress проверки должны завершиться. Отдельно: unsigned APK не является обычным installable release APK или прошивкой.

## Матрица реального статуса

Статус означает наличие исходников/контракта, а не подтверждённую работу на телефоне.

| Компонент | Статус | Фактическая граница |
|---|---|---|
| MANA Brain | PARTIAL | Есть единый local-first router и Android composition root; физический inference на целевом телефоне не подтверждён |
| LiteRT-LM / Gemma 4 E2B | PARTIAL / NEEDS_DEVICE_TEST | Есть настоящий Engine/Conversation runtime, каталог, согласие, size/SHA проверки; модель не поставляется в APK и не проверена на RMX5566 |
| OpenAI / Claude / Gemini / Grok | PARTIAL | Адаптеры присутствуют; API-ключи владельца не подключены и live API-запросы не проверены |
| Локальная память и знания | PARTIAL | AES-GCM/Keystore-backed хранилища и поиск есть в исходниках; физическая миграция/восстановление не проверены |
| Diary | PARTIAL | Зашифрованные CRUD/search сервисы есть; полный runtime сценарий на телефоне не проверен |
| Voice / avatar | PARTIAL / NEEDS_DEVICE_TEST | Есть voice/UI/3D компоненты; качество, разрешения и производительность на RMX5566 не проверены |
| Adaptive Learning | PARTIAL | Периодический поиск по темам владельца через Yandex Search; это не переобучение весов и не широкое автономное обучение |
| Future Updates | PARTIAL | Зашифрованный inbox идей и ручной журнал этапов; экран сам не собирает и не устанавливает обновления |
| Smart Receptionist | BLOCKED | Есть только policy object; реальное call screening/answering и двусторонний разговор не реализованы и зависят от Android/OEM telephony roles |
| Contact Intelligence | PARTIAL | Локальный PhoneLookup с READ_CONTACTS; более широкая аналитика истории звонков не реализована |
| Life Manager | PARTIAL | Локальный task CRUD; календарь, полноценные напоминания и внешние автоматизации не подключены |
| Yandex | PARTIAL | Официальные AI Studio/Search adapters; аккаунт, ключи и оплачиваемые запросы владельца не проверены |
| VK | PARTIAL | Только read-only `users.get` / `wall.get`; аккаунт и токен не проверены |
| Google services | NOT_IMPLEMENTED | Требование сохранено; Calendar/Maps/Drive/Search service connectors ещё отсутствуют. Gemini model adapter — отдельная функция |
| Game Intelligence / Minecraft | PARTIAL | Есть session monitor и adapter contract; реального Minecraft adapter/world control нет |
| Wearables / earbuds / vehicle | PARTIAL | Экран показывает уже сопряжённые Bluetooth-устройства и открывает системные настройки; device-specific control APIs не реализованы |
| One-action setup | PARTIAL | Профиль, encrypted checkpoint, WorkManager и продолжение запуска есть; это не полная активация всех модулей и не отменяет системных согласий |
| Self-development | PARTIAL / BLOCKED | Patch policy, bounded repair loop и CI есть; live dispatch из APK заблокирован до появления workflow на default branch и настройки владельцем repo token |
| AOSP / ROM / OTA / Recovery | BLOCKED | Есть source contracts, Soong/Binder scaffolding и fail-closed OTA gate; нет полного AOSP build, RMX5566 device tree, matching kernel/vendor/VINTF/HAL, подписанного ROM, boot/rollback/OTA испытаний |

## Оставшиеся блокеры

1. `.github/workflows/mana-engineering-pipeline.yml` присутствует только в feature branch; запрос файла на default branch `main` вернул 404. GitHub не разрешит app-originated `workflow_dispatch`, пока workflow не появится в default branch. Этот аудит не меняет `main` и не объединяет PR.
2. Установленный APK ещё не получил repository-scoped GitHub token владельца. Токен нельзя передавать в чат или коммитить.
3. Нет физического доказательства установки APK и работы локальной Gemma на RMX5566RU/RE645F.
4. Нет квалифицированного AOSP build host и полного device-specific source set для прошивки/OTA.
5. Внешние агенты OpenHands/Codex/Copilot/Claude Code не подключены к репозиторию как независимые исполнители; pinned Open Jarvis и Nyral — исследовательские submodules, не запущенная команда агентов.

## Следующий инженерный шаг

Дождаться завершения CI для `fc6795a20847da9dc48e4b176f767e508bbdb0d2`; исправлять только конкретные ошибки, найденные в этих runs. После зелёной Android сборки получить проверяемый APK artifact, затем отдельно проверить установку и локальный inference на физическом RMX5566. Workflow dispatch остаётся заблокирован до owner-approved попадания workflow в `main`.
