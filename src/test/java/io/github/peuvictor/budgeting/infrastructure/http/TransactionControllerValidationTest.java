package io.github.peuvictor.budgeting.infrastructure.http;

import io.github.peuvictor.budgeting.application.ListTransactionsByCategoryUseCase;
import io.github.peuvictor.budgeting.application.PersistTransactionUseCase;
import io.github.peuvictor.budgeting.application.ProcessAudioTransactionUseCase;
import io.github.peuvictor.budgeting.application.input.PersistTransactionInput;
import io.github.peuvictor.budgeting.application.output.TransactionOutput;
import io.github.peuvictor.budgeting.domain.InvalidTransactionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionControllerValidationTest {

    private MockMvc mockMvc;
    private PersistTransactionUseCase persistTransactionUseCase;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        persistTransactionUseCase = mock(PersistTransactionUseCase.class);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        TransactionController controller = new TransactionController(
                persistTransactionUseCase,
                mock(ListTransactionsByCategoryUseCase.class),
                mock(ProcessAudioTransactionUseCase.class)
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new TransactionValidationHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void rejectsInvalidRequestWithoutCallingUseCase(String json, String field) throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value(field));

        verifyNoInteractions(persistTransactionUseCase);
    }

    @Test
    void acceptsValidRequest() throws Exception {
        when(persistTransactionUseCase.execute(any(PersistTransactionInput.class)))
                .thenReturn(new TransactionOutput("id", "Mercado", "GROCERIES", 125.33));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Mercado","amount":12533,"category":"GROCERIES"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(125.33));

        verify(persistTransactionUseCase).execute(any(PersistTransactionInput.class));
    }

    @Test
    void convertsDomainValidationToBadRequest() throws Exception {
        when(persistTransactionUseCase.execute(any(PersistTransactionInput.class)))
                .thenThrow(new InvalidTransactionException("amount", "deve ser maior que zero"));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Mercado","amount":12533,"category":"GROCERIES"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("amount"));
    }

    private static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("""
                        {"amount":100,"category":"OTHER"}
                        """, "description"),
                Arguments.of("""
                        {"description":"   ","amount":100,"category":"OTHER"}
                        """, "description"),
                Arguments.of("{\"description\":\"" + "x".repeat(256)
                        + "\",\"amount\":100,\"category\":\"OTHER\"}", "description"),
                Arguments.of("""
                        {"description":"Mercado","amount":0,"category":"GROCERIES"}
                        """, "amount"),
                Arguments.of("""
                        {"description":"Mercado","amount":-100,"category":"GROCERIES"}
                        """, "amount"),
                Arguments.of("""
                        {"description":"Mercado","category":"GROCERIES"}
                        """, "amount"),
                Arguments.of("""
                        {"description":"Mercado","amount":100}
                        """, "category")
        );
    }
}
