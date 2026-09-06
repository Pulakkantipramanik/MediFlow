# MediFlow Project Context

## 1. Project Overview

Project name: MediFlow
Type: Spring Boot + MySQL e-pharmacy / medicine backend
Architecture: Modular Monolith + Layered Architecture

Main flow:
Controller → Service → Repository → MySQL

Main modules:
- User
- Medicine
- Prescription
- Order
- Payment
- Audit
- SecurityConfig
- Global exception handling/config

Technology:
- Spring Boot 4.0.8
- Java 17
- Spring WebMVC
- Spring Data JPA
- Spring Security
- MySQL
- JWT
- BCrypt
- Bean Validation
- Swagger/OpenAPI
- JUnit + Mockito
- Maven

## 2. User Preferences

- Explain implementation in simple Bengali/English mixed language.
- Code must be in English.
- Prefer exact package/class/file names.
- Explain why business logic is used.
- When adding/modifying code, put short, simple single-line `//` comments above important business logic, methods, and annotations/implementation.
- Do NOT use long multi-line `//` WHY comments.
- Do not silently change business logic. If business logic changes, explicitly explain it.
- Preserve the existing project structure and naming.
- When code is needed, ask for the specific current class instead of guessing.
- User likes implementing incrementally and then testing with Postman.
- User also wants interview-oriented explanations.
- User may pause implementation to study existing code deeply.

## 3. Security

SecurityConfig:
- `@EnableMethodSecurity`
- Public endpoints:
  - `/api/users/register`
  - `/api/users/login`
  - Swagger/OpenAPI paths
- All other endpoints require authentication.
- JWT filter is added before `UsernamePasswordAuthenticationFilter`.
- Custom 401 handler: `CustomAuthenticationEntryPoint`
- Custom 403 handler: `CustomAccessDeniedHandler`

JWT flow:
1. Client sends `Authorization: Bearer <token>`
2. `JwtAuthenticationFilter` reads token
3. `JwtService` validates token and extracts email
4. User is loaded from `UserRepository`
5. `UsernamePasswordAuthenticationToken` is created
6. Role is stored as `SimpleGrantedAuthority(user.getRole())`
7. Authentication is stored in `SecurityContext`

Roles:
- `ROLE_USER`
- `ROLE_ADMIN`

Authorization:
- `@PreAuthorize("hasRole('ADMIN')")`

Important concepts:
- Authentication = who are you?
- Authorization = what can you do?
- 401 = unauthenticated / invalid authentication
- 403 = authenticated but insufficient permission

JWT secret:
- `application.properties`: `jwt.secret=${JWT_SECRET}`
- `JwtService` uses `@Value("${jwt.secret}")`
- IntelliJ environment variable: `JWT_SECRET`

DB password:
- `spring.datasource.password=${DB_PASSWORD}`
- IntelliJ environment variable: `DB_PASSWORD=root`

Webhook secret:
- `payment.webhook.secret=${PAYMENT_WEBHOOK_SECRET}`
- IntelliJ environment variable: `PAYMENT_WEBHOOK_SECRET=mediflow-webhook-secret-2026`

## 4. Application Properties

Known current properties:

spring.application.name=medicine-app
spring.datasource.url=jdbc:mysql://localhost:3306/mediflow
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

prescription.upload-dir=E:/Project2026/uploads/prescriptions
payment.webhook.secret=${PAYMENT_WEBHOOK_SECRET}
jwt.secret=${JWT_SECRET}

logging.level.root=INFO
logging.level.com.mediflow=DEBUG

## 5. Application Entry

```java
@SpringBootApplication(scanBasePackages = "com.mediflow")
@EntityScan("com.mediflow")
@EnableJpaRepositories(basePackages = "com.mediflow")
public class MedicineAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(MedicineAppApplication.class, args);
    }
}
```

## 6. User Module

Entity:
```java
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;
    private String role;
}
```

