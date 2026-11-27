# MANA Genesis 4.0 — выбор готовой Android-основы и план адаптации OAssist

Дата проверки: 2026-11-27.

## Решение

В качестве готовой внешней основы для агентного Android-слоя выбран **OAssist**:

- Репозиторий: https://github.com/outsidepro-arts/OAssist
- Зафиксированный upstream commit: `61676533c88b6de11129ca50203588bc0b1c4c37`
- Ветка upstream: `master`
- Лицензия: MIT (обязательно сохранить исходное уведомление и LICENSE).
- Язык и UI: Kotlin, Jetpack Compose, Material 3.
- Текущий minSdk upstream: 31 (Android 12+); это НЕ соответствует заявленному MANA baseline Android 10/API 29. Нельзя просто понизить minSdk без аудита API-вызовов и зависимостей.

Выбор сделан не потому, что OAssist уже является MANA OS, а потому что его агентный слой ближе к требуемому функционалу, чем написание всего заново.

## Что уже есть в OAssist и можно адаптировать

- Системный assistant gesture / VoiceInteractionService.
- Три режима: Assistant, Chat, Share.
- Мультипровайдерная архитектура: OpenAI-compatible, Anthropic, Gemini, OpenCode.
- Более 50 системных и UI-инструментов.
- Android Intents, Accessibility UI automation, optional Shizuku/Root integration.
- MCP servers, skills, local conversation history and persistent memory.
- Speech input/output, configurable TTS, Russian language support.
- Tool permission/approval gate, read/write categories, one-time/session/persistent decisions.
- Encrypted credential storage backed by Android Keystore.
- Diagnostics, update pipeline, CI tests and accessibility requirements.

Наличие функций в upstream не означает, что они автоматически прошли тесты на всех устройствах или что они имеют системные привилегии без отдельной настройки.

## Что НЕ входит в OAssist и должно остаться из MANA

- MANA Brain и уже существующие Kotlin/JVM core.
- Текущий MANA Launcher / home UI и One-tap setup.
- 3D-аватар Genesis и его модели/анимации.
- MANA Diary, Life Manager, Future Updates, Contact Intelligence, Smart Receptionist.
- Adaptive learning / research engine и локальная база знаний.
- Minecraft/Game Intelligence и интеграции Yandex/VK.
- Device profiler, MANA OS/AOSP system services, Binder/AIDL и RMX5566RU firmware research.
- Существующие encrypted stores, CI/self-development loop, recovery/update policies.

Не создавать второй Brain, вторую память или второй параллельный лаунчер. OAssist следует использовать как донор агентного цикла и Android tool adapters, а не как новую независимую личность/продукт.

## Брендинг и интерфейс MANA

После импорта в отдельный модуль/ветку:

1. Видимое имя приложения, системного помощника, уведомлений, настроек и экранов заменить на **MANA** / **MANA Genesis 4.0**.
2. Убрать OAssist branding из пользовательского UI, сохранив авторство и MIT license в разделе Open Source Licenses / About.
3. Основная палитра:
   - background: `#071426`
   - surface: `#102B50`
   - primary: `#1765A8`
   - accent: `#54C8F5`
   - text: `#EAF4FF`
   - secondary: `#91A9C5`
4. Единая визуальная идентичность: тёмно-синяя MANA оболочка, Android-подобная навигация, единый Genesis avatar; без конкурирующих брендов и разных «персон».
5. Привязать assistant/chat/settings/tools к единому MANA Brain API.
6. Не копировать в MANA имя, логотип, аватар или закрытые ресурсы стороннего проекта.

## Архитектурная интеграция

Предпочтительный порядок:

1. Сохранить upstream в отдельном каталоге `third_party/oassist` или отдельном Gradle included build с точным commit pin и NOTICE.
2. Сначала собрать upstream без изменений и зафиксировать исходные тесты/сборку.
3. Создать адаптер `OAssistAgentAdapter` к существующему `ManaBrain`; не допускать второго независимого memory store.
4. Перенести provider adapters и tool registry за MANA interfaces; сопоставить инструменты с существующей capability/policy моделью MANA.
5. Перенести VoiceInteractionService/assistant gesture, chat/share flows и настройки.
6. Перепривязать UI к MANA theme и единому avatar surface.
7. Подключить MANA-specific modules через tool/plugin contracts.
8. Проверить Android 12+ сначала; затем провести отдельный API 29–30 compatibility audit, прежде чем заявлять Android 10 support.
9. Проверить сборку, unit/instrumentation tests, accessibility/TalkBack, разрешения, фоновые ограничения и физические устройства.
10. Только после проверки выпускать APK; отдельная AOSP/system integration остаётся отдельным этапом.

## Ограничения и честная оценка

- OAssist — Android assistant/agent app, не готовая ОС и не полноценный home-screen launcher.
- OAssist minSdk 31; MANA target baseline minSdk 29. Совместимость не подтверждена.
- OAssist имеет optional Shizuku/Root integrations, но не обходит Android protections и не получает привилегии автоматически.
- Интеграция upstream пока НЕ выполнена: этот документ фиксирует выбранную основу и точный upstream revision. Нельзя выдавать выбор/план за импорт исходников или собранный APK.
- До переноса требуется полная проверка upstream кода, зависимостей, лицензий, manifest permissions, update/signing pipeline и security policies.
- Системный MANA launcher/OS и RMX5566RU firmware по-прежнему требуют отдельной AOSP/device-specific работы.

## Критерии готовности адаптации

- [ ] Upstream source imported at the pinned commit, LICENSE/NOTICE preserved.
- [ ] Clean baseline build and tests pass before modifications.
- [ ] User-facing name, package/application ID and app icon are MANA-branded.
- [ ] OAssist UI theme replaced by MANA midnight-blue theme.
- [ ] Existing MANA Brain is the only orchestrator and canonical memory owner.
- [ ] MANA avatar and launcher are integrated without duplicate shells.
- [ ] Provider/tool/permission adapters mapped to MANA capabilities.
- [ ] API 31+ build and tests pass.
- [ ] API 29–30 support either proven by tests or explicitly excluded from the release.
- [ ] APK signed with MANA release key; SHA-256 and provenance published.
- [ ] Device testing and rollback evidence published.

