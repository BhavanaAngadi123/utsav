package com.utsav.config;

import com.utsav.config.UtsavProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * AI concierge: Spring AI ChatClient backed by Ollama (local, free).
 * The ChatClient bean only exists when Ollama is reachable; otherwise
 * the concierge runs in offline (pure-Java) mode.
 *
 * <p>Swap to a cloud LLM later via Spring AI's portable ChatClient abstraction.
 */
@Configuration
public class AiConfig {

  @Bean
  @ConditionalOnProperty(name = "utsav.concierge.enabled", havingValue = "true")
  @Conditional(OllamaAvailable.class)
  public ChatClient conciergeChatClient(
      UtsavProperties props,
      @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String baseUrl) {
    String model =
        props.getConcierge() != null && props.getConcierge().getOllamaModel() != null
            ? props.getConcierge().getOllamaModel()
            : "qwen3:8b";
    OllamaApi api = OllamaApi.builder().baseUrl(baseUrl).build();
    OllamaChatModel chatModel =
        OllamaChatModel.builder()
            .ollamaApi(api)
            .options(OllamaChatOptions.builder().model(model).temperature(0.4).build())
            .build();
    return ChatClient.builder(chatModel).build();
  }

  /** True when Ollama's /api/tags answers within 2 seconds. */
  static class OllamaAvailable implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      String baseUrl =
          context.getEnvironment().getProperty("spring.ai.ollama.base-url", "http://localhost:11434");
      try {
        HttpClient client =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        HttpRequest request =
            HttpRequest.newBuilder(URI.create(baseUrl + "/api/tags"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
        HttpResponse<Void> response =
            client.send(request, HttpResponse.BodyHandlers.discarding());
        return response.statusCode() / 100 == 2;
      } catch (Exception e) {
        return false;
      }
    }
  }
}
