package com.example.statisticsservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatisticsResponse {

    private final double sum;
    private final double avg;
    private final double max;
    private final double min;
    private final long count;

    public StatisticsResponse(@JsonProperty("sum") double sum,
                              @JsonProperty("avg") double avg,
                              @JsonProperty("max") double max,
                              @JsonProperty("min") double min,
                              @JsonProperty("count") long count) {
        this.sum = sum;
        this.avg = avg;
        this.max = max;
        this.min = min;
        this.count = count;
    }

    public double getSum() {
        return sum;
    }

    public double getAvg() {
        return avg;
    }

    public double getMax() {
        return max;
    }

    public double getMin() {
        return min;
    }

    public long getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "StatisticsResponse{" +
                "sum=" + sum +
                ", avg=" + avg +
                ", max=" + max +
                ", min=" + min +
                ", count=" + count +
                '}';
    }
}
