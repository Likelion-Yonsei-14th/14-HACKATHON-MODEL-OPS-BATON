package com.likelion.yonsei.baton.domain.modellab.exception;

import com.likelion.yonsei.baton.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ModelLabErrorCode implements ErrorCode {

	ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "MODELLAB-001", "Model Lab은 관리자만 접근할 수 있습니다."),
	PROMPT_VERSION_NOT_FOUND(HttpStatus.NOT_FOUND, "MODELLAB-002", "Prompt Version을 찾을 수 없습니다."),
	SCHEMA_VERSION_NOT_FOUND(HttpStatus.NOT_FOUND, "MODELLAB-003", "Schema Version을 찾을 수 없습니다."),
	MODEL_CONFIG_NOT_FOUND(HttpStatus.NOT_FOUND, "MODELLAB-004", "Model Config를 찾을 수 없습니다."),
	MODEL_CONFIG_NOT_DRAFT(HttpStatus.CONFLICT, "MODELLAB-005", "DRAFT 상태의 Config만 수정할 수 있습니다. 새 버전을 생성하세요."),
	DATASET_NOT_FOUND(HttpStatus.NOT_FOUND, "MODELLAB-006", "Eval Dataset을 찾을 수 없습니다."),
	SCENARIO_NOT_FOUND(HttpStatus.NOT_FOUND, "MODELLAB-007", "Eval Scenario를 찾을 수 없습니다."),
	REPLY_CASE_NOT_FOUND(HttpStatus.NOT_FOUND, "MODELLAB-008", "Eval Reply Case를 찾을 수 없습니다."),
	EVAL_RUN_NOT_FOUND(HttpStatus.NOT_FOUND, "MODELLAB-009", "Eval Run을 찾을 수 없습니다."),
	EVAL_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "MODELLAB-010", "Eval Result를 찾을 수 없습니다."),
	TASK_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "MODELLAB-011", "Dataset과 Model Config의 task_type이 일치하지 않습니다."),
	EMPTY_DATASET_SPLIT(HttpStatus.BAD_REQUEST, "MODELLAB-012", "선택한 Split에 해당하는 Scenario/Reply Case가 없습니다."),
	NO_PRODUCTION_CONFIG(HttpStatus.NOT_FOUND, "MODELLAB-013", "해당 Task의 Production Config가 아직 없습니다."),
	NO_PREVIOUS_PRODUCTION_CONFIG(HttpStatus.CONFLICT, "MODELLAB-014", "Rollback할 이전 Production Config가 없습니다."),
	FINE_TUNING_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "MODELLAB-015", "Fine-tuning Job을 찾을 수 없습니다."),
	FINE_TUNING_NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED, "MODELLAB-016", "Fine-tuning 실행은 아직 구현되지 않았습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;

	ModelLabErrorCode(HttpStatus status, String code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}

	@Override
	public HttpStatus getStatus() {
		return status;
	}

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
