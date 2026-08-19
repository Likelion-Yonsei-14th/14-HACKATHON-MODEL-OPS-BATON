# 핵심 API 흐름

기능 분류: Reference
완료 여부: No

## 최초 Slack 연결

```
GET /api/platform-connections/slack/connect
→ GET /platform-connections/slack/callback
→ POST /platform-connections/{connection_id}/conversations/sync
```

## 특정 대화 최초 접근

```
GET /api/conversations/{id}
→ POST /conversations/{id}/messages/sync
→ GET /conversations/{id}/messages
```

## BATON 생성

```
POST /api/conversations/{id}/messages
→ POST /batons
→ POST /batons/{id}/branches/generate
→ PATCH /batons/{id}/branches/{branchId}
→ POST /batons/{id}/activate
```

## 상대 답장 이후

```
POST /api/webhooks/slack/events
→ Message 저장
→ 활성 BATON 조회
→ OpenAI Classification
→ Classification 저장
→ Rule Engine 검증
→ Execution 생성
→ Slack 응답 발송
→ Message 저장
→ BATON 완료
```

## 모호한 답변

```
Webhook
→ Classification = AMBIGUOUS / NO_MATCH / GUARDRAIL_REJECTED
→ BATON = PENDING_REVIEW
→ POST /batons/{id}/resolve
```