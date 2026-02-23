# Матрица страниц и ролей (v1)

Документ фиксирует итоговую карту страниц и доступов после рефакторинга логики маршрутов.

## Общие страницы

| Путь | Назначение | Аноним | STUDENT | TEACHER | METHODIST | ADMIN |
|---|---|---:|---:|---:|---:|---:|
| `/` | Публичная главная | Да | Да | Да | Да | Да |
| `/programs` | Каталог программ (публичный) | Да | Да | Да | Да | Да |
| `/consultation` (GET/POST) | Форма консультации | Да | Да | Да | Да | Да |
| `/about` | Страница об авторе | Да | Да | Да | Да | Да |
| `/login` | Вход | Да | Да | Да | Да | Да |
| `/register` (GET/POST) | Регистрация слушателя | Да | Да | Да | Да | Да |

## Ролевые точки входа

`/dashboard` работает как role-aware redirect:

- `ADMIN` -> `/admin/dashboard-v2`
- `METHODIST` -> `/methodist/queue`
- `TEACHER` -> `/teacher/groups`
- `STUDENT` -> `/student/cabinet`

## Кабинет администратора

| Путь | Назначение | Доступ |
|---|---|---|
| `/admin/dashboard-v2` | Операционный BI-дашборд | `ADMIN` |
| `/admin/dashboard-v2/drilldown` | Drilldown по очередям | `ADMIN` |
| `/admin/dashboard-v2/case/{applicationId}` | Карточка кейса (контрольный режим) | `ADMIN` |
| `/admin/dashboard-v2/completed` | Аналитика завершенных услуг | `ADMIN` |

## Кабинет методиста

| Путь | Назначение | Доступ |
|---|---|---|
| `/methodist/queue` | Очереди документов/доступов/удостоверений | `METHODIST`, `ADMIN` |
| `/methodist/queue/{id}/docs` | Подтверждение/отклонение документов | `METHODIST`, `ADMIN` |
| `/methodist/queue/{id}/trial` | Открытие trial-доступа | `METHODIST`, `ADMIN` |
| `/methodist/queue/{id}/assign-teacher` | Назначение преподавателя | `METHODIST`, `ADMIN` |

## Кабинет преподавателя

| Путь | Назначение | Доступ |
|---|---|---|
| `/teacher/groups` | Группы преподавателя, список слушателей | `TEACHER`, `ADMIN` |
| `/teacher/groups/{programId}` | Детали группы | `TEACHER`, `ADMIN` |
| `/teacher/groups/{programId}/applications/{id}/progress` | Обновление прогресса | `TEACHER`, `ADMIN` |
| `/teacher/groups/{programId}/applications/{id}/complete` | Завершение цикла обучения | `TEACHER`, `ADMIN` |

## Кабинет слушателя

| Путь | Назначение | Доступ |
|---|---|---|
| `/student/cabinet` | ЛК слушателя (этап, прогресс, рекомендации) | `STUDENT` |

## Операционные CRUD-модули

| Путь | Назначение | Доступ |
|---|---|---|
| `/applications` | Реестр заявок | `STUDENT`, `METHODIST`, `ADMIN` |
| `/applications/new` | Новая заявка | `STUDENT` |
| `/applications/{id}/edit` | Редактирование заявки | `METHODIST`, `ADMIN` |
| `/applications/save` | Сохранение заявки | `STUDENT`, `METHODIST`, `ADMIN` |
| `/applications/{id}/delete` | Удаление заявки | `METHODIST`, `ADMIN` |
| `/programs/new` `/programs/{id}/edit` `/programs/save` `/programs/{id}/delete` | Управление программами | `METHODIST`, `ADMIN` |
| `/payments` и формы/операции | Управление оплатами | `METHODIST`, `ADMIN` |
| `/assessments` и формы/операции | Управление аттестацией | `TEACHER`, `ADMIN` |
| `/certificates` и формы/операции | Управление удостоверениями | `METHODIST`, `ADMIN` |

## Принципы унификации

1. Все страницы используют единый визуальный профиль: светлая тема, Tailwind-компоненты, одинаковые паттерны карточек/таблиц/кнопок.
2. Все enum-статусы в UI отображаются через русские `label`, а не технические значения (`PAID`, `PENDING` и т.д.).
3. Публичный каталог программ доступен без авторизации и не требует перехода через логин.
