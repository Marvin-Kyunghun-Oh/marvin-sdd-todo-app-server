# TRD — Marvin's Todo App 서버

> SDD 2단계(Plan) 산출물. 상태: **초안 (2026-11-11)** — 사람의 승인 전
> 근거 표기: `FR-xx`/`AC-xx-x`는 `docs/spec/REQUIREMENTS.md`, `D-xx`는 `docs/spec/DECISION_LOG.md`, `TD-xx`는 `docs/plan/DECISION_LOG.md`, `CLAUDE.md x.x`는 헌법.
> 충돌 시 명세가 우선한다 (CLAUDE.md 2.2.5). 이 문서가 명세를 바꾸는 항목은 7장에 모았고, 승인 전에는 구현하지 않는다 (CLAUDE.md 2.2.4).

## 1. 기술 스택 (TD-01, TD-02, CLAUDE.md 4장)
| 항목 | 선택 | 근거 |
| --- | --- | --- |
| 언어 | Java 21 | TD-01. 표준 `java.time`으로 KST 날짜(D-10)와 `Clock` 주입(CLAUDE.md 2.3.4)을 처리 |
| 프레임워크 | Spring Boot 3.x (`spring-boot-starter-web`) | TD-01 |
| 저장소 | 인메모리 | TD-02. 로컬 동작 우선, 영속성은 이번 범위 밖 |
| 테스트 | JUnit 5 + Mockito (`spring-boot-starter-test`) | TD-12 |
| 커버리지 | JaCoCo, 라인 80% 이상 | CLAUDE.md 2.4.2, TD-16 (승인됨) |
| 빌드 | Gradle (Kotlin DSL) | TD-17 |

입력 검증은 직접 구현한다. D-28(코드포인트 단위 글자 수)과 D-03/D-27(공백 제거)이 표준 어노테이션과 맞지 않아, `spring-boot-starter-validation`은 추가하지 않는다 (CLAUDE.md 2.1.5).

## 2. 레이어 구조 (CLAUDE.md 2.3.1~2.3.5, TD-11)
```
presentation ──▶ application ──▶ domain ◀── persistence
(HTTP, DTO)      (유스케이스)     (규칙, 포트)   (인메모리 구현)
```
| 패키지 (`com.marvin.todo`) | 책임 | 근거 |
| --- | --- | --- |
| `presentation` | 컨트롤러, 요청/응답 DTO, 오류 변환 | FR-14, CLAUDE.md 2.3.3 |
| `application` | `TodoService`: 유스케이스 조합, 요약 계산 | FR-05, FR-06 |
| `domain` | `Todo`, 입력 검증, 기한 지남 판정, `TodoRepository` 인터페이스, 도메인 예외 | FR-02, FR-10 |
| `persistence` | `InMemoryTodoRepository` | TD-02, CLAUDE.md 2.3.2 |

- 의존은 바깥에서 안쪽으로만 향한다. `domain`은 Spring, HTTP, 저장 방식을 모른다 (CLAUDE.md 2.3.2). 순환 의존은 금지한다 (2.3.5).
- 응답 DTO는 `Todo`와 별개 타입이다. 영속성 모델을 그대로 노출하지 않는다 (CLAUDE.md 2.3.3).
- `Clock`은 `Asia/Seoul` 고정 빈으로 주입하고 테스트에서 대체한다 (D-10, AC-10-6, CLAUDE.md 2.3.4).

## 3. 도메인 모델 (FR-01, FR-08, FR-09, TD-03)
| 필드 | 타입 | 규칙 | 근거 |
| --- | --- | --- | --- |
| id | `long` | 1부터 자동 증가. 삭제된 id는 재사용하지 않는다 | TD-03, D-26, AC-12-1 |
| title | `String` | 공백 제거 후 1~100 코드포인트 | FR-02, D-01, D-03, D-28 |
| description | `String` (null 허용) | 공백 제거 후 0~1,000 코드포인트. 빈 값은 null | FR-02, D-02, D-27 |
| dueDate | `LocalDate` (null 허용) | 과거 날짜 허용 | FR-09, D-25 |
| completed | `boolean` | 새 할 일은 false | FR-01 |

