# TRD — Marvin's Todo App 서버

> SDD 2단계(Plan) 산출물. 상태: **확정 (2026-11-11, Marvin-Oh)**

## 한눈에 보기 (사람용 요약)
> 승인은 이 부분만 읽고 한다. 아래 상세가 이 요약과 맞는지는 리뷰가 확인한다.

1. 자바 21과 스프링 부트 4.1.2로 만들고, 빌드는 그레이들 9.8.2를 쓴다. 이 버전들이 서로 맞는다는 것은 공식 문서로 확인했다.
2. 할 일은 서버 메모리에만 둔다. 서버를 끄면 모두 사라진다. 파일이나 데이터베이스에 저장하는 것은 이번 범위가 아니다.
3. 코드는 네 층으로 나눈다. 요청을 받는 층, 일을 처리하는 층, 할 일 규칙을 가진 층, 저장하는 층이다. 할 일 규칙은 스프링도 다른 층도 모른다. 이 층들을 이어 붙이는 설정 부분이 따로 있다.
4. "오늘"은 한국 시간 기준이다. 시계를 바꿔 끼울 수 있게 만들어서, 테스트에서는 날짜를 고정해 놓고 확인한다.
5. 이상한 요청은 너그럽게 받아 주지 않는다. 제목에 숫자를 넣거나, 모르는 항목을 보내거나, JSON이 아닌 형식이면 "요청 내용을 읽을 수 없다"고 돌려보낸다.
6. 마감일만 다르게 다룬다. 날짜가 아닌 값이 오면 "마감일을 올바른 날짜로 입력해 달라"고 알려 주고, 빈칸이면 마감일이 없는 것으로 본다.
7. 오류가 여러 개여도 하나만 알려 준다. 요청을 읽을 수 없으면 그것이 먼저, 없는 할 일이면 그다음, 나머지는 제목, 설명, 마감일 순서로 첫 번째 것만 알려 준다. 번호가 잘못된 것과 요청을 못 읽는 것이 한꺼번에 일어나면 어느 쪽이 나와도 된다.
8. 실패한 요청은 저장된 내용을 절대 바꾸지 않는다. 수정은 고친 사본을 만들어 검사한 뒤 통째로 바꾸고, 검사에서 걸리면 원래 값이 그대로 남는다.
9. 오류 응답은 언제나 같은 모양(오류 코드와 한국어 문구)이다. 예상하지 못한 오류도 내부 정보는 숨기고 같은 모양으로 답한다.
10. 명세에 없던 실패 상황 열 가지는 이 문서에서 정하지 않고 명세에 먼저 올려 승인받았다. 앞으로 명세에 없는 프레임워크 수준 오류는 기존 오류 중 하나로 보내고, 새 문구는 만들지 않는다.
11. 여러 요청이 동시에 와도 번호가 겹치거나 꼬이지 않게 만든다.
12. 새로 쓰는 라이브러리는 웹, 테스트(JUnit과 Mockito), 커버리지(JaCoCo)뿐이다. 입력 검사용 라이브러리는 쓰지 않고 직접 만든다.
13. 내 컴퓨터에서 실행할 때만 샘플 할 일을 미리 채운다. 기한 지남, 오늘 마감, 완료 같은 경우를 골고루 넣는다. 테스트에서는 채우지 않는다.
14. 모든 수용 기준을 테스트로 옮긴다. 테스트가 코드의 80% 이상을 지나가지 않으면 빌드가 실패한다.
15. 알려진 한계: 다른 주소에서 접속하는 화면(CORS)은 지원하지 않는다. 가족 이모지나 국기는 글자 수를 2자 이상으로 센다. 줄바꿈 없는 공백과 폭이 없는 공백은 공백으로 보지 않는다.

---

