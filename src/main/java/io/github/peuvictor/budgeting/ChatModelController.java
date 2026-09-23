package io.github.peuvictor.budgeting;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChatModelController {

    private final ChatModel chatModel;

    public ChatModelController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/chat-model")
    public String chat(@RequestParam String prompt) {
        return chatModel.call(prompt);
    }
}