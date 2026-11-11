# DECISION_LOG — SDD 2단계 (Plan)

> 기술 계획 단계의 결정을 기록한다. 1단계 결정(D-xx)은 `docs/spec/DECISION_LOG.md`에 있고, 충돌하지 않도록 이 문서는 `TD-xx`를 쓴다.
> 결정일: 2026-11-11 / 결정자: Marvin-Oh. "리뷰 반영" 표시는 TRD 리뷰 피드백에 따라 반영한 항목이며 TRD 확정(2026-11-11)과 함께 승인되었다.

| ID | 질문 | 결정 | 이유 | 반영 위치 |
| --- | --- | --- | --- | --- |
| TD-01 | 언어와 프레임워크 | Java 21 + Spring Boot **4.1.2** (리뷰 반영: 3.x에서 정확한 버전으로 확정) | Java 21은 0단계에서 정한 JDK이고 `java.time`/`Clock`/`codePointCount`로 D-10, D-28을 표준만으로 처리. Boot 4.1.2는 Gradle 9.8.2를 공식 지원하고 Java 17~27을 지원 | TRD 1 |
| TD-02 | 저장소 | 인메모리. 기능 구현이 우선이고 로컬에서 동작하면 충분 | 사용자 1명(D-19), 영속성은 이번 범위 밖 | TRD 1, 3.1 |
| TD-03 | 할 일 식별자 (D-26 위임) | 자동 증가 정수, 삭제된 id는 재사용하지 않음 | 단순하고 id 순서가 곧 등록 순서(D-11) | TRD 4 |
| TD-04 | API 스타일 | REST + JSON, camelCase (추천 수락) | 화면이 쓰기 쉽고 동작이 자원에 대응 | TRD 5 |
| TD-05 | 요약 위치 | 목록 응답에 `summary`를 함께 담음 | 요청 한 번으로 목록과 "N개 남음"을 채움 | TRD 5 |
| TD-06 | 수정과 완료 표시 | `PUT`(수정) + `PATCH {completed}`(완료 표시) 분리 (추천 수락) | "필드 없음"과 "삭제(null)"를 구분할 필요가 없음 | TRD 5 |
| TD-07 | 날짜 형식과 오류 범위 | ISO `YYYY-MM-DD`. 존재하지 않는 날짜도 날짜 오류 | 명세 예시와 일치 | TRD 3.2, 5 |
| TD-08 | 알 수 없는 `status` 값 | 400 거부. 명세에 D-30, AC-04-5로 반영 완료 | CLAUDE.md 2.4.3 | TRD 6, 8 |
| TD-09 | 본문 형식 오류 | 범위에 포함, 400 + 일반 문구 하나. D-31, AC-14-3으로 반영 완료 | CLAUDE.md 2.4.3 | TRD 6, 8 |
| TD-10 | 글자 수 기준 | 유니코드 코드포인트. 조합 이모지는 2자 이상으로 센다는 한계를 인정 | 구현이 단순하고 결과가 결정적 | TRD 4 |
| TD-11 | 레이어 구성 | presentation / application / domain / persistence | CLAUDE.md 2.3.1, 2.3.2 | TRD 2 |
| TD-12 | 테스트 도구 | JUnit 6 + Mockito (리뷰 반영: JUnit 5에서 정정, 스타터는 TD-29) | Boot 테스트 스타터에 포함 | TRD 1, 9 |
| TD-13 | 샘플 데이터 | `local` 프로파일에서만 MockData를 채움. 명세에 없는 개발 편의 기능. Runner도 TDD 대상(리뷰 반영) | 로컬에서 화면 확인용. 테스트 독립성을 위해 테스트에서는 로드하지 않음 | TRD 7, 9 |
| TD-14 | 문서 위치 | `docs/plan/TRD.md`와 이 파일. CLAUDE.md 4장은 TRD 확정 후 채우도록 제안 수락 | 단계별 문서 지도(CLAUDE.md 3장) | 전체 |
| TD-15 | 오류 응답 형식과 HTTP 상태 | `{error:{code,message}}`, 입력 오류 400, 없는 할 일 404 | FR-14, AC-14-1 | TRD 6 |
| TD-16 | JaCoCo 추가 | 승인. Gradle 내장 `jacoco` 플러그인, 라인 80% | CLAUDE.md 2.4.2 | TRD 1, 9 |
| TD-17 | 빌드 도구 | Gradle **9.8.2** (Kotlin DSL, wrapper) (리뷰 반영: 버전 고정) | 0단계에서 정한 개발 환경이며 Boot 4.1.2 지원 범위(8.14 이상 또는 9.x)에 포함 | TRD 1 |
| TD-18 | PUT 필드 규칙 | `title`, `completed` 필수. `description`, `dueDate`는 생략 또는 null이면 없음 | 부분 수정의 모호함 제거 | TRD 5 |
| TD-19 | 검증 순서 (리뷰 반영: 범위 축소) | 값 검증은 첫 오류 하나만, 순서는 제목→설명→마감일. "없는 id면 404 먼저"는 **값 검증 오류에만** 적용. 요청 읽기 실패(깨진 JSON 등)는 항상 id 조회보다 먼저 400 | 읽기 실패는 컨트롤러에 닿기 전에 실패 | TRD 3.3 |
| TD-20 | 마감일 파싱 위치 (리뷰 반영, 재검토로 수정) | 요청 DTO는 `dueDate`를 원본 JSON 값(`JsonNode`)으로 받고 `presentation` 매퍼가 변환: 없음/null → 마감일 없음, 문자열 → 그대로 `application`에 전달해 `domain`이 ISO 파싱, 문자열이 아닌 값 → JSON 원문 텍스트를 문자열로 전달해 파싱 실패(`INVALID_DUE_DATE`)로 처리. 해석은 D-36(승인됨)으로 명세에 남김 | `LocalDate`나 `String`으로 받으면 강제 변환을 끈 Jackson이 `MALFORMED_REQUEST`로 만들어 AC-09-4를 깸. `domain`이 Jackson 타입을 알면 안 됨(2.3.2) | TRD 3.2 |
| TD-21 | 스칼라 강제 변환 (리뷰 반영) | 강제 변환을 끄고, 알 수 없는 필드는 거부. `title` 누락/null은 `TITLE_REQUIRED`, `description` 종류 불일치는 `MALFORMED_REQUEST`. 설정 방법은 Red 테스트로 확정 | AC-14-3 "값의 종류가 맞지 않음은 거부" | TRD 3.2 |
| TD-22 | 처리 순서 전체 (리뷰 반영, 재검토로 수정) | 요청 읽기 → id 존재 → 값 검증 순서는 지킨다. id 형식 오류(1번)와 본문 읽기 실패(2번)가 동시에 나면 어느 쪽이 먼저여도 된다 | 명세가 이 조합을 요구하지 않음(AC-12-3은 본문이 정상인 경우만). 순서에 기대는 구현 복잡도를 줄임 | TRD 3.3 |
| TD-23 | Spring Boot와 Gradle 호환 (리뷰 반영) | Boot 4.1.2 + Gradle 9.8.2 + JDK 21. Boot 3.5.16은 Gradle 9를 문서상 지원하지 않아 제외 (공식 system-requirements 문서, 2026-11-11 확인) | 이미 정한 개발 환경(Gradle 9.8.2)을 유지하는 쪽을 택함. 반대로 가면 Boot 3.5.16 + Gradle 8.14.x | TRD 1 |
| TD-24 | 트랜잭션과 동시성 (리뷰 반영) | 서비스 메서드 하나가 작업 단위, `@Transactional` 미사용. `Todo`는 불변, 수정은 사본 교체. 저장소는 `AtomicLong` + `ConcurrentSkipListMap`, `update`는 원자적. 목록+요약은 `findAll()` 한 번의 결과로 계산 | 인메모리에 트랜잭션 매니저가 없고 실패 시 원본 불변(AC-08-3) 보장 | TRD 3.1 |
| TD-25 | 테스트 전략 확정 (리뷰 반영) | 도메인=순수 JUnit, 서비스=JUnit+Mockito, 저장소=JUnit, 웹=`@WebMvcTest`, 통합=`@SpringBootTest`+MockMvc. `Clock.fixed` 사용. 통합 테스트는 `@DirtiesContext(AFTER_EACH_TEST_METHOD)`로 격리. Runner도 단위+통합 테스트 | CLAUDE.md 2.4.1 독립성, 프로덕션에 테스트 전용 메서드 금지 | TRD 9 |
| TD-26 | 레이어 참조 규칙과 config (리뷰 반영) | presentation은 application과 domain 참조 가능(Todo는 DTO 변환, 예외는 오류 변환 용도). persistence 참조 금지. `config` 패키지는 구성 루트로 `Clock`, Runner, Jackson 설정을 두고 어떤 레이어도 config를 참조하지 않음 | CLAUDE.md 2.3.2 | TRD 2 |
| TD-27 | `status` 대소문자 (리뷰 반영) | 소문자 `all`, `active`, `done`만 허용. `ALL`은 `INVALID_STATUS_FILTER` | 허용 범위를 좁게 두는 쪽이 단순(YAGNI) | TRD 3.2 |
| TD-28 | 문서 분리 (리뷰 반영, 번호 정정) | 4장(모델, 처음에는 "3장"으로 적었으나 3장은 공통 정책이다)과 5장(API)은 이후 `DATA_MODEL.md`, `API_SPEC.md`로 옮기고 TRD에는 요약만 남김. 옮기기 전까지는 TRD가 기준 | 기준 문서가 두 개가 되는 것을 방지 | TRD 머리말 |
| TD-29 | Boot 4 의존성 이름 (재검토 반영, 승인됨) | 웹 스타터 `spring-boot-starter-webmvc`(구 `-web`은 deprecated), 테스트 스타터 `spring-boot-starter-webmvc-test`(`spring-boot-starter-test`를 함께 가져옴), Jackson 3(`tools.jackson`), JUnit 6. 테스트 스타터는 새 의존성 이름이라 승인 필요 | Boot 4.1.2 공식 문서와 4.0 마이그레이션 가이드로 확인(2026-11-11). `@WebMvcTest`가 `spring-boot-webmvc-test` 모듈로 분리됨 | TRD 1 |
| TD-30 | id 검사 위치와 415/406 (재검토 반영) | id 형식·범위 검사는 컨트롤러에서 직접 한다(`Converter`, 파라미터 순서 의존 없음). 415/406은 D-37에 따라 `MALFORMED_REQUEST`(400)로 변환 | 순서를 정하지 않기로 했으므로 단순한 쪽을 택함(TD-22). catch-all 처리기가 있으면 변환하지 않은 415/406이 500이 되므로 명시적으로 변환 | TRD 3.3, 3.2 |
| TD-31 | 커버리지 검증 연결 (재검토 반영) | `jacocoTestCoverageVerification`을 `check`에 연결 | `./gradlew build`만으로 80% 기준(2.4.2)이 지켜짐 | TRD 1, 9 |
| TD-32 | 멈추는 규칙 (재검토 반영) | 명세에 없는 프레임워크 수준 오류는 기존 코드 중 하나로 보내고 새 문구는 만들지 않는다. D-34, D-35는 유지하고 415/406은 D-31로 흡수(D-37) | Spring 기본 오류 응답은 `{error:{code,message}}`와 모양이 달라 FR-14, AC-14-1, 헌법 2.4.3을 어김. 반대로 새 문구가 계속 늘어나는 것도 막아야 함 | TRD 6 |
| TD-33 | "공백"의 범위 | 유니코드 공백(`String.strip()`). 전각 공백(U+3000) 포함. U+00A0, U+200B 등은 공백으로 보지 않는 한계를 인정 | 한글 입력에서 전각 공백만 넣은 제목이 AC-02-2를 우회하지 않게 함 | DATA_MODEL 2.1 |
| TD-34 | 마감일 텍스트 엄격도 (D-38로 수정) | 설명과 같은 규칙: 앞뒤 공백 제거 후 비면(`""`, 공백뿐) 마감일 없음, 아니면 제거한 값을 `YYYY-MM-DD`(ASCII 숫자 4-2-2, 실제 달력 날짜)로 엄격 파싱. 지우는 방법은 `null`, `""`, 공백뿐인 문자열 | `""`이 설명(D-27)과 반대로 오류가 되는 불일치를 없애고 빈 마감일을 보내는 클라이언트를 허용. 사용자에게 보이는 동작이라 명세 D-38, AC-09-6으로 올림. 날짜 패턴만으로는 5자리 연도가 통과하므로 정규식으로 먼저 거름 | DATA_MODEL 2.3 |
| TD-35 | 저장소 계약 위치와 반환형 | 계약은 DATA_MODEL 6장이 기준, TRD 3.1은 링크만. `findById`/`update`는 `Optional`, `findAll`은 id 오름차순 불변 `List`, `deleteById`는 `boolean`. `update`가 id를 바꾸면 거부 | 기준 문서가 둘이 되는 것을 방지(TD-28) | DATA_MODEL 6, TRD 3.1 |
| TD-36 | 불변식 위치와 입력 타입 (TD-40으로 보완) | `Todo`는 `record`. 정규화와 검증은 도메인 팩토리 한 곳, 마지막 확인은 compact 생성자. 입력은 원시(`application` 명령 객체)와 검증됨(`NewTodo`, `TodoUpdate`)으로 나눔. 수정은 기존 값을 교체 | FR-01·D-06은 추가 시 제목만 받음 | DATA_MODEL 2, 4 |
| TD-37 | 기준 시각과 요약 정의 | "오늘"은 요청 처리마다 `LocalDate.now(clock)` 한 번. 요약은 `application` 값 객체로 `findAll()` 스냅샷에서 계산, `remaining = active` | 한 응답 안의 항목이 서로 다른 "오늘"을 쓰지 않게 함(AC-05-2) | DATA_MODEL 3 |
| TD-38 | 상태별 보기 필터 위치 | 저장소가 아니라 `application`이 `findAll()` 결과를 거름 | 필터와 요약이 같은 스냅샷에서 나와 어긋나지 않음. 저장소 계약이 작아짐(YAGNI) | DATA_MODEL 6 |
| TD-39 | DTO 변환 규칙 (TD-41 반영) | 요청 DTO(`dueDate`는 `JsonNode`) → `presentation` 매퍼 → `NewTodo`/수정 입력 → `Todo` → 응답 DTO. 정규화는 `domain`만 한다. `dueDate`는 ISO 문자열 또는 `null`, `description` 없음도 `null`로 내보냄 | CLAUDE.md 2.3.2, 2.3.3, TD-20 | DATA_MODEL 5 |
| TD-40 | 생성 흐름과 불변식 보장 (DATA_MODEL 검토 반영) | 원시 입력은 `application`의 명령 객체(문자열). 도메인 팩토리(`NewTodo.of`, `TodoUpdate.of`)가 정규화·검증하고 입력 오류는 오류 코드를 가진 도메인 예외로 던짐. `Todo`·`NewTodo`·`TodoUpdate`의 compact 생성자가 불변식(id ≥ 1, 길이 등)을 다시 확인하고 어기면 `IllegalArgumentException`(→ `INTERNAL_ERROR`). 저장소는 검증된 `NewTodo`에 id만 붙임 | `record` 생성자는 public이라 팩토리 우회가 가능. 이전 문서의 "팩토리에서만 생성"과 "id는 저장소가 발급"이 모순이었음 | DATA_MODEL 2, 4, 6 |
| TD-41 | `overdue` 계산 주체 (DATA_MODEL 검토 반영) | 판정식은 `Todo.isOverdue(today)`(domain). `application`이 `Clock`으로 오늘을 한 번 계산해 `TodoView(todo, overdue)`와 `ListResult(items, summary)`를 돌려주고 `presentation` 매퍼는 옮겨 담기만 함 | `presentation`이 `Clock`에 의존하지 않게 함(TD-26) | DATA_MODEL 3.2, 5.3, TRD 2 |
| TD-42 | 수정 흐름 순서 (DATA_MODEL 검토 반영) | `findById` → 도메인 검증 → `update(id, old -> old.apply(검증된 값))`. 완료 표시는 검증 없이 `update`만 | 없는 id의 404가 값 검증보다 먼저(TD-19)이고 검증 실패 시 저장소를 건드리지 않음(AC-08-3) | DATA_MODEL 6 |
| TD-43 | 요청 헤더 규칙 (API_SPEC 작성, D-39) | `Content-Type: application/json`(charset 허용)은 POST/PUT/PATCH에만 요구, 없거나 다르면 `MALFORMED_REQUEST`. 본문이 비어도 같음. GET/DELETE의 본문은 무시. `Accept`는 없거나 JSON을 받을 수 있으면 통과, 아니면 **모든 엔드포인트**에서 `MALFORMED_REQUEST`. 오류 응답은 `Accept`와 무관하게 항상 JSON | D-37이 Accept 불일치를 D-31에 포함했으나 AC-14-3의 When이 본문 있는 요청만 나열 → 문구를 정정(D-39) | API_SPEC 1.2, TRD 3.2 |
| TD-44 | `status` 쿼리 규칙 (API_SPEC 작성) | 빈 값(`status=`)과 반복(`status=active&status=done`)은 `INVALID_STATUS_FILTER`. 생략만 `all`. 표에 없는 쿼리 파라미터는 무시 | AC-04-5 "허용 외 값은 거부"와 일관. 쿼리 거부는 명세에도 필요도 없어 YAGNI | API_SPEC 2.2 |
| TD-45 | 경로 id의 정규 형식 | 앞에 0이 없는 10진 양의 정수(`[1-9][0-9]*`)이고 long 범위 안일 때만 번호. `01`, `+1`, `1.5`, 범위 초과는 번호로 읽을 수 없는 값으로 404 `TODO_NOT_FOUND` | D-33의 "번호로 읽을 수 없는 값"의 범위 확정. 한 항목을 가리키는 주소를 하나로 유지 | API_SPEC 1.6 |
| TD-46 | 문서 분담 (API_SPEC 작성) | 오류 코드 표는 API_SPEC 1.8이 기준, TRD 6장은 형식·원칙·멈추는 규칙과 링크만. 처리 순서와 요청 읽기 정책의 구현은 TRD 3장이 기준, API_SPEC에는 결과만 요약 | 외부 계약은 API_SPEC, 구현 정책은 TRD. 기준 문서가 둘이 되는 것을 방지(TD-28) | API_SPEC 머리말, TRD 5, 6 |
| TD-47 | 응답 헤더 | `Content-Type: application/json`(UTF-8)만 계약. `Location`(201 포함), `Allow` 등은 보내지 않음. 204는 본문 없음 | 명세에 없는 부가 기능을 넣지 않음(YAGNI) | API_SPEC 1.3 |
| TD-48 | 경로·메서드 규칙 | 표에 없는 경로(끝 `/` 포함)는 404 `ENDPOINT_NOT_FOUND`, 있는 경로의 다른 메서드는 405 `METHOD_NOT_ALLOWED`. HEAD, OPTIONS는 계약 밖. 이미 삭제한 id의 재삭제는 404 `TODO_NOT_FOUND` | AC-14-5, AC-14-6, AC-11-2, AC-12-2 | API_SPEC 1.1, 2.6 |
| TD-49 | 예시와 추적표 | 예시는 "오늘 = 2026-11-10" 기준이고 값은 AC 예시를 사용. 모든 AC를 엔드포인트에 연결하고 특정 엔드포인트가 없는 AC(AC-14-1, 14-2, 14-4~6)는 "공통"으로 표기. 전체 56개 AC가 표에 모두 있음을 확인함 | PRD 6장 성공 기준(모든 AC가 자동 테스트로 확인됨)의 출발점 | API_SPEC 3 |

## 명세로 올려 승인된 항목
TRD가 정하지 않고 `docs/spec/`에 올려 승인받은 항목은 D-32(`completed` 누락), D-33(id가 번호가 아님), D-34(500), D-35(없는 경로/허용되지 않은 메서드)이다. TRD 6장의 표시는 지웠다.
재검토에서 올라온 D-36(문자열이 아닌 `dueDate`), D-37(415/406은 D-31에 포함)도 승인되었다. TD-29의 테스트 스타터(`spring-boot-starter-webmvc-test`)도 승인되었다.
