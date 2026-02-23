# Соответствие критериям

## Критерии по функционалу
1. Регистрация и авторизация: `AuthController`, страницы `/register` и `/login`.
2. Отображение данных в таблицах: страницы всех разделов (`/programs`, `/applications`, `/payments`, `/assessments`, `/certificates`) и ролевые кабинеты (`/student/cabinet`, `/methodist/queue`, `/teacher/groups`, `/admin/dashboard-v2`).
3. CRUD: контроллеры `ProgramController`, `ApplicationController`, `PaymentController`, `AssessmentController`, `CertificateController`.
4. Поиск и сортировка: разделы `programs`, `applications`, `payments`.
5. Роли пользователей и ролевые кабинеты: `RoleName`, `@PreAuthorize`, отдельные контроллеры по ролям.
6. Статистика: `HomeController` (`/dashboard`) + `StatsService` + `AdminDashboardV2Controller`.
7. Страница «Об авторе»: `/about`.
8. Устойчивость: `GlobalExceptionHandler`, `AppErrorController`, шаблоны `error/404.html`, `error/general.html`.
9. Линейная бизнес-логика: `ApplicationWorkflowService` (документы, trial 3 дня, 100% предоплата, порог аттестации 75, правила выдачи удостоверения).
10. Публичная точка входа: `/` (лендинг с программами и CTA вход/регистрация).

## Критерии по безопасности
1. SQL-инъекции: доступ к данным через JPA/Hibernate.
2. XSS: экранирование вывода в Thymeleaf + контролируемый вывод пользовательских полей.
3. Ролевой доступ: Spring Security + проверка ролей.

## Критерии по архитектуре
- Трехзвенная архитектура реализована:
  - UI: Thymeleaf
  - Сервер: Spring Boot
  - Данные: PostgreSQL

## Критерии по документации
1. Подготовлен комплект диаграмм в формате draw.io.
2. Подготовлен черновик ПЗ в формальном стиле.
3. Подготовлена структура презентации и скрипт защиты.
