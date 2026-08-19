package com.likelion.yonsei.baton.domain.modellab.entity;

/** Coarse status for a fine-tuning job. RUNNING/SUCCEEDED/FAILED are only meaningful once the
 * (currently stubbed) provider integration actually submits jobs — see FineTuningService. */
public enum FineTuningJobStatus {
	NOT_STARTED,
	QUEUED,
	RUNNING,
	SUCCEEDED,
	FAILED
}
