# Соответствие критериям

## Критерии по функционалу
1. Регистрация и авторизация: `SecurityConfig`, страница `/login`.
2. Отображение данных в таблицах: страницы всех разделов (`/programs`, `/applications`, `/payments`, `/assessments`, `/certificates`).
3. CRUD: контроллеры `ProgramController`, `ApplicationController`, `PaymentController`, `AssessmentController`, `CertificateController`.
4. Поиск и сортировка: разделы `programs`, `applications`, `payments`.
5. Роли пользователей: `RoleName`, `@PreAuthorize`.
6. Статистика: `HomeController` + `StatsService`.
7. Страница «Об авторе»: `/about`.
8. Устойчивость: `GlobalExceptionHandler`, `AppErrorController`, шаблоны `error/404.html`, `error/general.html`.

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
