package io.alchevrier.message;

public record ProduceResponse(long offset, Topic topic, String error) {
    public boolean isError() {
        return this.error != null;
    }
}
