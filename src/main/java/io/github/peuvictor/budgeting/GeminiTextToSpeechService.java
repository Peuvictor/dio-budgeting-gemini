package io.github.peuvictor.budgeting;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class GeminiTextToSpeechService {

    private final RestClient restClient;
    private final String model;
    private final String voice;

    public GeminiTextToSpeechService(
            @Value("${spring.ai.google.genai.api-key}") String apiKey,
            @Value("${app.gemini.tts.model}") String model,
            @Value("${app.gemini.tts.voice}") String voice
    ) {
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .defaultHeader("x-goog-api-key", apiKey)
                .build();

        this.model = model;
        this.voice = voice;
    }

    public byte[] synthesize(String text) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "input", List.of(
                        Map.of(
                                "type", "user_input",
                                "content", List.of(
                                        Map.of(
                                                "type", "text",
                                                "text", text
                                        )
                                )
                        )
                ),
                "response_format", Map.of(
                        "type", "audio"
                ),
                "generation_config", Map.of(
                        "speech_config", List.of(
                                Map.of(
                                        "voice", voice
                                )
                        )
                )
        );

        Map<?, ?> response = restClient.post()
                .uri("/interactions")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        List<?> steps = (List<?>) response.get("steps");

        Map<?, ?> modelOutput = (Map<?, ?>) steps.stream()
                .map(step -> (Map<?, ?>) step)
                .filter(step -> "model_output".equals(step.get("type")))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("Gemini não retornou model_output")
                );

        List<?> content = (List<?>) modelOutput.get("content");

        Map<?, ?> audioContent = (Map<?, ?>) content.stream()
                .map(item -> (Map<?, ?>) item)
                .filter(item -> "audio".equals(item.get("type")))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("Gemini não retornou conteúdo de áudio")
                );

        String audioBase64 = (String) audioContent.get("data");

        return Base64.getDecoder().decode(audioBase64);
    }
}