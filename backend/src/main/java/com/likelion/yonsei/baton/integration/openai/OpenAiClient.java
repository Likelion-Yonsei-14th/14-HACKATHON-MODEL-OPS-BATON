package com.likelion.yonsei.baton.integration.openai;

import com.likelion.yonsei.baton.common.exception.BusinessException;
import com.likelion.yonsei.baton.integration.llm.ChatCompletionClient;
import com.likelion.yonsei.baton.integration.openai.dto.OpenAiChatMessage;
import com.likelion.yonsei.baton.integration.openai.dto.OpenAiChatRequest;
import com.likelion.yonsei.baton.integration.openai.dto.OpenAiChatResponse;
import com.likelion.yonsei.baton.integration.openai.exception.OpenAiErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.util.List;

@Component
public class OpenAiClient implements ChatCompletionClient {

	private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

	private final RestClient openAiRestClient;
	private final OpenAiProperties properties;

	public OpenAiClient(RestClient openAiRestClient, OpenAiProperties properties) {
		this.openAiRestClient = openAiRestClient;
		this.properties = properties;
	}

	public String chat(String prompt) {
		requireConfigured();
		OpenAiChatRequest request = OpenAiChatRequest.of(properties.model(), List.of(OpenAiChatMessage.user(prompt)));
		return extractContent(callChatCompletions(request));
	}

	/** Structured-output call: instructs the model to return a single JSON object matching the caller's documented schema. */
	public String chatJson(String systemPrompt, String userPrompt) {
		requireConfigured();
		OpenAiChatRequest request = OpenAiChatRequest.ofJson(
				properties.model(),
				List.of(OpenAiChatMessage.system(systemPrompt), OpenAiChatMessage.user(userPrompt))
		);
		return extractContent(callChatCompletions(request));
	}

	/**
	 * Config-driven structured-output call for Model Lab: unlike {@link #chatJson}, the caller
	 * supplies the model and temperature explicitly (from an arbitrary {@code AiModelConfig}
	 * snapshot) instead of using the single production {@code openai.model} property. Still goes
	 * through the same authenticated RestClient and error handling as production calls, per the
	 * spec 17 requirement that Eval and Production share one inference path — the only difference is
	 * which config supplies the model/prompt/temperature. Also surfaces token usage, which production
	 * callers don't currently need but eval cost/latency metrics require.
	 */
	public ChatJsonResult chatJsonWithConfig(String model, Double temperature, String systemPrompt, String userPrompt) {
		requireConfigured();
		OpenAiChatRequest request = OpenAiChatRequest.ofJson(
				model,
				List.of(OpenAiChatMessage.system(systemPrompt), OpenAiChatMessage.user(userPrompt)),
				temperature
		);
		OpenAiChatResponse response = callChatCompletions(request);
		String content = extractContent(response);
		Integer inputTokens = response.usage() != null ? response.usage().promptTokens() : null;
		Integer outputTokens = response.usage() != null ? response.usage().completionTokens() : null;
		return new ChatJsonResult(content, inputTokens, outputTokens);
	}

	public record ChatJsonResult(String content, Integer inputTokens, Integer outputTokens) {
	}

	private void requireConfigured() {
		if (properties.apiKey() == null || properties.apiKey().isBlank()) {
			log.error("OpenAI API key is not configured");
			throw new BusinessException(OpenAiErrorCode.MISCONFIGURED);
		}
	}

	private String extractContent(OpenAiChatResponse response) {
		if (response == null || response.choices() == null || response.choices().isEmpty()) {
			log.error("OpenAI returned an empty response body");
			throw new BusinessException(OpenAiErrorCode.EMPTY_RESPONSE);
		}
		return response.choices().get(0).message().content();
	}

	/** Free/low-tier API keys can have single-digit requests-per-minute limits, which a sequential
	 * Eval Runner sweeping dozens of cases hits immediately — retry a 429 with a fixed backoff
	 * (OpenAI's rate-limit body already tells us it clears in a few seconds) instead of failing the
	 * whole run on the first throttled call. */
	private static final int RATE_LIMIT_MAX_RETRIES = 5;
	private static final long RATE_LIMIT_BACKOFF_MS = 8000;

	private OpenAiChatResponse callChatCompletions(OpenAiChatRequest request) {
		for (int attempt = 0; ; attempt++) {
			try {
				return openAiRestClient.post()
						.uri("/chat/completions")
						.body(request)
						.retrieve()
						.body(OpenAiChatResponse.class);
			} catch (HttpClientErrorException e) {
				if (e.getStatusCode().value() == 429 && attempt < RATE_LIMIT_MAX_RETRIES) {
					log.warn("OpenAI rate limited, retrying in {}ms (attempt {}/{})", RATE_LIMIT_BACKOFF_MS, attempt + 1, RATE_LIMIT_MAX_RETRIES);
					sleep(RATE_LIMIT_BACKOFF_MS);
					continue;
				}
				throw mapClientError(e);
			} catch (HttpServerErrorException e) {
				throw handleServerError(e);
			} catch (ResourceAccessException e) {
				throw handleAccessError(e);
			} catch (RestClientException e) {
				log.error("OpenAI request failed", e);
				throw new BusinessException(OpenAiErrorCode.REQUEST_FAILED);
			}
		}
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			throw new BusinessException(OpenAiErrorCode.REQUEST_FAILED);
		}
	}

	private BusinessException handleServerError(HttpServerErrorException e) {
		log.error("OpenAI server error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
		return new BusinessException(OpenAiErrorCode.UPSTREAM_ERROR);
	}

	private BusinessException handleAccessError(ResourceAccessException e) {
		if (e.getCause() instanceof SocketTimeoutException) {
			log.error("OpenAI request timed out", e);
			return new BusinessException(OpenAiErrorCode.TIMEOUT);
		}
		log.error("OpenAI request could not reach the server", e);
		return new BusinessException(OpenAiErrorCode.REQUEST_FAILED);
	}

	private BusinessException mapClientError(HttpClientErrorException e) {
		HttpStatusCode status = e.getStatusCode();
		log.error("OpenAI client error: status={}, body={}", status, e.getResponseBodyAsString());

		if (status.value() == 401 || status.value() == 403) {
			return new BusinessException(OpenAiErrorCode.UNAUTHORIZED);
		}
		if (status.value() == 429) {
			return new BusinessException(OpenAiErrorCode.RATE_LIMITED);
		}
		return new BusinessException(OpenAiErrorCode.INVALID_REQUEST);
	}
}
