package io.github.peuvictor.budgeting;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class GoogleGenAiChatClientIT {

    @Autowired
    private ChatModel chatModel;

    @Test
    void shouldExecuteSimplePrompt() {
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem("Você é um matemático.")
                .build();

        String response = chatClient.prompt()
                .user("Some 10 + 20. Depois subtraia 30 do resultado anterior. Exiba apenas o resultado final sem explicações.")
                .call()
                .content();

        assertTrue(response.contains("0"));

        System.out.println(response);
    }
}