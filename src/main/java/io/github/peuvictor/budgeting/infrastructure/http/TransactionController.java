package io.github.peuvictor.budgeting.infrastructure.http;

import io.github.peuvictor.budgeting.application.ListTransactionsByCategoryUseCase;
import io.github.peuvictor.budgeting.application.PersistTransactionUseCase;
import io.github.peuvictor.budgeting.application.ProcessAudioTransactionUseCase;
import io.github.peuvictor.budgeting.domain.Category;
import io.github.peuvictor.budgeting.infrastructure.http.request.TransactionRequest;
import io.github.peuvictor.budgeting.infrastructure.http.response.TransactionResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final PersistTransactionUseCase persistTransactionUseCase;
    private final ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase;
    private final ProcessAudioTransactionUseCase processAudioTransactionUseCase;

    public TransactionController(
            PersistTransactionUseCase persistTransactionUseCase,
            ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase,
            ProcessAudioTransactionUseCase processAudioTransactionUseCase
    ) {
        this.persistTransactionUseCase = persistTransactionUseCase;
        this.listTransactionsByCategoryUseCase = listTransactionsByCategoryUseCase;
        this.processAudioTransactionUseCase = processAudioTransactionUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(
            @RequestBody TransactionRequest request
    ) {
        var output = persistTransactionUseCase.execute(
                request.toInput()
        );

        return TransactionResponse.from(output);
    }

    @GetMapping("/{category}")
    public List<TransactionResponse> readTransactions(
            @PathVariable Category category
    ) {
        return listTransactionsByCategoryUseCase
                .execute(category)
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @PostMapping(
            value = "/ai",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "audio/wav"
    )
    public ResponseEntity<ByteArrayResource> processAudio(
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        byte[] audio = processAudioTransactionUseCase.execute(
                file.getBytes(),
                file.getContentType()
        );

        var resource = new ByteArrayResource(audio);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"response.wav\""
                )
                .contentType(MediaType.parseMediaType("audio/wav"))
                .contentLength(audio.length)
                .body(resource);
    }
}