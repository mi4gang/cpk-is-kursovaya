# ИС центра повышения квалификации

[![CI](https://github.com/mi4gang/cpk-is-kursovaya/actions/workflows/ci.yml/badge.svg)](https://github.com/mi4gang/cpk-is-kursovaya/actions/workflows/ci.yml)

Информационная система для управления программами обучения, заявками, оплатой, аттестацией и выдачей удостоверений.

## Быстрые ссылки
- Онлайн-версия (Render): [https://cpk-is-web.onrender.com/](https://cpk-is-web.onrender.com/)
- Репозиторий: [https://github.com/mi4gang/cpk-is-kursovaya](https://github.com/mi4gang/cpk-is-kursovaya)
- Инструкция по деплою: `docs/deploy/render.md`

## Стек
- Java 17
- Spring Boot 3.5
- Spring Security
- Spring Data JPA (Hibernate)
- Thymeleaf
- PostgreSQL
- H2 (локальный режим)

## Роли
- `ADMIN`
- `METHODIST`
- `TEACHER`
- `STUDENT`

## Основной процесс
`Program -> Application -> Payment -> AssessmentResult -> Certificate`

## Реализовано
- Ролевая авторизация и разграничение доступа.
- CRUD для ключевых сущностей.
- Поиск и сортировка в реестрах.
- Ролевые кабинеты:
  - `STUDENT`: `/student/cabinet`
  - `METHODIST`: `/methodist/queue`
  - `TEACHER`: `/teacher/groups`
  - `ADMIN`: `/admin/dashboard-v2`
- Бизнес-правила процесса:
  - проверка документов (`PENDING/APPROVED/REJECTED`);
  - доступ (`NO_ACCESS/TRIAL_ACCESS/FULL_ACCESS`);
  - пробный период 3 дня;
  - полный доступ после оплаты и проверки документов;
  - порог аттестации `>= 75`;
  - выдача удостоверения после успешной аттестации и завершения обучения.
- Единая обработка ошибок (`ControllerAdvice`, `404`, `500`).

## Локальный запуск
1. Запуск:
   ```bash
   ./scripts/start-local.sh
   ```
2. Проверка статуса:
   ```bash
   ./scripts/status-local.sh
   ```
3. Открыть `http://localhost:8080`.
4. Остановка:
   ```bash
   ./scripts/stop-local.sh
   ```

По умолчанию используется H2 in-memory, поэтому старт без внешней БД.

### Режимы сидирования (`APP_SEED_MODE`)
- `reset` — очистить БД и заново загрузить тестовые данные.
- `once` — загрузить тестовые данные только если БД пустая.
- `off` — не загружать тестовые данные.

## Запуск с PostgreSQL
1. Поднять PostgreSQL:
   ```bash
   docker compose up -d
   ```
2. Перед запуском приложения задать переменные:
   ```bash
   export DB_URL=jdbc:postgresql://localhost:5432/cpk_is
   export DB_USERNAME=postgres
   export DB_PASSWORD=postgres
   export DB_DRIVER=org.postgresql.Driver
   ```
3. Запустить приложение:
   ```bash
   ./scripts/start-local.sh
   ```

## Онлайн-деплой (Render)
В репозитории есть `render.yaml` и `Dockerfile`.
1. В Render создать `Blueprint` из репозитория.
2. Применить конфигурацию из `render.yaml`.
3. Проверить доступность `/login`.

## Тестовые аккаунты
- `admin / admin123`
- `methodist / method123`
- `teacher / teacher123`
- `student / student123`

## Структура репозитория
```text
src/                     # Код приложения
scripts/                 # Операционные скрипты
docs/deploy/             # Документация по деплою
docs/diagrams/           # IDEF/UML/DFD/IDEF1X
docs/pz/                 # Пояснительная записка
docs/screenshots/        # Скриншоты интерфейса
```

## Визуальные материалы
![Главная](docs/screenshots/home_public.png)
![Личный кабинет слушателя](docs/screenshots/student_cabinet.png)
![Очередь методиста](docs/screenshots/methodist_queue.png)
![Кабинет преподавателя](docs/screenshots/teacher_cabinet.png)
![Административная панель](docs/screenshots/admin_dashboard_v2.png)

## Документация
- Диаграммы: `docs/diagrams/`
- ПЗ (docx): `docs/pz/PZ_IS_CPK_FINAL.docx`
- ПЗ (pdf): `docs/pz/PZ_IS_CPK_FINAL.pdf`
