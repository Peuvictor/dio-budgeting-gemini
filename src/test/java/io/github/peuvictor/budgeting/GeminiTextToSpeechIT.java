package io.github.peuvictor.budgeting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(
        named = "GOOGLE_API_KEY",
        matches = ".+"
)
class GeminiTextToSpeechIT {

    @Autowired
    private GeminiTextToSpeechService textToSpeechService;

    @Test
    void shouldGenerateAudioWhenTextIsProvided() throws IOException {
        String text = """
                O valor total do serviço ficou em 80 reais.
                Posso confirmar o pagamento?
                """;

        byte[] audio = textToSpeechService.synthesize(text);

        assertThat(audio)
                .isNotNull()
                .isNotEmpty();

        assertThat(audio.length)
                .isGreaterThan(1024);

        Path tempFile = Files.createTempFile(
                "gemini-audio-",
                ".wav"
        );

        Files.write(tempFile, audio);

        System.out.println(
                "Áudio gerado em: " + tempFile.toAbsolutePath()
        );
    }
}