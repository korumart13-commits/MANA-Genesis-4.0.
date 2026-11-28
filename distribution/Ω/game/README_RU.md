# Игровые компоненты MANA XZ — Ω

В полном комплекте находятся:
- `mana-xz-world/` — исходный Godot-проект «Живая квартира» с общей моделью МАНЫ, сменой настроения и одежды.
- `minecraft-bedrock/` — исходники Bedrock companion/bridge pack.
- `MANA_Companion_BP.mcpack` — импортируемый Minecraft Bedrock Behavior Pack, собираемый CI.

Игровые компоненты используют одну личность MANA и не создают отдельный ИИ. Godot world — самостоятельный исходный проект, не встроенный экран Android APK. Minecraft pack работает внутри мира Bedrock и не подключён к Android MANA Brain; для некоторых Script API событий может требоваться включение экспериментальных/Beta API в мире. Совместимость с конкретной версией Minecraft проверяется отдельно.