## 상세 (에이전트용)
> 근거 표기: `FR-xx`/`AC-xx-x`는 `docs/spec/REQUIREMENTS.md`, `D-xx`는 `docs/spec/DECISION_LOG.md`, `TD-xx`는 `docs/plan/DECISION_LOG.md`, `CLAUDE.md x.x`는 헌법.
> 충돌 시 명세가 우선한다 (CLAUDE.md 2.2.5). 명세에 없는 실패 상황은 이 문서에서 정하지 않고 명세 제안(D-32~D-35)으로 올려 승인받았다 (8장).
> 4장(도메인 모델)은 `DATA_MODEL.md`로, 5장(API)과 6장의 오류 코드 표는 `API_SPEC.md`로 옮겼다 (TD-28, TD-35, TD-46). 이 문서에는 결정 요약과 구현 정책(3장)만 남는다.

## 1. 기술 스택 (TD-01, TD-02, TD-12, TD-16, TD-17, TD-23, CLAUDE.md 4장)
| 항목 | 선택 | 이유 (한 줄) |
| --- | --- | --- |
| 언어 | Java 21 | 개발 환경으로 정해진 JDK(0단계). `java.time`으로 KST 날짜(D-10)와 `Clock` 주입(CLAUDE.md 2.3.4), `codePointCount`로 D-28 글자 수를 표준만으로 처리 (TD-01) |
| 프레임워크 | Spring Boot **4.1.2** (`spring-boot-starter-webmvc`) | Gradle 9.8.2를 공식 지원하는 현재 안정 버전이고 Java 21을 지원. Boot 4에서 `spring-boot-starter-web`은 `webmvc`로 대체되어 deprecated (TD-01, TD-23, TD-29) |
| JSON | Jackson 3 (`tools.jackson`, Boot 4 기본) | Boot 4가 기본으로 쓰는 JSON 라이브러리라 별도 의존성이 없음. Jackson 2는 쓰지 않는다 (TD-29) |
| 저장소 | 인메모리 | 사용자 1명, 로컬 동작 우선. 영속성은 이번 범위 밖 (TD-02, D-19) |
| 테스트 | JUnit 6 + Mockito (`spring-boot-starter-webmvc-test`) | `@WebMvcTest`가 `spring-boot-webmvc-test` 모듈로 분리되어 이 스타터가 필요하며, `spring-boot-starter-test`(JUnit, Mockito, AssertJ)를 함께 가져옴 (TD-12, TD-29). 승인됨 (2026-11-11) |
| 커버리지 | Gradle 내장 `jacoco` 플러그인, 라인 80% 이상 | CLAUDE.md 2.4.2의 기준을 빌드에서 강제하기 위함 (TD-16, 승인됨) |
| 빌드 | Gradle **9.8.2** (Kotlin DSL, wrapper) | 0단계에서 정한 개발 환경이며 Boot 4.1.2 지원 범위(8.14 이상 또는 9.x)에 들어감 (TD-17, TD-23) |

- **호환성 확인 (2026-11-11, 공식 문서):** Boot 4.1.2는 Java 17 이상(27까지)과 Gradle "8.x(8.14 이상) 및 9.x"를 지원한다. 반면 Boot 3.5.16은 Gradle "7.6.4 이상 또는 8.4 이상 8.x"만 적혀 있어 Gradle 9.8.2와 맞지 않는다. 그래서 초안의 "3.x"를 4.1.2로 확정했다 (TD-23).
- 입력 검증은 직접 구현한다. D-28(코드포인트 단위 글자 수)과 D-03/D-27(공백 제거)이 표준 어노테이션과 맞지 않아 `spring-boot-starter-validation`은 추가하지 않는다 (CLAUDE.md 2.1.5, 2.5.1).
- **Boot 4 이름 확인 (2026-11-11, Spring Boot 4.1.2 공식 문서와 4.0 마이그레이션 가이드):** `spring-boot-starter-web`은 `spring-boot-starter-webmvc`로 대체되어 deprecated이고, 테스트 스타터는 `spring-boot-starter-<기술>-test` 형식이며 `spring-boot-starter-webmvc-test`가 있다. `@WebMvcTest`는 `spring-boot-webmvc-test` 모듈에 있고, 공식 문서는 JUnit 6과 Jackson 3(`tools.jackson`, 3.1.7)을 기준으로 한다. `@MockBean`은 제거되어 `@MockitoBean`을 쓴다. 따라서 초안의 "JUnit 5"는 "JUnit 6"으로 고쳤다 (TD-29).
- 새 의존성 추가는 위 표가 전부다(테스트 스타터 승인됨). 더 필요하면 사람의 승인을 받는다 (CLAUDE.md 2.5.1).
- JaCoCo 검증은 `check`에 연결한다: `tasks.check { dependsOn(tasks.jacocoTestCoverageVerification) }`. `./gradlew build`만으로 라인 80% 미만이 실패한다 (TD-31).

