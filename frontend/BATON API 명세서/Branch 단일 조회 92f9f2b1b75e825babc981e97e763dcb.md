# Branch 단일 조회

API Path: /api/branches/{id}
HTTP 메서드: GET
기능 분류: Branches
완료 여부: No

# API 설명

💡 특정 Branch의 조건, 결정, 응답, Action 설정을 조회하는 API입니다.

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
| id | Long | N | Branch id |

---

# Response Body

| field | type | Nullable | Description |
| --- | --- | --- | --- |
| id | Long | N | Branch id |
| baton_id | Long | N | BATON id |
| name | String | N | Branch 이름 |
| description | String | Y | 설명 |
| condition_text | String | N | 조건 |
| condition_rule_json | Object | Y | 구조화 조건 |
| decision_text | String | N | 결정 |
| response_text | String | Y | 응답 |
| action_type | String | N | Action |
| action_config_json | Object | Y | Action 설정 |
| execution_mode | String | N | 실행 모드 |
| sort_order | Integer | N | 정렬 순서 |

---

### ⭕Success⭕

**200 OK**

실제 예시

```json
{
  "success": true,
  "data": {
    "id": 30,
    "baton_id": 12,
    "name": "On time",
    "description": null,
    "condition_text": "Delivery is available by March 20",
    "condition_rule_json": {
      "on_or_before": "2026-03-20"
    },
    "decision_text": "Keep the current schedule",
    "response_text": "Great, we'll proceed with the current schedule.",
    "action_type": "SEND_REPLY",
    "action_config_json": null,
    "execution_mode": "AUTO",
    "sort_order": 1
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