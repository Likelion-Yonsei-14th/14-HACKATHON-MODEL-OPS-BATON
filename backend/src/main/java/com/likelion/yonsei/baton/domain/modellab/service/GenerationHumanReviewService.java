package com.likelion.yonsei.baton.domain.modellab.service;

import com.likelion.yonsei.baton.common.exception.BusinessException;
import com.likelion.yonsei.baton.domain.modellab.entity.EvalResult;
import com.likelion.yonsei.baton.domain.modellab.entity.GenerationHumanReview;
import com.likelion.yonsei.baton.domain.modellab.exception.ModelLabErrorCode;
import com.likelion.yonsei.baton.domain.modellab.repository.EvalResultRepository;
import com.likelion.yonsei.baton.domain.modellab.repository.GenerationHumanReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Human scoring for one Generation eval result (spec section 4.3/23). Upserts — a reviewer edits their own review rather than stacking duplicates. */
@Service
@Transactional
public class GenerationHumanReviewService {

	private final GenerationHumanReviewRepository reviewRepository;
	private final EvalResultRepository evalResultRepository;

	public GenerationHumanReviewService(GenerationHumanReviewRepository reviewRepository, EvalResultRepository evalResultRepository) {
		this.reviewRepository = reviewRepository;
		this.evalResultRepository = evalResultRepository;
	}

	public GenerationHumanReview upsert(
			Long evalResultId, Integer coverage, Integer separation, Integer granularity, Integer predecidability,
			Integer naturalness, Integer safety, Integer overall, String note, Long reviewerId
	) {
		EvalResult result = evalResultRepository.findById(evalResultId)
				.orElseThrow(() -> new BusinessException(ModelLabErrorCode.EVAL_RESULT_NOT_FOUND));

		return reviewRepository.findByEvalResultId(result.getId())
				.map(existing -> {
					existing.update(coverage, separation, granularity, predecidability, naturalness, safety, overall, note);
					return existing;
				})
				.orElseGet(() -> reviewRepository.save(new GenerationHumanReview(
						result.getId(), coverage, separation, granularity, predecidability, naturalness, safety, overall, note, reviewerId
				)));
	}
}