## 2. 레이어 구조 (CLAUDE.md 2.3.1~2.3.5, TD-11, TD-26)
```
presentation ──▶ application ──▶ domain ◀── persistence
      └────────────────────────────▲
config (구성 루트: 모든 레이어를 조립, 어떤 레이어도 config를 참조하지 않음)
```
| 패키지 (`com.marvin.todo`) | 책임 | 근거 |
| --- | --- | --- |
| `presentation` | 컨트롤러, 요청/응답 DTO, 전역 오류 처리기 | FR-14, CLAUDE.md 2.3.3 |
| `application` | `TodoService`: 유스케이스 조합, 오늘 계산(`Clock`), `TodoView`·요약(`ListResult`) 생성 | FR-05, FR-06, TD-41 |
| `domain` | `Todo`, 입력 검증, 기한 지남 판정, `TodoRepository` 인터페이스, 도메인 예외 | FR-02, FR-10 |
| `persistence` | `InMemoryTodoRepository` | TD-02, CLAUDE.md 2.3.2 |
| `config` | `Clock` 빈, 샘플 데이터 `ApplicationRunner`, Jackson 설정 | TD-13, TD-21, CLAUDE.md 2.3.4 |

**참조 규칙 (TD-26):**
- `presentation`은 `application`과 `domain`을 참조할 수 있다. `Todo`는 응답 DTO로 변환할 때만 읽고, 도메인 예외는 오류 처리기에서 오류 코드로 바꿀 때만 쓴다. `persistence`는 참조하지 않는다.
- `application`은 `domain`만 참조한다. `domain`은 어디도 참조하지 않으며 Spring도 모른다 (CLAUDE.md 2.3.2).
- `persistence`는 `domain`만 참조한다.
- 순환 의존은 금지한다 (CLAUDE.md 2.3.5).
- 응답 DTO는 `Todo`와 별개 타입이다. 영속성 모델을 그대로 노출하지 않는다 (CLAUDE.md 2.3.3).
- `Clock`은 `Asia/Seoul` 고정 빈이고, 테스트에서는 `Clock.fixed`로 대체한다 (D-10, AC-10-6, CLAUDE.md 2.3.4).

## 3. 공통 정책

### 3.1 트랜잭션과 동시성 (TD-24)
- 서비스 메서드 하나가 작업 단위다. `@Transactional`은 쓰지 않는다 (인메모리라 트랜잭션 매니저가 없음, TD-02).
- `Todo`는 불변 객체다. 수정은 사본을 만들어 검증한 뒤 저장소에 교체한다. 검증이 실패하면 저장소를 건드리지 않으므로 원본이 바뀌지 않는다 (AC-08-3, AC-12-2, AC-14-2).
- `InMemoryTodoRepository`는 스레드에 안전하다. id는 `AtomicLong`으로 발급해 중복이 없고, 저장은 `ConcurrentSkipListMap<Long, Todo>`(id 순 = 등록 순서, D-11)에 한다. 읽고 고치는 동작(`update`)은 맵의 원자적 연산으로 한다.
- 저장소 인터페이스(메서드, id 발급, 삭제된 id 미재사용, 정렬 기준)의 계약은 [`DATA_MODEL.md` 6장](DATA_MODEL.md)이 기준이다 (TD-03, TD-35).
- 목록과 요약은 `findAll()` 한 번의 결과에서 함께 계산한다. 같은 응답 안에서 서로 어긋나지 않는다 (AC-05-2).

