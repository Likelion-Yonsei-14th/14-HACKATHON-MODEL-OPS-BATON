package com.likelion.yonsei.baton.domain.modellab.security;

import com.likelion.yonsei.baton.common.crypto.ApiKeyGenerator;
import com.likelion.yonsei.baton.domain.user.entity.User;
import com.likelion.yonsei.baton.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelLabAdminInterceptorTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private HttpServletRequest request;
	@Mock
	private HttpServletResponse response;

	private final ApiKeyGenerator apiKeyGenerator = new ApiKeyGenerator();
	private final ObjectMapper objectMapper = new ObjectMapper();

	private User userWithAdminFlag(boolean isAdmin) {
		User user = new User("a@b.com", "name", "hash", "keyhash", null, null);
		ReflectionTestUtils.setField(user, "isAdmin", isAdmin);
		return user;
	}

	@Test
	void rejectsRequestsWithNoAuthorizationHeader() throws Exception {
		ModelLabAdminInterceptor interceptor = new ModelLabAdminInterceptor(userRepository, apiKeyGenerator, objectMapper);
		when(request.getHeader("Authorization")).thenReturn(null);
		StringWriter body = new StringWriter();
		when(response.getWriter()).thenReturn(new PrintWriter(body));

		boolean proceed = interceptor.preHandle(request, response, new Object());

		assertThat(proceed).isFalse();
		org.mockito.Mockito.verify(response).setStatus(401);
	}

	@Test
	void rejectsAnUnknownApiKey() throws Exception {
		ModelLabAdminInterceptor interceptor = new ModelLabAdminInterceptor(userRepository, apiKeyGenerator, objectMapper);
		when(request.getHeader("Authorization")).thenReturn("Bearer some-key");
		when(userRepository.findByApiKeyHash(apiKeyGenerator.hash("some-key"))).thenReturn(Optional.empty());
		when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

		boolean proceed = interceptor.preHandle(request, response, new Object());

		assertThat(proceed).isFalse();
		org.mockito.Mockito.verify(response).setStatus(401);
	}

	@Test
	void rejectsANonAdminUserEvenWithAValidApiKey() throws Exception {
		// This is the "권한 없는 Model Lab 접근 차단" requirement from spec section 32 — a real,
		// authenticated BATON user must still be rejected if is_admin is false.
		ModelLabAdminInterceptor interceptor = new ModelLabAdminInterceptor(userRepository, apiKeyGenerator, objectMapper);
		when(request.getHeader("Authorization")).thenReturn("Bearer valid-key");
		when(userRepository.findByApiKeyHash(apiKeyGenerator.hash("valid-key"))).thenReturn(Optional.of(userWithAdminFlag(false)));
		when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

		boolean proceed = interceptor.preHandle(request, response, new Object());

		assertThat(proceed).isFalse();
		org.mockito.Mockito.verify(response).setStatus(403);
	}

	@Test
	void allowsAnAdminUserThrough() throws Exception {
		ModelLabAdminInterceptor interceptor = new ModelLabAdminInterceptor(userRepository, apiKeyGenerator, objectMapper);
		when(request.getHeader("Authorization")).thenReturn("Bearer valid-key");
		when(userRepository.findByApiKeyHash(apiKeyGenerator.hash("valid-key"))).thenReturn(Optional.of(userWithAdminFlag(true)));

		boolean proceed = interceptor.preHandle(request, response, new Object());

		assertThat(proceed).isTrue();
	}
}
