# Slack OAuth Callback

API Path: /api/platform-connections/slack/callback
HTTP 메서드: GET
기능 분류: Platform Connections
완료 여부: No

# API 설명

💡 Slack OAuth 승인 후 전달받은 code를 access token으로 교환하고 플랫폼 연결을 완료하는 API입니다.

구현 응답 형식: 성공은 `{success, data, error: null}`, 실패는 `{success: false, data: null, error: {code, message}}` wrapper를 사용합니다.

---

# Request Parameter

| name | type | Nullable | Description |
| --- | --- | --- | --- |
| code | String | N | Slack OAuth authorization code |
| state | String | N | CSRF 검증용 state |

---

# Response Body

| field | type | Nullable | Description |
| --- | --- | --- | --- |
| id | Long | N | 플랫폼 연결 id |
| workspace_id | String | N | Slack workspace id |
| workspace_name | String | Y | 워크스페이스 이름 |
| connection_status | String | N | CONNECTED |

---

### ⭕Success⭕

**200 OK**

실제 예시

```json
{
  "success": true,
  "data": {
    "id": 3,
    "workspace_id": "T012345",
    "workspace_name": "BATON Test",
    "connection_status": "CONNECTED"
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