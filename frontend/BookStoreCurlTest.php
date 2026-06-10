<?php

define('JAVA_API_URL', 'http://localhost:8080/api/calculate');
define('PRODUCTS_FILE', '../data/products.json');
define('ORDERS_FILE',   '../data/orders.json');

// ─── Counter hasil ───────────────────────────────────────────
$passed = 0;
$failed = 0;

// ════════════════════════════════════════════════════════════
//  FUNGSI HELPER
// ════════════════════════════════════════════════════════════

/**
 * Fungsi cURL yang diekstrak dari index.php
 * Inilah fungsi yang kita uji secara terisolasi
 */
function sendThroughCurl(string $productId, int $quantity): array {
    $payload = json_encode([
        "product_id" => $productId,
        "quantity"   => $quantity
    ]);

    $ch = curl_init(JAVA_API_URL);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $payload);
    curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
    curl_setopt($ch, CURLOPT_TIMEOUT, 5); // timeout 5 detik

    $response   = curl_exec($ch);
    $httpCode   = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError  = curl_error($ch);
    curl_close($ch);

    return [
        'response'  => $response,
        'http_code' => $httpCode,
        'error'     => $curlError,
        'decoded'   => $response ? json_decode($response, true) : null
    ];
}

/**
 * Runner untuk setiap test case
 */
function testCase(string $name, callable $fn): void {
    global $passed, $failed;
    try {
        $fn();
        echo "\033[32m  ✓ PASS\033[0m  $name\n";
        $passed++;
    } catch (Exception $e) {
        echo "\033[31m  ✗ FAIL\033[0m  $name\n";
        echo "         → " . $e->getMessage() . "\n";
        $failed++;
    }
}

function assertTrue(bool $condition, string $message): void {
    if (!$condition) throw new Exception($message);
}

function assertEquals($expected, $actual, string $message): void {
    if ($expected != $actual) {
        throw new Exception("$message | Expected: $expected, Actual: $actual");
    }
}

function assertNotNull($value, string $message): void {
    if ($value === null) throw new Exception($message);
}

// ════════════════════════════════════════════════════════════
//  HEADER
// ════════════════════════════════════════════════════════════
echo "\n";
echo "\033[1m╔══════════════════════════════════════════════════════╗\033[0m\n";
echo "\033[1m║  PHP UNIT TEST - Proxy/Bridge cURL (BookStore)       ║\033[0m\n";
echo "\033[1m╚══════════════════════════════════════════════════════╝\033[0m\n";

// ════════════════════════════════════════════════════════════
//  BAGIAN D: Unit Test — Fungsi cURL (Proxy/Bridge PHP→Java)
// ════════════════════════════════════════════════════════════
echo "\n\033[1m\033[33m▶ FASE 2 NO. 1 — PHP UNIT TESTING\033[0m\n";
echo "  " . str_repeat("─", 52) . "\n";

// ── Kelompok D: cURL Connection & Request ────────────────────
echo "\n  \033[1mD. Fungsi Proxy/Bridge cURL (index.php → Java)\033[0m\n";

// D1: cURL extension tersedia
testCase("D1: Extension cURL tersedia di PHP", function() {
    assertTrue(
        function_exists('curl_init'),
        "Fungsi curl_init() tidak ditemukan. Pastikan extension cURL aktif."
    );
});

// D2: cURL berhasil konek ke Java backend
testCase("D2: cURL berhasil terhubung ke Java backend (port 8080)", function() {
    $result = sendThroughCurl('B001', 1);
    assertTrue(
        empty($result['error']),
        "cURL error: " . $result['error'] . " — Pastikan BookStoreServer.java berjalan!"
    );
});

// D3: HTTP status 200 diterima
testCase("D3: Java backend mengembalikan HTTP 200", function() {
    $result = sendThroughCurl('B001', 1);
    assertEquals(200, $result['http_code'],
        "HTTP status tidak sesuai"
    );
});

// D4: Response tidak kosong
testCase("D4: Response dari Java tidak kosong", function() {
    $result = sendThroughCurl('B001', 1);
    assertTrue(
        !empty($result['response']),
        "Response body kosong dari Java backend"
    );
});

// D5: Response adalah JSON valid
testCase("D5: Response dapat di-decode sebagai JSON", function() {
    $result = sendThroughCurl('B001', 1);
    assertNotNull(
        $result['decoded'],
        "json_decode() gagal: response bukan JSON valid. Raw: " . $result['response']
    );
});

// D6: Response mengandung field 'total'
testCase("D6: Response JSON memiliki field 'total'", function() {
    $result = sendThroughCurl('B001', 1);
    assertTrue(
        isset($result['decoded']['total']),
        "Field 'total' tidak ada dalam response JSON: " . $result['response']
    );
});

