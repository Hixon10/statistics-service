package com.example.statisticsservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class StatisticsService {

    private static final Logger log = LoggerFactory.getLogger(StatisticsService.class);
    private static final int POLL_PAUSE_IN_MS = 100;
    static final int BATCH_SIZE = 100;

    private static final Statistics EMPTY_STATISTICS = new Statistics(0, 0, 0, 0, 0);

    public StatisticsService(@Value("${statistics.sliding.window}") Duration slidingWindow) {
        this.slidingWindowInMs = slidingWindow.toMillis();
    }

    private static class QueueElement implements Comparable<QueueElement> {
        private final double amount;
        private final long timestamp;

        private QueueElement(double amount, long timestamp) {
            this.amount = amount;
            this.timestamp = timestamp;
        }

        @Override
        public int compareTo(QueueElement other) {
            return Long.compare(timestamp, other.timestamp);
        }

        @Override
        public String toString() {
            return "QueueElement{" +
                    "amount=" + amount +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }

    private final BlockingQueue<QueueElement> tasks = new LinkedBlockingQueue<>();
    private final Thread tasksThread = new Thread(this::processTasks, "statistics-thread");

    private final PriorityQueue<QueueElement> priorityQueue = new PriorityQueue<>();

    private volatile Statistics statistics = EMPTY_STATISTICS;

    private final long slidingWindowInMs;


    @PostConstruct
    public void init() {
        tasksThread.start();
    }

    @PreDestroy
    public void destroy() {
        log.info("Interrupting tasksThread");
        tasksThread.interrupt();
    }

    public void saveTransaction(double amount, long timestamp) {
        final QueueElement queueElement = new QueueElement(amount, timestamp);
        if (!tasks.offer(queueElement)) {
            log.error("task queue is full: queueElement={}", queueElement);
            // we cannot recalculate stats from this moment
            tasksThread.interrupt();
        }
    }

    public Statistics getStatisticsCache() {
        return statistics;
    }

    public boolean isValidTimestamp(long timestampInMs) {
        return timestampInMs >= System.currentTimeMillis() - slidingWindowInMs;
    }

    private void processTasks() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                final boolean hasProgress =
                        addNewElementsIfNeeded() ||
                        removeOldElementsIfNeeded();

                if (!hasProgress) {
                    Thread.sleep(POLL_PAUSE_IN_MS);
                }
            } catch (InterruptedException e) {
                log.info("task thread were interrupted", e);
                return;
            } catch (Exception e) {
                log.error("unexpected exception occurred", e);
                return;
            }
        }
    }

    boolean removeOldElementsIfNeeded() {
        boolean hasProgress = false;

        for (int index = 0; index < BATCH_SIZE; index++) {
            final QueueElement peekElement = priorityQueue.peek();
            if (peekElement == null || isValidTimestamp(peekElement.timestamp)) {
                break;
            }

            final QueueElement element = priorityQueue.poll();
            if (peekElement != element) {
                log.error("Got unexpected element from priorityQueue: peekElement={}, element={}",
                        peekElement, element);
            }

            hasProgress = true;
        }

        if (!hasProgress) {
            return false;
        }

        recalculateStatisticsForRemovingElements();
        return true;
    }

    private void recalculateStatisticsForRemovingElements() {
        if (priorityQueue.isEmpty()) {
            this.statistics = EMPTY_STATISTICS;
            return;
        }

        recalculateStatistics(priorityQueue, EMPTY_STATISTICS);
    }


    boolean addNewElementsIfNeeded() {
        final List<QueueElement> addedElements = new ArrayList<>(BATCH_SIZE);
        tasks.drainTo(addedElements, BATCH_SIZE);

        if (addedElements.isEmpty()) {
            return false;
        }

        recalculateStatisticsForAddingElements(addedElements);
        return true;
    }

    private void recalculateStatisticsForAddingElements(List<QueueElement> addedElements) {
        priorityQueue.addAll(addedElements);

        final Statistics currentStatistics = this.statistics;
        recalculateStatistics(addedElements, currentStatistics);
    }

    private void recalculateStatistics(final Collection<QueueElement> newElements,
                                       final Statistics currentStatistics) {
        double newSum = currentStatistics.getSum();
        long newCount = currentStatistics.getCount();
        double newMax = currentStatistics.getMax();
        double newMin = currentStatistics.getMin();

        for (QueueElement addedElement : newElements) {
            newSum = newSum + addedElement.amount;
            newCount = newCount + 1;

            if (newCount == 1) {
                newMax = addedElement.amount;
                newMin = addedElement.amount;
            } else {
                newMax = Math.max(newMax,  addedElement.amount);
                newMin = Math.min(newMin,  addedElement.amount);
            }
        }

        final double newAvg = newSum / newCount;
        this.statistics = new Statistics(newSum, newAvg, newMax, newMin, newCount);
    }
}
