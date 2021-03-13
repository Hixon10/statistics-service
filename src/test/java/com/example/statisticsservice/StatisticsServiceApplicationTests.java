package com.example.statisticsservice;

import com.example.statisticsservice.dto.StatisticsResponse;
import com.example.statisticsservice.dto.TransactionsRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {StatisticsServiceApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"statistics.sliding.window=2s"})
class StatisticsServiceApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void apiTest() throws InterruptedException {
        StatisticsResponse statisticsResponse = getStatisticsResponse();
        assertEmptyStatistics(statisticsResponse);

        ResponseEntity<String> saveTransactionResponse = restTemplate.exchange("http://localhost:" + port + "/transactions",
                HttpMethod.POST,
                new HttpEntity<>(new TransactionsRequest(1, System.currentTimeMillis())),
                new ParameterizedTypeReference<String>() {
                });
        assertEquals(HttpStatus.CREATED, saveTransactionResponse.getStatusCode());

        // wait for processing
        Thread.sleep(1000);

        statisticsResponse = getStatisticsResponse();
        assertEquals(1, statisticsResponse.getSum(), 0.01);
        assertEquals(1, statisticsResponse.getCount(), 1);
        assertEquals(1, statisticsResponse.getAvg(), 0.01);
        assertEquals(1, statisticsResponse.getMin(), 0.01);
        assertEquals(1, statisticsResponse.getMax(), 0.01);

        // wait for removing
        Thread.sleep(2000);
        statisticsResponse = getStatisticsResponse();
        assertEmptyStatistics(statisticsResponse);
    }

    private void assertEmptyStatistics(StatisticsResponse statisticsResponse) {
        assertEquals(statisticsResponse.getSum(), 0, 0.01);
        assertEquals(statisticsResponse.getCount(), 0);
        assertEquals(statisticsResponse.getAvg(), 0, 0.01);
        assertEquals(statisticsResponse.getMin(), 0, 0.01);
        assertEquals(statisticsResponse.getMax(), 0, 0.01);
    }

    private StatisticsResponse getStatisticsResponse() {
        final String statisticsUrl = "http://localhost:" + port + "/statistics";
        ResponseEntity<StatisticsResponse> statisticsEntity = restTemplate.getForEntity(statisticsUrl, StatisticsResponse.class);
        assertEquals(HttpStatus.OK, statisticsEntity.getStatusCode());
        return statisticsEntity.getBody();
    }

}
