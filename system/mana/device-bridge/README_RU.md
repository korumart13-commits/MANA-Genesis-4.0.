# MANA Device Bridge — системный контракт RMX5566RU

Статус: **АРХИТЕКТУРА / КОНТРАКТ, НЕ УСТАНОВЛЕННАЯ СЛУЖБА**. Платформа: realme RMX5566RU / RE645F, Android 16, SM6225/bengal.

## Назначение

Bridge — отдельный системный посредник между приложением com.mana.genesis и Android Framework. Он не является частью AI runtime и не даёт приложению произвольный root/shell. Его задача — предоставить MANA стабильный, версионируемый API системных возможностей, сохраняя Android permission model.

Поток: обнаружение версии и возможностей; проверка Binder UID, package ownership, signing certificate, Android user/profile и разрешения; проверка согласия владельца; выполнение конкретной allowlisted операции через Android Framework или существующий HAL; структурированный ответ и audit event. Отзыв разрешения немедленно прекращает соответствующую возможность.

## Группы возможностей

| Группа | Разрешённый путь | Ограничение |
|---|---|---|
| Device info | Build/API/ABI/capability descriptors | Не отдавать секреты, serial/IMEI/токены |
| Apps | PackageManager, launch intents | Только видимые/разрешённые пакеты |
| Display | Settings APIs / разрешённый системный адаптер | Проверка диапазонов и grant |
| Audio/media | AudioManager, MediaSession | Не перехватывать чужой аудиопоток |
| Notifications | NotificationListenerService | Только после явного включения пользователем |
| Files | SAF / MediaStore URI grants | Никакого обхода scoped storage |
| Connectivity | ConnectivityManager, Bluetooth/Wi-Fi public APIs | Никаких скрытых подключений |
| Sensors/location | SensorManager / Location APIs | Только после runtime permission |
| Power / DND | Role, public API, QS tile или системный контроллер | Не менять защищённые политики скрытно |
| System panels | Settings intents / разрешённые system APIs | Никакого произвольного выполнения команд |

Каждая capability имеет id, version, available, requiredGrant, ownerConsent и reasonIfUnavailable. Неизвестная capability запрещена.

## Аутентификация Binder

На каждом вызове обязательны: Binder.getCallingUid(); соответствие UID установленному com.mana.genesis; проверка package ownership через PackageManager; SHA-256 сертификата клиента из конфигурации релиза; Android user/profile; signature permission для системного API; отдельные runtime/special grants для каждой capability; актуальный owner consent и AppOps; rate limit, лимиты входных данных и timeout. Любая ошибка означает отказ.

Не доверять UID, package name или capability ID, переданным клиентом без повторной проверки. Не выполнять модельный текст как shell, SQL, JavaScript или системную команду.

## SELinux и системное размещение

SELinux остаётся Enforcing. Не добавлять setenforce 0, permissive domain или широкие allow *. До определения точного Android service context, process domain, service_contexts entry и init service для целевой сборки policy не включается в продукт. Правила должны быть минимальными: только регистрация конкретного Binder service и доступ к конкретным framework/HAL endpoints, если это действительно требуется. Сбой Bridge не должен останавливать загрузку Android.

## AIDL и совместимость

Контракт должен иметь версию API, capability discovery, structured status/error, request ID и timeout. Новая версия сохраняет обратную совместимость либо явно сообщает минимальную версию клиента. Неизвестные методы/поля отклоняются.

## Вход в систему и загрузка

Bridge не участвует в раннем bootloader/AVB этапе. Он запускается только после старта Android userspace и готовности PackageManager/permission services. Не блокирует Keyguard, PIN/пароль, биометрию, emergency calling или разблокировку. Если служба не стартовала, пользователь входит в систему обычным способом; MANA показывает «Системный мост недоступен» и повторяет обнаружение позже.

## Драйверы и HAL

**Новый драйвер сейчас не добавляется.** Для RMX5566RU сохраняется существующий стек Qualcomm/Realme. Для оборудования Bridge использует существующий Framework API/HAL.

Новый kernel driver разрешается рассматривать только при наличии всех доказательств: конкретная отсутствующая аппаратная функция и воспроизводимый тест; исходный код именно для kernel lineage 5.15.197; совпадающие DTS/DTSI/DTBO и board configuration; согласованный vendor/ODM HAL и VINTF; firmware blobs и лицензии; SELinux policy, ABI и тесты suspend/resume, power, thermal, reboot; безопасный rollback и подтверждённое восстановление. Кандидатный kernel 5.15.180 не считать совместимым с работающим на устройстве 5.15.197 без полного доказательства.

## Проверки перед включением

- [ ] AIDL service implementation и Soong build существуют.
- [ ] Signature/caller validation tests проходят.
- [ ] SELinux enforcing; никаких permissive domains.
- [ ] CTS/VTS/VINTF и permission allowlist проверки проходят.
- [ ] На RMX5566RU проверены загрузка, разблокировка, звонки, сеть, камера, звук, Wi-Fi, Bluetooth, GNSS, датчики, сон/пробуждение.
- [ ] Отзыв каждого разрешения прекращает доступ.
- [ ] При падении Bridge телефон продолжает нормально загружаться и работать.

До выполнения этих пунктов Bridge не включается в PRODUCT_PACKAGES.
