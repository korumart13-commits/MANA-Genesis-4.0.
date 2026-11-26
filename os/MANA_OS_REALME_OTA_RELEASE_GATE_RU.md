# MANA OS — выпуск обновления Realme OTA

Цель: один ZIP-пакет обновления для RMX5566RU, который можно выбрать в штатном приложении обновления Realme только если формат, подпись и политика конкретной версии Realme Update поддерживают такой пакет.

Статус: **RELEASE BLOCKED — установочный OTA ZIP не собран и не подписан**.

## Важное различие ZIP-файлов

Обычный ZIP, архив исходников, bootanimation ZIP и Android OTA ZIP — разные форматы. ZIP с файлами прошивки сам по себе не становится системным обновлением. Переименование архива в .ozip или .zip не добавляет подпись и совместимость.

Штатный Realme/Oplus updater проверяет формат пакета, подпись/сертификат, OTA metadata, базовую версию, регион/SKU, устройство и сценарий обновления. Публичный Android test-key или личный ключ разработчика не заменяет OEM update key на заблокированном загрузчике.

## Необходимые входные данные

- Полный исходный OTA именно RMX5566RU и текущей ветки Android 16, с проверенной контрольной суммой.
- Точный build fingerprint, регион/SKU, incremental version и partition metadata.
- Подтверждённый способ сборки incremental/full OTA для этого Oplus update client.
- Официально поддерживаемая подпись/ключ или согласованный OEM механизм установки.
- Точная схема A/B, dynamic partitions, payload metadata и rollback index.
- Проверенный stock restore package и инструкции аварийного восстановления.
- Полный воспроизводимый device/product build с соответствующими vendor blobs, kernel, DTBO, VINTF и SELinux.

## Состав будущего MANA update

Только прошедшие тесты компоненты: MANA Launcher, Settings hub и Control Center; точные ресурсы/overlays SystemUI, Keyguard, Recents, volume/brightness panels и Files, если целевая OEM сборка разрешает их overlay; MANA Android userspace boot animation; реально реализованный, подписанный и проверенный Device Bridge; минимальные permission allowlists и SELinux policy; OTA metadata, payload и корректные подписи.

Не включать неподтверждённый kernel, boot, vendor, modem, dtbo, vbmeta, recovery или firmware partitions. Не удалять Realme/Oplus приложения и пользовательские данные. Не менять bootloader splash на locked/AVB-verified устройстве.

## Проверки выпуска

1. Сверить исходный OTA fingerprint и target fingerprint.
2. Проверить все файлы и partition targets; запретить неизвестные partitions.
3. Сравнить kernel, DTBO, vendor/odm, VINTF и firmware с исходной сборкой.
4. Выполнить Soong build и unit/instrumentation tests.
5. Выполнить CTS/VTS, SELinux neverallow, privapp-permissions и overlay validation.
6. Проверить OTA payload signature, metadata signature, rollback index, A/B slot flow и postinstall.
7. Проверить recovery/restore и повторный запуск после неудачного обновления.
8. На физическом RMX5566RU проверить boot, unlock, radio, Wi-Fi, BT, camera, audio, sensors, charging, suspend/resume, data retention.
9. Только после всех gate выставить ROM_READY=true и публиковать ZIP как установочный.

## Текущие блокеры

- Точный полный RMX5566RU OTA пакет не закреплён как build input.
- Доступ к OEM private OTA signing key отсутствует и не предполагается.
- Полное дерево устройства и совпадающий kernel 5.15.197 source/DT/DTBO не подтверждены.
- Нет законченной SystemUI/Keyguard/Recents overlay integration.
- Device Bridge описан контрактом, но не подтверждён как собранная и работающая на телефоне системная служба.
- Нет device CTS/VTS/OTA/restore результатов.

**Итог:** нельзя честно выдать сейчас ZIP и назвать его обновлением Realme. Без точной базы и поддерживаемой OEM подписи это будет нештатный архив, который штатный updater может отвергнуть или который может вывести устройство из строя. Этот release gate запрещает публиковать такой архив как прошивку.
