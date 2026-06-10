package backend;

import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.*;

public class BookStoreServerTest {

    static Thread serverThread;

    // ─── Seed Data (Fase 2 No. 5 — Continuous Testing) ───────────
    static final String DATA_DIR        = "../data/";
    static final String PRODUCTS_FILE   = DATA_DIR + "products.json";
    static final String PRODUCTS_BACKUP = DATA_DIR + "products_seed.json";
    static final String ORDERS_FILE     = DATA_DIR + "orders.json";
    static final String ORDERS_BACKUP   = DATA_DIR + "orders_seed.json";

    @BeforeAll
    static void setUp() throws Exception {
        // 1. Backup seed data bersih sebelum semua tes dimulai
        System.out.println("[setUp] Menyalin seed data bersih...");
        Files.copy(Paths.get(PRODUCTS_FILE), Paths.get(PRODUCTS_BACKUP),
                   StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(Paths.get(ORDERS_FILE), "[]");
        Files.copy(Paths.get(ORDERS_FILE), Paths.get(ORDERS_BACKUP),
                   StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[setUp] Seed data berhasil dibackup.");

        // 2. Jalankan server Java
        serverThread = new Thread(() -> {
            try {
                BookStoreServer.main(new String[]{});
            } catch (Exception e) { }
        });
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(2000);
    }

    @AfterAll
    static void tearDown() throws Exception {
        // Kembalikan data ke kondisi semula setelah semua tes selesai
        System.out.println("[tearDown] Mengembalikan data ke kondisi semula...");
        Files.copy(Paths.get(PRODUCTS_BACKUP), Paths.get(PRODUCTS_FILE),
                   StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(ORDERS_BACKUP), Paths.get(ORDERS_FILE),
                   StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(Paths.get(PRODUCTS_BACKUP));
        Files.deleteIfExists(Paths.get(ORDERS_BACKUP));
        System.out.println("[tearDown] Data berhasil dikembalikan. File seed dihapus.");
    }

    @Test
    void testCalculateTotalNormal() {
        assertEquals(50000, BookStoreServer.calculateTotal(5, 10000));
    }

    @Test
    void testDiscount30Percent() {
        assertEquals(10500000, BookStoreServer.calculateTotal(100, 150000));
    }

    @Test
    void testEndpointCalculate() throws Exception {
        String payload = "{\"product_id\":\"B001\",\"quantity\":2}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/calculate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("total"));
    }
}