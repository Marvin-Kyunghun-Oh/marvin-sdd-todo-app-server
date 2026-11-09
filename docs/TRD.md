# TRD — Technical Requirements Document

> 상태: **초안** (사람의 검토·승인 전)
> 이 문서는 Constitution 단계의 산출물이며, 모든 하위 문서와 코드의 기준이다.
> 소유: 사람. 변경은 사람이 요청한 경우에만 한다.

## 1. 개요

Marvin's Todo 앱의 서버. Java + Spring Boot 기반 REST API 서버이며 SDD + TDD 방식으로 개발한다.
이 문서는 기술 스택, 아키텍처, 코딩·테스트 규칙, 개발 프로세스를 정의한다. 제품 요구사항은 `PRD.md`, `REQUIREMENTS.md`에서 다룬다.

## 2. 기술 스택

| 항목 | 결정 |
|---|---|
| 언어 | Java 17 |
| 프레임워크 | Spring Boot 3.x |
| 빌드 도구 | Gradle |
| 데이터 접근 | Spring Data JPA |
| 데이터베이스 | H2 (인메모리) |
| 테스트 | JUnit 5, AssertJ, MockMvc, Mockito(Spring Boot Starter Test 포함) |
| 기본 패키지 | `com.marvin.todo` |

### 허용 의존성

- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-validation`
- `com.h2database:h2`
- `lombok`
- `spring-boot-starter-test`

위 목록에 없는 의존성(예: Flyway, JaCoCo, Spring Security, Swagger)은 임의로 추가하지 않는다. 필요하면 멈추고 보고한다.

## 3. 아키텍처

### 3.1 계층 구조

```
Controller → Service → Repository
```

- 의존 방향은 한 방향이다. 하위 계층은 상위 계층을 참조하지 않는다.
- Controller: 요청 매핑, 입력 검증(Bean Validation), 응답 변환. 비즈니스 로직을 두지 않는다.
- Service: 비즈니스 로직과 트랜잭션 경계. 계층 간 변환(Entity ↔ DTO)을 담당한다.
- Repository: Spring Data JPA 인터페이스. 데이터 접근만 담당한다.
- Entity를 API 응답·요청으로 직접 노출하지 않는다. 요청/응답용 DTO를 별도로 둔다.

### 3.2 패키지 구조

도메인 단위로 나누고, 도메인 내부에서 계층별로 하위 패키지를 둔다.

```
com.marvin.todo
├── TodoApplication.java
├── common
│   ├── error        # 공통 에러 응답, 예외 핸들러
│   └── ...          # 도메인 공통 코드
└── <domain>         # 예: todo
    ├── controller
    ├── service
    ├── repository
    ├── domain       # Entity
    └── dto
