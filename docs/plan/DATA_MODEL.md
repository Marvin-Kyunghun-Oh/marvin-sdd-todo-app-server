# DATA_MODEL — Marvin's Todo App 서버

> SDD 2단계(Plan) 산출물. 상태: **초안 (2026-11-11)**
> TRD 4장(도메인 모델)과 3.1절의 저장소 계약을 옮겨 온 문서다 (TD-28, TD-35). 이 문서가 데이터의 모양과 규칙의 기준이다.
> 근거 표기: `FR-xx`/`AC-xx-x`는 `docs/spec/REQUIREMENTS.md`, `D-xx`는 `docs/spec/DECISION_LOG.md`, `TD-xx`는 `docs/plan/DECISION_LOG.md`, `TRD n`은 `docs/plan/TRD.md`의 n장.
> 경로, HTTP 메서드, 상태 코드는 `API_SPEC.md`에서 다룬다. 명세와 TRD에 없는 필드(생성 시각 등)는 두지 않는다 (CLAUDE.md 2.1.5, PRD 5.2 "완료 시각 기록").

## 1. Todo (도메인, `domain`)
불변 객체(`record`)다. 수정은 새 사본을 만들어 저장소에서 교체한다 (TRD 3.1).

| 필드 | 타입 | null | 규칙 | 근거 |
| --- | --- | --- | --- | --- |
| id | `long` | 불가 | 1 이상. 저장소가 발급한다. 삭제된 id는 다시 쓰지 않는다 | TD-03, D-26, AC-12-1 |
| title | `String` | 불가 | 공백 제거 후 1~100 코드포인트 | FR-02, D-01, D-03, D-28 |
| description | `String` | 허용 | 공백 제거 후 1~1,000 코드포인트. 제거 후 비면 null | FR-02, D-02, D-27 |
| dueDate | `LocalDate` | 허용 | 시각 없는 날짜. 과거 날짜 허용 | FR-09, D-25 |
| completed | `boolean` | 불가 | 새 할 일은 false | FR-01, D-14 |

저장하지 않는 값은 3장에서 계산한다. 이 표에 없는 필드는 만들지 않는다.

## 2. 불변식과 정규화 (`domain`, TD-36, TD-40)
규칙을 어긴 `Todo`가 존재할 수 없도록 역할을 둘로 나눈다.
- **팩토리** (`NewTodo.of`, `TodoUpdate.of`): 사용자가 보낸 **원시 입력**을 받아 정규화하고 검증한다. 입력 오류는 오류 코드를 가진 도메인 예외(`TITLE_REQUIRED` 등)로 돌려준다. 검증 순서는 2.4다.
- **`record`의 compact 생성자** (`Todo`, `NewTodo`, `TodoUpdate`): 이미 정규화된 값이 불변식을 지키는지 마지막으로 확인한다(id ≥ 1, `title`이 공백 제거된 1~100자, `description`이 null이거나 공백 제거된 1~1,000자 등). 어기면 `IllegalArgumentException`을 던진다. `record`의 생성자는 public이라 팩토리를 거치지 않고 만들 수 있기 때문이다. 이 예외는 프로그래밍 오류이므로 응답으로는 `INTERNAL_ERROR`가 된다 (TRD 6).
- 따라서 `NewTodo`, `TodoUpdate`, `Todo`는 모두 **검증이 끝난 값**이다. 원시 입력은 `application`의 명령 객체(문자열)로만 존재한다 (5장).
- 저장소는 입력을 검증하거나 정규화하지 않는다. `NewTodo`에 id만 붙여 `Todo`를 만든다 (6장).

### 2.1 공백 제거 (D-03, D-27, TD-33)
- `title`과 `description`은 앞뒤 공백을 제거한 값이 저장 값이다. 가운데 공백은 건드리지 않는다.
- 공백은 **유니코드 공백**(`String.strip()`)이다. 스페이스, 탭, 줄바꿈, 전각 공백(U+3000)을 포함한다.
- 한계 (TD-33): 줄바꿈 없는 공백(U+00A0), 제로폭 공백(U+200B)은 공백으로 보지 않는다. 이런 문자만 있는 제목은 비어 있는 제목이 아니다.
- `title`이 `null`이거나 제거 후 비면 `TITLE_REQUIRED`다 (AC-02-1, AC-02-2, AC-02-5).
- `description`이 `null`이거나 제거 후 비면 설명 없음(null)이다 (AC-08-5, AC-08-6).

