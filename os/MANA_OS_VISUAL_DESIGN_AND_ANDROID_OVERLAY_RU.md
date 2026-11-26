# MANA OS — визуальная система и Android overlay plan

Дата: 2026-11-26  
Устройство: realme RMX5566RU / RE645F  
База: сохранить установленную Android 16 / Realme platform, пока точная совместимость не докажет необходимость другой базы.

## 1. Основное архитектурное решение

**Не заменять Android и не переписывать ядро.** MANA OS строится как брендированная оболочка и набор системных overlay-ресурсов поверх совместимой заводской Android 16 платформы Realme. Использовать текущую OEM основу, если она соответствует RMX5566RU. AOSP framework меняется только точечно, когда overlay или публичный API недостаточен.

Цель — единый внешний вид MANA по всей системе и стабильная совместимость с заводскими драйверами, модемом, камерой, сенсорами, обновлениями и системными приложениями.

## 2. Направление дизайна

Собственная композиция MANA на основе визуальных принципов:
- старые Samsung TouchWiz / ранние One UI: ясные списки настроек, крупные зоны заголовков, компактные системные карточки, логичная иерархия;
- современные Samsung One UI: крупные удобные для пальца области управления, сглаженные панели, карточки и спокойная типографика;
- iPhone 13 / iOS: чистая визуальная иерархия, полупрозрачные/матовые слои там, где они улучшают читаемость, аккуратные скругления и лаконичные системные элементы.

Не копировать логотипы, точные макеты, иконки или proprietary assets. Создать оригинальный MANA визуальный язык.

### Цветовая палитра
- Background: `#071426`
- Surface: `#102B50`
- Primary: `#1765A8`
- Accent: `#54C8F5`
- Main text: `#EAF4FF`
- Secondary text: `#91A9C5`
- Optional light surfaces for daylight theme: cool neutral whites, with same navy/cyan accent hierarchy.

### Геометрия и motion
- единая шкала отступов 4/8/12/16/20/24 dp;
- скругления 12/16/20/24 dp по уровню компонента;
- понятные состояния pressed/focused/disabled;
- анимации короткие и ненавязчивые, без постоянной нагрузки на батарею;
- поддержка системного размера шрифта, дисплея, контраста и accessibility.

## 3. Области обязательного обновления дизайна

### A. Главный экран / Launcher
- MANA Home как HOME launcher.
- настраиваемая сетка 4xN/5xN, размер иконок, подписи, сетка, отступы;
- виджеты, dock, папки, жесты, поиск;
- пользовательские обои и темы;
- список всех установленных launchable apps с поиском, сортировкой и папками;
- не удалять и не скрывать существующие системные/OEM приложения; не очищать `PRODUCT_PACKAGES`, user data или app packages ради дизайна;
- поддержать переключение на штатный Realme launcher через Android HOME role, пока MANA не выбрана пользователем.

### B. App drawer (свайп вверх)
- полноэкранная сетка приложений, категории/папки, поиск;
- сортировка по имени и недавнему использованию (последнее — только если доступен разрешённый usage access);
- избранные приложения и настраиваемая плотность сетки;
- не скрывать/не удалять системные приложения; фильтровать только реально не-launchable системные компоненты из стандартного списка, не меняя их установку.

### C. Lock screen
- собственные часы/дата, погода только при наличии источника/разрешения, быстрые ярлыки, уведомления и media controls;
- настройка обоев, формата часов, плотности уведомлений;
- сохранить Android credential flow, PIN/пароль/биометрию, emergency/SOS, privacy indicators и защиту lockscreen;
- OEM/Keyguard интеграция через ресурсы/overlay, не делать отдельный фальшивый экран блокировки поверх системного.

### D. Верхняя панель / Quick Settings / уведомления
- полный redesign системной шторки, плиток, яркости, громкости/звуковых режимов, подключения, фонаря, поворота, энергосбережения и уведомлений;
- крупные удобные плитки, понятные состояния, MANA цвета и typography;
- notifications: группировка, компактный/расширенный вид, media player, privacy chip/indicators;
- использовать OEM SystemUI overlays и корректные platform APIs. Отдельное приложение Control Center не подменяет системную шторку и не должно изображать неработающие переключатели.

### E. Recents / экран недавних приложений
- переработать cards, скругления, превью, заголовки, split-screen/app-pair affordances и кнопку очистки;
- сохранить стандартную модель задач Android, multi-window и app continuity;
- не закрывать/не удалять приложения автоматически.

