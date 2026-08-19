# Classification 목록 조회

API Path: /api/batons/{baton_id}/classifications
HTTP 메서드: GET
기능 분류: Classifications
완료 여부: No

# API 설명

💡 특정 BATON에 대해 수행된 AI 판정 이력을 조회하는 API입니다. 판정 생성은 서버 내부에서 처리합니다.

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
| classifications | Array | N | 판정 목록 |

---

### ⭕Success⭕

**200 OK**

실제 예시

```json
{
  "success": true,
  "data": {
    "classifications": [
      {
        "id": 50,
        "selected_branch_id": 31,
        "confidence": 0.94,
        "is_ambiguous": false,
        "contains_new_question": false,
        "reasoning_summary": "The recipient explicitly stated March 27 as the delivery date.",
        "result_status": "MATCHED",
        "created_at": "2026-08-17T03:04:02+09:00"
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