Repository:
```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

Controller:
- POST `/api/users/register`
- POST `/api/users/login`

Registration:
- Checks email exists
- BCrypt encodes password
- Default role is `ROLE_USER`

Admin creation is currently done by registration then SQL:
```sql
UPDATE users SET role = 'ROLE_ADMIN' WHERE email = 'admin@mediflow.com';
```

Known test credentials:
- User: `pulak@gmail.com` / `password123`
- Admin: `admin@mediflow.com` / `admin123`

## 7. Medicine Module

Medicine entity includes:
- id
- name
- description
- category
- manufacturer
- price (`BigDecimal`)
- stockQuantity (`Integer`)
- expiryDate (`LocalDate`)
- prescriptionRequired (`Boolean`)
- status (`MedicineStatus`)
- createdAt
- updatedAt
- `@Version Long version`

Boolean getter:
- `getPrescriptionRequired()`

Features:
- CRUD
- DTOs
- validation
- custom exceptions
- pagination
- sorting
- search
- dynamic filtering
- optimistic locking

Optimistic locking:
- `@Version`
- Intended to protect concurrent stock updates.

Potential global handler improvement:
- `ObjectOptimisticLockingFailureException` may need handling if Spring wraps optimistic lock exceptions.

## 8. Prescription Module

Structure:
```text
com.mediflow.prescription
├── entity
│   ├── Prescription.java
│   └── PrescriptionStatus.java
├── repository
│   └── PrescriptionRepository.java
├── dto
│   ├── PrescriptionRequestDto.java
│   ├── PrescriptionResponseDto.java
│   └── PrescriptionRejectRequestDto.java
├── service
│   └── PrescriptionService.java
└── controller
    └── PrescriptionController.java
```

Entity fields:
- id
- userEmail
- medicineId
- prescriptionFileName
- status
- rejectionReason
- uploadedAt

Statuses:
- PENDING
- APPROVED
- REJECTED

Features:
- Multipart upload
- Local file storage
- DB stores UUID filename
- Admin pending/approve/reject
- User own prescriptions/file access
- Audit actions

Upload directory:
`E:/Project2026/uploads/prescriptions`

Known cleanup points:
- `PrescriptionService` has 2-arg and 3-arg file methods; controller uses 3-arg.
- 2-arg method can be removed later.
- 3-arg method currently uses `IllegalArgumentException` for not found; consistency improvement would be `PrescriptionNotFoundException`.
- `MedicineRepository` is injected but currently unused in upload; later validate medicine existence or remove dependency.
- `Objects.equals(file, null)` can later become `file == null`.

## 9. Order Module

Endpoints:
- POST `/api/orders`
- GET `/api/orders/my-orders`
- PATCH `/api/orders/{id}/cancel`
- ADMIN GET `/api/orders`
- ADMIN PATCH `/api/orders/{id}/approve`
- ADMIN PATCH `/api/orders/{id}/ship`
- ADMIN PATCH `/api/orders/{id}/deliver`
- ADMIN PATCH `/api/orders/{id}/reject`

Repository requirement:
```java
List<Order> findByUserEmail(String userEmail);
```

Statuses:
- PENDING
- APPROVED
- PROCESSING
- SHIPPED
- DELIVERED
- REJECTED
- CANCELLED

Strict transitions:
- PENDING → APPROVED
- PENDING → REJECTED
- PENDING → CANCELLED
- APPROVED → PROCESSING after successful payment webhook
- PROCESSING → SHIPPED
- SHIPPED → DELIVERED

Create order:
- validates medicine
- checks approved prescription if required
- checks stock
- calculates total
- reduces stock
- saves PENDING
- creates audit
- transactional

Audit actions:
- ORDER_CREATED
- ORDER_APPROVED
- ORDER_REJECTED
- ORDER_CANCELLED
- ORDER_SHIPPED
- ORDER_DELIVERED

Known fixes:
- shipOrder had a bug and was fixed by setting SHIPPED before save.
- restoreMedicineStock should use `MedicineNotFoundException`.

## 10. Payment Module

Structure:
```text
com.mediflow.payment
├── entity
│   ├── Payment.java
│   └── PaymentStatus.java
├── repository
│   └── PaymentRepository.java
├── dto
│   ├── PaymentRequestDto.java
│   ├── PaymentResponseDto.java
│   └── PaymentWebhookRequestDto.java
├── service
│   └── PaymentService.java
└── controller
    └── PaymentController.java
