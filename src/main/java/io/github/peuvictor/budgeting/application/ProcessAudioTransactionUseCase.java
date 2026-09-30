package io.github.peuvictor.budgeting.application;

import io.github.peuvictor.budgeting.application.port.AudioTranscriber;
import io.github.peuvictor.budgeting.application.port.FinancialAssistant;
import io.github.peuvictor.budgeting.application.port.SpeechSynthesizer;
import org.springframework.stereotype.Service;

@Service
public class ProcessAudioTransactionUseCase {

    private final AudioTranscriber audioTranscriber;
    private final FinancialAssistant financialAssistant;
    private final SpeechSynthesizer speechSynthesizer;

    public ProcessAudioTransactionUseCase(
            AudioTranscriber audioTranscriber,
            FinancialAssistant financialAssistant,
            SpeechSynthesizer speechSynthesizer
    ) {
        this.audioTranscriber = audioTranscriber;
        this.financialAssistant = financialAssistant;
        this.speechSynthesizer = speechSynthesizer;
    }

    public byte[] execute(byte[] audio, String contentType) {

        var transcription = audioTranscriber.transcribe(
                audio,
                contentType
        );

        var response = financialAssistant.process(
                transcription
        );

        return speechSynthesizer.synthesize(
                response
        );
    }
}