package io.alchevrier.message;

public record FlushResponse(String error) {
    public boolean isError() {
        return this.error != null;
    }
}
