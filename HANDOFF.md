# BATON Model Lab — 작업 인수인계

다른 세션(Claude)이 이어서 작업할 때 필요한 서버 접근, 배포 방법, 지금까지 한 모델 튜닝 작업
전체 맥락. 이 문서를 읽고 나서 반드시 [`backend/docs/QWEN_TUNING.md`](backend/docs/QWEN_TUNING.md)
도 읽을 것 — 실험 기록/결론이 거기 다 있음.

## 이게 뭔가

BATON(비동기 협업 도구)의 Reply Classification AI를 실험/튜닝하는 내부 콘솔.
레포: https://github.com/Likelion-Yonsei-14th/14-HACKATHON-MODEL-OPS-BATON
배포처: 가비아 서버 `http://1.201.116.120:8090/batons/models`

기존 BATON FE/BE 저장소(`14-HACKATHON-FRONTEND-BATON`, `14-HACKATHON-BACKEND-BATON`)를 포크해서
`domain/modellab`(백엔드) / `pages/modellab`(프론트) 도메인을 추가한 구조. 원본 서비스 코드는
안 건드림.

## 서버 접근

```text
Host: baton-server (SSH config에 alias 등록되어 있음, IP 1.201.116.120)
IdentityFile: ~/.ssh/gabia_baton_rsa
User: root
```

```bash
ssh baton-server
```

서버에 이미 떠있는 것들:
```text
baton-backend / baton-frontend       프로덕션 BATON (원본 FE/BE, CI/CD로 자동배포됨 — 절대 직접 손대지 말 것)
baton-postgres                       공유 DB (production + Model Lab 같이 씀)
baton-ollama                         프로덕션용 로컬 LLM (qwen3:0.6b, CPU만)
model-lab-backend / model-lab-frontend  이 프로젝트 (/opt/baton-model-lab)
```

**절대 하지 말 것**: `/opt/baton/backend`, `/opt/baton/frontend`(프로덕션 저장소)를 직접 수정하는 것.
거긴 git reset --hard로 배포되는 CI 관리 대상이라 직접 수정하면 다음 배포 때 사라지거나 충돌남.
프로덕션 쪽에 뭔가 필요하면 그쪽 팀에 PR로 요청할 것.

## 로컬 작업 → 배포 절차

로컬 저장소: `/Users/minjooncho/SandBox/baton-model-lab` (이 맥북 기준. 다른 머신이면 새로 clone)

```bash
# 1. 코드/마이그레이션 수정 후 반드시 테스트
cd backend && ./gradlew test

# 2. 서버로 동기화 (⚠️ 가끔 SSH 커넥션이 중간에 끊겨 rsync가 조용히 실패한다 — 아래 "자주 겪는 문제" 참고)
rsync -az --exclude='.git' --exclude='node_modules' --exclude='dist' --exclude='build' --exclude='.gradle' \
  /Users/minjooncho/SandBox/baton-model-lab/ baton-server:/opt/baton-model-lab/

# 3. 빌드 + 재기동 (백엔드/프론트 중 바뀐 것만)
ssh baton-server "cd /opt/baton-model-lab && docker compose build model-lab-backend && docker compose up -d model-lab-backend"

# 4. 마이그레이션/기동 확인
ssh baton-server "docker logs baton-model-lab-backend 2>&1 | grep -i 'migrat\|Started BatonApplication' | tail -10"
```

## 자주 겪은 문제 (시간 아끼려면 미리 알아둘 것)

1. **rsync가 SSH 재협상 중 조용히 실패할 때가 있다.** exit code는 0으로 나오지만 실제로는
   파일이 안 넘어간 경우가 있었음. 빌드 후 `docker logs`의 flyway migration 버전 번호가 기대한
   최신 버전인지 꼭 확인할 것. 의심되면 `ssh baton-server "grep -c <새로_추가한_문자열> /opt/baton-model-lab/backend/.../파일"`로 서버에 실제로 도착했는지 확인.

2. **`docker exec ... jar tf`가 안 먹는다** — 배포 이미지가 JRE라 `jar` 커맨드 자체가 없음
   (`exec: "jar": executable file not found`). 이걸 "클래스가 없다"는 증거로 착각하지 말 것.
   jar 내용 확인하려면 `docker cp <container>:/app/app.jar /tmp/x.jar` 후 로컬에서
   `unzip -l /tmp/x.jar`로 볼 것.

