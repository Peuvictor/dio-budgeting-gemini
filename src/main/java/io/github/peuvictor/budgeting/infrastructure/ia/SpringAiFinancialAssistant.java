package io.github.peuvictor.budgeting.infrastructure.ia;

import io.github.peuvictor.budgeting.application.ListTransactionsByCategoryUseCase;
import io.github.peuvictor.budgeting.application.PersistTransactionUseCase;
import io.github.peuvictor.budgeting.application.port.FinancialAssistant;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;

@Service
public class SpringAiFinancialAssistant implements FinancialAssistant {

    private final ChatClient chatClient;

    public SpringAiFinancialAssistant(
            PersistTransactionUseCase persistTransactionUseCase,
            ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase,
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:/prompts/system-message.st") Resource systemPrompt
    ) throws IOException {

        this.chatClient = chatClientBuilder
                .defaultSystem(
                        systemPrompt.getContentAsString(
                                Charset.defaultCharset()
                        )
                )
                .defaultTools(
                        persistTransactionUseCase,
                        listTransactionsByCategoryUseCase
                )
                .build();
    }

    @Override
    public String process(String message) {
        return chatClient
                .prompt()
                .user(message)
                .call()
                .content();
    }
}