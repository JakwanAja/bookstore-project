package backend;

import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

public class BookStoreServerTest {

    static Thread serverThread;

    @BeforeAll
    static void startServer() throws Exception {

        serverThread = new Thread(() -> {
            try {
                BookStoreServer.main(new String[]{});
            } catch (Exception e) {
            }
        });

        serverThread.setDaemon(true);
        serverThread.start();

        Thread.sleep(2000);
    }

    @Test
    void testCalculateTotalNormal() {
        assertEquals(
                50000,
                BookStoreServer.calculateTotal(5,10000)
        );
    }

    @Test
    void testDiscount30Percent() {
        assertEquals(
                10500000,
                BookStoreServer.calculateTotal(100,150000)
        );
    }

    @Test
    void testEndpointCalculate() throws Exception {

        String payload =
                "{\"product_id\":\"B001\",\"quantity\":2}";

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/calculate"))
                        .header("Content-Type","application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        assertEquals(200,response.statusCode());

        assertTrue(
                response.body().contains("total")
        );
    }
}