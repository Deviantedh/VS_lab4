# REST API Системы управления сотрудниками (WFM) • Лабораторная работа №4

> Распределённая микросервисная платформа на **Spring Boot 4.1.1 / Spring Cloud 2025.1.3 / Apache Kafka 3.8.0 KRaft / Spring Security 6 / Java 21 LTS**, реализующая событийно-ориентированное взаимодействие (**Event-Driven Architecture**), отказоустойчивый кластер брокеров сообщений **Kafka с фактором репликации 3**, два новых микросервиса по принципам **Чистой Архитектуры (Clean Architecture)** — `notification-service` и `file-service`, а также единую систему управления файлами и уведомлениями.
> 
> **Лабораторная работа №4** по дисциплине «Высокопроизводительные системы».

---

## 📑 Содержание
1. [Архитектура системы (Kafka + Микросервисы)](#-архитектура-системы-kafka--микросервисы)
2. [Требования ТЗ ЛР №4 и их реализация](#-требования-тз-лр-4-и-их-реализация)
3. [Чистая Архитектура (Clean Architecture)](#-чистая-архитектура-clean-architecture)
4. [Кластер Apache Kafka KRaft (Фактор репликации 3)](#-кластер-apache-kafka-kraft-фактор-репликации-3)
5. [Обзор микросервисов платформы](#-обзор-микросервисов-платформы)
6. [Клиентские интерфейсы (Frontend)](#-клиентские-интерфейсы-frontend)
7. [Интерактивная документация (Swagger UI)](#-интерактивная-документация-swagger-ui)
8. [Тестирование и покрытие кода (JaCoCo $\ge 70\%$)](#-тестирование-и-покрытие-кода-jacoco--70)
9. [Быстрый запуск и управление](#-быстрый-запуск-и-управление)

---

## 🏛 Архитектура системы (Kafka + Микросервисы)

```
                            [ Браузер / Клиенты / Swagger UI ]
                                            │
                                            │ :8080 (Authorization: Bearer <JWT>)
                                            ▼
                                ┌───────────────────────┐
                                │    Gateway Service    │
                                │ (Spring Cloud Gateway)│
                                └───────────┬───────────┘
                                            │
       ┌──────────────────┬─────────────────┼──────────────────┬──────────────────┐
       │ :8081            │ :8082           │ :8083            │ :8084            │ :8085
       ▼                  ▼                 ▼                  ▼                  ▼
┌──────────────┐   ┌──────────────┐  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│   Employee   │   │  Attendance  │  │   Schedule   │   │ Notification │   │ File Service │
│   Service    │   │   Service    │  │   Service    │   │   Service    │   │              │
│ (JPA + JWT)  │   │(R2DBC Flux)  │  │ (JPA + Flux) │   │ (Clean Arch) │   │ (Clean Arch) │
└──────┬───────┘   └──────┬───────┘  └──────┬───────┘   └──────▲───────┘   └──────┬───────┘
       │                  │                 │                  │                  │
       │ produces         │ produces        │ produces         │ consumes         │ produces
       ▼                  ▼                 ▼                  │                  ▼
╔══════════════════════════════════════════════════════════════╪══════════════════════════╗
║                    КЛАСТЕР APACHE KAFKA (KRaft, 3 БРОКЕРА)   │                          ║
║                                                              │                          ║
║  [kafka-1 :9092] ◄──────────────► [kafka-2 :9094] ◄──────────┴────────► [kafka-3 :9095] ║
║                                                                                         ║
║  Топики (Partitions: 3, Replication Factor: 3):                                         ║
║   • employee-events      (EMPLOYEE_CREATED, EMPLOYEE_UPDATED, EMPLOYEE_DISMISSED)       ║
║   • attendance-events    (CHECK_IN, CHECK_OUT, LATE_ARRIVAL, OVERTIME)                  ║
║   • schedule-events      (SHIFT_ASSIGNED, SHIFT_REMOVED, REQUEST_APPROVED, ...)         ║
║   • notification-events  (SUCCESS, WARNING, INFO, ALERT)                                ║
║   • file-events          (FILE_UPLOADED, FILE_DELETED)                                  ║
╚═════════════════════════════════════════════════════════════════════════════════════════╝
```

---

## ✅ Требования ТЗ ЛР №4 и их реализация

| № | Требование ТЗ | Статус | Реализация в проекте |
|---|---|:---:|---|
| 1 | **Межсервисное взаимодействие через брокер сообщений (Kafka)** | ✅ Выполнено | Все ключевые бизнес-события (`employee-service`, `attendance-service`, `schedule-service`, `file-service`) публикуются асинхронно через брокер Kafka, снижая связность сервисов. |
| 2 | **Уведомления с помощью Kafka (минимум 3 брокера, фактор репликации 3)** | ✅ Выполнено | Развёрнут кластер из **3 нод Kafka** (`kafka-1`, `kafka-2`, `kafka-3`) в режиме **KRaft**. Создано 5 топиков с `partitions: 3` и `replication-factor: 3`. Сервис `notification-service` слушает события и доставляет уведомления. |
| 3 | **Микросервис для загрузки, хранения и получения файлов** | ✅ Выполнено | Разработан `file-service` (порт 8085) для приёма multipart-загрузок, валидации, сохранения на диск и отдачи файлов/скачивания/метаданных. При загрузке и удалении генерируются события в `file-events`. |
| 4 | **Соответствие принципам Clean Architecture** | ✅ Выполнено | Сервисы `notification-service` и `file-service` строго структурированы по слоям: **Domain** $\leftarrow$ **Application (Use Cases & Ports)** $\leftarrow$ **Infrastructure (Adapters)**. Направление зависимостей направлено строго внутрь к ядру. |
| 5 | **Покрытие тестами $\ge 70\%$** | ✅ Выполнено | Общее покрытие проекта: **92.3% по строкам** (2128 из 2305 строк). Все сервисы без исключения имеют покрытие $\ge 80\%$, успешно выполняются **365+ тестов**. |
| 6 | **Запуск всех сервисов через Docker Compose** | ✅ Выполнено | Единый файл [`compose.yaml`](compose.yaml) оркеструет PostgreSQL 16, 3 ноды Kafka, init-скрипт создания топиков, Discovery (Eureka), Config Server, Gateway и 5 прикладных микросервисов. |

---

## 🧼 Чистая Архитектура (Clean Architecture)

В `notification-service` и `file-service` реализована классическая **Clean Architecture (Hexagonal / Ports and Adapters)**:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        INFRASTRUCTURE LAYER                            │
│  ┌───────────────────────┐                  ┌────────────────────────┐ │
│  │ Inbound Adapters:     │                  │ Outbound Adapters:     │ │
│  │ • REST Controllers    │                  │ • JPA Adapters & DB    │ │
│  │ • Kafka Consumers     │                  │ • Local Storage Adapter│ │
│  │ • Security & JWT      │                  │ • Kafka Producers      │ │
│  └───────────┬───────────┘                  └───────────▲────────────┘ │
│              │                                          │              │
│              ▼                                          │              │
│  ┌──────────────────────────────────────────────────────┴────────────┐ │
│  │                         APPLICATION LAYER                         │ │
│  │  Inbound Ports (Use Cases):         Outbound Ports (SPI):         │ │
│  │  • UploadFileUseCase                • FileStoragePort             │ │
│  │  • DownloadFileUseCase              • FileMetadataRepositoryPort  │ │
│  │  • GetNotificationsUseCase          • FileEventPort               │ │
│  │  • MarkNotificationReadUseCase      • NotificationRepositoryPort  │ │
│  │                                                                   │ │
│  │  Application Services: FileApplicationService,                    │ │
│  │                        NotificationApplicationService             │ │
│  │                                                                   │ │
│  │  ┌──────────────────────────────────────────────────────────────┐ │ │
│  │  │                         DOMAIN LAYER                         │ │ │
│  │  │  Domain Models (Entities): StoredFile, Notification          │ │ │
│  │  │  Domain Exceptions: FileNotFoundException,                   │ │ │
│  │  │                     FileStorageException,                    │ │ │
│  │  │                     NotificationNotFoundException            │ │ │
│  │  │  Value Objects: NotificationType                             │ │ │
│  │  │  (Полная изоляция, 0 зависимостей от внешних фреймворков)   │ │ │
│  │  └──────────────────────────────────────────────────────────────┘ │ │
│  └───────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

### Ключевые правила Clean Architecture:
1. **Правило зависимостей (The Dependency Rule):** Зависимости направлены исключительно внутрь к центру круга. Слой Domain ничего не знает об Application и Infrastructure. Слой Application знает о Domain, но не зависит от Spring Data, Kafka, HTTP или дисковой ФС.
2. **Инверсия зависимостей (DIP):** Внешние хранилища и шины объявляются как порты-интерфейсы (`FileStoragePort`, `FileEventPort`), а их конкретные адаптеры (`LocalDiskFileStorageAdapter`, `FileEventKafkaProducerAdapter`) реализуют эти порты на слое Infrastructure.

---

## ⚡ Кластер Apache Kafka KRaft (Фактор репликации 3)

- **Режим KRaft (Kafka Raft):** Исключает необходимость в устаревшем Apache Zookeeper. Метаданные и консенсус между брокерами реплицируются через встроенный Raft-протокол (`broker,controller`).
- **3 брокера:**
  - `kafka-1` (порт 9092)
  - `kafka-2` (порт 9094)
  - `kafka-3` (порт 9095)
- **Фактор репликации: 3** — каждая партиция топика реплицируется на все 3 брокера. При выходе из строя одного или даже двух брокеров данные не теряются, а кластер сохраняет работоспособность.
- **Топики системы:**
  1. `employee-events` (3 партиции, RF=3) — кадровые события (приём, обновление, увольнение).
  2. `attendance-events` (3 партиции, RF=3) — фиксация чекинов, чекаутов, переработок и опозданий.
  3. `schedule-events` (3 партиции, RF=3) — публикация смен и согласование отпусков/заявок.
  4. `notification-events` (3 партиции, RF=3) — персонализированные и широковещательные уведомления.
  5. `file-events` (3 партиции, RF=3) — загрузка и удаление файлов.

---

## 📦 Обзор микросервисов платформы

| Микросервис | Порт | Архитектурный стиль / Технологии | Назначение |
|---|:---:|---|---|
| **notification-service** | `8084` | Clean Architecture, Spring Boot 4, Kafka Consumer, JPA | Приём событий из 5 топиков Kafka, сохранение уведомлений, выдача непрочитанных, REST API. |
| **file-service** | `8085` | Clean Architecture, Spring Boot 4, Kafka Producer, Local Storage | Загрузка файлов (Multipart), валидация, отдача/скачивание, генерация событий `file-events`. |
| **employee-service** | `8081` | Spring Boot 4, JPA, Spring Security JWT, Kafka Producer | Управление сотрудниками, ролевой доступ, публикация `employee-events`. |
| **attendance-service** | `8082` | Spring WebFlux, Spring Data R2DBC, Kafka Producer | Реактивный учёт посещаемости, детекция опозданий и переработок в `attendance-events`. |
| **schedule-service** | `8083` | Spring Boot 4, JPA, WebClient, Kafka Producer | Графики смен, документооборот (заявки на отпуск со ссылками на файлы из `file-service`). |
| **gateway-service** | `8080` | Spring Cloud Gateway WebMvc | Единая точка входа, маршрутизация запросов к микросервисам, агрегация Swagger UI. |
| **discovery-service** | `8761` | Netflix Eureka Server | Динамическая регистрация и обнаружение сервисов (Service Discovery). |
| **config-service** | `8888` | Spring Cloud Config Server | Централизованное хранение и раздача конфигураций из репозитория. |

---

## 🎨 Клиентские интерфейсы (Frontend)

Система включает два клиентских веб-приложения (стилистика **Frutiger Aero**, чистый JavaScript без сторонних библиотек):
1. **Панель Администратора / HR (порт 3000):**
   - Интерактивный **колокольчик уведомлений Kafka** с бейджем непрочитанных и выпадающим списком в реальном времени.
   - Просмотр заявок на отпуск со ссылками на **скачивание прикреплённых документов** (`📎 Скачать документ`).
   - Управление организационной структурой, сотрудниками, сменами и табелем.
2. **Личный Кабинет Сотрудника (порт 3001):**
   - Личные смены, отметки Check-in / Check-out.
   - **Подача заявки на отпуск с прикреплением файла** (справка, скан заявления) — файл автоматически загружается через `file-service`, а событие публикуется в Kafka.
   - Колокольчик уведомлений о решениях по заявкам и назначениях на смены.

---

## 📊 Тестирование и покрытие кода (JaCoCo $\ge 70\%$)

Для гарантии надёжности проект протестирован на всех уровнях (юнит-тесты, тесты Use Case, адаптеров, контроллеров и конфигураций):

### Сводная таблица покрытия кода (JaCoCo):

| Микросервис / Модуль | Покрытие инструкций | Покрытие строк (Lines) | Статус ТЗ ($\ge 70\%$) |
|---|:---:|:---:|:---:|
| **notification-service** | **91.3%** (1172 / 1284) | **95.8%** (320 / 334) | ✅ Превышено (+25.8%) |
| **file-service** | **82.2%** (896 / 1090) | **87.6%** (241 / 275) | ✅ Превышено (+17.6%) |
| **employee-service** | **95.0%** (2690 / 2833) | **97.1%** (639 / 658) | ✅ Превышено (+27.1%) |
| **schedule-service** | **90.1%** (2912 / 3232) | **93.0%** (716 / 770) | ✅ Превышено (+23.0%) |
| **attendance-service** | **77.6%** (519 / 669) | **80.0%** (136 / 170) | ✅ Превышено (+10.0%) |
| **common-dto** | **78.0%** (291 / 373) | **72.2%** (57 / 79) | ✅ Превышено (+2.2%) |
| **gateway-service** | **100.0%** (54 / 54) | **100.0%** (13 / 13) | ✅ Превышено (+30.0%) |
| **discovery-service** | **100.0%** (8 / 8) | **100.0%** (3 / 3) | ✅ Превышено (+30.0%) |
| **config-service** | **100.0%** (8 / 8) | **100.0%** (3 / 3) | ✅ Превышено (+30.0%) |
| **ИТОГО ПО ПРОЕКТУ** | **89.5%** (8550 / 9551) | **92.3%** (2128 / 2305) | ✅ **ВЫПОЛНЕНО С ЗАПАСОМ** |

Запуск всех тестов и открытие сводного отчета:
```bash
./run-tests-with-coverage.sh --open
```

---

## 🚀 Быстрый запуск и управление

### 1. Запуск всей системы (Docker Compose + Фронтенд):
```bash
./start.sh
```

### 2. Ссылки для проверки:
- **Панель Администратора:** [http://localhost:3000](http://localhost:3000)
- **Кабинет Сотрудника:** [http://localhost:3001](http://localhost:3001)
- **API Gateway:** [http://localhost:8080/api](http://localhost:8080/api)
- **Swagger UI (все 5 сервисов):** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Eureka Dashboard:** [http://localhost:8761](http://localhost:8761)
- **Notification Service:** [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html)
- **File Service:** [http://localhost:8085/swagger-ui.html](http://localhost:8085/swagger-ui.html)
- **Брокеры Kafka:** `localhost:9092`, `localhost:9094`, `localhost:9095`

### 3. Остановка сервисов:
```bash
./stop.sh
```
