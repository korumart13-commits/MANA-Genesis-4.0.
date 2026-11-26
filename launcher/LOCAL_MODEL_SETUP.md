# MANA Genesis 4.0 — локальная модель

## Что подключено в Android-прототипе

- LiteRT-LM Android 0.16.1.
- Локальная модель имеет приоритет; GPU backend пробуется первым, затем runtime пытается загрузить CPU backend.
- Модель хранится отдельно от APK в приватном каталоге приложения.
- Импорт ограничен 16 ГБ, проверяет свободное место, считает SHA-256, пишет во временный файл и атомарно заменяет установленную модель.
- Облачный резерв OpenAI / Anthropic / Gemini / xAI выключен по умолчанию. Ключи хранятся в AES-GCM с ключом Android Keystore. Каждый облачный запрос требует отдельного подтверждения; контекст локальной памяти не передаётся в облако.

Официальный Android API: https://developers.google.com/edge/litert-lm/android

## Кандидат для RMX5566RU

Gemma 4 E2B IT в формате LiteRT-LM:
- Репозиторий: https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm
- Файл: gemma-4-E2B-it.litertlm
- Размер: около 2.59 GB на диске.
- Опубликованный SHA-256: 181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c.
- Репозиторий указывает Apache 2.0 для упаковки; исходные веса Gemma также подпадают под Gemma Terms of Use и Prohibited Use Policy.

Это кандидат, а не подтверждённая производительность на realme RMX5566RU. Публичные измерения карточки модели получены на других устройствах и не переносятся на SM6225. Для телефона нужно измерить загрузку, RAM, скорость генерации, нагрев и расход батареи. Не выбирай варианты Google Tensor / Tensor G5/G6 или специализированные NPU для другого устройства: в RMX5566RU Qualcomm SM6225.

## Более лёгкий резервный вариант

Для первого запуска и проверки совместимости можно использовать LiteRT Community Gemma 3 1B IT, если доступный .litertlm артефакт совместим с текущей версией LiteRT-LM. Он меньше, но слабее. Сравнение моделей и скорость на телефоне ещё предстоит проверить.

## Установка модели на телефоне

1. Открой официальный репозиторий модели на Hugging Face и проверь лицензионные условия.
2. Скачай именно файл .litertlm. Для Gemma 4 E2B выбери обычный gemma-4-E2B-it.litertlm, а не файл для Google Tensor или другого специализированного устройства.
3. В MANA открой аватар → «Выбрать локальную модель» → выбери скачанный файл.
4. Дождись завершения импорта и инициализации. Статус «модель готова» появляется только после успешной загрузки runtime.

Импортируемый файл получает локальную SHA-256 sidecar-сумму, но приложение ещё не сверяет её автоматически с доверенным каталогом издателя. Перед установкой критически важной модели проверяй SHA-256 по официальной карточке вручную.

## Ограничения

- Модель не вшита в APK и не скачивается скрытно.
- Устройство не прошло физический benchmark с Gemma 4 E2B; если памяти или ускорителя не хватит, выбери меньшую модель.
- Gradle compile проверяет сборку исходников, но не гарантирует, что конкретный файл модели загрузится на RMX5566RU.
- Этот Android-прототип ещё не является собранной MANA OS прошивкой.

## Full standalone APK model asset (MANA 4.0.4 build line)
The full APK workflow now downloads the official LiteRT Community Gemma 4 E4B IT model from the pinned upstream revision `2eee7ac325f20eb8c9ac1d0e972f7c84663062da`, checks the exact size `3,659,530,240` bytes and SHA-256 `0b2a8980ce155fd97673d8e820b4d29d9c7d99b8fa6806f425d969b145bd52e0`, then packages it uncompressed inside the APK. The APK itself is therefore expected to exceed 3.6 GB when this build step succeeds.

The model is not copied or loaded silently. The owner must open MANA's avatar panel and choose “Встроенная Gemma 4 E4B”, accept the model terms, and wait for the integrity-checked import. The app keeps E4B out of automatic boot loading. The phone needs roughly another 4.1 GB of free app-specific storage to import the model from the installed APK.

**Important target-device warning:** The E4B model's published CPU memory peak is about 3,283 MB on a Samsung S26 Ultra, not on the RMX5566RU. The realme has 8 GB physical RAM and a different Qualcomm chipset/GPU. The E4B model may be slow, may fail to initialize, or may cause memory pressure on this phone. It is included to meet the full offline-package requirement, not because compatibility has been proven. The smaller E2B model remains the safer first benchmark candidate.
