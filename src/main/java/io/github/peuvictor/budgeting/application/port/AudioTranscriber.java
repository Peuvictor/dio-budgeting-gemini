package io.github.peuvictor.budgeting.application.port;

public interface AudioTranscriber {

    String transcribe(byte[] audio, String contentType);
}