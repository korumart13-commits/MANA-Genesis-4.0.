# Задача Claude: аудит и поэтапная реализация MANA ASI

Основная спецификация: `docs/MANA_ASI_PRIMARY_ARCHITECTURE_RU.md`.

## Цель

Проверить существующий репозиторий и подготовить проверяемый план реализации MANA ASI как основной архитектуры MANA Genesis 4.0. Сохранить существующие функции и историю проекта. Не заменять продукт простым лаунчером.

## Обязательный аудит

1. Составить таблицу **implemented / partial / docs-only / blocked** для каждого слоя и ранее согласованного модуля.
2. Проверить реальный Android minSdk/targetSdk и совместимость с Android 8+; отдельно оценить Meizu, Samsung, realme, Redmi/Xiaomi, Huawei, Honor и Fly. Не заявлять OEM-поддержку без тестов или точных доказательств.
3. Разделить возможности обычного APK, Accessibility, role APIs, Device Owner, privileged app и системной прошивки.
4. Проверить существующие MANA Brain, System Bridge, device audit, bootstrapper, CI и SIOS submodule. SIOS — исследуемый upstream, не считать его интегрированным или совместимым без проверки.
5. Спроектировать MANA Deadman так, чтобы он находился вне изменяемого MANA Brain процесса; описать доверенную границу, ограничения каждой платформы и тесты отказа.
6. Проверить one-action installer UX и перечислить неизбежные подтверждения/предусловия Android. Не реализовывать обход permission prompts, Verified Boot или загрузчика.
7. Проверить RMX5566RU firmware blockers: exact device tree, kernel/vendor sources, VINTF/HAL, partition/AVB/signing и verified stock restore.

## Реализация

- Предлагать небольшие PR с реальным кодом, тестами и CI evidence.
- Начать с device discovery/capability report и compatibility matrix Android 8+.
- Сохранить существующие модули Smart Receptionist, Contact Intelligence, Life Manager, Diary, Game Intelligence, Adaptive Intelligence, Future Updates, Google/Yandex/VK и multi-provider architecture.
- Секреты и API keys не коммитить.
- Работать в отдельной ветке `claude/<task>`, не пушить напрямую в main.
- Не копировать код из SIOS до проверки лицензии AGPL-3.0-or-later, CLA и совместимости.
- Не создавать malware, worm, stealth persistence, credential theft, exploit chain, self-propagation или механизм обхода системной защиты.
- Не заявлять, что функция готова, пока код не собран и тесты не пройдены.

## Результат

Создать отчёт `ai-collaboration/inbox/claude-mana-asi-audit.md` с:
- статусом каждого компонента;
- матрицей OEM/API возможностей и неизвестных;
- архитектурой Deadman;
- точным one-action installation flow и необходимыми действиями владельца;
- блокерами RMX5566RU;
- приоритетным списком небольших PR;
- командами сборки и фактическими результатами тестов.

В отчёте отдельно отметить, что Android APK не может универсально получить root/system privileges или прошить все устройства без платформенных предпосылок.
