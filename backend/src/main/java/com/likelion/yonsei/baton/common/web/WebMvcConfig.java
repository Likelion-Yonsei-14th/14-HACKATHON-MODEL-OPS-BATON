package com.likelion.yonsei.baton.common.web;

import com.likelion.yonsei.baton.domain.modellab.security.ModelLabAdminInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	private final CurrentUserIdArgumentResolver currentUserIdArgumentResolver;
	private final ModelLabAdminInterceptor modelLabAdminInterceptor;
	private final List<String> allowedOrigins;

	public WebMvcConfig(
			CurrentUserIdArgumentResolver currentUserIdArgumentResolver,
			ModelLabAdminInterceptor modelLabAdminInterceptor,
			@Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}") String allowedOrigins
	) {
		this.currentUserIdArgumentResolver = currentUserIdArgumentResolver;
		this.modelLabAdminInterceptor = modelLabAdminInterceptor;
		this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
				.map(String::trim)
				.filter(origin -> !origin.isEmpty())
				.toList();
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(currentUserIdArgumentResolver);
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		// Sole enforcement point for "Model Lab is admin-only" (spec section 27) — every
		// /api/model-lab/** controller relies on this running first, instead of each doing its own check.
		registry.addInterceptor(modelLabAdminInterceptor).addPathPatterns("/api/model-lab/**");
	}

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		// The Jackson 3 message converter Spring Boot 4 auto-configures here defaults its response
		// charset to ISO-8859-1, which silently mangles every Korean error/DTO string into '?' — force
		// UTF-8 instead of leaving it to negotiate a charset per request.
		for (HttpMessageConverter<?> converter : converters) {
			if (converter instanceof AbstractJacksonHttpMessageConverter jacksonConverter) {
				jacksonConverter.setDefaultCharset(StandardCharsets.UTF_8);
			}
		}
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		// APP_CORS_ALLOWED_ORIGINS existed as a documented env var since PR #3 but was never wired up,
		// so the browser silently blocked every cross-origin call the frontend made. Register it for real.
		registry.addMapping("/api/**")
				.allowedOrigins(allowedOrigins.toArray(new String[0]))
				.allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
				.allowedHeaders("Authorization", "Content-Type")
				.maxAge(3600);
	}
}