3. **eval run curl이 타임아웃(exit code 28) 나도 서버는 계속 돈다.** `@Transactional` 메서드가
   끝까지 실행된 후 커밋되므로, 클라이언트가 먼저 끊겨도 DB에는 결과가 남는다. 타임아웃 났다고
   재요청부터 하지 말고 먼저 `select id,status,finished_at from eval_runs order by id desc limit 1;`
   로 확인할 것 (중복 실행 방지 + 시간 절약).

4. **Flyway 버전 번호는 production 백엔드 저장소와 절대 겹치면 안 된다.** 예전에 둘 다 `V5`를
   써서 production이 crash loop에 빠진 적 있음(`backend/docs/GABIA_DEPLOY_INCIDENT.md` 참고).
   지금은 Model Lab이 별도 Flyway 히스토리 테이블(`flyway_schema_history_model_lab`)을 쓰도록
   분리해놔서 이제 번호가 겹쳐도 안전하지만, 그래도 새 마이그레이션은 이 레포 안의 `V<N>` 다음
   번호로만 이어서 쓸 것 (지금까지 V21까지 씀).

5. **OpenAI API 키가 무료 티어라 하루 요청 50건 한도.** 이미 소진돼서 `CLS-seed-v1`(OpenAI
   기준선) eval을 못 돌렸음. 결제수단 등록 전까진 로컬 Ollama 모델로만 실험 가능.

## Model Lab 관리자 계정 만드는 법

로그인 UI가 있지만(`/login`), 임시 API 테스트용 계정은 이렇게 빠르게 만들 수 있다:

```bash
curl -sS -X POST http://1.201.116.120:8090/api/users -H "Content-Type: application/json" \
  -d '{"email":"YOUR_EMAIL","name":"YOUR_NAME","password":"SOME_PASSWORD"}'
# 응답의 data.api_key를 저장해두고
ssh baton-server "docker exec baton-postgres psql -U baton -d baton -c \"update users set is_admin=true where email='YOUR_EMAIL';\""
```

이후 `Authorization: Bearer <api_key>` 헤더로 `/api/model-lab/**` 전부 호출 가능. 다 쓰면
`DELETE /api/users/me`로 정리할 것(테스트 계정 안 남게).

## Eval 실행하는 법 (curl로 직접)

```bash
API_KEY="..."
# 데이터셋/config id 확인
curl -sS "http://1.201.116.120:8090/api/model-lab/model-configs?taskType=REPLY_CLASSIFICATION" -H "Authorization: Bearer $API_KEY"
curl -sS "http://1.201.116.120:8090/api/model-lab/datasets?taskType=REPLY_CLASSIFICATION" -H "Authorization: Bearer $API_KEY"

# 실행 전 케이스 수 미리보기 (비용/시간 확인)
curl -sS "http://1.201.116.120:8090/api/model-lab/classification-eval-runs/preview?datasetId=3&split=CORE" -H "Authorization: Bearer $API_KEY"

# 실행 (CORE는 300건 가까이 되니 timeout 크게, 백그라운드로)
curl -sS -m1800 -X POST http://1.201.116.120:8090/api/model-lab/classification-eval-runs \
  -H "Authorization: Bearer $API_KEY" -H "Content-Type: application/json" \
  -d '{"dataset_id":3,"split":"CORE","model_config_id":13}'
```

split은 `SMOKE`(빠른 8시나리오/56~62건) / `CORE`(나머지 42시나리오/~299건) 로 나눠져 있음.

## 지금까지의 튜닝 결론 (요약, 전체는 QWEN_TUNING.md)

- 데이터셋: `BATON Scenario Dataset v1` (50 시나리오, 350+ reply case, dataset_id=3)
- 최고 성능 config: **`CLS-qwen2.5-7b-v2-strict`** (id=13) — `qwen2.5:7b`, prompt v5, 스키마
  v2(decision-state), threshold 0.95. CORE 299건 기준 **False Auto-Send 1.53%**, Branch
  Accuracy 65.1%, No Match Recall 100%. 단 Auto-Send Coverage가 13.1%로 낮음(너무 보수적).
