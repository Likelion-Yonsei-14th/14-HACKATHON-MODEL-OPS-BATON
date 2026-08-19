package com.likelion.yonsei.baton.domain.modellab.service;

import com.likelion.yonsei.baton.common.exception.BusinessException;
import com.likelion.yonsei.baton.domain.modellab.entity.AiModelConfig;
import com.likelion.yonsei.baton.domain.modellab.entity.DeploymentAction;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelConfigStatus;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelDeploymentHistory;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabProvider;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import com.likelion.yonsei.baton.domain.modellab.exception.ModelLabErrorCode;
import com.likelion.yonsei.baton.domain.modellab.repository.AiModelConfigRepository;
import com.likelion.yonsei.baton.domain.modellab.repository.ModelDeploymentHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionModelRegistryServiceTest {

	@Mock
	private AiModelConfigRepository modelConfigRepository;
	@Mock
	private ModelDeploymentHistoryRepository deploymentHistoryRepository;

	private AiModelConfig configWithId(long id, ModelConfigStatus status) {
		AiModelConfig config = new AiModelConfig("CLS-" + id, ModelLabTaskType.REPLY_CLASSIFICATION, ModelLabProvider.OPENAI, "gpt-4o-mini", null, 1L, null, new BigDecimal("0.2"), new BigDecimal("0.7"), 1L);
		config.moveTo(status);
		ReflectionTestUtils.setField(config, "id", id);
		return config;
	}

	@Test
	void promotingWithNoExistingProductionSetsTargetProductionAndRecordsNullFrom() {
		ProductionModelRegistryService service = new ProductionModelRegistryService(modelConfigRepository, deploymentHistoryRepository);
		AiModelConfig target = configWithId(10L, ModelConfigStatus.EVALUATING);
		when(modelConfigRepository.findById(10L)).thenReturn(Optional.of(target));
		when(modelConfigRepository.findByTaskTypeAndStatus(ModelLabTaskType.REPLY_CLASSIFICATION, ModelConfigStatus.PRODUCTION)).thenReturn(Optional.empty());
		when(deploymentHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		ModelDeploymentHistory entry = service.promote(10L, 99L, "first promotion");

		assertThat(target.getStatus()).isEqualTo(ModelConfigStatus.PRODUCTION);
		assertThat(entry.getFromConfigId()).isNull();
		assertThat(entry.getToConfigId()).isEqualTo(10L);
		assertThat(entry.getAction()).isEqualTo(DeploymentAction.PROMOTE);
	}

	@Test
	void promotingArchivesThePreviousProductionConfig_soExactlyOneStaysProduction() {
		// This is the "task당 정확히 하나의 PRODUCTION" invariant from spec section 15/32.
		ProductionModelRegistryService service = new ProductionModelRegistryService(modelConfigRepository, deploymentHistoryRepository);
		AiModelConfig oldProd = configWithId(1L, ModelConfigStatus.PRODUCTION);
		AiModelConfig newTarget = configWithId(2L, ModelConfigStatus.EVALUATING);
		when(modelConfigRepository.findById(2L)).thenReturn(Optional.of(newTarget));
		when(modelConfigRepository.findByTaskTypeAndStatus(ModelLabTaskType.REPLY_CLASSIFICATION, ModelConfigStatus.PRODUCTION)).thenReturn(Optional.of(oldProd));
		when(deploymentHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		service.promote(2L, 99L, null);

		assertThat(oldProd.getStatus()).isEqualTo(ModelConfigStatus.ARCHIVED);
		assertThat(newTarget.getStatus()).isEqualTo(ModelConfigStatus.PRODUCTION);
	}

	@Test
	void rollbackRestoresThePreviousProductionConfigFromHistory() {
		ProductionModelRegistryService service = new ProductionModelRegistryService(modelConfigRepository, deploymentHistoryRepository);
		AiModelConfig current = configWithId(2L, ModelConfigStatus.PRODUCTION);
		AiModelConfig previous = configWithId(1L, ModelConfigStatus.ARCHIVED);
		ModelDeploymentHistory promoteEntry = new ModelDeploymentHistory(ModelLabTaskType.REPLY_CLASSIFICATION, DeploymentAction.PROMOTE, 1L, 2L, 99L, null);

		when(modelConfigRepository.findByTaskTypeAndStatus(ModelLabTaskType.REPLY_CLASSIFICATION, ModelConfigStatus.PRODUCTION)).thenReturn(Optional.of(current));
		when(deploymentHistoryRepository.findByTaskTypeOrderByCreatedAtDesc(ModelLabTaskType.REPLY_CLASSIFICATION)).thenReturn(List.of(promoteEntry));
		when(modelConfigRepository.findById(1L)).thenReturn(Optional.of(previous));
		when(deploymentHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		ModelDeploymentHistory entry = service.rollback(ModelLabTaskType.REPLY_CLASSIFICATION, 99L, "bad regression");

		assertThat(current.getStatus()).isEqualTo(ModelConfigStatus.ARCHIVED);
		assertThat(previous.getStatus()).isEqualTo(ModelConfigStatus.PRODUCTION);
		assertThat(entry.getAction()).isEqualTo(DeploymentAction.ROLLBACK);
	}

	@Test
	void rollbackWithNoPriorPromotionHistoryFails() {
		ProductionModelRegistryService service = new ProductionModelRegistryService(modelConfigRepository, deploymentHistoryRepository);
		AiModelConfig current = configWithId(2L, ModelConfigStatus.PRODUCTION);
		when(modelConfigRepository.findByTaskTypeAndStatus(ModelLabTaskType.REPLY_CLASSIFICATION, ModelConfigStatus.PRODUCTION)).thenReturn(Optional.of(current));
		when(deploymentHistoryRepository.findByTaskTypeOrderByCreatedAtDesc(ModelLabTaskType.REPLY_CLASSIFICATION)).thenReturn(List.of());

		assertThatThrownBy(() -> service.rollback(ModelLabTaskType.REPLY_CLASSIFICATION, 99L, null))
				.isInstanceOf(BusinessException.class)
				.satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ModelLabErrorCode.NO_PREVIOUS_PRODUCTION_CONFIG));
	}

	@Test
	void rollbackWithNoCurrentProductionConfigFails() {
		ProductionModelRegistryService service = new ProductionModelRegistryService(modelConfigRepository, deploymentHistoryRepository);
		when(modelConfigRepository.findByTaskTypeAndStatus(ModelLabTaskType.REPLY_CLASSIFICATION, ModelConfigStatus.PRODUCTION)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.rollback(ModelLabTaskType.REPLY_CLASSIFICATION, 99L, null))
				.isInstanceOf(BusinessException.class)
				.satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ModelLabErrorCode.NO_PRODUCTION_CONFIG));
	}

	@Test
	void getProductionConfigCachesAfterFirstLookup() {
		ProductionModelRegistryService service = new ProductionModelRegistryService(modelConfigRepository, deploymentHistoryRepository);
		AiModelConfig prod = configWithId(1L, ModelConfigStatus.PRODUCTION);
		when(modelConfigRepository.findByTaskTypeAndStatus(ModelLabTaskType.REPLY_CLASSIFICATION, ModelConfigStatus.PRODUCTION)).thenReturn(Optional.of(prod));

		service.getProductionConfig(ModelLabTaskType.REPLY_CLASSIFICATION);
		service.getProductionConfig(ModelLabTaskType.REPLY_CLASSIFICATION);

		org.mockito.Mockito.verify(modelConfigRepository, org.mockito.Mockito.times(1))
				.findByTaskTypeAndStatus(ModelLabTaskType.REPLY_CLASSIFICATION, ModelConfigStatus.PRODUCTION);
	}
}
