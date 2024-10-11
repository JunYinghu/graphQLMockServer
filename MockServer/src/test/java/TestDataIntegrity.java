import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.given;

public class TestDataIntegrity {
    int receivedPCWithInThreshold = 0;
    int receivedPCOutOfThreshold = 0;
    private WireMockServer wireMockServer;

    WireMockServer startWireMockServer(int port) {
        WireMockServer wireMockServer = new WireMockServer(port);
        wireMockServer.start();
        configureFor("localhost", port);
        stubFor(post(urlEqualTo("/api/data/receive")).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")));
        return wireMockServer;
    }

    void stopWireMockServer(WireMockServer wireMockServer) {
        wireMockServer.stop();
    }

    @Test(description = "To Verify Data Sending Integrity ")
    public void testDataIntegrity() {
        // Setup Mock Server
        wireMockServer = startWireMockServer(9090);
        long threshold = 10;
        int eventSentCount = 100;
        //Step To Send Data In a New Thread
        baseURI = "http://localhost:9090/api/data/receive";
        Thread dataSendRequest = new Thread(() -> {
            try {
                sendPost(eventSentCount, 100);
            } catch (Exception e) {
                System.out.println("Error: Sending Data");
            }
        });
        dataSendRequest.start();
        // Verify Data Sending Integrity
        dataValidation(eventSentCount, threshold);

        // Send Data Thread Join
        try {
            dataSendRequest.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // Stop WireMock Server
        stopWireMockServer(wireMockServer);
    }

    private List<ServeEvent> receivedDataMonitor(int expectedReceivedDataCount, long threshold) {
        long startTime = System.currentTimeMillis();
        long timeBox = TimeUnit.SECONDS.toMillis(40);
        long firstInterval = TimeUnit.SECONDS.toMillis(threshold);

        long loopStartTime = System.currentTimeMillis();
        long elapsedTime = 0;
        List<ServeEvent> serverEvents = null;
        boolean bValidation = false;
        int totalReceivedPCCount =0;

        while (System.currentTimeMillis() - startTime < timeBox) {
            serverEvents = wireMockServer.getAllServeEvents();
            long currentTime = System.currentTimeMillis();

            // Determine Posts Count Were Received in As Per Threshold
            if (currentTime - startTime < firstInterval) {
                receivedPCWithInThreshold = serverEvents.size();
            } else {
                receivedPCOutOfThreshold = serverEvents.size() - receivedPCWithInThreshold;
            }
            //System.out.println("Info Received Post Count (First " + threshold + " seconds): " + receivedPCWithInThreshold);
            //System.out.println("Info Received Post Count (After " + threshold + " seconds): " + receivedPCOutOfThreshold);
            totalReceivedPCCount = receivedPCWithInThreshold + receivedPCOutOfThreshold;
            if (totalReceivedPCCount == expectedReceivedDataCount && !bValidation) {
                long currentLoopTime = System.currentTimeMillis();
                elapsedTime = currentLoopTime - loopStartTime;
                System.out.println("Info Received Post Count " + receivedPCWithInThreshold + " Within " + threshold + "(Mills)");
                System.out.println("Info Received Post Count " + receivedPCOutOfThreshold + " Out of " + threshold + "(Mills)");
                System.out.println("Info Received Post Count " + totalReceivedPCCount + " Spend Total Period (Mills): " + elapsedTime);
                bValidation = true;
            }
            if (totalReceivedPCCount > expectedReceivedDataCount) {
                System.out.println("Error: Received Post Count Exceeds Expected  " + expectedReceivedDataCount);
            }
            waitForNext(500);
        }
        boolean verifiedDataReceived = totalReceivedPCCount == expectedReceivedDataCount;
        Assert.assertTrue(verifiedDataReceived && elapsedTime<=15000,"Error: Received Post Count Exceeds Expected" );

        return serverEvents;
    }

    void dataValidation(int expectedReceivedDataCount, long threshold) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<ServeEvent> serverEvents = receivedDataMonitor(expectedReceivedDataCount, threshold);
        int totalContentReceived = 0;
        int invalidContentSize = 0;
        int invalidContent = 0;
        // Validate Post Count
        if (!(serverEvents.size() == expectedReceivedDataCount)) {
            System.out.println("Error Received Post Count Does Not As Expected");
        } else {
            // Validate Post Content
            for (ServeEvent event : serverEvents) {
                totalContentReceived++;
                String postBody = event.getRequest().getBodyAsString();
                try {
                    Map<String, Object> postData = objectMapper.readValue(postBody, new TypeReference<Map<String, Object>>() {
                    });
                    // Validate Size Of Each Post
                    if (postBody.length() > 10240) { // 10 KB limit
                        invalidContentSize++;
                        //System.out.println("Error:  Post Content Exceeds 10 KB: " + postBody);
                    }
                    // Validate Field Existence In Each Post
                    if (!postData.containsKey("testPost")) {
                        invalidContent++;
                        //System.out.println("Error: Post Content Does Not Include 'testPost': " + postBody);
                    }
                } catch (IOException e) {
                    System.out.println("Error: Failed to Parse Post Body: " + postBody);
                }
            }
        }
        System.out.println("Info Total Received Post Count ： " + totalContentReceived);
        System.out.println("Info Total Received Post Count With Invalid Content Size： " + invalidContentSize);
        System.out.println("Info Total Received Post Count With Invalid Content ： " + invalidContent);

        Assert.assertEquals(expectedReceivedDataCount, serverEvents.size(), "Error Received Post Count Does Not As Expected");
        Assert.assertEquals(expectedReceivedDataCount, totalContentReceived, "Error Total Received Post Count As Expected");
        Assert.assertEquals(invalidContentSize, 0, "Error Total Received Post Count With Invalid Content Size Not As Expected");
        Assert.assertEquals(invalidContent, 50, "Error Total Received Post Count With Invalid Content s Not As Expected");

    }

    void waitForNext(int interval) {
        try {
            Thread.sleep(interval);
            //System.out.println("Next Re-check");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    void sendPost(int repeatTime, int interval) {
        String requestBody = null;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < repeatTime; i++) {
            if (i < repeatTime / 2) {
                requestBody = "{\"testPost\":\"testing data\"}";
            } else {
                requestBody = "{\"testPostInvalid\":\"testing data\"}";
            }
            given().contentType("application/json").body(requestBody).post();

            // Wait for interval
            waitForNext(interval);
        }
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        System.out.println("Total Time Spent: " + totalTime + " Milliseconds");
    }
}
