# Будущее превращение MANA XZ в MANA OS — Ω

Цель полноценной прошивки сохранена. APK и папка Ω не являются прошивкой и не прошивают телефон.

В комплекте сохранены AOSP/Soong/AIDL-направление, MANA Brain Binder service, исходники MANA Launcher/Settings/Control Center, профиль RMX5566RU и план интеграции Android 16. Целевая идентичность устройства — **MANA**: название MANA в пользовательских Settings/About и собственный MANA boot branding вместо Android остаются требованиями будущего ROM.

Для реального ROM ещё необходимы точные OEM kernel/device tree/vendor blobs/HAL/VINTF, разметка разделов, AVB signing, SELinux, recovery и проверенный способ возврата на штатную прошивку. Полный AOSP build и flashable image пока отсутствуют.

В этом комплекте раздел `firmware/` — исходники и документация будущей интеграции, а не установочный ROM. Не копировать его содержимое в системный раздел и не пытаться прошивать им устройство. Подробный критерий выпуска: `docs/FULL_ROM_RELEASE_TARGET.md`.
