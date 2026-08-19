# 기여 가이드

## 브랜치

- `main`: 배포 가능한 안정 버전
- `dev`: 통합 브랜치이자 기본 PR 대상
- `feature/{issue}-{summary}`: 기능 개발
- `fix/{issue}-{summary}`: 버그 수정
- `hotfix/{issue}-{summary}`: 운영 긴급 수정

새 작업은 원칙적으로 최신 `dev`에서 분기합니다. 브랜치 이름은 영문 소문자와 하이픈을 사용합니다.

## 커밋

```text
feat: 새로운 기능
fix: 버그 수정
refactor: 동작을 바꾸지 않는 구조 개선
test: 테스트 추가 또는 수정
docs: 문서 수정
style: 포맷 변경
chore: 설정과 도구 변경
```

한 커밋에는 하나의 논리적 변경을 담고, 비밀정보와 개인 환경 파일을 포함하지 않습니다.

## Pull Request

- PR 대상은 기본적으로 `dev`입니다.
- 변경 이유와 검증 방법을 작성합니다.
- UI 변경에는 확인 가능한 이미지나 영상을 첨부합니다.
- API 계약 변경은 백엔드 저장소와 함께 확인합니다.
- 직접 작성하지 않은 대규모 생성 파일을 이유 없이 포함하지 않습니다.

## 머지 전 확인

```bash
pnpm lint
pnpm typecheck
pnpm test
pnpm build
```

