# BATON 실행 타임라인 조회

API Path: /api/batons/{baton_id}/timeline
HTTP 메서드: GET
기능 분류: View
완료 여부: No

# API 설명

💡 messages, classifications, executions를 조합하여 BATON 진행 과정을 시간순 audit trail로 반환하는 API입니다.

구현 응답 형식: 성공은 `{success, data, error: null}`, 실패는 `{success: false, data: null, error: {code, message}}` wrapper를 사용합니다.

---

# Request Header

| name | type | description |
| --- | --- | --- |
| Authorization | String | Bearer &lt;api_key&gt; |

---

# Request Parameter

| name | type | Nullable | Description |
| --- | --- | --- | --- |
| baton_id | Long | N | BATON id |

---

# Response Body

| field | type | Nullable | Description |
| --- | --- | --- | --- |
| events | Array | N | 타임라인 이벤트 |

---

### ⭕Success⭕

**200 OK**

실제 예시

```json
{
  "success": true,
  "data": {
    "events": [
      {
        "type": "ARMED",
        "occurred_at": "2026-08-16T23:12:00+09:00",
        "description": "BATON이 활성화되었습니다."
      },
      {
        "type": "REPLY_RECEIVED",
        "occurred_at": "2026-08-17T03:04:00+09:00",
        "description": "상대방의 답장이 도착했습니다."
      },
      {
        "type": "BRANCH_MATCHED",
        "occurred_at": "2026-08-17T03:04:02+09:00",
        "description": "Delayed Branch와 일치했습니다."
      },
      {
        "type": "RESPONSE_SENT",
        "occurred_at": "2026-08-17T03:05:00+09:00",
        "description": "사전 승인된 응답을 발송했습니다."
      }
    ]
  },
  "error": null
}
```

---

### ❌Fail❌

**401 Unauthorized - 인증되지 않은 사용자일 때**

실제 예시

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "로그인이 필요합니다."
  }
}
```

**500 Internal Server Error - 서버 오류 시**

실제 예시

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "INTERNAL_SERVER_ERROR",
    "message": "서버 내부 오류가 발생했습니다."
  }
}
```