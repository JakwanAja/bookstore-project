package backend;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;


public class BookStoreTestRunner {

    // ─── Warna terminal ──────────────────────────────────────────
    static final String GREEN  = "\u001B[32m";
    static final String RED    = "\u001B[31m";
    static final String YELLOW = "\u001B[33m";
    static final String BOLD   = "\u001B[1m";
    static final String RESET  = "\u001B[0m";

    // ─── Counter hasil tes ────────────────────────────────────────
    static int passed = 0;
    static int failed = 0;

    // ─── Paths data ───────────────────────────────────────────────
    static final String DATA_DIR        = "../data/";
    static final String PRODUCTS_FILE   = DATA_DIR + "products.json";
    static final String PRODUCTS_BACKUP = DATA_DIR + "products_backup.json";
    static final String ORDERS_FILE     = DATA_DIR + "orders.json";
    static final String ORDERS_BACKUP   = DATA_DIR + "orders_backup.json";

    static HttpClient httpClient;
    static final String ENDPOINT = "http://localhost:8080/api/calculate";

    // ════════════════════════════════════════════════════════════
    //  MAIN
    // ════════════════════════════════════════════════════════════
    public static void main(String[] args) throws Exception {
        printHeader("BOOKSTORE - TEST SUITE (Fase 2: Unit & Integration)");

        // ── Setup: backup data ────────────────────────────────────
        backupData();
        httpClient = HttpClient.newHttpClient();

        // ── Fase 2 No. 1: Unit Testing ────────────────────────────
        printSection("FASE 2 NO. 1 — UNIT TESTING");
        printSubSection("A. calculateTotal() — Logika Diskon Bertingkat");
        runUnitTests_CalculateTotal();

        printSubSection("B. Baca/Tulis File JSON");
        runUnitTests_JsonIO();

        // ── Fase 2 No. 2: Integration Testing ────────────────────
        printSection("FASE 2 NO. 2 — INTEGRATION TESTING");
        printSubSection("C. Endpoint POST /api/calculate");

        // Mulai server
        Thread serverThread = startServer();
        Thread.sleep(1500); // tunggu server siap

        runIntegrationTests();

        serverThread.interrupt();

        // ── Restore data & Ringkasan ──────────────────────────────
        restoreData();
        printSummary();
    }

    // ════════════════════════════════════════════════════════════
    //  FASE 2 NO. 1 — UNIT TESTS: calculateTotal()
    // ════════════════════════════════════════════════════════════
    static void runUnitTests_CalculateTotal() {
        // A1: quantity = 0
        assertEquals(
            "A1: Quantity=0 → total harus 0",
            0.0, BookStoreServer.calculateTotal(0, 150000), 0.001
        );

        // A2: quantity negatif
        assertEquals(
            "A2: Quantity=-5 → total harus 0",
            0.0, BookStoreServer.calculateTotal(-5, 150000), 0.001
        );

        // A3: quantity 1 (batas bawah, tanpa diskon)
        assertEquals(
            "A3: Qty=1, price=150000 → total=150000 (diskon 0%)",
            150000.0, BookStoreServer.calculateTotal(1, 150000), 0.001
        );

        // A4: quantity 9 (batas atas tanpa diskon)
        assertEquals(
            "A4: Qty=9, price=150000 → total=1350000 (diskon 0%)",
            1350000.0, BookStoreServer.calculateTotal(9, 150000), 0.001
        );

        // A5: quantity 10 (batas bawah diskon 10%)
        assertEquals(
            "A5: Qty=10, price=85000 → total=765000 (diskon 10%)",
            765000.0, BookStoreServer.calculateTotal(10, 85000), 0.001
        );

        // A6: quantity 49 (batas atas diskon 10%)
        assertEquals(
            "A6: Qty=49, price=85000 → total=3748500 (diskon 10%)",
            3748500.0, BookStoreServer.calculateTotal(49, 85000), 0.001
        );

        // A7: quantity 50, harga > 100000 → diskon 20%
        assertEquals(
            "A7: Qty=50, price=150000 → total=6000000 (diskon 20%)",
            6000000.0, BookStoreServer.calculateTotal(50, 150000), 0.001
        );

        // A8: quantity 50, harga <= 100000 → diskon 15%
        assertEquals(
            "A8: Qty=50, price=85000 → total=3612500 (diskon 15%)",
            3612500.0, BookStoreServer.calculateTotal(50, 85000), 0.001
        );

        // A9: quantity 99, harga > 100000 → diskon 20%
        assertEquals(
            "A9: Qty=99, price=150000 → total=11880000 (diskon 20%)",
            11880000.0, BookStoreServer.calculateTotal(99, 150000), 0.001
        );

        // A10: quantity 100, harga > 100000 → diskon 30%
        assertEquals(
            "A10: Qty=100, price=150000 → total=10500000 (diskon 30%)",
            10500000.0, BookStoreServer.calculateTotal(100, 150000), 0.001
        );

        // A11: quantity 100, harga <= 100000 → diskon 25%
        assertEquals(
            "A11: Qty=100, price=85000 → total=6375000 (diskon 25%)",
            6375000.0, BookStoreServer.calculateTotal(100, 85000), 0.001
        );

        // A12: quantity sangat besar → diskon 30% (harga > 100000)
        assertEquals(
            "A12: Qty=999, price=150000 → total=104895000 (diskon 30%)",
            104895000.0, BookStoreServer.calculateTotal(999, 150000), 0.001
        );

        // A13: harga = 0 → total selalu 0
        assertEquals(
            "A13: Qty=50, price=0 → total=0 (harga nol)",
            0.0, BookStoreServer.calculateTotal(50, 0), 0.001
        );
    }

