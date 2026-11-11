# DECISION_LOG — SDD 2단계 (Plan)

> 기술 계획 단계의 결정을 기록한다. 1단계 결정(D-xx)은 `docs/spec/DECISION_LOG.md`에 있고, 충돌하지 않도록 이 문서는 `TD-xx`를 쓴다.
> 결정일: 2026-11-11 / 결정자: Marvin-Oh

| ID | 질문 | 결정 | 반영 위치 |
| --- | --- | --- | --- |
| TD-01 | 언어와 프레임워크 | Java 21 + Spring Boot | TRD 1 |
| TD-02 | 저장소 | 인메모리. 기능 구현이 우선이고 로컬에서 동작하면 충분 | TRD 1, 2 |
| TD-03 | 할 일 식별자 (D-26 위임) | 자동 증가 정수 | TRD 3 |
| TD-04 | API 스타일 | REST + JSON, camelCase (추천 수락) | TRD 4 |
| TD-05 | 요약 위치 | 목록 응답에 `summary`를 함께 담음 | TRD 4 |
| TD-06 | 수정과 완료 표시 | `PUT`(수정) + `PATCH {completed}`(완료 표시) 분리 (추천 수락) | TRD 4 |
| TD-07 | 날짜 형식과 오류 범위 | ISO `YYYY-MM-DD`. 존재하지 않는 날짜도 날짜 오류 | TRD 4 |
| TD-08 | 알 수 없는 `status` 값 | 400 거부, 새 문구 추가 허용 (문구는 승인 대기) | TRD 5, 7 |
| TD-09 | 본문 형식 오류 | 범위에 포함, 400 + 일반 문구 하나 (문구는 승인 대기) | TRD 5, 7 |
| TD-10 | 글자 수 기준 | 유니코드 코드포인트. 조합 이모지는 2자 이상으로 센다는 한계를 인정 | TRD 3 |
| TD-11 | 레이어 구성 | presentation / application / domain / persistence (제안에 이견 없음) | TRD 2 |
| TD-12 | 테스트 도구 | JUnit 5 + Mockito | TRD 1, 8 |
| TD-13 | 샘플 데이터 | `local` 프로파일에서만 MockData를 채움. 명세에 없는 개발 편의 기능으로 기록 | TRD 6 |
| TD-14 | 문서 위치 | `docs/plan/TRD.md`와 이 파일. CLAUDE.md 4장은 TRD 확정 후 채우도록 제안 수락 | 전체 |
| TD-15 | 오류 응답 형식과 HTTP 상태 | `{error:{code,message}}`, 입력 오류 400, 없는 할 일 404 | TRD 5 |
