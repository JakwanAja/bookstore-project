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
        System.out.println("[setUp] Menyalin seed data bersih...");
        Files.copy(Paths.get(PRODUCTS_FILE), Paths.get(PRODUCTS_BACKUP),
                   StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(Paths.get(ORDERS_FILE), "[]");
        Files.copy(Paths.get(ORDERS_FILE), Paths.get(ORDERS_BACKUP),
                   StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[setUp] Seed data berhasil dibackup.");

        serverThread = new Thread(() -> {
            try { BookStoreServer.main(new String[]{}); }
            catch (Exception e) { }
        });
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(2000);
    }

    @AfterAll
    static void tearDown() throws Exception {
        System.out.println("[tearDown] Mengembalikan data ke kondisi semula...");
        Files.copy(Paths.get(PRODUCTS_BACKUP), Paths.get(PRODUCTS_FILE),
                   StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(ORDERS_BACKUP), Paths.get(ORDERS_FILE),
                   StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(Paths.get(PRODUCTS_BACKUP));
        Files.deleteIfExists(Paths.get(ORDERS_BACKUP));
        System.out.println("[tearDown] Selesai.");
    }

    // ════════════════════════════════════════════════════════════
    //  UNIT TEST: calculateTotal() — semua branch
    // ════════════════════════════════════════════════════════════

    // Branch: quantity <= 0 → return 0
    @Test
    void testQuantityZeroReturnsZero() {
        assertEquals(0.0, BookStoreServer.calculateTotal(0, 150000), 0.001);
    }

    @Test
    void testQuantityNegativeReturnsZero() {
        assertEquals(0.0, BookStoreServer.calculateTotal(-5, 150000), 0.001);
    }

    // Branch: quantity 1-9 → diskon 0%
    @Test
    void testQuantity1NoDiscount() {
        assertEquals(150000.0, BookStoreServer.calculateTotal(1, 150000), 0.001);
    }

    @Test
    void testQuantity9NoDiscount() {
        assertEquals(1350000.0, BookStoreServer.calculateTotal(9, 150000), 0.001);
    }

    // Branch: quantity >= 10 && < 50 → diskon 10%
    @Test
    void testQuantity10Discount10Percent() {
        // 10 * 120000 = 1200000, diskon 10% = 1080000
        assertEquals(1080000.0, BookStoreServer.calculateTotal(10, 120000), 0.001);
    }

    @Test
    void testQuantity49Discount10Percent() {
        // 49 * 100000 = 4900000, diskon 10% = 4410000
        assertEquals(4410000.0, BookStoreServer.calculateTotal(49, 100000), 0.001);
    }

    // Branch: quantity >= 50 && < 100, basePrice <= 100000 → diskon 15%
    @Test
    void testQuantity50LowPriceDiscount15Percent() {
        // 50 * 85000 = 4250000, diskon 15% = 3612500
        assertEquals(3612500.0, BookStoreServer.calculateTotal(50, 85000), 0.001);
    }

    // Branch: quantity >= 50 && < 100, basePrice > 100000 → diskon 20%
    @Test
    void testQuantity50HighPriceDiscount20Percent() {
        // 50 * 150000 = 7500000, diskon 20% = 6000000
        assertEquals(6000000.0, BookStoreServer.calculateTotal(50, 150000), 0.001);
    }

    @Test
    void testQuantity99HighPriceDiscount20Percent() {
        // 99 * 200000 = 19800000, diskon 20% = 15840000
        assertEquals(15840000.0, BookStoreServer.calculateTotal(99, 200000), 0.001);
    }

    // Branch: quantity >= 100, basePrice <= 100000 → diskon 25%
    @Test
    void testQuantity100LowPriceDiscount25Percent() {
        // 100 * 80000 = 8000000, diskon 25% = 6000000
        assertEquals(6000000.0, BookStoreServer.calculateTotal(100, 80000), 0.001);
    }

    // Branch: quantity >= 100, basePrice > 100000 → diskon 30%
    @Test
    void testQuantity100HighPriceDiscount30Percent() {
        // 100 * 150000 = 15000000, diskon 30% = 10500000
        assertEquals(10500000.0, BookStoreServer.calculateTotal(100, 150000), 0.001);
    }

    @Test
    void testQuantity200HighPriceDiscount30Percent() {
        // 200 * 200000 = 40000000, diskon 30% = 28000000
        assertEquals(28000000.0, BookStoreServer.calculateTotal(200, 200000), 0.001);
    }

    // ════════════════════════════════════════════════════════════
    //  INTEGRATION TEST: endpoint POST /api/calculate
    // ════════════════════════════════════════════════════════════

    @Test
    void testEndpointHTTP200() throws Exception {
        HttpResponse<String> r = sendPost("{\"product_id\":\"B001\",\"quantity\":1}");
        assertEquals(200, r.statusCode());
    }

    @Test
    void testEndpointResponseContainsTotal() throws Exception {
        HttpResponse<String> r = sendPost("{\"product_id\":\"B001\",\"quantity\":1}");
        assertTrue(r.body().contains("total"));
    }

    @Test
    void testEndpointB001Qty1Total150000() throws Exception {
        HttpResponse<String> r = sendPost("{\"product_id\":\"B001\",\"quantity\":1}");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("150000"));
    }

    @Test
    void testEndpointB002Qty10Total1080000() throws Exception {
        HttpResponse<String> r = sendPost("{\"product_id\":\"B002\",\"quantity\":10}");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("1080000"));
    }

    @Test
    void testEndpointB003Qty50Total3612500() throws Exception {
        HttpResponse<String> r = sendPost("{\"product_id\":\"B003\",\"quantity\":50}");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("3612500"));
    }

    @Test
    void testEndpointQty0ReturnsTotal0() throws Exception {
        HttpResponse<String> r = sendPost("{\"product_id\":\"B001\",\"quantity\":0}");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("0"));
    }

    @Test
    void testEndpointUnknownProductReturnsTotal0() throws Exception {
        HttpResponse<String> r = sendPost("{\"product_id\":\"XXXXX\",\"quantity\":5}");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("0"));
    }

    @Test
    void testEndpointMethodGETReturns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/calculate"))
                .GET().build();
        HttpResponse<String> r = HttpClient.newHttpClient()
                .send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, r.statusCode());
    }

    @Test
    void testEndpointContentTypeIsJSON() throws Exception {
        HttpResponse<String> r = sendPost("{\"product_id\":\"B001\",\"quantity\":1}");
        String ct = r.headers().firstValue("Content-Type").orElse("");
        assertTrue(ct.contains("application/json"));
    }

    @Test
    void testEndpointIncompletePayloadNoServerCrash() throws Exception {
        HttpResponse<String> r = sendPost("{\"product_id\":\"B001\"}");
        assertNotNull(r);
        assertTrue(r.statusCode() == 200 || r.statusCode() >= 400);
    }

    // ─── Helper ──────────────────────────────────────────────────
    private HttpResponse<String> sendPost(String payload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/calculate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}