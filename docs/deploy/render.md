# Деплой в Render (Web + PostgreSQL)

## Что уже подготовлено в репозитории
- `Dockerfile` для Spring Boot приложения.
- `render.yaml` с описанием:
  - web-сервиса `cpk-is-web`;
  - managed PostgreSQL `cpk-is-db`;
  - переменных `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_DRIVER`.

## Быстрый сценарий деплоя
1. Открыть Render Dashboard.
2. Выбрать `New` -> `Blueprint`.
3. Подключить GitHub-репозиторий `mi4gang/cpk-is-kursovaya`.
4. Убедиться, что Render обнаружил `render.yaml`.
5. Нажать `Apply`.
6. Дождаться создания БД и первого деплоя.
7. Проверить `https://<service>.onrender.com/login`.

## Что проверить после деплоя
1. Открывается `/login`.
2. Вход под `admin/admin123` успешен.
3. Работает базовый сценарий: `Program -> Application -> Payment -> Assessment -> Certificate`.
4. Страница ошибок доступна при обращении к несуществующему URL.

## Примечание
Для учебного демо локальный режим (H2) остается как fallback.
