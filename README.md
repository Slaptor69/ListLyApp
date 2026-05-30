# Listly

Listly - Android-приложение для поиска фильмов, сериалов, аниме и игр, добавления медиапозиций в личный readlist и распределения их по пользовательским папкам.

## Ссылка на репозиторий

- Основной репозиторий: https://github.com/Slaptor69/ListLyApp
- Исходный upstream: https://github.com/MisterPotz/Elmslie-Demo
- Текущий базовый коммит для защиты в локальной копии: `23c4705`

## Что реализовано

- Авторизация и регистрация через backend API.
- Каталог медиапозиций с поиском и фильтрацией по типу.
- Добавление и удаление медиапозиции из readlist.
- Выбор папки при добавлении в readlist.
- Экран readlist с фильтрацией по типу, фильтрацией по папке и сортировкой.
- Управление папками: создание, переименование, удаление.
- Детальный экран медиапозиции.
- Настройки: смена темы, отображение аккаунта, выход из аккаунта.
- Архитектура на Compose + Navigation 3 + ELM/Elmslie + Dagger.

## Как запустить

1. Установить Android Studio с JDK 17.
2. Открыть проект как Gradle-проект.
3. Дождаться синхронизации зависимостей.
4. Запустить приложение на эмуляторе или устройстве.

Сборка debug APK из терминала:

```powershell
.\gradlew.bat assembleDebug
```

Запуск unit-тестов:

```powershell
.\gradlew.bat testDebugUnitTest
```

## Backend

По умолчанию приложение использует:

```text
http://158.160.251.150:8080
```

Контракты, которые использует клиент:

- `POST /auth/register`
- `POST /auth/login`
- `GET /health`
- `GET /media/search?query=...&limit=50&offset=0`
- `GET /media/{id}`
- `GET /user-media`
- `POST /user-media`
- `PATCH /user-media/{id}/folders`
- `DELETE /user-media/{id}`
- `GET /folders`
- `POST /folders`
- `PATCH /folders/{id}`
- `DELETE /folders/{id}`

## Как проверить качество результата

1. Выполнить `.\gradlew.bat testDebugUnitTest`.
2. Выполнить `.\gradlew.bat assembleDebug`.
3. Пройти end-to-end сценарий:
   - зарегистрироваться или войти;
   - открыть каталог;
   - найти медиапозицию;
   - добавить ее в readlist и выбрать папку;
   - открыть readlist;
   - проверить фильтрацию, сортировку и отображение папки;
   - открыть настройки, сменить тему, перейти в управление папками;
   - переименовать или удалить тестовую папку;
   - выйти из аккаунта.

## Документация

- [Итоговый отчет и схемы](docs/FINAL_REPORT.md)
- [Краткий план презентации](docs/PRESENTATION.md)
- [PNG-картинки схем](docs/diagrams)
- [Передача backend-проверок](BACKEND_HANDOFF.md)
