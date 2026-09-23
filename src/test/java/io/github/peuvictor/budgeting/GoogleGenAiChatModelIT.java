package io.github.peuvictor.budgeting;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class GoogleGenAiChatModelIT {

    @Autowired
    private ChatModel chatModel;

    @Test
    void shouldReceiveResponseWhenChatModelIsCalled() {
        String response = chatModel.call(
                "Gere um registro de gasto com descrição, valor em reais e local."
        );

        assertNotNull(response);
        assertFalse(response.isBlank());

        System.out.println(response);
    }
}