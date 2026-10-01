package io.github.peuvictor.budgeting.infrastructure.http;

import io.github.peuvictor.budgeting.application.ListTransactionsByCategoryUseCase;
import io.github.peuvictor.budgeting.application.PersistTransactionUseCase;
import io.github.peuvictor.budgeting.application.ProcessAudioTransactionUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionControllerAudioUploadTest {

    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final byte[] AUDIO = {1, 2, 3};
    private static final byte[] RESPONSE = {4, 5, 6};

    private MockMvc mockMvc;
    private TransactionController controller;
    private ProcessAudioTransactionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = mock(ProcessAudioTransactionUseCase.class);
        MultipartProperties properties = new MultipartProperties();
        properties.setMaxFileSize(DataSize.ofBytes(MAX_FILE_SIZE));
        controller = new TransactionController(
                mock(PersistTransactionUseCase.class),
                mock(ListTransactionsByCategoryUseCase.class),
                useCase,
                new AudioUploadValidator(properties)
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new TransactionValidationHandler(), new AudioUploadExceptionHandler())
                .build();
    }

    @Test
    void rejectsMissingFile() throws Exception {
        mockMvc.perform(multipart("/transactions/ai").accept("audio/wav"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("file"))
                .andExpect(jsonPath("$.errors[0].message").value("O arquivo é obrigatório"));

        verifyNoInteractions(useCase);
    }

    @Test
    void rejectsEmptyFile() throws Exception {
        mockMvc.perform(multipart("/transactions/ai")
                        .file(new MockMultipartFile("file", "empty.m4a", "audio/m4a", new byte[0]))
                        .accept("audio/wav"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("file"))
                .andExpect(jsonPath("$.errors[0].message").value("O arquivo de áudio não pode estar vazio"));

        verifyNoInteractions(useCase);
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @ValueSource(strings = {" ", "application/octet-stream", "text/plain", "image/png", "audio/ogg",
            "audio/*", "invalid", "audio/wav; charset=\"unterminated"})
    void rejectsUnsupportedMimeTypes(String contentType) throws Exception {
        mockMvc.perform(multipart("/transactions/ai")
                        .file(new MockMultipartFile("file", "recording.m4a", contentType, AUDIO))
                        .accept("audio/wav"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.detail").value("Arquivo de áudio inválido"))
                .andExpect(jsonPath("$.errors[0].field").value("file"))
                .andExpect(jsonPath("$.errors[0].message").value("Informe um tipo de áudio aceito: M4A, MP3 ou WAV"));

        verifyNoInteractions(useCase);
    }

    @ParameterizedTest
    @MethodSource("acceptedMimeTypes")
    void acceptsAudioAndNormalizesMimeType(String contentType, String normalizedType) throws Exception {
        when(useCase.execute(any(byte[].class), anyString())).thenReturn(RESPONSE);

        mockMvc.perform(multipart("/transactions/ai")
                        .file(new MockMultipartFile("file", "recording", contentType, AUDIO))
                        .accept("audio/wav"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/wav"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"response.wav\""))
                .andExpect(header().longValue("Content-Length", RESPONSE.length))
                .andExpect(content().bytes(RESPONSE));

        verify(useCase).execute(AUDIO, normalizedType);
    }

    @Test
    void acceptsFileAtSizeLimit() throws Exception {
        byte[] audio = new byte[MAX_FILE_SIZE];
        when(useCase.execute(any(byte[].class), anyString())).thenReturn(RESPONSE);

        mockMvc.perform(multipart("/transactions/ai")
                        .file(new MockMultipartFile("file", "recording.wav", "audio/wav", audio)))
                .andExpect(status().isOk());

        verify(useCase).execute(audio, "audio/wav");
    }

    @Test
    void rejectsFileOneByteAboveSizeLimit() throws Exception {
        mockMvc.perform(multipart("/transactions/ai")
                        .file(new MockMultipartFile("file", "recording.wav", "audio/wav", new byte[MAX_FILE_SIZE + 1]))
                        .accept("audio/wav"))
                .andExpect(status().isContentTooLarge())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.errors[0].field").value("file"));

        verifyNoInteractions(useCase);
    }

    @Test
    void rejectsOversizedFileBeforeReadingBytes() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn((long) MAX_FILE_SIZE + 1);

        assertThrows(InvalidAudioUploadException.class, () -> controller.processAudio(file));

        verify(file, never()).getBytes();
        verifyNoInteractions(useCase);
    }

    @Test
    void usesConfiguredFileLimit() throws Exception {
        MultipartProperties properties = new MultipartProperties();
        properties.setMaxFileSize(DataSize.ofBytes(2));
        TransactionController smallLimitController = new TransactionController(
                mock(PersistTransactionUseCase.class), mock(ListTransactionsByCategoryUseCase.class),
                useCase, new AudioUploadValidator(properties)
        );

        assertThrows(InvalidAudioUploadException.class, () -> smallLimitController.processAudio(
                new MockMultipartFile("file", "recording.wav", "audio/wav", AUDIO)
        ));
        verifyNoInteractions(useCase);
    }

    private static Stream<Arguments> acceptedMimeTypes() {
        return Stream.of(
                Arguments.of("audio/m4a", "audio/m4a"),
                Arguments.of("audio/mp4", "audio/m4a"),
                Arguments.of("audio/x-m4a", "audio/m4a"),
                Arguments.of("audio/mp3", "audio/mp3"),
                Arguments.of("audio/mpeg", "audio/mp3"),
                Arguments.of("audio/wav", "audio/wav"),
                Arguments.of("audio/x-wav", "audio/wav"),
                Arguments.of("AUDIO/X-WAV; charset=UTF-8", "audio/wav")
        );
    }
}
