package io.alchevrier.message.broker;

public record ProduceResponse(int partition, Long offset, String error) {
    public boolean isError() {
        return this.error != null;
    }
}