### 2.2 글자 수 (D-28, TD-10)
- 공백 제거 **후** 유니코드 코드포인트 수(`String.codePointCount`)를 센다. 한글, 영문, 숫자, 공백, 이모지는 1자다.
- ZWJ 조합 이모지(👨‍👩‍👧), 국기는 2자 이상으로 센다. 알려진 한계다.
- 제목: 100자 이하(100자는 통과, 101자는 `TITLE_TOO_LONG`, AC-02-3, AC-02-4). 설명: 1,000자 이하(1,001자는 `DESCRIPTION_TOO_LONG`, AC-02-6).

### 2.3 마감일 텍스트 (FR-09, D-38, TD-34)
- 도메인은 사용자가 보낸 마감일 **텍스트**를 받아 파싱한다 (TRD 3.2). 설명과 같은 규칙으로 읽는다 (D-27, D-38).
  1. `null`이면 마감일 없음이다.
  2. 앞뒤 공백을 제거(2.1의 유니코드 공백)한 값이 비어 있으면 마감일 없음이다. `""`과 `"   "`가 여기에 든다 (AC-09-6).
  3. 아니면 제거한 값을 파싱한다. 형식은 `YYYY-MM-DD`뿐이다. ASCII 숫자 4자리-2자리-2자리이고 실제 달력에 있는 날짜여야 한다.
- 다음은 `INVALID_DUE_DATE`다 (AC-09-4): `2026-1-5`, `2026-11-12T00:00`, `+12345-01-01`, `2026-02-30`, `abc`, 문자열이 아닌 값의 원문 텍스트(`123`, `true` 등).
- `" 2026-11-12 "`처럼 앞뒤 공백이 있는 값은 공백을 제거한 뒤 파싱하므로 통과한다.
- 마감일을 지우는 방법은 `null`, `""`, 공백뿐인 문자열이다 (AC-09-2, AC-09-6).
- 과거 날짜는 거부하지 않는다 (D-25, AC-09-5).
- 구현 메모: 형식은 정규식 `[0-9]{4}-[0-9]{2}-[0-9]{2}`로 먼저 걸러 낸 뒤 `LocalDate`로 파싱한다. 날짜 패턴만으로는 5자리 이상 연도가 통과할 수 있다.

### 2.4 검증 순서 (TD-19)
한 입력에 오류가 여럿이면 **첫 오류 하나**만 돌려준다. 순서는 제목 → 설명 → 마감일이다. 검증이 끝나기 전에는 저장소를 건드리지 않는다 (AC-08-3, AC-14-2).

## 3. 저장하지 않고 계산하는 값

### 3.1 기준 시각 (D-10, TD-37)
- "오늘"은 주입된 `Clock`(`Asia/Seoul`)으로 `LocalDate.now(clock)`을 계산한 값이다.
- 요청 처리 한 번에 **한 번만** 계산해 그 요청의 모든 항목에 같게 적용한다. 항목마다 따로 계산하지 않는다.

### 3.2 overdue (FR-10, TD-41)
```
overdue(todo, today) = !todo.completed && todo.dueDate != null && todo.dueDate < today
```
- 판정식은 `domain`의 `Todo.isOverdue(today)`다. `Clock`을 아는 것은 `application`뿐이다. `application`이 오늘을 한 번 계산해 항목마다 `isOverdue(today)`를 호출하고, 결과를 `overdue`가 포함된 `TodoView(todo, overdue)`로 돌려준다. `presentation`은 `Clock`에 의존하지 않는다 (5.3).
| 경우 | 결과 | 근거 |
| --- | --- | --- |
| 진행 중, 마감일이 오늘보다 이전 | true | D-08, AC-10-1 |
| 진행 중, 마감일이 오늘 | false | D-08, AC-10-2 |
| 진행 중, 마감일이 오늘보다 이후 | false | AC-10-3 |
| 완료, 마감일이 지남 | false | D-09, AC-10-4 |
| 마감일 없음 | false | AC-09-2 |
- 완료로 표시하면 같은 항목의 overdue는 해제된다 (AC-10-5).
- 한국 시간 00:30(UTC로는 전날)처럼 두 시간대의 날짜가 다른 시각에도 한국 날짜로 판정한다 (AC-10-6).

