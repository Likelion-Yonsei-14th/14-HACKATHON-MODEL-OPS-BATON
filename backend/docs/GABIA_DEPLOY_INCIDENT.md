# Gabia 배포 중 발생한 Flyway 버전 충돌

## 무슨 일이 있었나

Model Lab 백엔드와 production BATON 백엔드(`14-HACKATHON-BACKEND-BATON`)는 가비아 서버의 같은
PostgreSQL 인스턴스(`baton-postgres`, DB `baton`)를 공유합니다. 두 저장소가 서로 모르는 상태로
각자 `V5__...sql` 마이그레이션을 추가했고(하나는 Model Lab 전체 스키마 생성 + `users.is_admin`,
다른 하나는 production 쪽에서 별도로 추가한 `users.is_admin`), Model Lab 배포가 먼저 DB에
`V5`를 적용하면서 production 백엔드의 Flyway가 자기 `V5` 파일과 체크섬이 달라 검증에 실패,
`baton-backend` 컨테이너가 crash loop에 빠졌습니다.

## 임시 조치

`flyway/flyway repair` CLI로 `flyway_schema_history`의 버전 5 체크섬을 production 쪽 스크립트
기준으로 맞춰 즉시 복구했습니다 (컬럼 정의 자체는 두 스크립트가 동일해 데이터 손실 없음).

## 근본 조치

Model Lab은 이제 **별도의 Flyway 히스토리 테이블**(`flyway_schema_history_model_lab`)을 사용합니다
(`application.yml`의 `spring.flyway.table`). `baseline-version: 7`로 물리적으로 이미 적용된
V1~V7 상태를 기준선으로 잡고, 이후 Model Lab 마이그레이션은 이 별도 테이블에서만 추적됩니다.
production 백엔드의 `flyway_schema_history`와는 이제 완전히 독립적이라 앞으로 두 저장소가 같은
버전 번호를 써도 서로 충돌하지 않습니다.

## 교훈

같은 DB를 공유하는 두 개의 독립 Flyway 클라이언트는 버전 번호가 우연히도 반드시 겹치지 않아야만
안전합니다. 별도 히스토리 테이블 분리가 그 가정 자체를 없애는 유일한 확실한 방법입니다.
