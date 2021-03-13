package com.example.statisticsservice.controller;

import com.example.statisticsservice.dto.StatisticsResponse;
import com.example.statisticsservice.dto.TransactionsRequest;
import com.example.statisticsservice.service.Statistics;
import com.example.statisticsservice.service.StatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    private final StatisticsService statisticsService;

    public MainController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @PostMapping("/transactions")
    public ResponseEntity<Void> transactions(@RequestBody TransactionsRequest request) {
        if (request == null || request.getTimestamp() < 0) {
            log.warn("Invalid request: request={}", request);
        } else if (!statisticsService.isValidTimestamp(request.getTimestamp())) {
            log.warn("Got request from past: request={}", request);
        } else {
            statisticsService.saveTransaction(request.getAmount(), request.getTimestamp());
        }

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        final Statistics stat = statisticsService.getStatisticsCache();
        return ResponseEntity.ok(new StatisticsResponse(
                stat.getSum(),
                stat.getAvg(),
                stat.getMax(),
                stat.getMin(),
                stat.getCount()
        ));
    }
}