    // ════════════════════════════════════════════════════════════
    //  FASE 2 NO. 1 — UNIT TESTS: JSON I/O
    // ════════════════════════════════════════════════════════════
    static void runUnitTests_JsonIO() {
        // B1: products.json bisa dibaca
        testCase("B1: products.json dapat dibaca dan tidak kosong", () -> {
            String content = new String(Files.readAllBytes(Paths.get(PRODUCTS_FILE)));
            assertTrue(!content.trim().isEmpty(), "File products.json tidak boleh kosong");
        });

        // B2: products.json berisi B001
        testCase("B2: products.json berisi produk B001", () -> {
            String content = new String(Files.readAllBytes(Paths.get(PRODUCTS_FILE)));
            assertTrue(content.contains("B001"), "Harus ada produk B001");
        });

        // B3: products.json memiliki field price dan stock
        testCase("B3: products.json memiliki field 'price' dan 'stock'", () -> {
            String content = new String(Files.readAllBytes(Paths.get(PRODUCTS_FILE)));
            assertTrue(content.contains("\"price\""), "Harus ada field price");
            assertTrue(content.contains("\"stock\""), "Harus ada field stock");
        });

        // B4: orders.json bisa dibaca (boleh kosong [])
        testCase("B4: orders.json dapat dibaca (boleh array kosong)", () -> {
            String content = new String(Files.readAllBytes(Paths.get(ORDERS_FILE)));
            assertTrue(content.trim().startsWith("["), "orders.json harus berformat JSON array");
        });

        // B5: tulis dan baca ulang orders.json
        testCase("B5: Tulis data ke orders.json lalu baca ulang berhasil", () -> {
            String testData = "[{\"order_id\":\"TEST-001\",\"product_id\":\"B001\","
                            + "\"quantity\":2,\"total\":300000}]";
            Files.write(Paths.get(ORDERS_FILE), testData.getBytes());
            String readBack = new String(Files.readAllBytes(Paths.get(ORDERS_FILE)));
            assertTrue(readBack.contains("TEST-001"), "Data harus tersimpan dan terbaca kembali");
            assertTrue(readBack.contains("B001"), "product_id harus tersimpan dengan benar");
        });
    }

