# Контекст проекта: REST API Системы управления сотрудниками (WFM)

> Данный документ фиксирует эволюцию архитектуры, предметную область, модель данных и реализованную функциональность для долгосрочного сопровождения и перехода между лабораторными работами (ЛР 1 $\rightarrow$ ЛР 2 $\rightarrow$ ЛР 3 $\rightarrow$ ЛР 4).

---

## 1. Общие сведения о проекте
* **Название проекта:** WFM Portal / Внутренний корпоративный портал управления сотрудниками и учетом рабочего времени.
* **Предметная область:** Внутренний корпоративный портал и система учета рабочего времени сотрудников для сети филиалов/подразделений (общепит, ритейл, офисы).
* **Стек технологий (ЛР №4):**
  * **Язык:** Java 21 LTS
  * **Сборщик:** Maven 3.9 (Maven Wrapper `./mvnw`)
  * **Фреймворк:** Spring Boot 4.1.1 / Spring Cloud 2025.1.3
  * **Брокер сообщений:** Apache Kafka 3.8.0 в режиме **KRaft** (3 брокера, фактор репликации 3)
  * **Безопасность:** Spring Security 6, JWT Bearer аутентификация, RBAC, BCrypt
  * **БД:** PostgreSQL 16 (в Docker)
  * **Миграции БД:** Flyway (версионированные миграции)
  * **Документация:** OpenAPI 3 / Swagger UI (`springdoc-openapi`)
  * **Тестирование:** JUnit 5 (Jupiter), Mockito, Testcontainers, JaCoCo ($\ge 70\%$, факт: **92.3%**)
  * **Фронтенд:** Vanilla JavaScript, HTML5/CSS3 (Frutiger Aero), чистый Node.js сервер раздачи статики (порты 3000 и 3001)

---

## 2. Эволюция архитектуры по лабораторным работам

1. **Лабораторная №1 (Монолит):**
   * Единое монолитное приложение Spring Boot.
   * 14 таблиц PostgreSQL, транзакционный документооборот, пагинация, бесконечная лента Slice, план/факт явок.
2. **Лабораторная №2 (Микросервисы & Реактивный стек):**
   * Декомпозиция монолита на `discovery-service` (Eureka), `config-service` (Config Server), `gateway-service` (Gateway WebMvc), `employee-service` (JPA), `attendance-service` (WebFlux + R2DBC), `schedule-service` (WebFlux + JPA).
   * Межсервисные вызовы через OpenFeign и Resilience4j Circuit Breaker.
3. **Лабораторная №3 (Безопасность & JWT & RBAC):**
   * Внедрение Spring Security 6 и JWT токенов (Bearer).
   * 4 системные роли: `ADMIN(0)`, `HR(1)`, `MANAGER(2)`, `EMPLOYEE(3)`.
   * Хэширование паролей BCrypt (cost factor 12).
   * Изоляция данных: сотрудники имеют доступ исключительно к собственным персональным данным (`/api/employees/my`).
4. **Лабораторная №4 (Kafka KRaft Cluster & Clean Architecture & File Service):**
   * Отказоустойчивый кластер брокеров сообщений **Apache Kafka (3 брокера KRaft, фактор репликации 3, 5 топиков с 3 партициями)**.
   * Асинхронное событийно-ориентированное взаимодействие (EDA): кадровые события (`employee-events`), явки (`attendance-events`), смены и отпуска (`schedule-events`), системные и персональные уведомления (`notification-events`), файловые операции (`file-events`).
   * **Чистая Архитектура (Clean Architecture / Hexagonal Architecture):** микросервисы `notification-service` и `file-service` разделены на изолированные слои Domain, Application (Use Cases, In/Out Ports), Infrastructure (REST/Kafka Inbound Adapters, JPA/Disk/Kafka Outbound Adapters).
   * **Микросервис файлов (`file-service`):** приём multipart/form-data загрузок ($\le 50$ МБ), безопасное хранение на диске, отдача/скачивание, генерация событий.
   * **Микросервис уведомлений (`notification-service`):** приём событий из всех топиков, агрегация, колокольчик в UI, отметка прочитанных.
   * **Покрытие тестами JaCoCo $\ge 70\%$:** факт — **92.3%** строк по всей кодовой базе, 365+ тестов.

---

## 3. Таблица микросервисов ЛР №4

| Сервис | Порт | Архитектура / Стек | Роль в системе |
|---|:---:|---|---|
| `gateway-service` | 8080 | Spring Cloud Gateway WebMvc | Единая точка входа, маршрутизация, Swagger UI |
| `employee-service` | 8081 | JPA, Spring Security JWT, Kafka Producer | Компании, филиалы, должности, сотрудники, пользователи |
| `attendance-service`| 8082 | Spring WebFlux, R2DBC, Kafka Producer | Реактивный учет явок, расчет опозданий и переработок |
| `schedule-service` | 8083 | Spring Boot, JPA, WebClient, Kafka Producer | Графики смен, отпуска со ссылками на прикрепленные файлы |
| `notification-service`| 8084 | Clean Architecture, Kafka Consumer, JPA | Агрегация уведомлений, REST API ленты, колокольчик |
| `file-service` | 8085 | Clean Architecture, Local Disk Storage, Kafka Producer | Загрузка, скачивание и метаданные документов и аватарок |
| `discovery-service`| 8761 | Eureka Server | Реестр инстансов микросервисов |
| `config-service` | 8888 | Spring Cloud Config Server | Централизованная раздача конфигураций |
| `kafka-1..3` | 9092, 9094, 9095 | Apache Kafka 3.8.0 (KRaft) | Отказоустойчивый распределенный кластер брокеров |
