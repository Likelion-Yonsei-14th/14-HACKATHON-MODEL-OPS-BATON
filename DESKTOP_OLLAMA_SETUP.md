# 데스크탑을 BATON Model Lab의 로컬 LLM 추론 서버로 쓰기

## 배경

BATON Model Lab(AI Eval/Ops 콘솔)이 가비아 서버(1.201.116.120)에서 돌고 있는데, 거기 붙어있는
Ollama가 CPU-only 인스턴스라 `qwen2.5:1.5b` 기준 케이스당 5~30초씩 걸림 (56건 SMOKE eval 한
번에 5~10분). GPU 있는 이 데스크탑에 Ollama를 옮겨서 추론 속도를 올리고, 더 큰 모델(7b/14b 등)도
시도해보려는 것.

레포: https://github.com/Likelion-Yonsei-14th/14-HACKATHON-MODEL-OPS-BATON
튜닝 기록(무슨 실험을 왜 했는지 전체 맥락): [`backend/docs/QWEN_TUNING.md`](https://github.com/Likelion-Yonsei-14th/14-HACKATHON-MODEL-OPS-BATON/blob/main/backend/docs/QWEN_TUNING.md)

읽어두면 좋음 — 지금까지 시도한 프롬프트/모델 조합, 왜 `qwen3:0.6b`에서 `qwen2.5:1.5b`로
옮겼는지, False Auto-Send / branch accuracy가 왜 중요한 지표인지 다 정리되어 있음.

## 목표 구성

```text
가비아 서버 (Model Lab backend, Postgres)
   ↓ Tailscale 사설망 경유
데스크탑 (Ollama, GPU 추론)
```

DB/백엔드/프론트는 계속 가비아에 둔다. 이 데스크탑은 순수하게 Ollama 추론만 맡는다. 백엔드
코드는 안 건드림 — `OLLAMA_BASE_URL` 환경변수 하나만 이 데스크탑의 Tailscale 주소로 바꾸면 끝
(그건 가비아 쪽에서 별도로 처리할 것, 여기선 안 해도 됨).

## 할 일

### 1. Ollama 설치

```bash
curl -fsSL https://ollama.com/install.sh | sh
```

(macOS면 https://ollama.com 에서 앱 다운로드해도 됨. Windows는 https://ollama.com/download/windows)

### 2. 외부(Tailscale 사설망)에서 접근 가능하게 바인딩

기본값은 `127.0.0.1`만 리슨해서 로컬 밖에서 못 붙는다. 아래처럼 모든 인터페이스에 바인딩되게
환경변수를 설정하고 재시작한다.

**Linux (systemd)**:
```bash
sudo systemctl edit ollama
# 아래 내용 추가 후 저장:
# [Service]
# Environment="OLLAMA_HOST=0.0.0.0"
sudo systemctl restart ollama
```

**macOS**: `launchctl setenv OLLAMA_HOST 0.0.0.0` 실행 후 Ollama 앱 재시작 (또는 터미널에서
`OLLAMA_HOST=0.0.0.0 ollama serve` 로 직접 실행)

**Windows**: 시스템 환경변수에 `OLLAMA_HOST=0.0.0.0` 추가 후 Ollama 재시작

### 3. 모델 pull

이미 검증된 모델부터:
```bash
ollama pull qwen2.5:1.5b
```

GPU 여유 있으면 더 큰 것도 받아서 나중에 비교해볼 것 (VRAM 감안해서 고르기):
```bash
ollama pull qwen2.5:7b
ollama pull qwen2.5:14b
```

### 4. 로컬 동작 확인

```bash
curl http://localhost:11434/api/tags
```
받아둔 모델 목록이 JSON으로 나오면 정상.

### 5. Tailscale 설치 + 로그인

Ollama 자체에 인증이 없어서 그냥 포트를 공인망에 열면 위험함. Tailscale로 가비아 서버랑 이
데스크탑을 사설망으로 묶는다 (외부에 전혀 노출 안 됨, 같은 tailnet 안에서만 접근 가능).

```bash
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up
```

(macOS는 https://tailscale.com/download 에서 앱 설치 후 로그인)

`tailscale up` 실행하면 로그인 URL이 출력됨 — 브라우저로 열어서 계정으로 로그인/인증할 것
(Google/GitHub 계정 아무거나로 새 tailnet 만들면 됨. 가비아 서버도 같은 계정으로 붙일 예정이니
어떤 계정으로 로그인했는지 기억해둘 것).

### 6. Tailscale IP 확인 후 알려주기

```bash
tailscale ip -4
```

나온 IP(예: `100.x.x.x`)를 알려줄 것 — 가비아 서버 쪽 `OLLAMA_BASE_URL`을
`http://<그 IP>:11434/v1` 로 설정하는 데 필요함.

### 7. (확인용) Tailscale 상태

```bash
tailscale status
```

## 하지 말 것

- Ollama를 공인 IP/포트로 직접 노출하지 말 것 (포트포워딩 등) — 인증이 없어서 누구나 모델을
  무료로 쓰거나 리소스를 소모시킬 수 있음. 반드시 Tailscale 경유로만 접근하게 할 것.
- 이 레포의 코드/DB를 직접 건드리지 말 것 — 이 작업은 순수 로컬 인프라(Ollama+Tailscale)
  세팅이고, Model Lab 쪽 연결 작업은 가비아 서버 쪽에서 따로 처리함.

## 완료 후 보고할 것

- Tailscale IP
- pull한 모델 목록과 각 GPU 메모리 사용량(`ollama ps` 실행 중 상태에서, 또는 `nvidia-smi`)
- 로컬에서 curl 테스트 성공 여부