- 지금 진행 중: prompt v8(`CLS-qwen2.5-7b-v3-tightened`, id는 배포 후 확인)로 NO_MATCH
  과다 트리거 문제 개선 시도 중 — 모델이 애매하면 무조건 NO_MATCH로 도망가는 경향이 있어서
  branch accuracy가 저평가됨. 이어서 CORE 돌려서 확인할 것.
- 핵심 교훈: **모델 크기가 지배적 변수였다.** 0.6b→1.5b→7b로 갈수록 모든 지표가 단조 개선.
  프롬프트/스키마(단일콜 vs 2-stage vs decision-state) 튜닝은 작은 모델에서 미미한 개선,
  모델 크기 키우니 질적으로 다른 성능이 나옴.
- `DateRangeGuardrail`(`backend/.../service/DateRangeGuardrail.java`): 날짜 범위 조건을 정규식
  으로 파싱해서 LLM 답변을 사후 보정하는 결정론적 레이어. LLM의 날짜 산술 오류를 코드로 고침.
  분기 조건이 날짜가 아니면(요일/가격 등) 자동으로 개입 안 함. 한국어/영어 날짜 둘 다 지원.
- **최종 확정 Classification config: `CLS-qwen2.5-7b-v2-strict` (id=13)**. 서로 다른 두 데이터셋
  (v1: dataset_id=3, v2: dataset_id=4)에서 각각 CORE 규모(~300건)로 검증: False Auto-Send
  0~1.5%, Branch Accuracy 65~86%. branch accuracy만 더 높은 대안(prompt v9, 최대 92%)도
  있었지만 confidence 캘리브레이션이 깨져서(threshold 0.95→coverage 0%, 0.90→False Auto-Send
  37~40%) production 후보에서 제외함 — 자세한 내막은 QWEN_TUNING.md Follow-up 3/R4 섹션 참고.
- **Generation(Track A)도 처음 테스트해서 실사용 가능 수준까지 옴**: `GEN-qwen2.5-7b-v2-korean`
  (id=19), hard rule pass 96.8~100%. `GenerationEvalRunnerService`가 OpenAI에 하드코딩되어
  있던 버그를 고쳐서 Ollama도 쓸 수 있게 했음. **단, 이건 hard rule(스키마 유효성)만 확인한
  것 — spec이 요구하는 진짜 품질(Coverage/Naturalness 등)은 아직 Human Review 안 함.**
  `/batons/models/generation`에서 사람이 직접 점수 매겨야 Production 승격 검토 가능.

## 데스크탑 GPU Ollama (선택적, 붙여놓은 상태)

프로덕션 `baton-ollama`(CPU-only, 5~30초/케이스)보다 훨씬 빠른 추론을 위해 팀원 데스크탑
(RTX 2060)의 Ollama를 Tailscale로 연결해서 씀 (0.2~4초/케이스).

```text
가비아 서버 Tailscale IP: 100.99.179.35 (hostname: baton-gabia)
데스크탑 Tailscale IP:   100.74.196.80 (hostname: desktop-6hmb3e3)
```

`/opt/baton-model-lab/docker-compose.yml`의 `model-lab-backend.environment.OLLAMA_BASE_URL`이
이 데스크탑 IP를 가리키고 있음. **데스크탑이 꺼져있거나 Tailscale이 끊기면 Ollama 기반 eval이
전부 실패한다** — 안 되면 제일 먼저 이것부터 의심할 것:

```bash
ssh baton-server "curl -sS -m10 http://100.74.196.80:11434/api/tags"
```

빈 응답/타임아웃이면 데스크탑 쪽 확인 필요(전원, Ollama 서비스, Tailscale 연결).

## 하지 말아야 할 것 (반복 강조)

- production 저장소(`/opt/baton/backend`, `/opt/baton/frontend`) 직접 수정 금지
- Flyway 마이그레이션 파일 수정 금지(이미 적용된 것). 바꿀 게 있으면 새 버전 추가
- `git push --force`, `docker system prune`, `git reset --hard` 등 되돌리기 어려운 명령은 먼저
  물어보고 실행할 것