### 3.2 요청 읽기 정책 (TD-20, TD-21)
- 스칼라 강제 변환을 끈다. `title: 123`을 "123"으로, `completed: "true"`를 `true`로 받아들이지 않는다. 값의 종류가 맞지 않으면 `MALFORMED_REQUEST`다 (AC-14-3).
- 알 수 없는 필드가 있는 요청도 `MALFORMED_REQUEST`로 거부한다 (AC-14-3, CLAUDE.md 2.4.3).
- `title`이 없거나 `null`이면 비어 있는 제목으로 보고 `TITLE_REQUIRED`다 (AC-02-1). `description`이 문자열이 아니면 `MALFORMED_REQUEST`다.
- **`dueDate`는 요청 DTO에서 원본 JSON 값(`tools.jackson.databind.JsonNode`)으로 받는다.** `LocalDate`로 받으면 `"abc"`와 `2026-02-30`이 Jackson 단계에서 실패해 `MALFORMED_REQUEST`가 되어 AC-09-4가 깨진다. `String`으로 받는 것도 안 된다. 같은 절에서 강제 변환을 끄므로 `dueDate: 123`이 Jackson 단계에서 `MALFORMED_REQUEST`가 된다. 변환은 `presentation`의 매퍼가 한다.
  - 값이 없거나 `null`이면 마감일 없음으로 넘긴다.
  - 문자열이면 그 문자열을 `application`에 넘기고, 파싱은 `domain`이 한다. 앞뒤 공백을 제거해 비면(`""` 포함) 마감일 없음이고, 아니면 ISO `YYYY-MM-DD`로 파싱하며 실패하면 `INVALID_DUE_DATE`다 (D-38, AC-09-6).
  - 문자열이 아닌 값(`123`, `true`, 객체, 배열)은 JSON 원문 텍스트(예: `"123"`)를 문자열로 넘긴다. ISO 날짜 모양이 될 수 없으므로 도메인 파싱에서 `INVALID_DUE_DATE`가 되고, 검증 순서(제목→설명→마감일)도 그대로 지켜진다.
  - 이렇게 하면 `domain`과 `application`은 Jackson 타입을 모른다 (CLAUDE.md 2.3.2). 문자열이 아닌 `dueDate`를 날짜 오류로 보는 해석은 AC-09-4와 AC-14-3이 겹치는 부분이라 D-36으로 명세에 남긴다 (D-36, TD-20).
- `Content-Type`이 없거나 JSON이 아닌 요청(본문이 있는 추가·수정·완료 표시)과, JSON을 받을 수 없는 `Accept`(모든 요청)는 D-31 범위에 포함되어 `MALFORMED_REQUEST`(400)로 응답한다. 새 문구는 만들지 않는다. 오류 처리기가 명시적으로 변환하지 않으면 catch-all 처리기에서 500이 되므로 변환 규칙을 두고, 오류 응답은 `Accept`와 무관하게 항상 JSON으로 쓴다 (D-37, D-39, TD-30, TD-32, TD-43).
- 강제 변환을 끄는 구체적 설정 방법은 Boot 4.1.2의 Jackson 버전에서 확인한다. 테스트(9장 웹 슬라이스)가 `title: 123`과 `completed: "true"` 거부를 먼저 실패시켜 설정을 확정한다 (CLAUDE.md 2.1.2).
- `status` 값은 소문자 `all`, `active`, `done`만 허용한다. `ALL` 같은 대문자는 `INVALID_STATUS_FILTER`다 (AC-04-5, TD-27).
- **`status` 기본값 함정 (TD-50):** `@RequestParam(defaultValue = "all")`은 파라미터가 없을 때뿐 아니라 빈 문자열일 때도 기본값을 넣어 `status=`가 400이 아니라 `all`이 된다. `required = false`로 받고 **`null`일 때만 `all`**로 직접 처리한다. 반복 값(`status=active&status=done`)은 `"active,done"`으로 합쳐져 허용 값이 아니므로 자연히 거부된다. 웹 슬라이스 Red 테스트(`status=` → 400)로 확인한다.
- **`Accept` 검사와 오류 응답의 형식 (TD-51):** (1) `Accept`가 JSON이 아니면 컨트롤러가 실행되기 전에 걸러 낸다. 응답을 쓰는 시점에 검사하면 수정이 저장된 뒤에 400이 나가는 모순이 생긴다(AC-08-3). 매핑의 `produces = application/json`으로 걸러 내는 방식을 쓴다. (2) `Accept`가 `text/html`뿐이어도 오류 처리기는 응답의 `Content-Type`을 `application/json`으로 명시해 돌려준다. 그러면 콘텐츠 협상을 건너뛰어 JSON 본문을 쓸 수 있다. 협상에 맡기면 본문 없는 406이 날 수 있다. 두 동작 모두 웹 슬라이스 Red 테스트(`Accept: text/html` → 400 + JSON 본문, 저장 상태 불변)로 먼저 확인한다 (CLAUDE.md 2.1.2).

