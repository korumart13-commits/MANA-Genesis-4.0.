# MANA XZ — Ω (OMEGA FINAL)

**Ω** — конечная целевая комплектация MANA XZ / MANA Genesis 4.0. Ветка разработки: `Final`.

## Состав комплекта

- `app/` — APK приложения.
- `engine/` — исходники MANA Brain, OS Core и экспериментальной ветки llama.cpp; LiteRT-LM runtime компилируется внутрь APK.
- `models/Gemma/` — локальные веса Gemma 4 E2B IT в формате LiteRT-LM; отдельный файл внутри полного Ω ZIP.
- `voice/ru/` — ресурсы русского офлайн-голоса Piper Irina.
- `avatar/characters/` — glTF-модели одного постоянного 3D-аватара МАНЫ.
- `design/` — палитра и UX-токены Android-first.
- `game/` — Godot-проект и Minecraft Bedrock companion pack/source.
- `firmware/` — AOSP local manifest, MANA system modules и профиль устройства для будущей ROM-интеграции.
- `config/` — пакетный манифест, настройки и реестр всех модулей со статусами.
- `lockscreen/`, `memory/`, `knowledge/`, `updates/`, `logs/` — описания runtime-каталогов, создаваемых приложением.

## Сборка полного комплекта

GitHub Actions workflow `MANA Launcher — Gradle source verification` собирает `MANA-Omega-FULL-INSTALLER.zip`. В успешный архив попадают APK, локальная модель, голос, аватар, движок, игровые исходники, Minecraft `.mcpack`, AOSP-исходники и документация. Архив содержит SHA-256 manifest и build report.

## Установка на Android

1. Распаковать полный ZIP на телефоне.
2. Установить `installer/INSTALL_MANA_XZ.apk`.
3. Запустить MANA и пройти системные запросы разрешений.
4. В мастере локальной модели выбрать `models/Gemma/gemma-4-E2B-it.litertlm`.
5. Дождаться проверки SHA-256 и импорта модели в приватное хранилище приложения.

LiteRT-LM runtime находится внутри APK; файл Gemma — отдельные веса в той же папке. После импорта базовый диалог доступен офлайн. Облачные провайдеры и внешние сервисы опциональны и не требуют вшитых ключей.

## Честные ограничения текущей реализации

- APK — устанавливаемое Android-приложение/debug-прототип, не прошивка.
- Полноценный AOSP ROM не собран и не готов к прошивке. Папка `firmware/` содержит исходники/план, но не kernel/device tree/vendor blobs/flashable image.
- Автоматическая обработка звонков секретарём, Android–Minecraft bridge, Google services OAuth, wearable/car adapters, Shogun/Hydra/Microworld и Adaptive System Merge имеют статусы в `config/modules.json`; не считать их готовыми без указанных тестов.
- Обои создаются для экрана блокировки; сейчас это автоматически генерируемые статические изображения, не подтверждённая анимированная live wallpaper service.
- Фоновые задачи работают в рамках ограничений Android; непрерывные 24/7 гарантии нет.
- Порог MANA для запуска локальной модели — ориентировочно 3 548 MB доступной физической RAM по внутренней консервативной оценке. Это не официальное требование Gemma и не заменяет тест на RMX5566. Динамическое расширение RAM не считается физической памятью.

Не копировать `firmware/` в системный раздел и не пытаться прошивать телефон этим ZIP.
