# CLAUDE.md — marvin's Todo (Server)

Marvin's Todo 앱의 Java + Spring Boot 기반 서버.
SDD + TDD 방식으로 개발한다.
이 파일은 진입점이며, 기술 스택과 코딩·테스트 규칙은 `docs/TRD.md`에 있다.

## 현재 상태

- 단계: 2. Specify (REQUIREMENTS 작성 중)
- 진행 중인 작업: 없음

> 이 섹션은 사람이 갱신한다.

## 작업 시작 시

1. `docs/TRD.md`를 읽는다. (항상)
2. 위 "현재 상태"에서 단계와 작업을 확인한다.
3. 아래 문서 지도에서 작업에 필요한 문서만 골라 읽는다.

## 문서 지도

| 문서 | 단계 | 역할 | 소유 | 언제 읽나 |
|---|---|---|---|---|
| `docs/TRD.md` | Constitution | 기술 스택, 아키텍처, 코딩·TDD 규칙 | 사람 | 항상 |
| `docs/wireframes/` | Specify | 화면 기획 (서버 요구사항의 근거) | 사람 | 요구사항 작성 시 |
| `docs/PRD.md` | Specify | 제품 목적, 대상, 범위 | 사람 | 요구사항 작성, 범위 판단 시 |
| `docs/REQUIREMENTS.md` | Specify | 기능 요구사항(REQ-xxx), 인수 조건 | 사람 | 설계, 검증 시 |
| `docs/DATA_MODEL.md` | Plan | 엔티티, 필드, 제약 조건 | 에이전트 초안 → 사람 승인 | Entity, Repository 작업 시 |
| `docs/API_SPEC.md` | Plan | 엔드포인트, 요청/응답, 에러 코드 | 에이전트 초안 → 사람 승인 | Controller, DTO 작업 시 |
| `docs/TEST_CASES.md` | Plan | 테스트 케이스(TC-xxx)와 REQ 매핑 | 에이전트 초안 → 사람 승인 | 모든 구현 작업 시 |
| `docs/BACKEND_TASKS.md` | Tasks | 작업 목록(T-xxx), 진행 상태 | 에이전트 초안 → 사람 승인 | 작업 시작, 완료 시 |

## 문서 우선순위

충돌 시 위쪽이 기준이다.

```
TRD > REQUIREMENTS > DATA_MODEL · API_SPEC > TEST_CASES > BACKEND_TASKS > 코드
```

PRD는 REQUIREMENTS의 근거 문서다. 하위 문서가 상위 문서와 충돌하면 어느 쪽도 고치지 말고 멈춰서 보고한다.

## 단계와 승인 게이트

```
Constitution → Specify → Plan → Tasks → Implement → Validate
```

- 각 단계의 산출물은 사람의 승인을 받은 뒤에 다음 단계로 넘어간다.
- 지시받은 단계의 범위만 수행한다. 문서 작성을 요청받았다면 코드를 작성하지 않는다.

## 문서 수정 권한

- 사람 소유 문서 (TRD, PRD, REQUIREMENTS): 사람이 요청한 경우에만 초안을 작성하거나 수정한다. 확정은 사람이 한다. 요청 없이 변경이 필요하다고 판단되면 제안만 한다.
- **Plan / Tasks 문서**: 지시받은 경우에만 수정하고, 무엇을 바꿨는지 요약해서 보고한다.
- **BACKEND_TASKS 체크박스**: 해당 작업의 테스트가 모두 통과하면 체크한다.

## 멈추고 보고해야 하는 경우

- 문서끼리 충돌하거나, 명세가 모호하거나, 필요한 내용이 명세에 없을 때
- 명세가 틀렸다고 판단될 때 (직접 고치지 않는다)
- 기존 테스트를 수정하거나 삭제해야 한다고 판단될 때
- TRD에 없는 의존성을 추가해야 할 때
- 현재 작업 범위를 넘는 변경이 필요할 때

## 구현 작업 진행 방식

- `BACKEND_TASKS.md`의 작업을 한 번에 하나씩 진행한다.
- TDD 절차는 `docs/TRD.md`의 개발 프로세스 섹션을 따른다.
- 작업이 끝나면 실행한 테스트 결과와 변경한 파일을 보고한다.

## 명령어

> 프로젝트 생성 후 실제 명령어로 갱신한다.

- 테스트: `./gradlew test`
- 빌드: `./gradlew build`
- 실행: `./gradlew bootRun`