package io.github.peuvictor.budgeting;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DioBudgetingGeminiApplication {

	@Bean
	ChatClient chatClient(ChatClient.Builder builder) {
		return builder.build();
	}

	public static void main(String[] args) {
		SpringApplication.run(DioBudgetingGeminiApplication.class, args);
	}
}