package io.github.peuvictor.budgeting.application.port;

public interface SpeechSynthesizer {

    byte[] synthesize(String text);
}