package com.example.statisticsservice.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsServiceTest {

    @Test
    public void isValidTimestampTest() {
        StatisticsService statisticsService = new StatisticsService(Duration.ofSeconds(60));
        assertFalse(statisticsService.isValidTimestamp(System.currentTimeMillis() - 62_000));
        assertTrue(statisticsService.isValidTimestamp(System.currentTimeMillis() - 58_000));
    }

    @Test
    public void emptyStatisticsTest() {
        StatisticsService statisticsService = new StatisticsService(Duration.ofSeconds(60));
        Statistics stat = statisticsService.getStatisticsCache();
        assertEmptyStatistics(stat);
    }

    @Test
    public void emptyStatisticsInvariantTest() throws InterruptedException {
        final int windowMillis = 500;
        StatisticsService statisticsService = new StatisticsService(Duration.ofMillis(windowMillis));

        statisticsService.saveTransaction(1, System.currentTimeMillis());
        assertTrue(statisticsService.addNewElementsIfNeeded());

        Statistics stat = statisticsService.getStatisticsCache();
        assertEquals(stat.getSum(), 1, 0.01);
        assertEquals(stat.getCount(), 1);
        assertEquals(stat.getAvg(), 1, 0.01);
        assertEquals(stat.getMin(), 1, 0.01);
        assertEquals(stat.getMax(), 1, 0.01);

        assertFalse(statisticsService.addNewElementsIfNeeded());
        Thread.sleep(windowMillis);

        assertTrue(statisticsService.removeOldElementsIfNeeded());
        stat = statisticsService.getStatisticsCache();
        assertEmptyStatistics(stat);

        assertFalse(statisticsService.removeOldElementsIfNeeded());
    }

    @Test
    public void addBatchTest() {
        final int windowMillis = 1000;
        StatisticsService statisticsService = new StatisticsService(Duration.ofMillis(windowMillis));

        List<Double> elements = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            elements.add((double)i);
        }

        Collections.shuffle(elements);

        for (Double amount : elements) {
            statisticsService.saveTransaction(amount, System.currentTimeMillis());
        }

        assertTrue(statisticsService.addNewElementsIfNeeded());
        Statistics stat = statisticsService.getStatisticsCache();
        assertEquals(stat.getSum(), 5050, 0.01);
        assertEquals(stat.getCount(), StatisticsService.BATCH_SIZE);
        assertEquals(stat.getAvg(), 50.5, 0.1);
        assertEquals(stat.getMin(), 1, 0.01);
        assertEquals(stat.getMax(), 100, 0.01);
    }

    @Test
    public void batchTest() throws InterruptedException {
        final int windowMillis = 1000;
        StatisticsService statisticsService = new StatisticsService(Duration.ofMillis(windowMillis));

        final long startMs = System.currentTimeMillis();
        final int totalElements = 252;
        for (int index = 1; index <= totalElements; index++) {
            final long timeMillis;
            if (index < 101) {
                timeMillis = startMs;
            } else if (index < 201) {
                timeMillis = startMs + 1;
            } else {
                timeMillis = startMs + 2;
            }

            statisticsService.saveTransaction(index, timeMillis);
        }

        // add first batch
        assertTrue(statisticsService.addNewElementsIfNeeded());
        Statistics stat = statisticsService.getStatisticsCache();
        assertEquals(stat.getSum(), 5050, 0.01);
        assertEquals(stat.getCount(), StatisticsService.BATCH_SIZE);
        assertEquals(stat.getAvg(), 50.5, 0.1);
        assertEquals(stat.getMin(), 1, 0.01);
        assertEquals(stat.getMax(), 100, 0.01);

        // there are no transactions for removing
        assertFalse(statisticsService.removeOldElementsIfNeeded());

        Thread.sleep(windowMillis / 2);

        // check, that we don't remove anything
        assertFalse(statisticsService.removeOldElementsIfNeeded());
        stat = statisticsService.getStatisticsCache();
        assertEquals(stat.getSum(), 5050, 0.01);
        assertEquals(stat.getCount(), StatisticsService.BATCH_SIZE);
        assertEquals(stat.getAvg(), 50.5, 0.1);
        assertEquals(stat.getMin(), 1, 0.01);
        assertEquals(stat.getMax(), 100, 0.01);

        // add second batch
        assertTrue(statisticsService.addNewElementsIfNeeded());
        stat = statisticsService.getStatisticsCache();
        assertEquals(stat.getSum(), 20100, 0.01);
        assertEquals(stat.getCount(), 2 * StatisticsService.BATCH_SIZE);
        assertEquals(stat.getAvg(), 100.5, 0.1);
        assertEquals(stat.getMin(), 1, 0.01);
        assertEquals(stat.getMax(), 200, 0.01);

        // remove first batch
        Thread.sleep(windowMillis / 2);
        assertTrue(statisticsService.removeOldElementsIfNeeded());
        stat = statisticsService.getStatisticsCache();
        assertEquals(stat.getSum(), 15050, 0.01);
        assertEquals(stat.getCount(), StatisticsService.BATCH_SIZE);
        assertEquals(stat.getAvg(), 150.5, 0.1);
        assertEquals(stat.getMin(), 101, 0.01);
        assertEquals(stat.getMax(), 200, 0.01);

        // add third batch
        assertTrue(statisticsService.addNewElementsIfNeeded());
        stat = statisticsService.getStatisticsCache();
        assertEquals(stat.getSum(), 26828, 0.01);
        assertEquals(stat.getCount(), totalElements - StatisticsService.BATCH_SIZE);
        assertEquals(stat.getAvg(), 176.5, 0.1);
        assertEquals(stat.getMin(), 101, 0.01);
        assertEquals(stat.getMax(), 252, 0.01);

        assertFalse(statisticsService.addNewElementsIfNeeded());

        // remove second batch
        assertTrue(statisticsService.removeOldElementsIfNeeded());
        stat = statisticsService.getStatisticsCache();
        assertEquals(stat.getSum(), 11778, 0.01);
        assertEquals(stat.getCount(), totalElements - 2 * StatisticsService.BATCH_SIZE);
        assertEquals(stat.getAvg(), 226.5, 0.1);
        assertEquals(stat.getMin(), 201, 0.01);
        assertEquals(stat.getMax(), 252, 0.01);

        // remove third batch
        assertTrue(statisticsService.removeOldElementsIfNeeded());
        stat = statisticsService.getStatisticsCache();
        assertEmptyStatistics(stat);

        assertFalse(statisticsService.addNewElementsIfNeeded());
        assertFalse(statisticsService.removeOldElementsIfNeeded());
    }

    private void assertEmptyStatistics(Statistics stat) {
        assertEquals(stat.getSum(), 0, 0.01);
        assertEquals(stat.getCount(), 0);
        assertEquals(stat.getAvg(), 0, 0.01);
        assertEquals(stat.getMin(), 0, 0.01);
        assertEquals(stat.getMax(), 0, 0.01);
    }
}
