# NoiseFlow

Генератор шума для сна под Android. Синтезирует звук в реальном времени вместо проигрывания зацикленных записей: бесконечный поток без швов, работа полностью офлайн, минимальное энергопотребление.

**Главная отличительная черта — слайдер Tone: непрерывный наклон спектра от −12 до +6 dB/окт вместо семи кнопок с названиями цветов.** Точность подтверждена измерением: ошибка не больше 0.05 dB/окт по всему диапазону (`docs/08-verification.md`).

## Статус

Этапы 0 и 1 закрыты, ядро DSP проверено 32 тестами. Код этапов 2–6 написан целиком, но **не скомпилирован**: в среде разработки нет Android SDK. Подробности — в [08-verification.md](docs/08-verification.md).

## Сборка

Ядро DSP — чистый Kotlin/JVM, собирается и тестируется без Android SDK:

```
./gradlew :core:audio:test
```

Всё остальное требует Android SDK:

```
./gradlew assemblePlayDebug        # Google Play
./gradlew assembleRustoreDebug     # RuStore, без Google Play Services
./gradlew ktlintCheck detekt
```

## Структура

```
core/audio          DSP: генераторы, фильтр наклона, микшер, лимитер  — чистый Kotlin/JVM
core/playback       AudioTrack, MediaSession, foreground-сервис, таймер сна
core/data           модели, JSON DataStore, пресеты, ссылки для обмена
core/designsystem   скины, визуализаторы, Tone-контрол
core/i18n           все пользовательские строки и десять локалей
feature/*           player, mixer, presets, settings, paywall
billing/api         единый интерфейс покупок
billing/play        Google Play Billing
billing/rustore     заглушка, ждёт SDK (см. docs/07)
app                 навигация, DI, флейворы play/rustore, виджет, плитка
```

## Документация

| Документ | О чём |
|---|---|
| [01 — Концепция](docs/01-concept.md) | позиционирование, аудитория, ядро, монетизация, метрики |
| [02 — Аналоги](docs/02-competitors.md) | BetterSleep, Endel, Noisli, TMSoft и другие |
| [03 — План по этапам](docs/03-roadmap.md) | этапы 0–7, критерии готовности, риски, статус |
| [04 — Архитектура](docs/04-architecture.md) | стек, модули, аудио-ядро, два стора, CI |
| [05 — Дизайн и скины](docs/05-design.md) | принципы, экраны, система скинов |
| [06 — Многоязычность](docs/06-localization.md) | волны языков, plurals, RTL, процесс |
| [07 — Журнал решений](docs/07-open-questions.md) | все принятые решения с обоснованием |
| [08 — Верификация](docs/08-verification.md) | что измерено, что нет |

## Языки

en, ru, uk, de, es, fr, it, pt-BR, pl, tr — полный комплект строк в каждом, с правильными формами множественного числа для славянских языков. Переключение языка отдельно от системного (per-app language).
