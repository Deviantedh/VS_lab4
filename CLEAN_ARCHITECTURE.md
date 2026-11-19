# Полное руководство по Чистой Архитектуре (Clean Architecture)
**Дисциплина: Высокопроизводительные системы (ИТМО) • Лабораторная работа №4**  
**Реализация в сервисах: `file-service` и `notification-service`**

---

## 📑 Оглавление
1. [Что такое Чистая Архитектура (Clean Architecture)?](#1-что-такое-чистая-архитектура-clean-architecture)
2. [Главный закон: Правило зависимостей (The Dependency Rule)](#2-главный-закон-правило-зависимостей-the-dependency-rule)
3. [Слои Чистой Архитектуры: Разбор на коде `file-service`](#3-слои-чистой-архитектуры-разбор-на-коде-file-service)
   - [Слой 1: Domain (Домен / Бизнес-ядро)](#слой-1-domain-домен--бизнес-ядро)
   - [Слой 2: Application (Сценарии / Use Cases и Порты)](#слой-2-application-сценарии--use-cases-и-порты)
   - [Слой 3: Infrastructure (Инфраструктура / Адаптеры)](#слой-3-infrastructure-инфраструктура--адаптеры)
4. [Инверсия зависимостей (DIP) и концепция Ports & Adapters](#4-инверсия-зависимостей-dip-и-концепция-ports--adapters)
5. [Разбор сервиса `notification-service` по Чистой Архитектуре](#5-разбор-сервиса-notification-service-по-чистой-архитектуре)
6. [Зачем это нужно? (Плюсы vs Классическая Layered Architecture)](#6-зачем-это-нужно-плюсы-vs-классическая-layered-architecture)
7. [Шпаргалка к защите (Вопросы преподавателя и готовые ответы)](#7-шпаргалка-к-защите-вопросы-преподавателя-и-готовые-ответы)

---

## 1. Что такое Чистая Архитектура (Clean Architecture)?

**Чистая Архитектура (Clean Architecture)** — это архитектурный подход, предложенный Робертом Мартином (Uncle Bob / Дядей Бобом) в 2012 году.

Главная идея: **бизнес-логика приложения должна быть полностью изолирована от внешнего мира — баз данных, веб-фреймворков (Spring Boot), брокеров сообщений (Kafka) и протоколов (HTTP/REST).**

### Родственные архитектурные стили:
* **Hexagonal Architecture (Гексагональная архитектура / Ports & Adapters)** Алистера Кокберна (2005 г.).
* **Onion Architecture (Луковая архитектура)** Джеффри Палермо (2008 г.).

Все они говорят об одном и том же: ядро системы не должно зависеть от деталей инфраструктуры.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        INFRASTRUCTURE LAYER                            │
│  (Внешний мир: Spring Controllers, Kafka Listeners, JPA, Файловая ФС)  │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                        APPLICATION LAYER                         │  │
│  │     (Use Cases / Сценарии использования, Inbound/Outbound Порты) │  │
│  │  ┌────────────────────────────────────────────────────────────┐  │  │
│  │  │                       DOMAIN LAYER                         │  │  │
│  │  │        (Сущности, Бизнес-правила, Доменные ошибки)         │  │  │
│  │  │         ПОЛНАЯ ИЗОЛЯЦИЯ: НОЛЬ ВНЕШНИХ БИБЛИОТЕК            │  │  │
│  │  └────────────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Главный закон: Правило зависимостей (The Dependency Rule)

> **The Dependency Rule:**  
> Исходный код и зависимости могут быть направлены **СТРОГО СНАРУЖИ ВНУТРЬ**.  
> Ничто во внутреннем круге не может знать о том, что существует во внешнем круге!

* **Слой Domain** не знает НИЧЕГО. В нем нет импортов Spring, Hibernate, Jackson, Kafka. Это чистая Java (POJO).
* **Слой Application** знает о Domain, но не знает о Spring Web, JPA, Kafka или диске.
* **Слой Infrastructure** знает обо всех внутренних слоях (Application и Domain) и соединяет их с реальным миром (Spring, PostgreSQL, Apache Kafka).

---

## 3. Слои Чистой Архитектуры: Разбор на коде `file-service`

Открой пакет [file-service/src/main/java/portal/file](file:///c:/Users/alex-/OneDrive/Рабочий%20стол/homeWork/ITMO/ВС/Lab4/VS_lab4/file-service/src/main/java/portal/file).  
Он четко разбит на 3 каталога: `domain`, `application`, `infrastructure`.

```
portal.file/
├── domain/                               <-- 1. Слой Домена
│   ├── model/StoredFile.java
│   └── exception/
│       ├── FileNotFoundException.java
│       ├── FileStorageException.java
│       └── InvalidFileException.java
│
├── application/                          <-- 2. Слой Сценариев
│   ├── port/
│   │   ├── in/                           (Входящие порты / Use Cases)
│   │   │   ├── UploadFileUseCase.java
│   │   │   ├── DownloadFileUseCase.java
│   │   │   ├── GetFileMetadataUseCase.java
│   │   │   └── DeleteFileUseCase.java
│   │   └── out/                          (Исходящие порты SPI)
│   │       ├── FileStoragePort.java
│   │       ├── FileMetadataRepositoryPort.java
│   │       └── FileEventPort.java
│   └── service/
│       └── FileApplicationService.java   (Оркестратор Use Cases)
│
└── infrastructure/                       <-- 3. Слой Инфраструктуры
    ├── adapter/
    │   ├── in/rest/
    │   │   └── FileController.java       (Входящий REST-адаптер)
    │   └── out/
    │       ├── storage/
    │       │   └── LocalDiskFileStorageAdapter.java  (Пишет байты на диск)
    │       ├── persistence/
    │       │   ├── FileMetadataJpaEntity.java        (JPA Entity для базы)
    │       │   ├── SpringDataFileMetadataRepository.java
    │       │   └── FileMetadataRepositoryAdapter.java
    │       └── kafka/
    │           └── FileEventKafkaProducerAdapter.java (Шлет события в Кафку)
    ├── config/
    └── security/
```

---

### Слой 1: Domain (Домен / Бизнес-ядро)

Это святилище системы. Здесь описаны модели и правила предметной области.

#### Файл: [`StoredFile.java`](file:///c:/Users/alex-/OneDrive/Рабочий%20стол/homeWork/ITMO/ВС/Lab4/VS_lab4/file-service/src/main/java/portal/file/domain/model/StoredFile.java)
```java
package portal.file.domain.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredFile {
    private Long id;
    private String filename;          // Уникальное имя (UUID)
    private String originalFilename;  // Исходное имя пользователя
    private String contentType;       // MIME-тип (image/png, application/pdf)
    private Long size;                // Размер в байтах
    private String storagePath;       // Путь в хранилище
    private String uploadedBy;        // Логин загрузившего
    private LocalDateTime uploadedAt; // Дата загрузки
}
```
> **Что показать преподавателю:**  
> В этом классе **НЕТ аннотации `@Entity`**, нет `@Table`, нет `@Id`, нет Jackson `@JsonProperty`. Это абсолютно чистый Java-объект (Plain Old Java Object — POJO). Он не привязан к PostgreSQL и может использоваться хоть в мобильном приложении, хоть в консольной утилите.

---

### Слой 2: Application (Сценарии / Use Cases и Порты)

Слой Application отвечает на вопрос: **ЧТО умеет делать система?**  
Он оперирует интерфейсами, которые делятся на **Входящие (Inbound)** и **Исходящие (Outbound)** порты.

#### 1. Входящие порты (Inbound Ports / Use Cases):
Интерфейсы того, что внешний мир может попросить сделать:
* [`UploadFileUseCase.java`](file:///c:/Users/alex-/OneDrive/Рабочий%20стол/homeWork/ITMO/ВС/Lab4/VS_lab4/file-service/src/main/java/portal/file/application/port/in/UploadFileUseCase.java):
  ```java
  public interface UploadFileUseCase {
      StoredFile upload(String originalFilename, String contentType, byte[] data, String uploadedBy);
  }
  ```
* `DownloadFileUseCase.java`, `GetFileMetadataUseCase.java`, `DeleteFileUseCase.java`.

#### 2. Исходящие порты (Outbound Ports / Service Provider Interfaces — SPI):
Интерфейсы того, ЧТО сервису нужно от внешнего мира для выполнения сценария:
* [`FileStoragePort.java`](file:///c:/Users/alex-/OneDrive/Рабочий%20стол/homeWork/ITMO/ВС/Lab4/VS_lab4/file-service/src/main/java/portal/file/application/port/out/FileStoragePort.java) — контракт на сохранение/удаление сырых байтов:
  ```java
  public interface FileStoragePort {
      String store(String filename, byte[] data);
      byte[] load(String filename);
      void delete(String filename);
  }
  ```
* [`FileMetadataRepositoryPort.java`](file:///c:/Users/alex-/OneDrive/Рабочий%20стол/homeWork/ITMO/ВС/Lab4/VS_lab4/file-service/src/main/java/portal/file/application/port/out/FileMetadataRepositoryPort.java) — контракт на сохранение метаданных в БД.
* [`FileEventPort.java`](file:///c:/Users/alex-/OneDrive/Рабочий%20стол/homeWork/ITMO/ВС/Lab4/VS_lab4/file-service/src/main/java/portal/file/application/port/out/FileEventPort.java) — контракт на отправку события во внешнюю шину (Kafka).

#### 3. Сервис приложения: [`FileApplicationService.java`](file:///c:/Users/alex-/OneDrive/Рабочий%20стол/homeWork/ITMO/ВС/Lab4/VS_lab4/file-service/src/main/java/portal/file/application/service/FileApplicationService.java)
Он оркестрирует сценарий загрузки:
```java
@Service
@RequiredArgsConstructor
@Transactional
public class FileApplicationService implements UploadFileUseCase, DownloadFileUseCase ... {

    // Сервис зависит ТОЛЬКО от интерфейсов-портов:
    private final FileStoragePort storagePort;
    private final FileMetadataRepositoryPort repositoryPort;
    private final FileEventPort eventPort;

    @Override
    public StoredFile upload(String originalFilename, String contentType, byte[] data, String uploadedBy) {
        // 1. Бизнес-валидация
        if (data == null || data.length == 0) throw new InvalidFileException("Файл пуст");

        // 2. Генерация уникального UUID имени
        String uniqueFilename = UUID.randomUUID() + extension;

        // 3. Сохранение сырых байтов через порт хранилища
        String storagePath = storagePort.store(uniqueFilename, data);

        // 4. Формирование доменной модели
        StoredFile file = StoredFile.builder()...build();

        // 5. Сохранение метаданных через порт БД
        StoredFile saved = repositoryPort.save(file);

        // 6. Публикация события через порт шины событий
        eventPort.publishFileUploaded(saved);

        return saved;
    }
}
```
> **Что сказать на защите:**  
> `FileApplicationService` понятия не имеет, куда сохраняются файлы (на диск или в Amazon S3), какая СУБД используется (Postgres или MongoDB) и куда публикуется событие (в Kafka или RabbitMQ). Он зависит исключительно от абстракций (портов).

---

### Слой 3: Infrastructure (Инфраструктура / Адаптеры)

Здесь живут все низкоуровневые технические детали — Spring Web, Spring Data JPA, Kafka, работа с жестким диском.

#### Адаптеры бывают двух типов:
1. **Входящий адаптер (Inbound Adapter):**  
   [`FileController.java`](file:///c:/Users/alex-/OneDrive/Рабочий%20стол/homeWork/ITMO/ВС/Lab4/VS_lab4/file-service/src/main/java/portal/file/infrastructure/adapter/in/rest/FileController.java).  
   Принимает HTTP Multipart-запрос (`@RequestParam("file") MultipartFile file`), достает имя, байты и вызывает Входящий порт `UploadFileUseCase.upload(...)`.
2. **Исходящие адаптеры (Outbound Adapters) — реализуют интерфейсы-порты:**
   * [`LocalDiskFileStorageAdapter.java`](file:///c:/Users/alex-/OneDrive/Рабочий%20стол/homeWork/ITMO/ВС/Lab4/VS_lab4/file-service/src/main/java/portal/file/infrastructure/adapter/out/storage/LocalDiskFileStorageAdapter.java):
     ```java
     @Component
     public class LocalDiskFileStorageAdapter implements FileStoragePort {
         // Физически берет Files.write(...) и пишет байты в папку /app/uploads на диск!
     }
     ```
   * [`FileEventKafkaProducerAdapter.java`](file:///c:/Users/alex-/OneDrive/Рабочий%20стол/homeWork/ITMO/ВС/Lab4/VS_lab4/file-service/src/main/java/portal/file/infrastructure/adapter/out/kafka/FileEventKafkaProducerAdapter.java):
     ```java
     @Component
     public class FileEventKafkaProducerAdapter implements FileEventPort {
         private final KafkaTemplate<String, Object> kafkaTemplate;
         // Физически берет KafkaTemplate и шлет DTO в топик file-events!
     }
     ```
   * [`FileMetadataRepositoryAdapter.java`](file:///c:/Users/alex-/OneDrive/Рабочий%20стол/homeWork/ITMO/ВС/Lab4/VS_lab4/file-service/src/main/java/portal/file/infrastructure/adapter/out/persistence/FileMetadataRepositoryAdapter.java):
     Использует `@Entity` класс `FileMetadataJpaEntity`, вызывает `SpringDataFileMetadataRepository` и маппит результат в чистый доменный `StoredFile`.

---

## 4. Инверсия зависимостей (DIP) и концепция Ports & Adapters

Посмотрим, как Чистая Архитектура ломает привычный монолитный подход:

### В классической слоистой архитектуре (Layered):
```
[ Controller ] ──зависит от──► [ Service ] ──зависит от──► [ Repository (JPA / SQL) ]
```
Бизнес-логика напрямую завязана на Hibernate/SQL. Если поменять базу, переписывать придется сервис.

### В Чистой Архитектуре (Clean / Hexagonal):
```
Слой Application:
   [ FileApplicationService ] ──зависит от──► [ Интерфейс FileStoragePort ]
                                                      ▲
                                             реализует (implements)
                                                      │
Слой Infrastructure:
                      [ LocalDiskFileStorageAdapter ]
```
Интерфейс `FileStoragePort` объявлен **внутри** слоя Application. А внешняя реализация `LocalDiskFileStorageAdapter` находится **снаружи** в Infrastructure.
**Стрелка зависимости развернулась в обратную сторону!** Это буквальное применение буквы **D** из принципов SOLID: **Dependency Inversion Principle**.

---

## 5. Разбор сервиса `notification-service` по Чистой Архитектуре

Сервис уведомлений [notification-service](file:///c:/Users/alex-/OneDrive/Рабочий%20стол/homeWork/ITMO/ВС/Lab4/VS_lab4/notification-service/src/main/java/portal/notification) устроен аналогично:

1. **Domain:**
   * Модель [`Notification.java`](file:///c:/Users/alex-/OneDrive/Рабочий%20стол/homeWork/ITMO/ВС/Lab4/VS_lab4/notification-service/src/main/java/portal/notification/domain/model/Notification.java) (чистый класс без `@Entity`).
   * Перечисление `NotificationType` (`INFO`, `WARNING`, `SUCCESS`, `ALERT`).
2. **Application:**
   * Входящие порты: `CreateNotificationUseCase`, `GetNotificationsUseCase`, `MarkNotificationReadUseCase`.
   * Исходящий порт: `NotificationRepositoryPort`.
   * Сервис: `NotificationApplicationService`.
3. **Infrastructure:**
   * **Два входящих адаптера:**
     1. REST-контроллер: [`NotificationController.java`](file:///c:/Users/alex-/OneDrive/Рабочий%20стол/homeWork/ITMO/ВС/Lab4/VS_lab4/notification-service/src/main/java/portal/notification/infrastructure/adapter/in/rest/NotificationController.java) (когда фронтенд запрашивает уведомления).
     2. Kafka-слушатель: [`NotificationKafkaConsumer.java`](file:///c:/Users/alex-/OneDrive/Рабочий%20стол/homeWork/ITMO/ВС/Lab4/VS_lab4/notification-service/src/main/java/portal/notification/infrastructure/adapter/in/kafka/NotificationKafkaConsumer.java) (когда события прилетают из Kafka).
   * **Исходящий адаптер:**
     * `NotificationRepositoryAdapter.java` (сохраняет уведомления в PostgreSQL).

---

## 6. Зачем это нужно? (Плюсы vs Традиционный подход)

| Критерий | Классический Spring (Layered) | Чистая Архитектура (Clean Architecture) |
|---|---|---|
| **Привязка к БД** | Сервис завязан на `@Entity` и JPA-репозитории. | Сервис работает с доменными моделями через интерфейсы-порты. |
| **Замена технологий** | Замена диска на Amazon S3 сломает бизнес-код. | Пишется новый адаптер `S3StorageAdapter implements FileStoragePort`. **Бизнес-код не меняется ни на строчку!** |
| **Юнит-тестирование** | Нужен поднятый контекст Spring (`@SpringBootTest`, H2/Testcontainers). | Тесты пишутся за 5 миллисекунд с помощью Mockito: мокаются интерфейсы портов. Ноль Spring Boot, ноль БД. |
| **Сложность кодовой базы** | Меньше классов и файлов. | Больше интерфейсов и мапперов (Entity $\leftrightarrow$ Domain $\leftrightarrow$ DTO). |

> **Вывод:** Чистая Архитектура требует чуть больше шаблонного кода (порты, адаптеры, мапперы), но дает **абсолютную независимость и бессмертность бизнес-логики** при смене любых внешних библиотек и технологий.

---

## 7. Шпаргалка к защите (Вопросы преподавателя и готовые ответы)

### Вопрос 1: В чем главное отличие Clean Architecture от классической трехслойной (Controller $\to$ Service $\to$ Repository)?
> **Ответ:**  
> *«В классической архитектуре зависимости идут сквозным путем: Контроллер зависит от Сервиса, а Сервис напрямую зависит от базы данных (JPA Repository). В Чистой Архитектуре применен принцип инверсии зависимостей (DIP): ядро приложения (Domain и Use Cases) находится в центре и не зависит ни от чего. База данных, REST API и брокер Kafka являются лишь внешними адаптерами, которые подключаются к портам приложения»*.

### Вопрос 2: Покажите класс из слоя Domain. Почему в нем нет `@Entity`?
> **Ответ:**  
> *«Открываем `portal.file.domain.model.StoredFile`. В нем нет `@Entity`, потому что `@Entity` — это аннотация библиотеки Jakarta Persistence / Hibernate. Слой Domain должен быть чистым Java-классом (POJO) без зависимостей от сторонних фреймворков. Для сохранения в базу у нас в слое Infrastructure есть отдельный класс `FileMetadataJpaEntity`, куда маппится доменная модель»*.

### Вопрос 3: Что такое Порты (Ports) и Адаптеры (Adapters)?
> **Ответ:**  
> *«**Порт** — это Java-интерфейс, контракт взаимодействия. Входящие порты (Inbound) объявляют сценарии использования сервиса (`UploadFileUseCase`). Исходящие порты (Outbound) объявляют то, что сервису нужно во внешнем мире (`FileStoragePort`).  
> **Адаптер** — это конкретная реализация порта под определенную технологию. Например, `LocalDiskFileStorageAdapter` реализует порт `FileStoragePort` для сохранения файлов на локальный диск»*.

### Вопрос 4: Как протестировать `FileApplicationService` без базы данных и Kafka?
> **Ответ:**  
> *«Очень просто: в модульном тесте мы создаем моки (через `@Mock`) для интерфейсов `FileStoragePort`, `FileMetadataRepositoryPort` и `FileEventPort` и передаем их в конструктор `FileApplicationService`. Тест запускается за считанные миллисекунды без подъема Spring-контекста и Docker-контейнеров»*.