```

실제 도메인 구성은 `DATA_MODEL.md`에서 정한다.

## 4. API 규칙

- REST + JSON. 요청·응답의 Content-Type은 `application/json`이다.
- 엔드포인트, 요청/응답 필드, 에러 코드 목록은 `API_SPEC.md`에서 정의한다.
- 인증/인가는 없다. 모든 엔드포인트는 인증 없이 접근 가능하다.
- HTTP 상태 코드는 의미에 맞게 사용한다. (성공 2xx, 입력 오류 4xx, 서버 오류 5xx)

### 4.1 공통 에러 응답

모든 에러 응답은 아래 형식으로 통일한다.

```json
{
  "code": "ERROR_CODE",
  "message": "사람이 읽을 수 있는 메시지",
  "details": [
    { "field": "title", "reason": "must not be blank" }
  ]
}
```

- `code`: 에러를 식별하는 문자열. 개별 코드는 `API_SPEC.md`에서 정의한다.
- `message`: 에러 요약 메시지.
- `details`: 필드 단위 오류 등 부가 정보. 없으면 빈 배열이다.
- 에러 변환은 `@RestControllerAdvice` 한 곳에서 처리한다. Controller에서 개별적으로 에러 응답을 만들지 않는다.
- 스택 트레이스 등 내부 정보를 응답에 노출하지 않는다.

## 5. 데이터 규칙

- H2 인메모리 모드를 사용한다. 애플리케이션 재시작 시 데이터는 초기화된다.
- 스키마는 JPA가 관리한다: `spring.jpa.hibernate.ddl-auto=create-drop`.
- 마이그레이션 도구(Flyway, Liquibase)는 사용하지 않는다.
- 엔티티, 필드, 제약 조건의 상세는 `DATA_MODEL.md`에서 정의한다.
- Entity에는 `@Setter`를 두지 않는다. 상태 변경은 의도가 드러나는 메서드로 한다.
- 트랜잭션은 Service 계층에서 선언한다. 조회 전용 메서드는 `@Transactional(readOnly = true)`를 사용한다.

## 6. 코딩 규칙

- 클래스명은 PascalCase, 메서드·변수명은 camelCase, 상수는 UPPER_SNAKE_CASE.
- 계층별 클래스 접미사: `*Controller`, `*Service`, `*Repository`, DTO는 용도가 드러나는 이름(예: `*Request`, `*Response`).
- 의존성 주입은 생성자 주입을 사용한다. 필드 주입(`@Autowired` 필드)은 사용하지 않는다.
- Lombok 사용을 허용한다. 다만 Entity에서는 `@Data`, `@Setter`를 사용하지 않는다.
- 입력 검증은 Bean Validation 애너테이션으로 DTO에 선언한다.
- 비즈니스 예외는 전용 예외 클래스로 표현하고, 공통 예외 핸들러가 에러 응답으로 변환한다.
- 주석은 "왜"가 필요한 곳에만 작성한다. 코드로 드러나는 내용은 주석으로 반복하지 않는다.

## 7. 테스트 규칙

- 프레임워크: JUnit 5 + AssertJ. 단언은 AssertJ(`assertThat`)를 사용한다.
- 계층별 테스트 방식:

| 계층 | 방식 |
|---|---|
| Service | 단위 테스트. 협력 객체는 Mockito로 대체 |
| Repository | `@DataJpaTest` |
| Controller | `@WebMvcTest` + MockMvc. Service는 목으로 대체 |

- 테스트 메서드는 무엇을 검증하는지 드러나게 이름 짓고, Given-When-Then 구조로 작성한다.
- 테스트는 서로 독립적이어야 하며 실행 순서에 의존하지 않는다.
- 테스트 케이스(TC-xxx)는 `TEST_CASES.md`에서 REQ와 매핑해 관리한다. 구현 테스트는 해당 TC를 따른다.
- 커버리지 수치 목표는 정하지 않는다. 대신 `TEST_CASES.md`의 모든 TC가 테스트로 구현되어야 한다.

## 8. 개발 프로세스

### 8.1 SDD 단계

```
Constitution → Specify → Plan → Tasks → Implement → Validate
```

각 단계의 산출물은 사람의 승인을 받은 뒤 다음 단계로 넘어간다. 지시받은 단계의 범위만 수행한다.

### 8.2 TDD 절차

작업(T-xxx) 하나당 아래 순서를 따른다.

1. **Red**: `TEST_CASES.md`의 해당 TC를 실패하는 테스트로 먼저 작성하고, 실패를 확인한다.
2. **Green**: 테스트를 통과시키는 최소한의 코드를 작성한다.
3. **Refactor**: 테스트가 통과한 상태를 유지하며 코드를 정리한다.
4. 전체 테스트(`./gradlew test`)를 실행해 기존 테스트가 깨지지 않았는지 확인한다.

### 8.3 규칙

- 테스트 없이 구현 코드를 먼저 작성하지 않는다.
- 기존 테스트를 수정·삭제해야 한다고 판단되면 직접 하지 않고 멈춰서 보고한다.
- 작업은 `BACKEND_TASKS.md`의 항목을 한 번에 하나씩 진행한다.
- 해당 작업의 테스트가 모두 통과하면 체크박스를 체크한다.

### 8.4 완료 기준

- 해당 작업에 매핑된 TC가 모두 테스트로 구현되고 통과한다.
- 전체 테스트가 통과한다.
- 작업 범위를 벗어난 변경이 없다.
- 실행한 테스트 결과와 변경한 파일을 보고한다.

### 8.5 멈추고 보고해야 하는 경우

- 문서끼리 충돌하거나, 명세가 모호하거나, 필요한 내용이 명세에 없을 때
- 명세가 틀렸다고 판단될 때 (직접 고치지 않는다)
- 기존 테스트를 수정하거나 삭제해야 한다고 판단될 때
- TRD에 없는 의존성을 추가해야 할 때
- 현재 작업 범위를 넘는 변경이 필요할 때

## 9. 비범위

- 인증/인가
- DB 마이그레이션 도구
- 커버리지 수치 목표 및 관련 도구
- 영속 데이터베이스 (H2 인메모리 외)

## 10. 열린 항목

- Spring Boot 3.x의 구체 버전과 Gradle 래퍼 버전은 프로젝트 생성 시 확정한다.
- 빌드·실행 명령어는 프로젝트 생성 후 `CLAUDE.md`에 갱신한다.