```

Payment statuses:
- PENDING
- SUCCESS
- FAILED

Payment entity:
```java
@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long orderId;

    private String userEmail;
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String transactionId;
    private LocalDateTime paymentDate;

    @Column(unique = true, nullable = false)
    private String idempotencyKey;
}
```

Repository:
```java
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);
    List<Payment> findByUserEmail(String userEmail);
    List<Payment> findByStatus(PaymentStatus status);
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
```

PaymentRequestDto:
- orderId `@NotNull`
- idempotencyKey `@NotBlank`

PaymentResponseDto:
- id
- orderId
- userEmail
- amount
- status
- transactionId
- paymentDate
- idempotencyKey

PaymentWebhookRequestDto:
- paymentId `@NotNull`
- status `@NotNull`
- transactionId `@NotBlank`

Webhook:
- POST `/api/payments/webhook`
- header `X-Webhook-Secret`
- body paymentId/status/transactionId
- webhook should NOT use `@PreAuthorize`
- manual success/failed endpoints should be ADMIN-only

Current PaymentService business logic:

### createPayment(request, userEmail)
Flow:
1. Check idempotency key
2. If existing:
   - verify same user
   - verify same order
   - return existing payment
3. Find order
4. Verify authenticated user owns order
5. Only APPROVED order can be paid
6. Only one payment per order
7. Amount comes from order totalPrice, not client
8. Create PENDING payment
9. Set paymentDate
10. Save
11. Audit `PAYMENT_CREATED`
12. Return DTO

### markPaymentSuccess(paymentId)
- Find payment
- Only PENDING allowed
- Generate `TXN-<timestamp>`
- Set SUCCESS
- Save
- Audit `PAYMENT_SUCCESS`

### markPaymentFailed(paymentId)
- Find payment
- Only PENDING allowed
- Set FAILED
- Save
- Audit `PAYMENT_FAILED`

### processWebhook(request)
- Find payment
- Only PENDING allowed
- Webhook status must be SUCCESS or FAILED
- Set transactionId/status
- On SUCCESS:
  - find order
  - only APPROVED order can move to PROCESSING
  - set PROCESSING
  - save order
- Save payment
- Audit `PAYMENT_WEBHOOK_PROCESSED`

### Read/Admin methods
- `getMyPayments(userEmail)`
- `getPaymentById(paymentId, userEmail)`
- `getAllPayments(pageable)`
- `getPaymentsByStatus(status, pageable)`

Important known point:
- `processWebhook()` currently throws if final payment receives another webhook after status is no longer PENDING. Robust repeated-webhook idempotency could be improved later, but do not silently change business logic.

Current PaymentService source has some long multi-line `// PURPOSE` / `// WHY` comments. User wants these eventually changed to short single-line comments.

## 11. Audit Module

AuditLog fields:
- id
- userEmail
- action
- entityType
- entityId
- description
- createdAt

Repository:
- `findByUserEmail`
- `findByEntityTypeAndEntityId`

Service:
- `log(...)`
- `getEntityAuditLogs(...)`

Admin GET endpoints:
- `/api/audit/orders/{orderId}`
- `/api/audit/prescriptions/{prescriptionId}`
- `/api/audit/payments/{paymentId}`

Completed and verified.

## 12. Global Exception Handling

ErrorResponse:
- status
- message
- timestamp
- errors

Handlers include:
- MedicineNotFoundException → 404
- OrderNotFoundException → 404
- PrescriptionNotFoundException → 404
- PaymentNotFoundException → 404
- MethodArgumentNotValidException → 400 with field map
- IllegalArgumentException → 400
- OptimisticLockException → 409

Potential addition:
- ObjectOptimisticLockingFailureException → 409

## 13. Logging

Properties:
```properties
logging.level.root=INFO
logging.level.com.mediflow=DEBUG
```

PaymentService uses:
```java
private static final Logger log =
        LoggerFactory.getLogger(PaymentService.class);
```

Logs cover:
- payment creation
- success
- failure
- webhook
- warnings for invalid state

Never log:
- passwords
- JWT
- webhook secret
- card details
- other sensitive payment data

Do not add artificial try/catch just to use `log.error`.

## 14. Swagger/OpenAPI

