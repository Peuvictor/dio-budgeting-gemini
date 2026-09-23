package io.github.peuvictor.budgeting;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GeminiAudioTranscriptionIT {

    @Autowired
    private ChatModel chatModel;

    @Test
    void shouldTranscribeAudio() {
        ClassPathResource audio = new ClassPathResource(
                "audio/Recording1.m4a"
        );

        var userMessage = UserMessage.builder()
                .text("""
                        Transcreva este áudio em português brasileiro.
                        Retorne apenas o texto falado, sem explicações adicionais.
                        """)
                .media(List.of(
                        new Media(
                                MimeTypeUtils.parseMimeType("audio/m4a"),
                                audio
                        )
                ))
                .build();

        ChatResponse response = chatModel.call(
                new Prompt(userMessage)
        );

        String transcription = response
                .getResult()
                .getOutput()
                .getText();

        assertThat(transcription)
                .isNotNull()
                .isNotBlank();

        System.out.println(transcription);
    }
}