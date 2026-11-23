# Ключ Google Maps SDK

Экран карты уже подключает Maps SDK for Android (`play-services-maps`, `maps-compose`).
Ключ попадает в манифест как `com.google.android.geo.API_KEY` со значением `${MAPS_API_KEY}`.
Настоящий ключ в git не кладётся: репозиторий публичный.

Сборку делает Secrets Gradle Plugin
(`com.google.android.libraries.mapsplatform.secrets-gradle-plugin`). Он читает
`MAPS_API_KEY` из `truckerload/local.properties`. Если ключа там нет, значение
пустое (`truckerload/local.defaults.properties`). Переменная окружения
`MAPS_API_KEY` перекрывает оба файла. Так CI подставляет секрет, не записывая
его в репозиторий.

`local.properties` уже в `.gitignore` (и в корне репозитория, и в `truckerload/`).
Его не коммитить.

## Локальная сборка

В `truckerload/local.properties` добавьте строку:

```
MAPS_API_KEY=ваш_ключ
```

Файл может уже содержать `sdk.dir`. Допишите ключ в него же.
В Google Cloud Console включите только Maps SDK for Android и ограничьте ключ
пакетом `com.truckorig` и SHA-1 сертификата, которым подписывается приложение.

Если строки `MAPS_API_KEY` нет, проект всё равно собирается: в манифест попадает
пустая строка. Без ключа тайлы карты не загружаются.

Раньше сборка читала `GOOGLE_MAPS_API_KEY`. Это имя больше не используется:
переименуйте строку в `local.properties` в `MAPS_API_KEY`.

## CI

Владелец репозитория добавляет секрет GitHub Actions с именем `MAPS_API_KEY`
(Settings → Secrets and variables → Actions → New repository secret).

Job `android-release` в `.github/workflows/truckerload-ci.yml` передаёт его в
окружение сборки:

```yaml
env:
  MAPS_API_KEY: ${{ secrets.MAPS_API_KEY }}
```

`assembleRelease` и `bundleRelease` читают одну и ту же переменную. Пока секрета
нет, CI собирается с пустым ключом, и проверка не падает.

Не вставляйте настоящий ключ в `local.defaults.properties`, workflow-файлы,
исходники, коммиты и pull request.