// ── Kelompok E: Validasi Nilai Kalkulasi via cURL ────────────
echo "\n  \033[1mE. Validasi Hasil Kalkulasi Melalui cURL\033[0m\n";

// E1: B001 qty=1 → 150000 (tanpa diskon)
testCase("E1: B001, qty=1 → total=150000 via cURL (diskon 0%)", function() {
    $result = sendThroughCurl('B001', 1);
    $total  = $result['decoded']['total'] ?? null;
    assertNotNull($total, "Field total tidak ditemukan");
    assertEquals(150000, $total, "Total tidak sesuai untuk B001 qty=1");
});

// E2: B002 qty=10 → 1080000 (diskon 10%)
testCase("E2: B002, qty=10 → total=1080000 via cURL (diskon 10%)", function() {
    $result = sendThroughCurl('B002', 10);
    $total  = $result['decoded']['total'] ?? null;
    assertNotNull($total, "Field total tidak ditemukan");
    assertEquals(1080000, $total, "Total tidak sesuai untuk B002 qty=10");
});

// E3: B003 qty=50 → 3612500 (diskon 15%)
testCase("E3: B003, qty=50 → total=3612500 via cURL (diskon 15%)", function() {
    $result = sendThroughCurl('B003', 50);
    $total  = $result['decoded']['total'] ?? null;
    assertNotNull($total, "Field total tidak ditemukan");
    assertEquals(3612500, $total, "Total tidak sesuai untuk B003 qty=50");
});

// E4: B001 qty=100 → 10500000 (diskon 30%)
testCase("E4: B001, qty=100 → total=10500000 via cURL (diskon 30%)", function() {
    $result = sendThroughCurl('B001', 100);
    $total  = $result['decoded']['total'] ?? null;
    assertNotNull($total, "Field total tidak ditemukan");
    assertEquals(10500000, $total, "Total tidak sesuai untuk B001 qty=100");
});

// E5: quantity 0 → total 0
testCase("E5: Quantity=0 → total=0 via cURL (input tidak valid)", function() {
    $result = sendThroughCurl('B001', 0);
    $total  = $result['decoded']['total'] ?? null;
    assertNotNull($total, "Field total tidak ditemukan");
    assertEquals(0, $total, "Quantity 0 harus menghasilkan total 0");
});

// ── Kelompok F: Validasi Payload JSON yang Dikirim ───────────
echo "\n  \033[1mF. Validasi Pembentukan Payload JSON\033[0m\n";

// F1: json_encode membentuk payload dengan benar
testCase("F1: json_encode() membentuk payload product_id dengan benar", function() {
    $payload = json_encode(["product_id" => "B001", "quantity" => 5]);
    assertTrue(
        str_contains($payload, '"product_id":"B001"'),
        "Payload tidak mengandung product_id yang benar: $payload"
    );
});

// F2: json_encode quantity sebagai integer
testCase("F2: json_encode() memastikan quantity bertipe integer", function() {
    $payload = json_encode(["product_id" => "B001", "quantity" => (int)"10"]);
    assertTrue(
        str_contains($payload, '"quantity":10'),
        "Quantity harus integer bukan string: $payload"
    );
});

// F3: products.json dapat dibaca oleh PHP
testCase("F3: file_get_contents() berhasil membaca products.json", function() {
    $content = file_get_contents(PRODUCTS_FILE);
    assertTrue(
        $content !== false && !empty($content),
        "file_get_contents() gagal membaca products.json"
    );
});

// F4: products.json bisa di-decode oleh PHP
testCase("F4: json_decode() berhasil mem-parsing products.json", function() {
    $content  = file_get_contents(PRODUCTS_FILE);
    $products = json_decode($content, true);
    assertTrue(
        is_array($products) && count($products) > 0,
        "json_decode() gagal atau array kosong pada products.json"
    );
});

// F5: Setiap produk memiliki field id, title, price, stock
testCase("F5: Setiap produk di products.json memiliki field wajib", function() {
    $products = json_decode(file_get_contents(PRODUCTS_FILE), true);
    foreach ($products as $p) {
        foreach (['id', 'title', 'price', 'stock'] as $field) {
            assertTrue(
                isset($p[$field]),
                "Produk {$p['id']} tidak memiliki field '$field'"
            );
        }
    }
});

// ════════════════════════════════════════════════════════════
//  RINGKASAN
// ════════════════════════════════════════════════════════════
$total = $passed + $failed;
echo "\n\033[1m" . str_repeat("═", 54) . "\033[0m\n";
echo "\033[1m  HASIL: $passed/$total tes lulus\033[0m\n";
if ($failed === 0) {
    echo "\033[32m\033[1m  ✓ SEMUA TES BERHASIL!\033[0m\n";
} else {
    echo "\033[31m\033[1m  ✗ $failed TES GAGAL\033[0m\n";
}
echo "\033[1m" . str_repeat("═", 54) . "\033[0m\n\n";