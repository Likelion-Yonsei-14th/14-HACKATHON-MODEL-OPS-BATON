# Slack OAuth 연결 시작

API Path: /api/platform-connections/slack/connect
HTTP 메서드: GET
기능 분류: Platform Connections
완료 여부: No

# API 설명

💡 Slack User OAuth 인증을 시작하기 위한 authorization URL을 생성하는 API입니다.

구현 응답 형식: 성공은 `{success, data, error: null}`, 실패는 `{success: false, data: null, error: {code, message}}` wrapper를 사용합니다.

---

# Request Header

| name | type | description |
| --- | --- | --- |
| Authorization | String | Bearer &lt;api_key&gt; |

---

# Response Body

| field | type | Nullable | Description |
| --- | --- | --- | --- |
| redirect_url | String | N | Slack OAuth authorization URL |

---

### ⭕Success⭕

**200 OK**

실제 예시

```json
{
  "success": true,
  "data": {
    "redirect_url": "https://slack.com/oauth/v2/authorize?client_id=..."
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