### 3.3 summary (FR-05, D-12, TD-37)
`application`의 값 객체(`Summary`)이고, 한 번의 `findAll()` 결과(스냅샷)에서 계산한다 (TRD 3.1).
| 값 | 계산 | 근거 |
| --- | --- | --- |
| total | 전체 개수 | AC-05-1 |
| active | `completed == false`인 개수 | AC-05-1 |
| done | `completed == true`인 개수 | AC-05-1 |
| remaining | `active`와 같은 값 | D-12 |
- 항상 전체 기준이다. 상태별 보기 선택에 영향받지 않는다 (AC-05-2). 할 일이 없으면 모두 0이다 (AC-05-3, AC-13-1).
- 항상 `total == active + done`이다.

## 4. 입력의 모양과 상태 변화

입력은 두 단계 타입으로 나눈다 (TD-40). 원시 입력은 `application`의 명령 객체이고, 도메인 팩토리를 거쳐야 검증된 값이 된다.

### 4.1 추가 (FR-01, D-06, TD-36, TD-40)
| 타입 | 위치 | 필드 | 설명 |
| --- | --- | --- | --- |
| `CreateTodoCommand` | `application` | `title: String` (null 가능) | **원시 입력.** 검증 전 |
| `NewTodo` | `domain` | `title: String` (null 불가) | **검증이 끝난 값.** 공백 제거 후 1~100자 (2.1~2.2) |

- `NewTodo.of(rawTitle)`가 정규화와 검증을 하고, 실패하면 `TITLE_REQUIRED`/`TITLE_TOO_LONG` 도메인 예외를 던진다.
- 설명과 마감일은 추가 때 받지 않는다. 만들어지는 `Todo`는 `description=null`, `dueDate=null`, `completed=false`다 (AC-01-1).
- 같은 제목이 이미 있어도 별도 항목이 만들어진다 (D-05, AC-01-2).

### 4.2 수정 (FR-08, TD-18, TD-36, TD-40)
| 타입 | 위치 | 필드 | 설명 |
| --- | --- | --- | --- |
| `UpdateTodoCommand` | `application` | `title`, `description`, `dueDateText`: `String` (null 가능), `completed: boolean` | **원시 입력.** 검증 전 |
| `TodoUpdate` | `domain` | `title: String`, `description: String` (null 가능), `dueDate: LocalDate` (null 가능), `completed: boolean` | **검증이 끝난 값** |

- `TodoUpdate.of(rawTitle, rawDescription, rawDueDateText, completed)`가 2.4의 순서(제목 → 설명 → 마감일)로 검증한다. 첫 오류 하나만 도메인 예외로 던진다.
- `title`이 null이면 `TITLE_REQUIRED`, `description`이 null이면 설명 없음, `dueDateText`는 2.3을 따른다. `completed`는 필수다.
- 수정은 **교체**다. 네 값이 기존 값을 모두 대체한다. 생략된 값을 "그대로 둔다"로 보지 않는다 (TD-18).
- `Todo.apply(TodoUpdate)`는 같은 id의 새 `Todo`를 돌려준다. `id`는 바뀌지 않는다. 검증이 실패하면 `Todo`가 만들어지지 않아 기존 값은 하나도 바뀌지 않는다 (AC-08-3).

### 4.3 완료 표시와 되돌리기 (FR-06, D-14, D-29)
| 현재 | 요청 | 결과 |
| --- | --- | --- |
| 진행 중 | 완료로 표시 | 완료 (요약의 active −1, done +1, AC-06-1) |
| 완료 | 진행 중으로 되돌리기 | 진행 중 (AC-06-2) |
| 완료 | 완료로 표시 | 변화 없음, 오류 없음 (AC-06-3) |
| 진행 중 | 진행 중으로 되돌리기 | 변화 없음, 오류 없음 (AC-06-4, D-29) |
- 바뀌는 것은 `completed`뿐이다. 나머지 필드와 id는 그대로다.
- 완료한 시각은 기록하지 않는다 (PRD 5.2).

## 5. 레이어별 표현과 변환 (TRD 2, TD-39)
```
요청 DTO ─(presentation 매퍼)─▶ Command(원시 문자열) ─(application)─▶ NewTodo/TodoUpdate(검증됨) ─▶ Todo
                                                                          │
응답 DTO ◀─(presentation 매퍼, 옮겨 담기만)─ TodoView / ListResult ◀─(application: 오늘 계산, 요약)─┘
```
`Todo`는 응답으로 직접 나가지 않고, 요청 DTO는 `domain`으로 들어가지 않는다 (CLAUDE.md 2.3.3). `JsonNode`는 `presentation` 밖으로 나가지 않는다 (CLAUDE.md 2.3.2). `application`이 돌려주는 결과 타입(`TodoView`, `ListResult`)은 5.3에 있다.

