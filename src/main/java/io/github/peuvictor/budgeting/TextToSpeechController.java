package io.github.peuvictor.budgeting;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TextToSpeechController {

    private final GeminiTextToSpeechService textToSpeechService;

    public TextToSpeechController(
            GeminiTextToSpeechService textToSpeechService
    ) {
        this.textToSpeechService = textToSpeechService;
    }

    @PostMapping(
            value = "/synthesize",
            produces = "audio/wav"
    )
    public ResponseEntity<ByteArrayResource> synthesize(
            @RequestBody TextToSpeechRequest request
    ) {
        byte[] audio = textToSpeechService.synthesize(
                request.text()
        );

        ByteArrayResource resource =
                new ByteArrayResource(audio);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"speech.wav\""
                )
                .contentType(
                        MediaType.parseMediaType("audio/wav")
                )
                .contentLength(audio.length)
                .body(resource);
    }
}