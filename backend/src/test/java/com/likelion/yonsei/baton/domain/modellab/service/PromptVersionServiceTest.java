package com.likelion.yonsei.baton.domain.modellab.service;

import com.likelion.yonsei.baton.domain.modellab.entity.AiPromptVersion;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import com.likelion.yonsei.baton.domain.modellab.repository.AiPromptVersionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptVersionServiceTest {

	@Mock
	private AiPromptVersionRepository repository;

	@Test
	void firstPromptForATaskTypeGetsVersionOne() {
		PromptVersionService service = new PromptVersionService(repository);
		when(repository.findTopByTaskTypeOrderByVersionDesc(ModelLabTaskType.REPLY_CLASSIFICATION)).thenReturn(Optional.empty());
		when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

		AiPromptVersion created = service.create(ModelLabTaskType.REPLY_CLASSIFICATION, "system prompt", null, null, 1L);

		assertThat(created.getVersion()).isEqualTo(1);
	}

	@Test
	void nextPromptForATaskTypeIncrementsFromTheLatestVersion() {
		PromptVersionService service = new PromptVersionService(repository);
		AiPromptVersion existingV3 = new AiPromptVersion(ModelLabTaskType.REPLY_CLASSIFICATION, 3, "v3 prompt", null, null, 1L);
		when(repository.findTopByTaskTypeOrderByVersionDesc(ModelLabTaskType.REPLY_CLASSIFICATION)).thenReturn(Optional.of(existingV3));
		ArgumentCaptor<AiPromptVersion> captor = ArgumentCaptor.forClass(AiPromptVersion.class);
		when(repository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

		AiPromptVersion created = service.create(ModelLabTaskType.REPLY_CLASSIFICATION, "v4 prompt", null, "note", 1L);

		assertThat(created.getVersion()).isEqualTo(4);
		assertThat(captor.getValue().getSystemPrompt()).isEqualTo("v4 prompt");
	}

	@Test
	void promptVersionExposesNoMutationMethodOtherThanConstruction() {
		// AiPromptVersion intentionally has no setters/update method — this is what "immutable" means
		// for prompt versions per spec section 13. Verified structurally: only getters + the constructor.
		var methods = AiPromptVersion.class.getDeclaredMethods();
		boolean hasMutator = java.util.Arrays.stream(methods)
				.anyMatch(m -> m.getName().startsWith("set") || m.getName().equals("update"));
		assertThat(hasMutator).isFalse();
	}

	@Test
	void differentTaskTypesVersionIndependently() {
		PromptVersionService service = new PromptVersionService(repository);
		when(repository.findTopByTaskTypeOrderByVersionDesc(ModelLabTaskType.BRANCH_GENERATION)).thenReturn(Optional.empty());
		when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

		AiPromptVersion created = service.create(ModelLabTaskType.BRANCH_GENERATION, "gen prompt", null, null, 1L);

		assertThat(created.getVersion()).isEqualTo(1);
		verify(repository).findTopByTaskTypeOrderByVersionDesc(ModelLabTaskType.BRANCH_GENERATION);
	}
}