- 기한 지남(`overdue`)은 저장하지 않고 조회 시 `Clock` 기준 오늘로 계산한다: `!completed && dueDate != null && dueDate < today` (FR-10, D-08, D-09, D-10).
- 글자 수는 `String.codePointCount`로 센다. ZWJ 조합 이모지(예: 👨‍👩‍👧)나 국기는 2자 이상으로 센다. 이는 알려진 한계다 (TD-10, D-28).
- 생성 시 id가 곧 등록 순서다 (D-11, FR-03).

## 4. API 설계 (TD-04~TD-07)
JSON, 필드는 camelCase, 상태 값은 영어 (TD-04).

| 요구 | 메서드와 경로 | 요청 | 성공 응답 | 근거 |
| --- | --- | --- | --- | --- |
| 추가 | `POST /todos` | `{title}` | 201 + Todo | FR-01, D-06 |
| 목록 + 요약 | `GET /todos?status=all\|active\|done` | - | 200 + `{items, summary}` | FR-03, FR-04, FR-05, FR-13, TD-05 |
| 한 건 | `GET /todos/{id}` | - | 200 + Todo | FR-07 |
| 수정 | `PUT /todos/{id}` | `{title, description, dueDate, completed}` | 200 + Todo | FR-08, FR-09, TD-06 |
| 완료 표시/되돌리기 | `PATCH /todos/{id}` | `{completed}` | 200 + Todo | FR-06, D-14, D-29, TD-06 |
| 삭제 | `DELETE /todos/{id}` | - | 204 | FR-11, D-15 |

**Todo 응답:** `{id, title, description, dueDate, completed, overdue}` (AC-03-3, AC-07-1). 설명과 마감일이 없으면 `null`이다 (AC-09-3).

**목록 응답:** `{items: [Todo...], summary: {total, active, done, remaining}}` (AC-05-1~3). `status`를 생략하면 `all`이다 (FR-04). `summary`는 `status` 값과 무관하게 항상 전체 기준이다 (AC-05-2, D-12). `remaining`은 `active`와 같은 값이다. 서버가 계산해 주면 화면이 "N개 남음"에 그대로 쓸 수 있다.

**멱등성:** 이미 완료된 항목을 완료로 표시하거나 이미 진행 중인 항목을 되돌려도 200이다 (AC-06-3, AC-06-4).

**수정(PUT) 규칙** (TD-06): `title`은 필수다. `description`, `dueDate`는 생략하거나 `null`이면 없음으로 저장한다. 따라서 "필드 없음"과 "삭제"를 구분할 필요가 없고 (AC-09-2, AC-08-5), 마감일 지우기는 `null`로 보낸다. `completed`는 필수 불리언이다 (AC-08-2, TD-18).

**날짜:** ISO `YYYY-MM-DD` 문자열이다. 파싱에 실패하는 값(`"abc"`, `2026-02-30` 등)은 모두 날짜 오류다 (AC-09-4, TD-07).

## 5. 오류 응답 (FR-14, TD-08)
모든 실패는 같은 구성이다 (AC-14-1):
```json
{ "error": { "code": "TITLE_REQUIRED", "message": "제목을 입력해 주세요." } }
```
| code | HTTP | message | 근거 |
| --- | --- | --- | --- |
| `TITLE_REQUIRED` | 400 | 제목을 입력해 주세요. | AC-02-1, AC-02-2, AC-02-5 |
| `TITLE_TOO_LONG` | 400 | 제목은 100자 이하로 입력해 주세요. | AC-02-3 |
| `DESCRIPTION_TOO_LONG` | 400 | 설명은 1,000자 이하로 입력해 주세요. | AC-02-6 |
| `INVALID_DUE_DATE` | 400 | 마감일을 올바른 날짜로 입력해 주세요. | AC-09-4 |
| `TODO_NOT_FOUND` | 404 | 할 일을 찾을 수 없어요. 이미 삭제되었을 수 있어요. | FR-11 AC-11-2, FR-12 |
| `INVALID_STATUS_FILTER` | 400 | 보기 구분이 올바르지 않아요. 전체, 진행 중, 완료 중에서 골라 주세요. | AC-04-5, D-30, TD-08 |
| `MALFORMED_REQUEST` | 400 | 요청 내용을 읽을 수 없어요. 입력한 내용을 확인해 주세요. | AC-14-3, D-31, TD-09 |

