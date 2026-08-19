# Classification 단일 조회

API Path: /api/classifications/{id}
HTTP 메서드: GET
기능 분류: Classifications
완료 여부: No

# API 설명

💡 특정 AI 판정의 구조화된 추출 데이터와 선택 Branch를 조회하는 API입니다.

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
| id | Long | N | Classification id |

---

# Response Body

| field | type | Nullable | Description |
| --- | --- | --- | --- |
| id | Long | N | 판정 id |
| baton_id | Long | N | BATON id |
| selected_branch_id | Long | Y | 선택 Branch id |
| confidence | Decimal | Y | confidence |
| is_ambiguous | Boolean | N | 모호성 여부 |
| contains_new_question | Boolean | N | 새 질문 포함 여부 |
| extracted_data_json | Object | Y | 추출 데이터 |
| reasoning_summary | String | Y | 판정 요약 |
| result_status | String | N | 판정 결과 |
| model_name | String | Y | 모델명 |
| created_at | DateTime | N | 생성 시간 |

---

### ⭕Success⭕

**200 OK**

실제 예시

```json
{
  "success": true,
  "data": {
    "id": 50,
    "baton_id": 12,
    "selected_branch_id": 31,
    "confidence": 0.94,
    "is_ambiguous": false,
    "contains_new_question": false,
    "extracted_data_json": {
      "deliveryDate": "2026-03-27"
    },
    "reasoning_summary": "The recipient explicitly stated March 27 as the delivery date.",
    "result_status": "MATCHED",
    "model_name": "gpt-5.6",
    "created_at": "2026-08-17T03:04:02+09:00"
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