### 5.1 요청 DTO (`presentation`)
| DTO | 필드와 타입 | 비고 |
| --- | --- | --- |
| CreateTodoRequest | `title: String` | 없거나 null이면 `TITLE_REQUIRED` (TD-21) |
| UpdateTodoRequest | `title: String`, `description: String`, `dueDate: JsonNode`, `completed: Boolean` | `completed`가 없거나 null이면 `MALFORMED_REQUEST` (D-32) |
| CompleteTodoRequest | `completed: Boolean` | 없거나 null이면 `MALFORMED_REQUEST` (D-32) |
- 스칼라 강제 변환은 끈다. `title: 123`, `completed: "true"`, 문자열이 아닌 `description`은 `MALFORMED_REQUEST`다 (TRD 3.2, AC-14-3). 알 수 없는 필드도 같다.
- `dueDate`만 `JsonNode`로 받는다. `LocalDate`나 `String`으로 받으면 `INVALID_DUE_DATE`가 `MALFORMED_REQUEST`로 바뀌기 때문이다 (TD-20).
- `completed`는 래퍼 타입(`Boolean`)이어야 "없음"을 구분할 수 있다.

### 5.2 요청 → 도메인 변환
| 요청 값 | 도메인에 넘기는 값 |
| --- | --- |
| `title` | 문자열 그대로, 없으면 `null` |
| `description` | 문자열 그대로, 없으면 `null` |
| `dueDate`가 없음, `null`, 또는 JSON null | `dueDateText = null` |
| `dueDate`가 JSON 문자열 | 그 문자열 그대로 (`""`, 공백 포함) |
| `dueDate`가 그 밖의 JSON 값 (숫자, 불리언, 객체, 배열) | JSON 원문 텍스트 (예: `123`, `true`, `{"a":1}`) |
- 원문 텍스트는 2.3의 형식이 될 수 없으므로 도메인에서 `INVALID_DUE_DATE`로 거부된다 (TD-20). 그래서 검증 순서(제목→설명→마감일)도 그대로 지켜진다.
- 정규화(공백 제거, 빈 설명 → null, 빈 마감일 → 없음)는 `presentation`이 하지 않고 `domain`이 한다 (2.1, 2.3).
- 변환 결과는 `application`의 명령 객체(`CreateTodoCommand`, `UpdateTodoCommand`)이며 모두 문자열이다. 도메인 타입은 `application`이 팩토리로 만든다 (4장).

### 5.3 application 결과와 응답 DTO (FR-03, FR-05, FR-07, TD-41)
`application`이 `Clock`으로 오늘을 한 번 계산해 결과를 만들고, `presentation` 매퍼는 이를 응답 DTO로 옮겨 담기만 한다. `presentation`은 `Clock`과 `overdue` 판정식을 모른다.
| application 결과 | 필드 |
| --- | --- |
| `TodoView` | `todo: Todo`, `overdue: boolean` |
| `ListResult` | `items: List<TodoView>`, `summary: Summary` |

| DTO | 필드와 타입 | 비고 |
| --- | --- | --- |
| TodoResponse | `id: long`, `title: String`, `description: String \| null`, `dueDate: String \| null`, `completed: boolean`, `overdue: boolean` | AC-03-3, AC-07-1 |
| SummaryResponse | `total`, `active`, `done`, `remaining` (모두 정수) | AC-05-1 |
| TodoListResponse | `items: TodoResponse[]`, `summary: SummaryResponse` | TD-05 |
- `dueDate`는 `YYYY-MM-DD` 문자열(예: `"2026-11-12"`)로 내보내고, 없으면 `null`이다 (AC-09-3).
- `description`이 없으면 필드를 빼지 않고 `null`로 내보낸다.
- `overdue`는 `TodoView.overdue`를 그대로 담는다. 목록의 모든 항목이 같은 "오늘"을 쓴다 (3.1, 3.2).
- `items`는 `findAll()` 결과 순서(id 오름차순)를 유지한다 (D-11, AC-03-1, AC-03-2).

