package io.github.peuvictor.budgeting.infrastructure.ia;

import io.github.peuvictor.budgeting.application.port.AudioTranscriber;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;
import java.util.List;

@Service
public class GeminiAudioTranscriptionService implements AudioTranscriber {

    private final ChatModel chatModel;

    public GeminiAudioTranscriptionService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String transcribe(byte[] audio, String contentType) {

        MimeType mimeType;

        if (contentType == null
                || contentType.isBlank()
                || contentType.equalsIgnoreCase("application/octet-stream")) {

            mimeType = MimeTypeUtils.parseMimeType("audio/m4a");

        } else {
            mimeType = MimeTypeUtils.parseMimeType(contentType);
        }

        var audioResource = new ByteArrayResource(audio);

        var userMessage = UserMessage.builder()
                .text("""
                        Transcreva este áudio em português brasileiro.
                        Retorne apenas o texto falado, sem explicações adicionais.
                        """)
                .media(List.of(
                        new Media(
                                mimeType,
                                audioResource
                        )
                ))
                .build();

        ChatResponse response = chatModel.call(
                new Prompt(userMessage)
        );

        return response
                .getResult()
                .getOutput()
                .getText();
    }

    public String transcribe(Resource audio, String contentType) {
        try {
            return transcribe(
                    audio.getContentAsByteArray(),
                    contentType
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Não foi possível ler o arquivo de áudio",
                    exception
            );
        }
    }
}