package backend;

import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.*;

public class BookStoreServerTest {

    static Thread serverThread;
    static Path dataDir;
    static Path productsFile;
    static Path productsBackup;
    static Path ordersFile;
    static Path ordersBackup;

    @BeforeAll
    static void setUp() throws Exception {
        // Cari data dir secara dinamis — works di lokal maupun CI
        Path cwd = Paths.get("").toAbsolutePath();
        // Coba ../data dulu (saat run dari backend/), lalu data/ (saat run dari root)
        if (Files.exists(cwd.resolve("../data/products.json"))) {
            dataDir = cwd.resolve("../data");
        } else if (Files.exists(cwd.resolve("data/products.json"))) {
            dataDir = cwd.resolve("data");
        } else {
            // Fallback: cari ke atas
            Path p = cwd;
            while (p != null && !Files.exists(p.resolve("data/products.json"))) {
                p = p.getParent();
            }
            dataDir = (p != null) ? p.resolve("data") : cwd.resolve("../data");
        }

        productsFile   = dataDir.resolve("products.json");
        productsBackup = dataDir.resolve("products_seed.json");
        ordersFile     = dataDir.resolve("orders.json");
        ordersBackup   = dataDir.resolve("orders_seed.json");

        System.out.println("[setUp] data dir: " + dataDir.toAbsolutePath());

        // Backup seed data
        Files.copy(productsFile, productsBackup, StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(ordersFile, "[]");
        Files.copy(ordersFile, ordersBackup, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[setUp] Seed data berhasil dibackup.");

        // Start server
        serverThread = new Thread(() -> {
            try { BookStoreServer.main(new String[]{}); }
            catch (Exception e) { }
        });
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(2000);
        System.out.println("[setUp] Server started.");
    }

    @AfterAll
    static void tearDown() throws Exception {
        System.out.println("[tearDown] Mengembalikan data...");
        if (Files.exists(productsBackup))
            Files.copy(productsBackup, productsFile, StandardCopyOption.REPLACE_EXISTING);
        if (Files.exists(ordersBackup))
            Files.copy(ordersBackup, ordersFile, StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(productsBackup);
        Files.deleteIfExists(ordersBackup);
        System.out.println("[tearDown] Selesai.");
    }

    // ════════════════════════════════════════════════════════════
    //  UNIT TEST: calculateTotal() — cover semua 8 branch
    // ════════════════════════════════════════════════════════════

    @Test void testQty0ReturnsZero() {
        assertEquals(0.0, BookStoreServer.calculateTotal(0, 150000), 0.001);
    }

    @Test void testQtyNegativeReturnsZero() {
        assertEquals(0.0, BookStoreServer.calculateTotal(-1, 150000), 0.001);
    }

    @Test void testQty1NoDiscount() {
        // qty=1, price=150000 → 150000 * 1.0 = 150000
        assertEquals(150000.0, BookStoreServer.calculateTotal(1, 150000), 0.001);
    }

    @Test void testQty9NoDiscount() {
        // qty=9, price=100000 → 900000
        assertEquals(900000.0, BookStoreServer.calculateTotal(9, 100000), 0.001);
    }

    @Test void testQty10Discount10Pct() {
        // qty=10, price=120000 → 1200000 - 10% = 1080000
        assertEquals(1080000.0, BookStoreServer.calculateTotal(10, 120000), 0.001);
    }

    @Test void testQty49Discount10Pct() {
        // qty=49, price=100000 → 4900000 - 10% = 4410000
        assertEquals(4410000.0, BookStoreServer.calculateTotal(49, 100000), 0.001);
    }

    @Test void testQty50LowPriceDiscount15Pct() {
        // qty=50, price=85000 (<=100000) → 4250000 - 15% = 3612500
        assertEquals(3612500.0, BookStoreServer.calculateTotal(50, 85000), 0.001);
    }

    @Test void testQty50HighPriceDiscount20Pct() {
        // qty=50, price=150000 (>100000) → 7500000 - 20% = 6000000
        assertEquals(6000000.0, BookStoreServer.calculateTotal(50, 150000), 0.001);
    }

    @Test void testQty99HighPriceDiscount20Pct() {
        // qty=99, price=120000 → 11880000 - 20% = 9504000
        assertEquals(9504000.0, BookStoreServer.calculateTotal(99, 120000), 0.001);
    }

    @Test void testQty100LowPriceDiscount25Pct() {
        // qty=100, price=80000 (<=100000) → 8000000 - 25% = 6000000
        assertEquals(6000000.0, BookStoreServer.calculateTotal(100, 80000), 0.001);
    }

    @Test void testQty100HighPriceDiscount30Pct() {
        // qty=100, price=150000 (>100000) → 15000000 - 30% = 10500000
        assertEquals(10500000.0, BookStoreServer.calculateTotal(100, 150000), 0.001);
    }

    @Test void testQty200HighPriceDiscount30Pct() {
        // qty=200, price=200000 → 40000000 - 30% = 28000000
        assertEquals(28000000.0, BookStoreServer.calculateTotal(200, 200000), 0.001);
    }

    // ════════════════════════════════════════════════════════════
    //  INTEGRATION TEST: endpoint POST /api/calculate
    // ════════════════════════════════════════════════════════════

    @Test void testEndpointHttp200() throws Exception {
        HttpResponse<String> r = post("{\"product_id\":\"B001\",\"quantity\":1}");
        assertEquals(200, r.statusCode());
    }

    @Test void testEndpointContentTypeJson() throws Exception {
        HttpResponse<String> r = post("{\"product_id\":\"B001\",\"quantity\":1}");
        assertTrue(r.headers().firstValue("Content-Type").orElse("").contains("application/json"));
    }

    @Test void testEndpointResponseHasTotal() throws Exception {
        HttpResponse<String> r = post("{\"product_id\":\"B001\",\"quantity\":1}");
        assertTrue(r.body().contains("total"));
    }

    @Test void testEndpointB001Qty1() throws Exception {
        HttpResponse<String> r = post("{\"product_id\":\"B001\",\"quantity\":1}");
        assertTrue(r.body().contains("150000"));
    }

    @Test void testEndpointB002Qty10() throws Exception {
        HttpResponse<String> r = post("{\"product_id\":\"B002\",\"quantity\":10}");
        assertTrue(r.body().contains("1080000"));
    }

    @Test void testEndpointB003Qty50() throws Exception {
        HttpResponse<String> r = post("{\"product_id\":\"B003\",\"quantity\":50}");
        assertTrue(r.body().contains("3612500"));
    }

    @Test void testEndpointQty0() throws Exception {
        HttpResponse<String> r = post("{\"product_id\":\"B001\",\"quantity\":0}");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("0"));
    }

    @Test void testEndpointUnknownProduct() throws Exception {
        HttpResponse<String> r = post("{\"product_id\":\"XXXXX\",\"quantity\":5}");
        assertEquals(200, r.statusCode());
    }

    @Test void testEndpointGETReturns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/calculate"))
                .GET().build();
        HttpResponse<String> r = HttpClient.newHttpClient()
                .send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, r.statusCode());
    }

    @Test void testEndpointIncompletePayload() throws Exception {
        HttpResponse<String> r = post("{\"product_id\":\"B001\"}");
        assertNotNull(r);
    }

    @Test void testEndpointB001Qty100Discount30Pct() throws Exception {
        HttpResponse<String> r = post("{\"product_id\":\"B001\",\"quantity\":100}");
        assertEquals(200, r.statusCode());
        // 100 * 150000 = 15000000, diskon 30% = 10500000
        String body = r.body();
        assertTrue(body.contains("10500000") || body.contains("1.05E7") || body.contains("1.05e7"));
    }

    // ─── Helper ──────────────────────────────────────────────────
    private static HttpResponse<String> post(String payload) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/calculate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        return HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
    }
}