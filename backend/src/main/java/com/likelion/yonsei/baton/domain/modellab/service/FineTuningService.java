package com.likelion.yonsei.baton.domain.modellab.service;

import com.likelion.yonsei.baton.common.exception.BusinessException;
import com.likelion.yonsei.baton.domain.modellab.entity.FineTuningJob;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabProvider;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import com.likelion.yonsei.baton.domain.modellab.exception.ModelLabErrorCode;
import com.likelion.yonsei.baton.domain.modellab.repository.FineTuningJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Phase 5 (spec section 16/30): lowest priority, real scaffolding only. Creating a job record and
 * listing jobs works end to end; actually submitting a fine-tuning job to OpenAI's Files/Fine-tuning
 * API is a deliberate, clearly-labeled TODO — see {@link #submit} — per the "never fake it" rule in
 * spec section 37. The frontend must show "fine-tuning execution not yet implemented" for this
 * action rather than a spinner that implies real progress.
 */
@Service
@Transactional
public class FineTuningService {

	private final FineTuningJobRepository repository;

	public FineTuningService(FineTuningJobRepository repository) {
		this.repository = repository;
	}

	public List<FineTuningJob> list() {
		return repository.findAllByOrderByCreatedAtDesc();
	}

	public FineTuningJob getById(Long id) {
		return repository.findById(id).orElseThrow(() -> new BusinessException(ModelLabErrorCode.FINE_TUNING_JOB_NOT_FOUND));
	}

	public FineTuningJob create(ModelLabTaskType taskType, ModelLabProvider provider, String baseModel, Long trainingDatasetId, Long createdBy) {
		return repository.save(new FineTuningJob(taskType, provider, baseModel, trainingDatasetId, createdBy));
	}

	/**
	 * TODO(fine-tuning): call OpenAI's Files API to upload the exported training set, then the
	 * Fine-tuning Jobs API to start training, and persist the resulting provider_job_id. Not wired
	 * up yet — this MVP does not fabricate a fake in-progress state, it says so explicitly.
	 */
	public FineTuningJob submit(Long id) {
		getById(id); // validates existence before reporting the real limitation
		throw new BusinessException(ModelLabErrorCode.FINE_TUNING_NOT_IMPLEMENTED);
	}
}
