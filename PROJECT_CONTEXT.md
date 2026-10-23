# Контекст проекта: REST API Системы управления сотрудниками

> Данный документ фиксирует архитектурный контекст, предметную область, модель данных и реализованную функциональность для долгосрочного сопровождения и перехода между лабораторными работами (Лаб 1 -> Лаб 2 -> Лаб 3 -> Лаб 4).

---

## 1. Общие сведения о проекте
* **Название проекта:** REST API Системы управления сотрудниками
* **Предметная область:** Внутренний корпоративный портал и система учета рабочего времени сотрудников для сети филиалов/подразделений (общепит, ритейл, офисы).
* **Стек технологий:**
  * **Язык:** Java 21 LTS
  * **Сборщик:** Maven 3.9 (Maven Wrapper `./mvnw`)
  * **Фреймворк:** Spring Boot 4.x
  * **БД:** PostgreSQL 16 (в Docker)
  * **Миграции БД:** Flyway (директория `src/main/resources/db/migration`)
  * **Документация:** OpenAPI 3 / Swagger UI (`springdoc-openapi`)
  * **Тестирование:** JUnit 5 (Jupiter), Mockito, Testcontainers

---

## 2. Ключевая концепция: Разделение ПЛАН vs ФАКТ
* **План (Schedule / Shifts):** когда сотрудник *должен* выйти на смену согласно расписанию филиала.
* **Факт (Attendance):** когда сотрудник *фактически* нажал Check-In (пришёл) и Check-Out (ушёл).
* **Расчёт отклонений:** при закрытии смены автоматически рассчитываются минуты опоздания (`lateMinutes`) и переработки (`overtimeMinutes`).

---

## 3. Архитектура и структура кода (Package-by-layer)
```text
portal
├── config          # OpenApiConfig (Swagger), JacksonConfig (JavaTimeModule)
├── controller      # REST контроллеры (10 штук с аннотациями Swagger @Operation)
├── service         # Бизнес-логика, транзакции (@Transactional)
├── repository      # Spring Data JPA интерфейсы (с JPQL запросами)
├── entity          # Модели БД (@Entity, связи 1:N, M:N, M:N с доп. полями)
├── dto             # Request/Response DTO с валидацией jakarta.validation
└── exception       # ResourceNotFoundException, BusinessConflictException, GlobalExceptionHandler
```

---

## 4. Даталогическая модель данных (14 таблиц в БД)

1. **`roles`:** справочник ролей:
   * `0: ADMIN` — Администратор (полный доступ)
   * `1: HR` — HR-специалист (персонал, оргструктура, заявки)
   * `2: MANAGER` — Управляющий точкой (графики, смены, явка)
   * `3: EMPLOYEE` — Линейный персонал (бариста, пекарь, кассир)
2. **`users`:** учетные записи (`login`, `role_id`, `employee_id`, `is_active`, `password_hash` [nullable до лабы 3]).
3. **`companies`:** организации и сети (`name`).
4. **`branches`:** филиалы/точки (`company_id`, `name`, `address`, `phone`, `is_active`).
5. **`positions`:** справочник должностей (`title`).
6. **`employees`:** сотрудники (`name`, `phone`, `birth_date`, `hire_date`, `dismissal_date`, `status`).
7. **`employee_assignments`:** история назначений на точку и должность (**Many-to-Many с дополнительными полями** `started_at`, `ended_at`, `is_primary`).
8. **`schedules`:** расписания по филиалам на период (`branch_id`, `date_from`, `date_to`, `created_by`).
9. **`shifts`:** плановые смены (`schedule_id`, `date`, `time_from`, `time_to`, `break_minutes`).
10. **`shift_employees`:** связка смен и сотрудников (**чистая Many-to-Many**).
11. **`shift_employee_logs`:** аудит-лог изменений состава смены (`shift_id`, `employee_id`, `action = ASSIGNED/REMOVED`, `created_by`).
12. **`attendance_records`:** факт явки и отработанного времени (`employee_id`, `shift_id`, `actual_start`, `actual_end`).
13. **`requests`:** универсальные заявки сотрудников (`employee_id`, `type = VACATION/DAY_OFF/CERTIFICATE/MED_EXAM`, `status = PENDING/APPROVED/REJECTED`, `request_data JSONB`).
14. **`employee_absences`:** календарь зафиксированных отсутствий (`employee_id`, `type`, `date_from`, `date_to`, `request_id`).

---

## 5. Выполненные требования ТЗ Лабораторной №1
* **Связи всех типов:** 1:N (`Company` -> `Branch`), M:N (`Shift` <-> `Employee`), M:N с доп. полями (`EmployeeAssignment`).
* **Enums как строки:** `@Enumerated(EnumType.STRING)` во всех сущностях.
* **Валидация:** на уровне контроллеров (`@Valid`, `@Max(50)`) и Entity (`@NotBlank`, `@Pattern`, `@NotNull`).
* **Ошибки:** `GlobalExceptionHandler` отдаёт понятные человекочитаемые ответы с корректными HTTP-кодами (404, 400, 409, 500).
* **Пагинация $\le 50$:** жесткое ограничение в параметрах всех запросов.
* **Бесконечная лента (Slice) без count(\*):** `GET /api/attendance` возвращает `SliceResponse` с флагом `hasNext`.
* **Пагинация со счетчиком в HTTP-хедере:** `GET /api/shifts` отдаёт общее число смен и страниц в заголовках `X-Total-Count` и `X-Total-Pages` (открыты в CORS).
* **Транзакционный сценарий 1 (`ShiftService.assignEmployee`):** атомарная проверка отсутствий, пересечений смен, назначение сотрудника и запись в аудит-лог `shift_employee_logs`.
* **Транзакционный сценарий 2 (`RequestService.processRequest`):** при одобрении отпуска атомарно меняется статус заявки, создаётся запись в `employee_absences` и сотрудник автоматически снимается со всех плановых смен на даты отпуска.
* **Контейнеризация:** multi-stage `Dockerfile` + `compose.yaml` (Postgres + App).
* **Тестирование:** JUnit 5 + Mockito (`CompanyServiceTest`, `ShiftServiceTest`, `RequestServiceTest`), Testcontainers для интеграционных тестов (все 106 тестов успешно проходят, 0 сбоев).

---

## 6. Планы на следующие лабораторные работы
* **Лабораторная №2:** Декомпозиция монолита на микросервисы (Eureka, Config Server, Spring Cloud Gateway, Feign Client, Resilience4j Circuit Breaker, Reactor + R2DBC).
* **Лабораторная №3:** Авторизация Spring Security + JWT, роли `ADMIN(0)`, `HR(1)`, `MANAGER(2)`, `EMPLOYEE(3)`, хеширование паролей BCrypt.
* **Лабораторная №4:** Межсервисное взаимодействие и уведомления через брокер сообщений (Kafka/RabbitMQ), микросервис для работы с файлами.