### 3.3 처리 순서 (TD-19, TD-22, D-33)
1. 경로의 id가 번호가 아니거나 1 미만이면 404 `TODO_NOT_FOUND` (D-33). 컨트롤러가 id를 받아 직접 검사한다.
2. 요청 본문 읽기 실패(깨진 JSON, 종류 불일치, 알 수 없는 필드, `completed` 누락, 지원하지 않는 Content-Type/Accept)는 400 `MALFORMED_REQUEST` (D-31, D-32, D-37).
3. id가 가리키는 할 일이 없으면 404 `TODO_NOT_FOUND` (FR-12).
4. 값 검증 오류(`TITLE_*`, `DESCRIPTION_TOO_LONG`, `INVALID_DUE_DATE`)는 첫 오류 하나만 돌려준다. 순서는 제목, 설명, 마감일이다.

**1번과 2번은 순서를 정하지 않는다 (TD-22, TD-30, TD-52).** id 형식 오류와 본문 읽기 실패가 동시에 나면 어느 쪽 오류가 나와도 된다. `Accept` 불일치가 id 형식 오류(1번)나 없는 id(3번)와 겹칠 때도 같다. 명세는 이 조합을 요구하지 않는다(AC-12-3은 본문이 정상인 경우만 다룬다). 그래서 파라미터 선언 순서나 `Converter`에 기대지 않고, 컨트롤러에서 검사한다. 테스트는 두 오류를 한 요청에 겹치지 않고 각각 따로 검증한다. 2번 이후의 순서는 지킨다.

"없는 id면 본문 검증보다 404가 먼저"라는 규칙은 **3번과 4번 사이, 즉 값 검증 오류에만** 적용된다. 읽기 실패(2번)는 컨트롤러에 닿기 전이나 DTO 단계에서 실패하므로 id 존재 여부(3번)보다 먼저 400이다 (TD-19).

## 4. 도메인 모델 (FR-01, FR-08, FR-09, TD-03, TD-28)
상세는 [`DATA_MODEL.md`](DATA_MODEL.md)가 기준이다. 요약만 남긴다.
- `Todo`: `id`(long), `title`, `description`(null 허용), `dueDate`(`LocalDate`, null 허용), `completed`. 불변 `record`이고 도메인 팩토리를 거쳐야만 만들어진다 (DATA_MODEL 1, 2).
- 공백 제거는 유니코드 공백(`strip()`), 글자 수는 코드포인트, 마감일 텍스트는 공백 제거 후 비면 없음, 아니면 엄격한 `YYYY-MM-DD` (D-03, D-27, D-28, TD-10, TD-33, TD-34).
- 기한 지남(`overdue`)과 요약은 저장하지 않고 요청마다 `Clock` 기준 오늘로 계산한다 (FR-05, FR-10, DATA_MODEL 3).
- id가 곧 등록 순서다 (D-11, FR-03).

## 5. API 설계 (TD-04~TD-07, TD-28)
호출 계약은 [`API_SPEC.md`](API_SPEC.md)가 기준이다. 요약만 남긴다.
- REST + JSON, 필드는 camelCase, 날짜는 `YYYY-MM-DD` 문자열 (TD-04).
- 엔드포인트 6개: `POST /todos`, `GET /todos`(목록 + 요약), `GET /todos/{id}`, `PUT /todos/{id}`(수정), `PATCH /todos/{id}`(완료 표시), `DELETE /todos/{id}` (FR-01~FR-13, TD-05, TD-06).
- 수정은 네 값을 모두 교체하고(TD-18), 완료 표시는 `completed` 하나만 받는다 (FR-06, FR-08).
- 요청 헤더 규칙(Content-Type은 본문 있는 요청, Accept는 모든 요청)은 TD-43, 쿼리·id 형식은 TD-44, TD-45에 있다.