## 6. TodoRepository 계약 (`domain` 인터페이스, `persistence` 구현, TD-35)
| 메서드 | 반환 | 계약 |
| --- | --- | --- |
| `create(NewTodo)` | `Todo` | 검증이 끝난 `NewTodo`에 새 id만 붙여 `Todo`(`description=null`, `dueDate=null`, `completed=false`)를 만들어 저장하고 돌려준다 (TD-40) |
| `findById(long id)` | `Optional<Todo>` | 없으면 빈 `Optional` |
| `findAll()` | `List<Todo>` | **id 오름차순**(= 등록 순서)의 변경 불가능한 스냅샷. 없으면 빈 목록 |
| `update(long id, UnaryOperator<Todo> change)` | `Optional<Todo>` | 대상이 있으면 `change`를 적용해 교체하고 새 값을 돌려준다. 없으면 빈 `Optional`이며 아무것도 바꾸지 않는다. 읽고 교체하는 동작은 원자적이다 |
| `deleteById(long id)` | `boolean` | 삭제했으면 true, 없었으면 false |

- **id 발급:** 1부터 시작해 하나씩 증가하는 값이다. 발급은 원자적이며 중복이 없다 (스레드 안전, TRD 3.1). 삭제된 id는 다시 발급하지 않는다 (TD-03, AC-12-1).
- **정렬 기준:** id 오름차순 하나뿐이다. 마감일, 완료 여부와 무관하다 (D-11, AC-03-2). 정렬 방식을 바꾸는 기능은 범위 밖이다 (PRD 5.2).
- **update의 제약:** `change`가 돌려준 `Todo`의 id가 다르면 거부한다(`IllegalStateException`). id는 바뀌지 않는다.
- **수정 흐름 (TD-42):** `findById` → 도메인 검증(`TodoUpdate.of`) → `update(id, old -> old.apply(검증된 값))`. 존재 확인이 검증보다 먼저이므로 없는 id는 값 검증 오류보다 404가 먼저 나오고(TD-19), 검증이 `update` 호출 전에 끝나므로 실패하면 저장소를 건드리지 않아 원본이 그대로다(AC-08-3, AC-12-2).
- **조회와 수정 사이 삭제:** 서비스가 `findById`로 존재를 확인한 뒤 `update`를 호출했는데 그 사이에 삭제되어 `update`가 빈 값을 돌려주면 서비스는 `TODO_NOT_FOUND`로 처리한다 (FR-12).
- **완료 표시 흐름:** 검증할 값이 없으므로 `update(id, old -> old.withCompleted(값))`만 호출한다. 빈 값이 돌아오면 `TODO_NOT_FOUND`다 (AC-12-2).
- **상태별 보기 필터는 저장소가 하지 않는다 (TD-38).** `application`이 `findAll()` 결과를 받아 `all`/`active`/`done`으로 거르고, 같은 결과에서 요약을 계산한다. 필터와 요약이 어긋나지 않는다 (AC-04-1~3, AC-05-2).
- 저장소는 입력을 검증하거나 정규화하지 않는다. 그것은 `domain` 팩토리의 책임이다 (2장). 저장소가 받는 `NewTodo`는 이미 검증된 값이고, 만든 `Todo`는 compact 생성자의 마지막 확인을 거친다.

## 7. 근거 추적표
| 이 문서의 절 | 근거 |
| --- | --- |
| 1 Todo | FR-01, FR-02, FR-08, FR-09, D-01~D-03, D-25~D-28, TD-03 |
| 2 불변식 | FR-02, D-01~D-03, D-27, D-28, D-38, AC-09-4, AC-09-6, TD-10, TD-19, TD-33, TD-34, TD-36, TD-40 |
| 3 계산 값 | FR-05, FR-10, D-08~D-10, D-12, TD-37, TD-41 |
| 4 입력과 상태 변화 | FR-01, FR-06, FR-08, D-05, D-06, D-14, D-29, TD-18, TD-36, TD-40 |
| 5 DTO와 변환 | FR-03, FR-05, FR-07, TD-05, TD-20, TD-21, TD-39, TD-41, CLAUDE.md 2.3.2, 2.3.3 |
| 6 저장소 계약 | FR-03, FR-04, FR-12, D-11, TD-03, TD-19, TD-24, TD-35, TD-38, TD-40, TD-42 |
