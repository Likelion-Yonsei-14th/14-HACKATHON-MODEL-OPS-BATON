package com.likelion.yonsei.baton.domain.modellab.security;

import com.likelion.yonsei.baton.common.crypto.ApiKeyGenerator;
import com.likelion.yonsei.baton.common.exception.CommonErrorCode;
import com.likelion.yonsei.baton.common.response.ApiResponse;
import com.likelion.yonsei.baton.domain.modellab.exception.ModelLabErrorCode;
import com.likelion.yonsei.baton.domain.user.entity.User;
import com.likelion.yonsei.baton.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

/**
 * Single enforcement point for the "Model Lab is admin-only" rule (spec section 27): every request
 * under {@code /api/model-lab/**} must come from a user with {@code users.is_admin = true}, checked
 * here so no individual controller can accidentally forget the gate.
 *
 * <p>Runs before Spring MVC's {@code @CurrentUserId} argument resolution, so it duplicates the small
 * bearer-token -> user lookup done in {@code CurrentUserIdArgumentResolver} rather than depending on
 * it. This is intentionally the whole "AdminRequired" mechanism for the MVP — see
 * {@code User.isAdmin()} for how the flag is stored (no role/SSO system exists yet).
 */
@Component
public class ModelLabAdminInterceptor implements HandlerInterceptor {

	private static final String HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final UserRepository userRepository;
	private final ApiKeyGenerator apiKeyGenerator;
	private final ObjectMapper objectMapper;

	public ModelLabAdminInterceptor(UserRepository userRepository, ApiKeyGenerator apiKeyGenerator, ObjectMapper objectMapper) {
		this.userRepository = userRepository;
		this.apiKeyGenerator = apiKeyGenerator;
		this.objectMapper = objectMapper;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		String apiKey = extractApiKey(request.getHeader(HEADER));
		if (apiKey == null) {
			return reject(response, CommonErrorCode.UNAUTHORIZED.getStatus().value(), CommonErrorCode.UNAUTHORIZED.getCode(), CommonErrorCode.UNAUTHORIZED.getMessage());
		}

		Optional<User> user = userRepository.findByApiKeyHash(apiKeyGenerator.hash(apiKey));
		if (user.isEmpty()) {
			return reject(response, CommonErrorCode.UNAUTHORIZED.getStatus().value(), CommonErrorCode.UNAUTHORIZED.getCode(), CommonErrorCode.UNAUTHORIZED.getMessage());
		}
		if (!user.get().isAdmin()) {
			return reject(response, ModelLabErrorCode.ADMIN_REQUIRED.getStatus().value(), ModelLabErrorCode.ADMIN_REQUIRED.getCode(), ModelLabErrorCode.ADMIN_REQUIRED.getMessage());
		}

		request.setAttribute("modelLabUserId", user.get().getId());
		return true;
	}

	private String extractApiKey(String header) {
		if (header == null || !header.startsWith(BEARER_PREFIX)) {
			return null;
		}
		String apiKey = header.substring(BEARER_PREFIX.length()).trim();
		return apiKey.isEmpty() ? null : apiKey;
	}

	private boolean reject(HttpServletResponse response, int status, String code, String message) throws java.io.IOException {
		response.setStatus(status);
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json");
		response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(code, message)));
		return false;
	}
}
