# ИС центра повышения квалификации (КР)

Учебный MVP-проект по курсовой работе.

## Стек
- Java 17+
- Spring Boot 3.5
- Spring Security
- Spring Data JPA (Hibernate)
- PostgreSQL
- Thymeleaf

## Что реализовано
- Авторизация и ролевой доступ (`ADMIN`, `METHODIST`, `TEACHER`, `STUDENT`).
- CRUD для `Program`, `Application`, `Payment`, `AssessmentResult`, `Certificate`.
- Поиск и сортировка в ключевых разделах.
- Статистика на главной (слушатели, активные программы, сумма оплат).
- Страница `Об авторе`.
- Единая обработка ошибок (`ControllerAdvice` + страницы `404/500`).

## Быстрый старт
1. Запустить приложение (скрипт сам использует локальный JDK из `.tools`):
   ```bash
   ./scripts/start-local.sh
   ```
2. Проверить статус:
   ```bash
   ./scripts/status-local.sh
   ```
3. Открыть [http://localhost:8080](http://localhost:8080).
4. Остановить:
   ```bash
   ./scripts/stop-local.sh
   ```

## Режим БД
- По умолчанию используется встроенная H2 (in-memory), чтобы проект запускался сразу.
- Для PostgreSQL перед запуском задайте переменные:
  - `DB_URL=jdbc:postgresql://localhost:5432/cpk_is`
  - `DB_USERNAME=postgres`
  - `DB_PASSWORD=postgres`
  - `DB_DRIVER=org.postgresql.Driver`

## Демо-пользователи
- `admin / admin123`
- `methodist / method123`
- `teacher / teacher123`
- `student / student123`

## Документы
- Спецификация: `docs/spec/01_requirements_spec.md`
- Привязка к критериям: `docs/spec/criteria_mapping.md`
- Диаграммы: `docs/diagrams/*.drawio`
- Черновик ПЗ: `docs/pz/Пояснительная_записка_черновик.md`
- DOCX ПЗ: `docs/pz/Пояснительная_записка_черновик.docx`
- Презентация (структура): `docs/presentation/presentation_outline.md`
- Скрипт защиты: `docs/presentation/defense_script.md`