### F. Settings
- MANA Settings как единая навигация и внешний вид настроек;
- категории, поиск, крупные заголовки, аккуратные строки, индикаторы состояния;
- переоформить заводские Settings resources/overlays, не теряя существующие Realme/Android пункты, скрытые страницы диагностики, update, security, accounts и accessibility;
- сохранять OEM-функции и системные разделы. Не заменять реальные системные настройки пустыми заглушками.

### G. Files / File Manager
- переоформить доступное OEM Files/File Manager приложение или использовать совместимый open-source/system file manager после проверки лицензии и API;
- категории Recent / Images / Video / Audio / Documents / Downloads / Archives / Internal storage;
- карточки/список, сортировка, поиск, storage meter, multi-select, copy/move/share;
- не удалять, не перемещать и не переиндексировать пользовательские файлы автоматически;
- сохранить Storage Access Framework и scoped storage ограничения Android.

### H. Системные диалоги и дополнительные экраны
- volume panel, power menu, screenshot UI, share sheet, clipboard overlay, app permission dialogs, installer, battery panel, network dialogs, notification shade, app info, default apps, accessibility pages;
- заменить только внешний вид и ресурсы, сохраняя исходные действия, текстовые предупреждения, защиту и поведение;
- не маскировать системные запросы согласия под MANA-подтверждения.

## 4. Сохранение Android и приложений

- Оставить Android 16 и заводскую платформу Realme как базу.
- Не выполнять factory reset, очистку data, удаление OEM пакетов или удаление игр.
- Сохранить все существующие системные и пользовательские приложения и данные.
- В продуктовых makefiles не удалять OEM `PRODUCT_PACKAGES`; MANA добавляется как дополнительный слой.
- Не использовать `PRODUCT_PACKAGES -=`, `pm uninstall`, `disable-user` или массовую замену пакетов в дизайн-ветке.
- Перед любым OTA/flash — backup/restore, сохранение user data и проверка совместимости разделов.

## 5. Подготовка для отдельного приложения MANA

Отдельное приложение `com.mana.genesis` может позднее получить максимально глубокую поддерживаемую интеграцию:
- default assistant role / VoiceInteractionService при выполнении контрактов Android;
- app-specific Binder/AIDL APIs для строго ограниченных системных действий;
- системная предустановка и точечные привилегии только после проверки подписи, разрешений, partition и SELinux;
- отдельная панель полномочий, журнал действий и отзыв доступов.

Не предоставлять произвольному APK platform signature и не обещать полное снятие Android sandbox. Системные действия должны оставаться auditable и permission-gated.

## 6. План реализации в исходниках

1. **MANA resource foundation:** единые colors, themes, dimensions, typography, shapes, drawable assets и light/dark variants.
2. **Launcher:** HOME, dock, widgets, app drawer, folders, icon labels, gestures, settings; не удалять OEM apps.
3. **Settings:** переоформить MANA Settings hub и подготовить resource overlays для заводских Android Settings.
4. **SystemUI:** overlays и интеграция в точную OEM/AOSP SystemUI версию: status bar, QS, notifications, brightness, volume, media.
5. **Keyguard/Recents:** OEM/framework overlays для lock screen и overview/task switcher.
6. **Files:** определить установленный на OEM base file manager и применить безопасную тему/ресурсы; при необходимости отдельный MANA Files UI без удаления штатного.
7. **System dialogs:** постепенно привести к MANA design language.
8. **Build/test:** AOSP/Soong product build, overlay validation, screenshots, accessibility, battery, notifications, rotation, multi-window, update/rollback.
9. **Device validation:** только на RMX5566RU с подтверждёнными OEM blobs и восстановлением.

## 7. Текущий статус

- MANA Launcher/Settings modules и первые MANA color/style resources существуют.
- MANA Control Center пока отдельный settings hub, не системная Quick Settings замена.
- SystemUI, Keyguard, Recents, OEM Settings/File Manager overlays ещё не интегрированы.
- Точная RMX5566RU product/device tree и vendor inputs пока не завершены.
- Этот документ задаёт новое направление: **Android остаётся; MANA меняет визуальный слой и готовит системные интерфейсы**.
- Это план реализации и текущие исходники, не готовая прошивка.
