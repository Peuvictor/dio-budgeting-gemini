package io.github.peuvictor.budgeting.infrastructure.http;

import io.github.peuvictor.budgeting.application.ListTransactionsByCategoryUseCase;
import io.github.peuvictor.budgeting.application.PersistTransactionUseCase;
import io.github.peuvictor.budgeting.application.ProcessAudioTransactionUseCase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.servlet.autoconfigure.MultipartAutoConfiguration;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AudioUploadHttpTest {

    private static final int MIB = 1024 * 1024;
    private static final byte[] RESPONSE = {1, 2, 3};
    private static final String BOUNDARY = "audio-upload-test-boundary";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static ServletWebServerApplicationContext context;
    private static HttpClient client;
    private static URI endpoint;
    private static ProcessAudioTransactionUseCase useCase;

    @BeforeAll
    static void startServer() {
        SpringApplication application = new SpringApplication(TestApplication.class);
        application.setWebApplicationType(WebApplicationType.SERVLET);
        context = (ServletWebServerApplicationContext) application.run(
                "--server.address=127.0.0.1", "--server.port=0", "--spring.main.banner-mode=off"
        );
        endpoint = URI.create("http://127.0.0.1:" + context.getWebServer().getPort() + "/transactions/ai");
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        useCase = context.getBean(ProcessAudioTransactionUseCase.class);
    }

    @AfterAll
    static void stopServer() {
        if (client != null) {
            client.close();
        }
        if (context != null) {
            context.close();
        }
    }

    @BeforeEach
    void setUp() {
        clearInvocations(useCase);
        when(useCase.execute(any(byte[].class), anyString())).thenReturn(RESPONSE);
    }

    @Test
    void acceptsTenMibFileThroughMultipartParser() throws Exception {
        byte[] audio = new byte[10 * MIB];
        HttpResponse<byte[]> response = upload(new Part("file", "audio/wav", audio));

        assertEquals(200, response.statusCode());
        assertEquals("audio/wav", response.headers().firstValue("Content-Type").orElseThrow());
        assertArrayEquals(RESPONSE, response.body());
        verify(useCase).execute(audio, "audio/wav");
    }

    @Test
    void rejectsFileOneByteAboveTenMibThroughMultipartParser() throws Exception {
        HttpResponse<byte[]> response = upload(new Part("file", "audio/wav", new byte[10 * MIB + 1]));

        assertProblem(response, 413);
        verifyNoInteractions(useCase);
    }

    @Test
    void rejectsWholeRequestAboveElevenMibWithFilesBelowIndividualLimit() throws Exception {
        HttpResponse<byte[]> response = upload(
                new Part("file", "audio/wav", new byte[6 * MIB]),
                new Part("extra", "audio/wav", new byte[6 * MIB])
        );

        assertProblem(response, 413);
        verifyNoInteractions(useCase);
    }

    @Test
    void rejectsMissingFileOverHttp() throws Exception {
        HttpResponse<byte[]> response = upload(new Part("other", "audio/m4a", new byte[]{1}));

        assertProblem(response, 400);
        verifyNoInteractions(useCase);
    }

    @Test
    void rejectsGenericMimeTypeOverHttp() throws Exception {
        HttpResponse<byte[]> response = upload(new Part("file", "application/octet-stream", new byte[]{1}));

        assertProblem(response, 415);
        verifyNoInteractions(useCase);
    }

    private HttpResponse<byte[]> upload(Part... parts) throws Exception {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (Part part : parts) {
            body.write(("--" + BOUNDARY + "\r\n"
                    + "Content-Disposition: form-data; name=\"" + part.name() + "\"; filename=\"recording\"\r\n"
                    + "Content-Type: " + part.contentType() + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(part.bytes());
            body.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        body.write(("--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                .header("Accept", "audio/wav")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private void assertProblem(HttpResponse<byte[]> response, int expectedStatus) {
        assertEquals(expectedStatus, response.statusCode());
        assertEquals("application/problem+json", response.headers().firstValue("Content-Type").orElseThrow());
        JsonNode problem = JSON.readTree(response.body());
        assertEquals(expectedStatus, problem.get("status").asInt());
        assertEquals("Arquivo de áudio inválido", problem.get("detail").asText());
        assertEquals("file", problem.get("errors").get(0).get("field").asText());
        assertFalse(problem.get("errors").get(0).get("message").asText().isBlank());
    }

    private record Part(String name, String contentType, byte[] bytes) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    @ImportAutoConfiguration({
            TomcatServletWebServerAutoConfiguration.class,
            DispatcherServletAutoConfiguration.class,
            WebMvcAutoConfiguration.class,
            MultipartAutoConfiguration.class,
            JacksonAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class
    })
    @Import({TransactionController.class, AudioUploadValidator.class, AudioUploadExceptionHandler.class})
    static class TestApplication {

        @Bean
        PersistTransactionUseCase persistTransactionUseCase() {
            return mock(PersistTransactionUseCase.class);
        }

        @Bean
        ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase() {
            return mock(ListTransactionsByCategoryUseCase.class);
        }

        @Bean
        ProcessAudioTransactionUseCase processAudioTransactionUseCase() {
            return mock(ProcessAudioTransactionUseCase.class);
        }
    }
}