## 6. 오류 응답 (FR-14, TD-08, TD-15, TD-46)
오류 코드, HTTP 상태, 확정 문구의 전체 표는 [`API_SPEC.md` 1.8](API_SPEC.md)이 기준이다. 형식은 다음과 같다 (AC-14-1):
```json
{ "error": { "code": "TITLE_REQUIRED", "message": "제목을 입력해 주세요." } }
```
- 이 표의 모든 코드는 명세(AC-14-3~6 포함)에 근거가 있다. 새 실패 상황이 생기면 코드와 문구를 정하기 전에 명세로 올린다 (CLAUDE.md 2.2.4).
- 실패한 요청은 저장 상태를 바꾸지 않는다 (AC-08-3, AC-12-2, AC-14-2). 검증은 저장 호출 전에 끝낸다.
- 오류를 삼키지 않는다 (CLAUDE.md 2.4.3). 처리되지 않은 예외는 `INTERNAL_ERROR`로 바꾸고 예외 메시지나 스택은 응답에 담지 않으며 서버 로그에 남긴다.
- **멈추는 규칙 (TD-32, D-37):** 명세에 없는 프레임워크 수준 오류(415, 406 등)는 위 코드 중 하나로 보내고, 새 문구는 만들지 않는다. 이 규칙으로 처리할 수 없는 새 실패 상황만 명세 변경(역류 규칙)으로 올린다.

## 7. 샘플 데이터 (TD-13)
- 로컬 실행에서만 샘플 할 일을 미리 채운다. `config`의 `local` 프로파일 전용 `ApplicationRunner`가 `TodoService`를 통해 넣는다.
- 마감일은 `Clock` 기준 오늘에 대한 상대 날짜다. 기한 지남, 오늘 마감, 미래, 마감 없음, 완료 항목을 골고루 포함한다 (FR-04, FR-10을 화면에서 확인하기 위함).
- 이 기능은 PRD에 없는 개발 편의 기능이다. 사람이 요청했으므로 포함하되 PRD 5.1 범위 밖임을 TD-13에 기록한다 (CLAUDE.md 2.2.1).
- Runner도 프로덕션 코드이므로 TDD 대상이다. 테스트는 9장에 있다.

## 8. 명세 역류 (CLAUDE.md 2.2.4)
**승인됨 (2026-11-11):** 알 수 없는 `status` → AC-04-5, D-30 / 읽을 수 없는 요청 내용 → AC-14-3, D-31.

**D-32~D-37 승인됨 (TRD 리뷰에서 발견, 이 문서가 정하지 않고 명세로 올림):**
| ID | 상황 | 제안 | 반영 위치 |
| --- | --- | --- | --- |
| D-32 | PUT/PATCH에서 `completed` 누락 | 400 `MALFORMED_REQUEST` | AC-06-5, AC-08-7 |
| D-33 | id가 번호가 아님 (`/todos/abc`, 0, 음수) | 404 `TODO_NOT_FOUND` | AC-12-3 |
| D-34 | 500 응답 | `INTERNAL_ERROR` + 확정 문구 | AC-14-4 |
| D-35 | 없는 경로(404), 허용되지 않은 메서드(405) | 같은 형식으로 응답, 문구 2개 | AC-14-5, AC-14-6 |
| D-36 | `dueDate`가 문자열이 아님 (`123`, `true` 등) | 400 `INVALID_DUE_DATE` (AC-09-4 해석 명시) | AC-09-4 |
| D-37 | Content-Type 누락/미지원(415), Accept 불일치(406) | 새 문구 없이 D-31 범위에 포함. 400 `MALFORMED_REQUEST` | AC-14-3 |

## 9. 테스트 전략 (CLAUDE.md 2.1, 2.4, TD-25)
각 AC를 테스트 하나 이상으로 옮기고 이름에 AC 번호를 넣는다 (PRD 6장). 종류별로 쓰는 도구를 하나로 정한다.

