package io.alchevrier.message;

public record ProduceResponse(int partition, Long offset, String error) {
    public boolean isError() {
        return this.error != null;
    }
}