- 실패한 요청은 저장 상태를 바꾸지 않는다 (AC-08-3, AC-12-2, AC-14-2). 검증은 저장 호출 전에 끝낸다.
- 오류를 삼키지 않는다. 처리되지 않은 예외는 500과 같은 모양의 안내로 변환하고, 내부 정보는 노출하지 않는다 (FR-14, CLAUDE.md 2.4.3).
- 한 요청에 오류가 여럿이면 첫 오류 하나만 돌려준다. 검증 순서는 제목, 설명, 마감일이다 (TD-19).
- 존재하지 않는 id에 본문이 잘못된 수정 요청이 오면 404를 먼저 돌려준다 (FR-12, TD-19).

## 6. 샘플 데이터 (TD-13)
- 로컬 실행에서만 샘플 할 일을 미리 채운다. `local` 프로파일 전용 `ApplicationRunner`가 `TodoService`를 통해 넣는다.
- 마감일은 `Clock` 기준 오늘에 대한 상대 날짜로 만든다. 기한 지남, 오늘 마감, 미래, 마감 없음, 완료 항목을 골고루 포함한다 (FR-04, FR-10 화면 확인용).
- 테스트에서는 로드하지 않는다. 테스트 독립성을 지키기 위함이다 (CLAUDE.md 2.4.1).
- 이 기능은 명세(PRD)에 없는 개발 편의 기능이다. 사람이 요청했으므로 포함하되 PRD 5.1 범위 밖임을 TD-13에 기록한다 (CLAUDE.md 2.2.1).

## 7. 명세 역류 (CLAUDE.md 2.2.4)
이 TRD가 일으킨 명세 변경은 승인(2026-11-11)을 받아 `REQUIREMENTS.md`에 먼저 반영했다.
- 알 수 없는 `status` → AC-04-5, D-30 (TD-08)
- 읽을 수 없는 요청 내용 → AC-14-3, D-31 (TD-09)

## 8. 테스트 전략 (CLAUDE.md 2.1, 2.4)
- 각 AC를 테스트 하나 이상으로 옮긴다. 이름에 AC 번호를 넣는다 (PRD 6장).
- 도메인 규칙(검증, 기한 지남, 글자 수)은 순수 JUnit 단위 테스트로 한다. Spring을 띄우지 않는다.
- 서비스는 Mockito로 `TodoRepository`와 `Clock`을 대체해 검증한다.
- API는 `@WebMvcTest`(+ 서비스 대체) 또는 `MockMvc` + 인메모리 저장소로 상태 코드와 오류 문구를 정확한 문장으로 검사한다 (D-24).
- 시간은 고정 `Clock`으로 주입한다. AC-10-6은 UTC 날짜와 KST 날짜가 다른 시각으로 검증한다.
- 각 테스트는 새 저장소를 만들어 독립적으로 실행한다 (CLAUDE.md 2.4.1).

## 9. FR 추적표
| FR | 구현 위치 |
| --- | --- |
| FR-01 | `POST /todos`, `TodoService.create` |
| FR-02 | `domain` 입력 검증 (제목/설명) |
| FR-03, FR-04, FR-13 | `GET /todos`, `status` 필터, 저장소의 id 순 조회 |
| FR-05 | `TodoService`의 요약 계산 |
| FR-06 | `PATCH /todos/{id}` |
| FR-07 | `GET /todos/{id}` |
| FR-08, FR-09 | `PUT /todos/{id}`, 날짜 파싱 |
| FR-10 | `domain` 기한 지남 판정 + `Clock` |
| FR-11, FR-12 | `DELETE`, `TODO_NOT_FOUND` 공통 처리 |
| FR-14 | `presentation` 전역 오류 변환 |