Dependency:
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>3.1.0</version>
</dependency>
```

SecurityConfig permits:
- `/swagger-ui/**`
- `/swagger-ui.html`
- `/v3/api-docs/**`

OpenApiConfig:
- API title: MediFlow API
- version: 1.0
- bearer JWT security scheme
- global bearer security requirement

Swagger:
- UI: `/swagger-ui/index.html`
- JSON: `/v3/api-docs`
- YAML: `/v3/api-docs.yaml`

Public endpoints are documented with:
```java
@SecurityRequirements
```
on:
- register
- login
- webhook

Swagger generation was confirmed working.

## 15. Unit Testing Status

Current test class:
`src/test/java/com/mediflow/payment/service/PaymentServiceTest.java`

Uses:
- `@ExtendWith(MockitoExtension.class)`
- `@Mock`
- `@InjectMocks`
- Mockito `when`, `verify`, `never`
- JUnit `assertEquals`, `assertNotNull`, `assertThrows`

Currently implemented tests:
1. `markPaymentSuccess_shouldChangePendingPaymentToSuccess`
2. `markPaymentSuccess_shouldThrowException_whenPaymentIsNotPending`
3. `markPaymentSuccess_shouldThrowException_whenPaymentNotFound`
4. `markPaymentFailed_shouldChangePendingPaymentToFailed`
5. `markPaymentFailed_shouldThrowException_whenPaymentIsNotPending`
6. `createPayment_shouldReturnExistingPayment_whenIdempotencyKeyAlreadyExists`

Important current correction:
This assertion currently has an extra duplicate third argument and should be:
```java
// Check the error message.
assertEquals(
        "Only PENDING payment can be marked as FAILED",
        exception.getMessage()
);
```

Potential import issue:
Current test imports:
`com.mediflow.medicine.exception.PaymentNotFoundException`
This should only be changed if the actual exception class is located elsewhere; verify the real package before changing.

Remaining PaymentService unit test candidates:
- createPayment valid new payment
- idempotency key belongs to another user
- idempotency key belongs to another order
- order not found
- user does not own order
- order not APPROVED
- payment already exists for order
- webhook SUCCESS
- webhook FAILED
- webhook payment not found
- webhook payment already final
- webhook invalid status
- webhook order not found
- webhook order not APPROVED
- getMyPayments
- getPaymentById owner
- getPaymentById unauthorized
- getPaymentById not found
- getAllPayments pagination
- getPaymentsByStatus pagination

Unit testing is currently intentionally paused so implementation can continue.

## 16. Integration Testing — Current Position

Roadmap moved to Integration Testing after pausing Unit Testing.

Planned first integration test:
- User Registration
- User Login
- Protected API with JWT

Expected integration flow:
Client / MockMvc
→ Controller
→ Service
→ Repository
→ test database

Current `pom.xml` already contains:
- `spring-boot-starter-data-jpa-test` test scope
- `spring-boot-starter-validation-test` test scope
- `spring-boot-starter-webmvc-test` test scope

No extra JUnit/Mockito dependency is currently required just to begin.

Current `pom.xml` has duplicate:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```
This appears twice; later clean one duplicate.

Next exact integration-test step:
Need the current versions of:
- `UserController.java`
- `UserRequestDto.java`

Then create:
`src/test/java/com/mediflow/user/controller/UserControllerIntegrationTest.java`

Use exact current endpoint, request fields, validation, and response shape rather than guessing.

## 17. Roadmap

```text
1. Prescription Service/Controller       ✅
2. Prescription Audit                    ✅
3. PaymentService + Webhook               ✅
4. Audit GET APIs                         ✅
5. Logging                                ✅
6. Swagger/OpenAPI                        ✅
7. Security/Config Cleanup                ✅
8. Unit Testing                            ⏸️ Paused
9. Integration Testing                     ▶️ Current
10. Docker Compose                         ⏭️
11. CI/CD                                  ⏭️
12. AWS Deployment                         ⏭️
```

## 18. Interview Architecture Summary

Interview summary:
“MediFlow is a Spring Boot based modular-monolith e-pharmacy backend. It provides JWT-based authentication and role-based authorization, medicine management, prescription verification, order lifecycle management, idempotent payment processing with webhook support, audit logging, pagination and optimistic locking for concurrent stock updates. The application follows a layered architecture with Controller, Service and Repository layers and uses MySQL for persistence.”

Key interview concepts:
- Modular Monolith
- Layered Architecture
- DTO
- Repository Pattern
- Dependency Injection / IoC
- Spring Security Filter Chain
- JWT + BCrypt + RBAC
- Transaction management
- State transition / lifecycle
- Idempotency
- Webhook
- Audit trail
- Pagination
- Optimistic locking
- Local file storage
- Future scalability

## 19. Important Working Rule for Future Chats

When continuing this project:
- Treat this document as the current project context.
- Continue from the roadmap position.
- Do not redo completed modules unless needed.
- Ask only for exact current class/code when implementation depends on details not present here.
- Preserve business logic unless explicitly asked to change it.
