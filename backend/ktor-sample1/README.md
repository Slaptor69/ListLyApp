# Listly Backend (Ktor + MongoDB)

Backend для мобильного приложения **Listly**.

## Что уже есть
- JWT авторизация (`/auth/register`, `/auth/login`)
- Коллекция пользователя (`/user-media`)
- MongoDB + Docker Compose
- Unit/integration tests

## Документация
- API контракт для backend и Android: `docs/API_V1.md`
- Процесс поддержки документации: `docs/DOCS_PROCESS.md`

## Быстрый старт (локально)

### 1) Поднять MongoDB в Docker
```bash
docker compose up -d mongo
```

### 2) Экспортировать переменные
```bash
export MONGO_URI='mongodb://listly_admin:secret123@localhost:27017/listlydb?authSource=admin'
```

### 3) Запустить backend
```bash
./gradlew run
```

Сервер: `http://localhost:8080`

## Полный запуск через Docker Compose
```bash
docker compose up --build
```

Сервисы:
- `app` -> `http://localhost:8080`
- `mongo` -> `localhost:27017`

## Полезные команды
```bash
./gradlew test
./gradlew build
```

## Как синхронизироваться с Android-клиентом
1. Любое изменение API сначала/сразу фиксируйте в `docs/API_V1.md`.
2. Backend и Android сверяют DTO/enum/error-коды только по этому файлу.
3. В каждом PR с API-изменением добавляйте секцию `API changes`.

## Текущая целевая модель данных
- `MediaItem` — глобальная сущность (видят все пользователи)
- `UserMediaItem` — пользовательская сущность со ссылкой `mediaId` на `MediaItem`

Это целевая модель для следующего этапа рефакторинга backend.