    // ════════════════════════════════════════════════════════════
    //  FASE 2 NO. 2 — INTEGRATION TESTS
    // ════════════════════════════════════════════════════════════
    static void runIntegrationTests() {
        // C1: HTTP 200 OK
        testCase("C1: POST /api/calculate → HTTP 200 OK", () -> {
            HttpResponse<String> r = sendPost("{\"product_id\":\"B001\",\"quantity\":1}");
            assertTrue(r.statusCode() == 200, "Harus return HTTP 200, dapat: " + r.statusCode());
        });

        // C2: Content-Type JSON
        testCase("C2: Response Content-Type adalah application/json", () -> {
            HttpResponse<String> r = sendPost("{\"product_id\":\"B001\",\"quantity\":1}");
            String ct = r.headers().firstValue("Content-Type").orElse("");
            assertTrue(ct.contains("application/json"), "Content-Type harus application/json, dapat: " + ct);
        });

        // C3: Response mengandung field 'total'
        testCase("C3: Response body mengandung field 'total'", () -> {
            HttpResponse<String> r = sendPost("{\"product_id\":\"B001\",\"quantity\":1}");
            assertTrue(r.body().contains("\"total\""), "Response harus ada field 'total': " + r.body());
        });

        // C4: B001, qty=1 → 150000
        testCase("C4: B001, qty=1 → total=150000 (tanpa diskon)", () -> {
            HttpResponse<String> r = sendPost("{\"product_id\":\"B001\",\"quantity\":1}");
            assertTrue(r.statusCode() == 200, "HTTP harus 200");
            assertTrue(r.body().contains("150000"), "Total harus 150000, dapat: " + r.body());
        });

        // C5: B002, qty=10 → 1080000
        testCase("C5: B002, qty=10 → total=1080000 (diskon 10%)", () -> {
            HttpResponse<String> r = sendPost("{\"product_id\":\"B002\",\"quantity\":10}");
            assertTrue(r.statusCode() == 200, "HTTP harus 200");
            assertTrue(r.body().contains("1080000"), "Total harus 1080000, dapat: " + r.body());
        });

        // C6: B003, qty=50 → 3612500
        testCase("C6: B003, qty=50 → total=3612500 (diskon 15%)", () -> {
            HttpResponse<String> r = sendPost("{\"product_id\":\"B003\",\"quantity\":50}");
            assertTrue(r.statusCode() == 200, "HTTP harus 200");
            assertTrue(r.body().contains("3612500"), "Total harus 3612500, dapat: " + r.body());
        });

        // C7: B001, qty=100 → 10500000
        testCase("C7: B001, qty=100 → total=10500000 (diskon 30%)", () -> {
            HttpResponse<String> r = sendPost("{\"product_id\":\"B001\",\"quantity\":100}");
            assertTrue(r.statusCode() == 200, "HTTP harus 200");
            // Java bisa render sebagai 1.05E7 (notasi ilmiah) atau 10500000
            boolean valid = r.body().contains("10500000")
                         || r.body().contains("1.05E7")
                         || r.body().contains("1.05e7");
            assertTrue(valid, "Total harus 10500000, dapat: " + r.body());
        });

        // C8: qty=0 → total=0
        testCase("C8: quantity=0 → total=0 (tidak valid)", () -> {
            HttpResponse<String> r = sendPost("{\"product_id\":\"B001\",\"quantity\":0}");
            assertTrue(r.statusCode() == 200, "HTTP harus 200");
            assertTrue(r.body().contains("0.0") || r.body().contains(": 0}"),
                "Quantity 0 harus total 0, dapat: " + r.body());
        });

        // C9: product_id tidak dikenal → tidak crash, total 0
        testCase("C9: product_id tidak dikenal → server tidak crash, total=0", () -> {
            HttpResponse<String> r = sendPost("{\"product_id\":\"XXXXX\",\"quantity\":5}");
            assertTrue(r.statusCode() == 200, "Server tidak boleh crash, dapat: " + r.statusCode());
            assertTrue(r.body().contains("0"), "Product tidak dikenal harus total 0: " + r.body());
        });

        // C10: GET → 405
        testCase("C10: Method GET ke /api/calculate → HTTP 405 Method Not Allowed", () -> {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT))
                    .GET().build();
            HttpResponse<String> r = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            assertTrue(r.statusCode() == 405, "GET harus return 405, dapat: " + r.statusCode());
        });

        // C11: Payload tidak lengkap → server stabil
        testCase("C11: Payload tidak lengkap → server tetap merespons (tidak crash)", () -> {
            HttpResponse<String> r = sendPost("{\"product_id\":\"B001\"}");
            assertNotNull(r, "Server harus tetap memberikan respons");
            assertTrue(r.statusCode() == 200 || r.statusCode() >= 400,
                "Status code harus valid, dapat: " + r.statusCode());
        });
    }

    // ════════════════════════════════════════════════════════════
    //  HELPER METHODS
    // ════════════════════════════════════════════════════════════

    @FunctionalInterface
    interface TestBlock {
        void run() throws Exception;
    }

    static void testCase(String name, TestBlock block) {
        try {
            block.run();
            System.out.printf("  %s✓ PASS%s  %s%n", GREEN, RESET, name);
            passed++;
        } catch (AssertionError | Exception e) {
            System.out.printf("  %s✗ FAIL%s  %s%n", RED, RESET, name);
            System.out.printf("         → %s%n", e.getMessage());
            failed++;
        }
    }

    static void assertEquals(String name, double expected, double actual, double delta) {
        testCase(name, () -> {
            if (Math.abs(expected - actual) > delta) {
                throw new AssertionError(
                    String.format("Expected %.2f, got %.2f", expected, actual));
            }
        });
    }

    static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static void assertNotNull(Object obj, String message) {
        if (obj == null) throw new AssertionError(message);
    }

    static HttpResponse<String> sendPost(String payload)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    static Thread startServer() {
        Thread t = new Thread(() -> {
            try {
                BookStoreServer.main(new String[]{});
            } catch (IOException e) {
                System.out.println("[INFO] Server: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }

    static void backupData() throws IOException {
        Files.copy(Paths.get(PRODUCTS_FILE), Paths.get(PRODUCTS_BACKUP),
                   StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(ORDERS_FILE), Paths.get(ORDERS_BACKUP),
                   StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[setUp]  Data di-backup ke *_backup.json");
    }

    static void restoreData() throws IOException {
        Files.copy(Paths.get(PRODUCTS_BACKUP), Paths.get(PRODUCTS_FILE),
                   StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(ORDERS_BACKUP), Paths.get(ORDERS_FILE),
                   StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(Paths.get(PRODUCTS_BACKUP));
        Files.deleteIfExists(Paths.get(ORDERS_BACKUP));
        System.out.println("[tearDown] Data dikembalikan ke kondisi semula");
    }

    // ─── Formatting output ────────────────────────────────────────
    static void printHeader(String title) {
        System.out.println();
        System.out.println(BOLD + "╔══════════════════════════════════════════════════════╗" + RESET);
        System.out.printf(BOLD + "║  %-52s  ║%n" + RESET, title);
        System.out.println(BOLD + "╚══════════════════════════════════════════════════════╝" + RESET);
    }

    static void printSection(String title) {
        System.out.println();
        System.out.println(BOLD + YELLOW + "▶ " + title + RESET);
        System.out.println("  " + "─".repeat(52));
    }

    static void printSubSection(String title) {
        System.out.println();
        System.out.println("  " + BOLD + title + RESET);
    }

    static void printSummary() {
        int total = passed + failed;
        System.out.println();
        System.out.println(BOLD + "══════════════════════════════════════════════════════" + RESET);
        System.out.printf(BOLD + "  HASIL: %d/%d tes lulus%n" + RESET, passed, total);
        if (failed == 0) {
            System.out.println(GREEN + BOLD + "  ✓ SEMUA TES BERHASIL!" + RESET);
        } else {
            System.out.printf(RED + BOLD + "  ✗ %d TES GAGAL%n" + RESET, failed);
        }
        System.out.println(BOLD + "══════════════════════════════════════════════════════" + RESET);
        System.out.println();
    }
}