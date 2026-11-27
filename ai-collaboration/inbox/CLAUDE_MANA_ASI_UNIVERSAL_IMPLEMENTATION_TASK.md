# Задание: реализовать универсальную MANA ASI и one-action app setup

Прочитать:
- docs/MANA_ASI_UNIVERSAL_ARCHITECTURE_RU.md
- device/RMX5566RU_TARGET_PROFILE.md
- docs/PROJECT_EXECUTION_STATUS_2026-11-27.md
- CLAUDE.md и docs/AI_COLLABORATION_PROTOCOL_RU.md

## Цель
Развивать MANA ASI как универсальную Android 8+ архитектуру. RMX5566RU/RE645F — первый тестовый аппарат, не единственная целевая модель.

## Работы
1. Провести аудит текущего APK, minSdk, manifest permissions, first-boot setup, device profiler, WorkManager и подписывания артефактов.
2. Спроектировать/реализовать capability registry и OEM-independent feature adapters. Использовать публичные Android API; учитывать API 26+.
3. Создать тестируемый first-run orchestrator: inventory → capability resolution → owner consent → component integrity verification → setup report.
4. Разделить установку обычного APK и привилегированную системную интеграцию. Не симулировать системные права.
5. Добавить тесты на границы API, отсутствие разрешений, OEM-ограничения, offline first boot, прерывание/повтор запуска и проверку подписанных компонентов.
6. Подготовить device test matrix для Samsung, Xiaomi/Redmi, realme, Huawei, Honor, Meizu, Fly и generic AOSP; отдельно перечислить физические модели и прошивки, которые реально проверены.
7. Проверить, что Deadman находится вне полномочий Brain и fail-closed.
8. Сборку и тесты выполнять в CI; отчёт должен отделять PASS от NOT TESTED.

## Не делать
Не создавать вирус, exploit chain, privilege escalation, stealth persistence, self-propagation, credential theft или защитный bypass. Не обещать silent install, отключение Android confirmation dialogs, автоматическое получение root/Device Owner, отключение AVB или прошивку без штатного подтверждения.

## Требуемый результат
- Изменения в отдельной ветке и Pull Request; main не менять напрямую.
- Конкретные файлы/классы/тесты, CI run IDs, APK artifact и SHA-256.
- Явный список незакрытых блокеров.
- Не выдавать документацию за реализацию и APK за ROM.