| 종류 | 도구 | 대상 | 확인하는 것 |
| --- | --- | --- | --- |
| 단위 (도메인) | JUnit 6만, Spring 없음 | `domain` | 제목/설명 검증, 글자 수(D-28), 공백 제거, 기한 지남 판정(FR-10, `Clock.fixed`) |
| 단위 (서비스) | JUnit 6 + Mockito | `application` | 유스케이스 흐름, 요약 계산, 실패 시 저장소 미호출. `TodoRepository`만 목(mock)으로 대체 |
| 단위 (저장소) | JUnit 6 | `persistence` | 등록 순서, id 중복 없음(여러 스레드 생성), 삭제된 id 미재사용, 원자적 수정 |
| 웹 슬라이스 | `@WebMvcTest` + `@MockitoBean TodoService` | `presentation` | 경로/메서드/상태 코드, DTO 변환, 오류 처리기의 코드와 정확한 문구, 요청 읽기 정책(3.2: 강제 변환 거부, 알 수 없는 필드, `dueDate` 파싱 위치) |
| 통합 | `@SpringBootTest` + `MockMvc` (모킹 없음) | 전체 | 빈 연결, `Clock` 빈(KST), 전역 오류 처리기, Jackson 설정, 대표 시나리오(추가→수정→완료→삭제→목록/요약), `INTERNAL_ERROR`, 404/405 |

- **Clock:** Mockito로 대체하지 않고 `Clock.fixed`를 쓴다. 통합 테스트는 `@TestConfiguration`의 `@Primary` 고정 `Clock`으로 바꾼다. AC-10-6은 UTC 날짜와 KST 날짜가 다른 시각으로 검증한다.
- **테스트 독립성 (CLAUDE.md 2.4.1):** 단위 테스트는 테스트마다 새 저장소를 만든다. 통합 테스트는 컨텍스트 캐시 때문에 저장소 싱글턴이 공유되므로 클래스에 `@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)`를 붙여 테스트마다 컨텍스트와 저장소를 새로 만든다. 저장소에 테스트 전용 초기화 메서드를 만들지 않는다(프로덕션 코드 오염 방지). 통합 테스트 수가 적어 비용이 작다.
- **샘플 데이터 Runner (TD-13):**
  - 단위: `TodoService` 목 + `Clock.fixed`로 만들어진 항목의 개수, 상대 마감일, 완료 항목 포함 여부를 검증한다.
  - 통합: `local` 프로파일을 켠 컨텍스트에서 샘플이 채워지는지, 프로파일이 없을 때는 비어 있는지를 검증한다. 두 번째가 다른 테스트에서 샘플이 섞이지 않음을 보장한다.
- AC-06-5, AC-08-7, AC-12-3, AC-14-3(415/406), AC-14-4~6, AC-09-4(문자열이 아닌 dueDate), AC-09-6(빈 마감일)도 같은 방식으로 테스트한다. 웹 슬라이스에서 코드와 정확한 문구를, 통합 테스트에서 실제 404/405/500 응답을 확인한다.
- 커버리지는 JaCoCo로 측정한다. `jacocoTestCoverageVerification`(라인 80% 미만이면 실패)을 `check`에 연결하므로 `./gradlew build`만으로 기준이 지켜진다 (CLAUDE.md 2.4.2, TD-31).

## 10. FR 추적표
| FR | 구현 위치 |
| --- | --- |
| FR-01 | `POST /todos`, `TodoService.create` |
| FR-02 | `domain` 입력 검증 (제목/설명) |
| FR-03, FR-04, FR-13 | `GET /todos`, `status` 필터, 저장소의 id 순 조회 |
| FR-05 | `TodoService`의 요약 계산 |
| FR-06 | `PATCH /todos/{id}` |
| FR-07 | `GET /todos/{id}` |
| FR-08, FR-09 | `PUT /todos/{id}`, 날짜 직접 파싱(3.2) |
| FR-10 | `domain` 기한 지남 판정 + `Clock` |
| FR-11, FR-12 | `DELETE`, `TODO_NOT_FOUND` 공통 처리 |
| FR-14 | `presentation` 전역 오류 처리기 |
