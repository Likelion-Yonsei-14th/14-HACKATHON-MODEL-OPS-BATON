package com.likelion.yonsei.baton.domain.modellab.service;

import com.likelion.yonsei.baton.common.exception.BusinessException;
import com.likelion.yonsei.baton.domain.modellab.entity.AiModelConfig;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelConfigStatus;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabProvider;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import com.likelion.yonsei.baton.domain.modellab.exception.ModelLabErrorCode;
import com.likelion.yonsei.baton.domain.modellab.repository.AiModelConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelConfigServiceTest {

	@Mock
	private AiModelConfigRepository repository;

	private AiModelConfig draftConfig() {
		return new AiModelConfig("CLS-test", ModelLabTaskType.REPLY_CLASSIFICATION, ModelLabProvider.OPENAI, "gpt-4o-mini", null, 1L, null, new BigDecimal("0.20"), new BigDecimal("0.70"), 1L);
	}

	@Test
	void newConfigStartsAsDraft() {
		ModelConfigService service = new ModelConfigService(repository);
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		AiModelConfig created = service.create("CLS-1", ModelLabTaskType.REPLY_CLASSIFICATION, ModelLabProvider.OPENAI, "gpt-4o-mini", null, 1L, null, new BigDecimal("0.2"), new BigDecimal("0.7"), 1L);

		assertThat(created.getStatus()).isEqualTo(ModelConfigStatus.DRAFT);
	}

	@Test
	void draftConfigCanBeUpdatedInPlace() {
		ModelConfigService service = new ModelConfigService(repository);
		AiModelConfig config = draftConfig();
		when(repository.findById(1L)).thenReturn(Optional.of(config));

		AiModelConfig updated = service.updateDraft(1L, "renamed", null, null, null, null, new BigDecimal("0.5"), new BigDecimal("0.8"));

		assertThat(updated.getName()).isEqualTo("renamed");
		assertThat(updated.getConfidenceThreshold()).isEqualByComparingTo("0.8");
	}

	@Test
	void nonDraftConfigCannotBeUpdated() {
		// Spec section 15: "Production config는 immutable하게 취급한다" — enforced here for every
		// non-DRAFT status, not just PRODUCTION, since EVALUATING/STAGING/ARCHIVED must also stay fixed
		// once a run has referenced them (eval_runs snapshots depend on this).
		ModelConfigService service = new ModelConfigService(repository);
		AiModelConfig config = draftConfig();
		config.moveTo(ModelConfigStatus.PRODUCTION);
		when(repository.findById(1L)).thenReturn(Optional.of(config));

		assertThatThrownBy(() -> service.updateDraft(1L, "renamed", null, null, null, null, null, null))
				.isInstanceOf(BusinessException.class)
				.satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ModelLabErrorCode.MODEL_CONFIG_NOT_DRAFT));
	}

	@Test
	void cloneAsDraftAlwaysProducesADraftRegardlessOfSourceStatus() {
		ModelConfigService service = new ModelConfigService(repository);
		AiModelConfig source = draftConfig();
		source.moveTo(ModelConfigStatus.PRODUCTION);
		when(repository.findById(1L)).thenReturn(Optional.of(source));
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		AiModelConfig clone = service.cloneAsDraft(1L, "CLS-2", 2L);

		assertThat(clone.getStatus()).isEqualTo(ModelConfigStatus.DRAFT);
		assertThat(clone.getPromptVersionId()).isEqualTo(source.getPromptVersionId());
	}
}
