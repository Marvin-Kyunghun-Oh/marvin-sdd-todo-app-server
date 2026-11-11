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
| TD-34 | 마감일 텍스트 엄격도 | `YYYY-MM-DD`(ASCII 숫자 4-2-2, 실제 달력 날짜)만 허용. 앞뒤 공백 미제거, `""`은 `INVALID_DUE_DATE`. 지우는 방법은 `null` 하나 | 마감일 없음의 표현을 하나로 유지(TD-18). 날짜 패턴만으로는 5자리 연도가 통과하므로 정규식으로 먼저 거름 | DATA_MODEL 2.3 |
| TD-35 | 저장소 계약 위치와 반환형 | 계약은 DATA_MODEL 6장이 기준, TRD 3.1은 링크만. `findById`/`update`는 `Optional`, `findAll`은 id 오름차순 불변 `List`, `deleteById`는 `boolean`. `update`가 id를 바꾸면 거부 | 기준 문서가 둘이 되는 것을 방지(TD-28) | DATA_MODEL 6, TRD 3.1 |
| TD-36 | 불변식 위치와 입력 타입 | `Todo`는 `record`, 정규화와 검증은 도메인 팩토리 한 곳. `NewTodo(title)`, 수정 입력은 `(title, description, dueDateText, completed)`로 기존 값을 교체 | 규칙을 어긴 `Todo`가 존재할 수 없게 함. FR-01·D-06은 추가 시 제목만 받음 | DATA_MODEL 2, 4 |
| TD-37 | 기준 시각과 요약 정의 | "오늘"은 요청 처리마다 `LocalDate.now(clock)` 한 번. 요약은 `application` 값 객체로 `findAll()` 스냅샷에서 계산, `remaining = active` | 한 응답 안의 항목이 서로 다른 "오늘"을 쓰지 않게 함(AC-05-2) | DATA_MODEL 3 |
| TD-38 | 상태별 보기 필터 위치 | 저장소가 아니라 `application`이 `findAll()` 결과를 거름 | 필터와 요약이 같은 스냅샷에서 나와 어긋나지 않음. 저장소 계약이 작아짐(YAGNI) | DATA_MODEL 6 |
| TD-39 | DTO 변환 규칙 | 요청 DTO(`dueDate`는 `JsonNode`) → `presentation` 매퍼 → `NewTodo`/수정 입력 → `Todo` → 응답 DTO. 정규화는 `domain`만 한다. `dueDate`는 ISO 문자열 또는 `null`, `description` 없음도 `null`로 내보냄 | CLAUDE.md 2.3.2, 2.3.3, TD-20 | DATA_MODEL 5 |

## 명세로 올려 승인된 항목
TRD가 정하지 않고 `docs/spec/`에 올려 승인받은 항목은 D-32(`completed` 누락), D-33(id가 번호가 아님), D-34(500), D-35(없는 경로/허용되지 않은 메서드)이다. TRD 6장의 표시는 지웠다.
재검토에서 올라온 D-36(문자열이 아닌 `dueDate`), D-37(415/406은 D-31에 포함)도 승인되었다. TD-29의 테스트 스타터(`spring-boot-starter-webmvc-test`)도 승인되었다.
