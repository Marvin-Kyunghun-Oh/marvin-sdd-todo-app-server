# VALIDATION REPORT — Marvin's Todo App 서버

> SDD 5단계(Validate) 산출물. 상태: **초안, 사람 확인 대기 (2026-11-11)**
> 검증 대상: 브랜치 `SDD-5_Validate`, 기준 커밋 `f79b813` (4단계 PR #7 병합 직후). 이 검증에서 코드와 기존 문서는 수정하지 않았다.

## 한눈에 보기 (사람용 요약)
> 결정이 필요한 항목은 맨 아래 "사람이 결정할 항목"에 모았다.

**결론: 명세가 요구한 동작은 모두 구현되고 테스트로 확인된다. 다만 명세가 다루지 않은 가장자리에서 어긋남이 4건 나왔다.**

1. **빌드와 테스트:** 전체 빌드가 성공했고 테스트 287개가 모두 통과했다(실패 0, 건너뜀 0). 코드 줄 기준 커버리지는 99.6%이고 기준(80%)보다 훨씬 높다.
2. **명세의 약속 56개:** 모두 하나 이상의 테스트에 연결되어 있고 모두 통과한다. "검증 안 됨"은 없다. 다만 목록 보기의 `status=all`(전체를 명시하는 값)은 코드가 한 번도 실행되지 않았다. 직접 호출해 보니 정상이었다.
3. **명세에 없는데 구현된 것:** 제품 동작을 새로 만든 것은 없다. 승인된 로컬 샘플 데이터만 명세 밖이다. 다만 프레임워크가 알아서 해 주는 동작이 몇 개 있다(`HEAD`, `OPTIONS` 응답, 같은 키가 두 번 오면 마지막 값 사용 등).
4. **서버를 직접 띄워 약 120번 호출한 결과:** 정상 요청과 대부분의 오류 요청은 명세와 같았다. 어긋난 곳은 네 곳이다.
   - 서버에 `/error` 주소로 요청하면 약속된 오류 모양이 아닌 스프링 기본 모양(500)이 나온다.
   - 목록 요청의 `status` 값이 깨진 인코딩(`%zz`)이면 "잘못된 보기 구분"(400)이어야 할 것이 "서버 오류"(500)로 나오고 서버 로그에 오류가 남는다.
   - 주소 자체가 깨진 요청(`/todos/%zz`)은 톰캣이 HTML 오류 페이지를 돌려준다. 명세가 이 경우를 정하지 않았다.
   - 응답 헤더에 `charset=UTF-8`이 붙지 않는다. 내용은 정상 UTF-8이라 실제 문제는 없지만 문서 문구와 다르다.
5. **문서끼리의 모순:** 기술 문서 3종(TRD, 데이터 모델, API 명세) 사이에 모순은 없었다. 작업 목록(TASKS)에는 4단계 중 두 번 고친 이력이 문서 안에 남아 있지 않고, 한 작업의 커밋 종류가 실제와 다르다. 영향은 작다.
6. **분류:** 구현을 고칠 것 2건, 문서(명세)를 고칠 것 5건, 둘 중 하나를 사람이 정할 것 1건이다.

## 사람이 결정할 항목
| # | 질문 | 권장 |
| --- | --- | --- |
| Q1 | 깨진 주소(`/todos/%zz`)를 톰캣의 HTML 오류 대신 약속된 JSON 오류로 바꿀까, 아니면 알려진 한계로 문서에 적을까? 바꾸려면 톰캣 수준의 설정이 필요해 현재 설계(스프링 층) 밖이다 | 알려진 한계로 문서에 적기 (명세 수정) |
| Q2 | `HEAD`, `OPTIONS` 요청을 어떻게 볼까? 지금은 프레임워크 기본으로 200이 나온다 | "계약 밖, 프레임워크 기본 동작에 맡김"으로 문서에 적기 (명세 수정) |
| Q3 | 응답 `Content-Type`에 `charset=UTF-8`을 붙일까, 문서 문구를 고칠까? | 문구 수정 (JSON은 원래 UTF-8이라 영향 없음) |
| Q4 | `status=%zz`는 "보기 구분 오류"(400 `INVALID_STATUS_FILTER`)로 처리해도 되나? 명세 문장("전체, 진행 중, 완료 외의 값")을 그대로 읽은 결과다 | 그대로 진행 |
| Q5 | 작업 목록(TASKS)에 4단계 중 변경 이력을 적고, CLAUDE.md 변경 이력에 "4단계 확정" 행을 추가할까? | 추가 (`docs:` 커밋 하나) |

구현 수정은 새 실패 테스트를 먼저 쓰고 고치는 별도 작업으로 진행해야 한다(CLAUDE.md 2.1). 명세 수정은 역류 규칙에 따라 문서를 먼저 고치고 승인을 받은 뒤 진행한다.

---

## 상세 (에이전트용)

## 1. 빌드, 테스트, 커버리지
실행: `./gradlew clean build` (Windows, Gradle 9.8.2, 툴체인 JDK 21). 결과는 아래와 같다.

| 항목 | 결과 |
| --- | --- |
| 빌드 | `BUILD SUCCESSFUL` (33초, 실행된 태스크 8개). `jacocoTestCoverageVerification` 통과 |
| 테스트 | 287개, 실패 0, 오류 0, 건너뜀 0 (`build/test-results/test/*.xml` 합산). 테스트 클래스 27개 |
| 라인 커버리지 | **99.6%** (243줄 중 1줄 미실행). 기준 80% |
| 명령어 커버리지 | 99% (1,266 중 4 미실행), 분기 96% (88 중 3 미실행) |
| 패키지별 미실행 줄 | `presentation` 1줄, 나머지 0줄 |
| 미실행 줄 | `StatusParam.java:15` `case "all" -> StatusFilter.ALL`. `status=all`을 명시한 테스트가 없다 |
| 부분 실행 줄 | `SampleDataRunner.java:38` `dueDate != null \|\| completed` 분기 일부 |

- 참고: `jacocoTestReport`는 `build`에 연결되어 있지 않아 HTML 보고서를 따로 생성해 읽었다(`build/reports/jacoco/test/html`, 빌드 산출물이며 git 추적 대상 아님).
- 환경 메모: 이 PC의 기본 `java`는 17이다. 빌드는 Gradle 툴체인(21)을 써서 영향이 없지만, 만들어진 JAR를 `java -jar`로 직접 실행하려면 21이 필요하다(17에서는 `UnsupportedClassVersionError`). README의 실행 안내는 `./gradlew bootRun`만 다루므로 문서와 충돌하지 않는다.

## 2. 수용 기준 56개 → 테스트 매칭
방법: 테스트의 `@DisplayName`이 `AC-xx-x`로 시작하는 것을 모두 추출해 AC별로 묶었다. 파라미터화 테스트는 하나로 센다. 모든 테스트가 통과했으므로 "결과"는 모두 통과다. 굵게 표시한 테스트가 규칙이 사는 층이다(TASKS 3장과 일치).

약어: Dom=`NewTodoTest`/`TodoUpdateTest`/`TodoTest`, Repo=`InMemoryTodoRepositoryTest`, Svc=`TodoServiceTest`, Web=`TodoControllerTest`, Hdr=`RequestHeaderPolicyTest`, Fw=`FrameworkErrorTest`, Int=`ScenarioIntegrationTest`, Api=`ApiIntegrationTest`.

| AC | 내용 (요약) | 검증 테스트 | 결과 |
| --- | --- | --- | --- |
| AC-01-1 | 제목만으로 추가 | Repo, Svc, Web, Int | 통과 |
| AC-01-2 | 같은 제목도 별도 생성 | Svc, Int | 통과 |
| AC-01-3 | 제목 앞뒤 공백 제거 | Dom, Svc | 통과 |
| AC-02-1 | 빈 제목 거부 | Dom, Web | 통과 |
| AC-02-2 | 공백뿐인 제목 거부 | Dom, Web | 통과 |
| AC-02-3 | 101자 제목 거부 | Dom, Web | 통과 |
| AC-02-4 | 100자 제목 허용 | Dom | 통과 |
| AC-02-5 | 수정 시 빈 제목 거부 | Dom, Svc, Web | 통과 |
| AC-02-6 | 1,001자 설명 거부 | Dom, Web | 통과 |
| AC-03-1 | 등록 순서 목록 | Repo, Svc, Web, Int | 통과 |
| AC-03-2 | 마감일·완료와 무관한 순서 | Svc, Int | 통과 |
| AC-03-3 | 목록 항목의 다섯 값 | Svc, Web | 통과 |
| AC-04-1 | 진행 중만 보기 | Svc, Web | 통과 |
| AC-04-2 | 완료만 보기 | Svc, Web | 통과 |
| AC-04-3 | 전체 보기 | Svc, Web | 통과 (`status` 생략만 검증. 명시값 `all`은 **검증 안 됨**, 아래 3.1 참고) |
| AC-04-4 | 완료 없을 때 빈 목록 | Svc, Web | 통과 |
| AC-04-5 | 잘못된 보기 구분 거부 | Web (`foo`, `ALL`, 빈 값, 반복) | 통과 (퍼센트 인코딩이 깨진 값은 **어긋남**, 4장 F2) |
| AC-05-1 | 요약 4/3/1/3 | Svc, Int | 통과 |
| AC-05-2 | 보기와 무관한 요약 | Svc, Web, Int | 통과 |
| AC-05-3 | 빈 상태 요약 0 | Svc, Web | 통과 |
| AC-06-1 | 완료 표시 | Svc, Web, Int | 통과 |
| AC-06-2 | 진행 중으로 되돌리기 | Svc, Web, Int | 통과 |
| AC-06-3 | 완료 재표시 무오류 | Svc, Web | 통과 |
| AC-06-4 | 진행 중 재표시 무오류 | Svc, Web | 통과 |
| AC-06-5 | PATCH에서 `completed` 누락 거부 | Web | 통과 |
| AC-07-1 | 한 건 보기 | Svc, Web | 통과 |
| AC-08-1 | 세 값 수정 | Svc, Web, Int | 통과 |
| AC-08-2 | 수정에서 완료 표시 | Svc, Web | 통과 |
| AC-08-3 | 거부된 수정은 불변 | Svc, Int | 통과 |
| AC-08-5 | 설명 비우기 | Svc | 통과 |
| AC-08-6 | 공백뿐인 설명 | Svc | 통과 |
| AC-08-7 | PUT에서 `completed` 누락 거부 | Web | 통과 |
| AC-09-1 | 마감일 지정 | Dom, Svc, Web | 통과 |
| AC-09-2 | 마감일 지우기 | Dom, Svc, Web, `TodoTest` | 통과 |
| AC-09-3 | 마감일 없음 응답 | Svc, Web | 통과 |
| AC-09-4 | 날짜가 아닌 값 거부 (문자열이 아닌 값 포함) | Dom, Web | 통과 |
| AC-09-5 | 과거 마감일 저장 | Dom, Svc | 통과 |
| AC-09-6 | 빈 문자열·공백 마감일 | Dom, Web | 통과 |
| AC-10-1 | 기한 지남 | Dom, Svc | 통과 |
| AC-10-2 | 마감 당일은 아님 | Dom, Svc | 통과 |
| AC-10-3 | 미래 마감은 아님 | Dom, Svc | 통과 |
| AC-10-4 | 완료는 아님 | Dom, Svc | 통과 |
| AC-10-5 | 완료하면 해제 | Svc | 통과 |
| AC-10-6 | 한국 날짜 기준 | Svc, Api | 통과 |
| AC-11-1 | 삭제 후 사라짐 | Svc, Web, Int | 통과 |
| AC-11-2 | 삭제 후 열면 없음 | Repo, Svc, Web, Int | 통과 |
| AC-12-1 | 없는 할 일 열기 | Repo, Svc, Web | 통과 |
| AC-12-2 | 없는 할 일 수정·완료·삭제 | Repo, Svc, Web, Int | 통과 |
| AC-12-3 | 번호가 아닌 id | Web (GET·DELETE·PUT·PATCH) | 통과 |
| AC-13-1 | 빈 목록 | Svc, Web, Int | 통과 |
| AC-14-1 | 같은 오류 모양 | Web | 통과 |
| AC-14-2 | 거부된 추가는 불변 | Svc, Int | 통과 |
| AC-14-3 | 읽을 수 없는 요청 | Web, Hdr, Api, Int | 통과 |
| AC-14-4 | 500과 내부 정보 은닉 | Fw, Api | 통과 |
| AC-14-5 | 없는 주소 404 | Fw, Api | 통과 |
| AC-14-6 | 405 | Fw, Api | 통과 |

집계: 56개 중 56개에 테스트가 있고 56개 통과. "검증 안 됨" 0개. AC-04-3의 명시값 `all`은 3.1에서 직접 호출로 확인했다.
PRD의 성공 기준 두 가지: (1) "모든 AC가 자동 테스트로 확인된다"는 충족. (2) "와이어프레임의 모든 화면과 상태가 서버 동작만으로 채워진다"는 요구사항 추적표(REQUIREMENTS)의 와이어프레임 요소 10개가 모두 FR에 연결되고 해당 FR이 구현·테스트됨을 확인했다.

## 3. 명세에 없는데 구현된 동작
제품 코드(`src/main`, 31개 파일)와 설정을 읽고, 서버를 띄워 확인했다. `application.properties`는 없고 의존성은 `webmvc` 스타터와 테스트 스타터뿐이다(TRD 1장과 일치). 열린 주소는 명세의 6개뿐이다(`/actuator` 등은 404).

| # | 동작 | 근거·확인 | 평가 |
| --- | --- | --- | --- |
| U1 | 로컬 프로파일 샘플 데이터 5건 | `SampleDataRunner`, TRD 7장·TD-13에 근거. PRD 5.1 범위 밖임을 TD-13에 기록 | 승인된 예외. 조치 없음 |
| U2 | `HEAD /todos` → 200(본문 없음), `OPTIONS /todos` → 200 + `Allow: GET,HEAD,POST,OPTIONS` + `Accept-Patch:` 헤더. `/todos/{id}`도 같음 | 직접 호출. API_SPEC 1.1은 "HEAD, OPTIONS는 계약에 넣지 않는다", 1.3은 "`Allow` 같은 부가 헤더는 계약에 포함하지 않는다"라고만 적음 | 프레임워크 기본 동작. Q2 |
| U3 | `GET /error`, `POST /error` 등이 열려 있음(스프링 기본 오류 처리기의 주소) | 직접 호출. 4장 F1 | 의도하지 않은 주소. F1로 분류 |
| U4 | JSON 요청에서 같은 키가 두 번 오면 마지막 값 사용 (`{"title":"a","title":"b"}` → `b`) | 직접 호출. 명세에 규칙 없음 | 낮음. 문서화 선택 사항 |
| U5 | 요청 `Content-Type`의 다른 charset을 받아들임 (`charset=UTF-16LE` 본문 → 201) | 직접 호출. API_SPEC 1.2는 "문자 인코딩은 UTF-8이다"라고만 적음 | 낮음. 문서화 선택 사항 |
| U6 | 요청 `Accept`가 JSON을 받을 수 없으면 존재하지 않는 주소에도 영향이 없음 (`/nothing` + `Accept: text/html` → 404 JSON) | 직접 호출. API_SPEC은 "모든 엔드포인트"라고만 적음 | 합리적. 조치 없음 |

### 3.1 명시값 `status=all`
테스트가 없어(`src/test`에 `status=all` 문자열 없음) 서버를 띄워 직접 확인했다. `GET /todos?status=all` → 200, 전체 목록과 요약이 정상으로 돌아온다. 동작은 명세와 같고, 테스트만 비어 있다. 분류: 구현 수정이 아니라 **테스트 보강**(새 테스트가 처음부터 통과할 가능성이 높음. CLAUDE.md 2.1에 따라 먼저 통과하면 보고).

## 4. 실제 서버 호출 결과 vs API_SPEC
방법: `build/libs/*-SNAPSHOT.jar`를 JDK 21로 `--server.port=18080`으로 실행(기본 프로파일, 빈 상태)하고 `curl`로 약 120회 호출했다. 확인 후 서버를 종료했다. 한글 본문은 Git Bash의 인자 인코딩 때문에 400이 나와, 본문은 ASCII 또는 UTF-8 파일로 보냈다(한글 본문 자체는 정상 201, 공백 제거 확인).

### 4.1 명세와 일치한 것 (대표)
| 엔드포인트 | 확인한 요청 → 실제 응답 |
| --- | --- |
| `POST /todos` | 정상 → 201 + Todo 6필드(`description`·`dueDate`는 `null`). 공백 제거. 같은 제목 → 별도 id. 제목 없음·`null`·공백 → 400 `TITLE_REQUIRED`. 101자 → `TITLE_TOO_LONG`, 100자 → 201. `title:123`, 추가 필드, 깨진 JSON, 빈 본문, Content-Type 없음·`text/plain` → 400 `MALFORMED_REQUEST`. `application/json; charset=UTF-8` → 201 |
| `GET /todos` | 빈 상태 → 200 + 빈 `items`, 요약 0. `status=active`/`done`/`all` → 필터링되고 요약은 전체 기준. `ALL`, `foo`, 빈 값, 반복 → 400 `INVALID_STATUS_FILTER`. 모르는 파라미터는 무시. `Accept: text/html` → 400 JSON. `Accept: */*` → 200 |
| `GET /todos/{id}` | 존재 → 200. `abc`, `0`, `-1`, `01`, `+1`, `1.5`, 범위 초과, `9223372036854775807`, 없는 id → 모두 404 `TODO_NOT_FOUND` |
| `PUT /todos/{id}` | 세 값 수정 → 200. 과거 마감일 저장 + `overdue:true`. 마감일 `"   "`·생략 → `null`. 설명 공백 → `null`. 설명 1,000자 → 200, 1,001자 → `DESCRIPTION_TOO_LONG`. `dueDate` `"abc"`·`"2026-02-30"`·`123`·`true`·`"2026-1-5"`·`"20261112"`·`"…T00:00:00"`·`"2026-13-01"` → `INVALID_DUE_DATE`. `" 2026-11-12 "` → 통과. `completed` 누락·`"true"`, 숫자 `description` → `MALFORMED_REQUEST`. 처리 순서: 제목 오류가 설명·마감일보다 먼저, 없는 id + 값 오류 → 404, 없는 id + 깨진 본문 → 400. 거부된 수정 후 상태 불변 |
| `PATCH /todos/{id}` | 완료·재완료·되돌리기 → 200. `{}`·`null`·추가 필드 → 400. 없는 id·`0` → 404. 기한 지난 항목 완료 → `overdue:false` |
| `DELETE /todos/{id}` | 204(본문·`Content-Type` 없음). 재삭제·삭제된 id 열기·`abc` → 404. 본문은 무시 |
| 공통 | `/nothing`, `/todos/`, `/todos//`, `/todos/1/extra`, `/actuator` → 404 `ENDPOINT_NOT_FOUND`. `DELETE /todos`, `PUT /todos`, `POST /todos/1`, `TRACE /todos` → 405 `METHOD_NOT_ALLOWED`. 모든 JSON 오류 응답이 `application/json` |
| 기한 지남 | 서버 날짜(한국 시간 2026-11-11 기준) 마감일 11-10 → `true`, 11-11 → `false`, 11-12 → `false` |

### 4.2 어긋난 곳
| # | 요청 | 명세 | 실제 | 비고 |
| --- | --- | --- | --- | --- |
| F1 | `GET /error`, `POST /error` | API_SPEC 1.1: 표에 없는 경로는 404 `ENDPOINT_NOT_FOUND`. 1.7·AC-14-1: 모든 실패는 같은 오류 모양 | **500** `{"timestamp":"…","status":999,"error":"None"}` (스프링 기본 오류 처리기의 주소에 직접 접근) | 내부 정보는 아니지만 모양이 다르고 상태가 틀림 |
| F2 | `GET /todos?status=%zz` | AC-04-5·API_SPEC 1.8: 허용 값이 아니면 400 `INVALID_STATUS_FILTER` | **500** `INTERNAL_ERROR` + 서버 로그에 `ERROR … InvalidParameterException: Character decoding failed` | 클라이언트 잘못이 서버 오류로 나옴. 로그에 오류 스택이 남는 것도 부적절 |
| F3 | `GET /todos/%zz`, `GET /%` (깨진 퍼센트 인코딩 경로) | API_SPEC 1.7, AC-14-1: 모든 실패는 같은 오류 모양 | **400** `text/html` 톰캣 오류 페이지 | 톰캣이 스프링에 닿기 전에 거부. 명세가 이 상황을 정하지 않음 |
| F4 | 모든 JSON 응답 | API_SPEC 1.3: `Content-Type: application/json`(UTF-8) | `Content-Type: application/json` (charset 없음) | 본문은 UTF-8이고 JSON 표준상 UTF-8이 기본이라 실제 영향 없음 |

### 4.3 서버 내부 오류(500)의 실제 확인
AC-14-4는 테스트 전용 컨트롤러로만 일으킬 수 있다고 TASKS가 정했고 그렇게 통과한다. 이번 호출에서는 F2가 실제로 500을 일으켜, 응답 본문이 `{"error":{"code":"INTERNAL_ERROR","message":"서버에 문제가 생겼어요. 잠시 후 다시 시도해 주세요."}}`이고 내부 정보가 없음을 확인했다(AC-14-4의 모양은 실서버에서도 지켜짐).

## 5. 4단계 중 변경된 문서와 TRD·DATA_MODEL·API_SPEC의 정합성
4단계 중 변경된 문서: `docs/tasks/TASKS.md` 2건(`e842df5` 불변식 테스트 추가, `dad4a0f` 구성 조립을 T21에서 T13으로 이동), `CLAUDE.md`(T24, 변경 이력 1행), `README.md`(신규).

| # | 확인 | 결과 |
| --- | --- | --- |
| C1 | TRD·DATA_MODEL·API_SPEC 상호 정합 (처리 순서, 오류 코드 10개, 필드, 저장소 계약, 요청 읽기 정책) | **모순 없음.** TRD 3.3 ↔ API_SPEC 1.9, TRD 3.2 ↔ DATA_MODEL 5.1~5.2, DATA_MODEL 6 ↔ TRD 3.1이 서로 일치 |
| C2 | TASKS 케이스 ↔ 세 문서 | 케이스의 입력·기대값(T04 날짜 입력, T15 `status` 입력, T16 id 입력, T20 경로·메서드)이 DATA_MODEL 2.3, API_SPEC 1.6·2.2·1.1과 일치 |
| C3 | T13으로 옮긴 구성 조립 ↔ TRD 2·9장 | **일치.** TRD 2의 `config` 행(Clock, Jackson, 샘플), TRD 9장 통합 행(빈 연결, `Clock` KST)과 모순 없음. 실제 구현도 `TodoConfig`, `JacksonConfig`, `SampleDataRunner`로 같은 배치 |
| C4 | TASKS 요약(사람용) ↔ 상세 | 요약 10행은 순서를 "…전체 연결, 샘플 데이터, 마무리 순"이라고만 적어 조립이 T13에 들어간 변경을 언급하지 않는다. 모순은 아니지만 `dad4a0f`가 요약을 건드리지 않았다. CLAUDE.md 2.6.5("상세를 고치면 같은 변경에서 요약도 고친다")에 비추면 미반영 소지 |
| C5 | TASKS 변경 이력 | TASKS의 상태 줄은 "확정 (2026-11-11)" 그대로이고 4단계 중 2번 고친 기록이 문서 안에 없다(커밋 메시지에만 있음). CLAUDE.md 변경 이력에도 3단계 확정·T24 행만 있다 |
| C6 | T21의 커밋 종류 | TASKS는 T21 커밋을 `feat:`로 지정했으나, 조립을 T13으로 옮긴 뒤 T21은 테스트만 추가한다(실제 `85ddf90`은 `ApiIntegrationTest.java` 1개 추가). CLAUDE.md 2.6.3은 테스트만 변경하면 `test:`다(T22는 `test:`로 맞게 커밋됨). `dad4a0f`가 T21의 커밋 종류를 갱신하지 않았다 |
| C7 | TASKS의 "커밋:" 줄 ↔ 실제 | TASKS는 `feat: 오류 응답 형식…` 식 메시지를 지정했지만 실제 커밋은 사용자가 정한 `feat: 4단계 T0N …` 형식이다 | 

C6·C7은 이미 병합된 이력이라 고치려면 이력 재작성이 필요하다. CLAUDE.md 2.5.5에 따라 사람의 확인 없이는 하지 않으며, 권장하지 않는다. TASKS에 사실을 적는 쪽으로 정리한다.

## 6. 어긋남 분류
| # | 어긋남 | 분류 | 이유 | 후속 작업 |
| --- | --- | --- | --- | --- |
| F1 | `/error` 직접 접근이 500 기본 모양 | **구현 수정** | 명세가 명확하다(표에 없는 경로는 404 `ENDPOINT_NOT_FOUND`, 모든 실패는 같은 모양). 구현이 따르지 못함 | 새 테스트(Red) 먼저. 설계 선택은 구현 시 보고(예: 기본 오류 경로 비활성화 또는 처리기) |
| F2 | `status=%zz` → 500 | **구현 수정** | AC-04-5의 문장이 "전체, 진행 중, 완료 외의 보기 구분"이다. 깨진 값도 여기에 속하고, 클라이언트 잘못이 500이면 AC-14-4(예상 못한 서버 오류만 500)의 취지에도 어긋난다. 어느 코드로 보낼지는 Q4에서 확인 | 새 테스트(Red) 먼저 |
| F3 | 깨진 URL → 톰캣 HTML 400 | **명세 수정 (역류)** — Q1 | 현재 설계(스프링 층)에서는 해결할 수 없고 톰캣 설정이 필요하다. TRD 6장 "멈추는 규칙"대로 새 문구·새 방식을 임의로 정하지 않는다. 기본 권장은 API_SPEC·TRD의 알려진 한계에 적는 것 | 문서 수정 → 승인 → (선택) 구현 |
| F4 | 응답에 `charset` 없음 | **명세 수정** — Q3 | 실제 영향 없음. 구현 변경보다 문구 정정(예: "JSON은 UTF-8이며 charset 표기는 계약에 포함하지 않는다")이 단순 | 문서 수정 |
| U2 | `HEAD`/`OPTIONS` 기본 동작 | **명세 수정** — Q2 | API_SPEC 1.1이 "계약에 넣지 않는다"고만 적어 동작이 정의되어 있지 않다. 구현은 프레임워크 기본 | 문서 수정 |
| U4·U5 | 중복 키 마지막 값, 다른 charset 본문 허용 | **명세 수정 (선택)** | 명세가 침묵한다. 위험이 작아 "계약 밖"이라고 한 줄 적으면 충분 | 문서 수정 (Q2와 묶음) |
| 3.1 | `status=all` 테스트 없음 | **테스트 보강** (구현·명세 수정 아님) | 동작은 명세와 같음(직접 호출로 확인). 커버리지 1줄 | 테스트 추가 (F2 작업과 묶을 수 있음) |
| C4·C5·C6·C7 | TASKS 요약 미반영, 변경 이력 없음, T21 커밋 종류, 커밋 형식 | **명세(문서) 수정** — Q5 | 코드와 무관한 문서 기록 문제. 이력 재작성은 하지 않는다 | `docs:` 커밋 하나 |

## 7. 재현 방법
```bash
./gradlew clean build                 # 1장
./gradlew jacocoTestReport            # build/reports/jacoco/test/html 생성
# JDK 21로 실행 (기본 java가 17이면 JAVA_HOME 지정)
java -jar build/libs/marvin-sdd-todo-app-server-0.0.1-SNAPSHOT.jar --server.port=18080
curl -i http://localhost:18080/error                    # F1
curl -i "http://localhost:18080/todos?status=%zz"       # F2
curl -i --path-as-is http://localhost:18080/todos/%zz   # F3
```
