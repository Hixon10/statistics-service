package com.example.statisticsservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TransactionsRequest {

    private final double amount;

    private final long timestamp;

    public TransactionsRequest(@JsonProperty("amount") double amount,
                               @JsonProperty("timestamp") long timestamp) {
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public double getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "TransactionsRequest{" +
                "amount=" + amount +
                ", timestamp=" + timestamp +
                '}';
    }
}
