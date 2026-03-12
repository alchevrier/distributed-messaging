package io.alchevrier.message.broker;

public record FlushResponse(String error) {
    public boolean isError() {
        return this.error != null;
    }
}
