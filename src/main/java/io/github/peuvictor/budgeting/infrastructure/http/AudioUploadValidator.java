package io.github.peuvictor.budgeting.infrastructure.http;

import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Map;

@Component
public class AudioUploadValidator {

    private static final Map<String, String> SUPPORTED_TYPES = Map.of(
            "audio/m4a", "audio/m4a",
            "audio/mp4", "audio/m4a",
            "audio/x-m4a", "audio/m4a",
            "audio/mp3", "audio/mp3",
            "audio/mpeg", "audio/mp3",
            "audio/wav", "audio/wav",
            "audio/x-wav", "audio/wav"
    );

    private final long maxFileSize;

    public AudioUploadValidator(MultipartProperties multipartProperties) {
        this.maxFileSize = multipartProperties.getMaxFileSize().toBytes();
    }

    public String validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidAudioUploadException(
                    HttpStatus.BAD_REQUEST, "O arquivo de áudio não pode estar vazio"
            );
        }
        if (file.getSize() > maxFileSize) {
            throw new InvalidAudioUploadException(
                    HttpStatus.CONTENT_TOO_LARGE, "O arquivo excede o tamanho máximo permitido"
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw unsupportedType();
        }

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(contentType.trim());
        } catch (InvalidMediaTypeException exception) {
            throw unsupportedType();
        }

        String normalizedType = (mediaType.getType() + "/" + mediaType.getSubtype())
                .toLowerCase(Locale.ROOT);
        String supportedType = SUPPORTED_TYPES.get(normalizedType);
        if (supportedType == null) {
            throw unsupportedType();
        }
        return supportedType;
    }

    private InvalidAudioUploadException unsupportedType() {
        return new InvalidAudioUploadException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Informe um tipo de áudio aceito: M4A, MP3 ou WAV"
        );
    }
}
