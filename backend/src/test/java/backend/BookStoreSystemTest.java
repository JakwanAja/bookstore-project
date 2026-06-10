package backend;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Select;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.*;

public class BookStoreSystemTest {

    private WebDriver driver;

    static final String DATA_DIR      = "../data/";
    static final String PRODUCTS_FILE = DATA_DIR + "products.json";
    static final String PRODUCTS_SEED = DATA_DIR + "products_system_seed.json";
    static final String ORDERS_FILE   = DATA_DIR + "orders.json";
    static final String ORDERS_SEED   = DATA_DIR + "orders_system_seed.json";

    @BeforeAll
    static void setUpData() throws Exception {
        System.out.println("[setUp] System Test: menyalin seed data...");
        Files.copy(Paths.get(PRODUCTS_FILE), Paths.get(PRODUCTS_SEED),
                   StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(Paths.get(ORDERS_FILE), "[]");
        Files.copy(Paths.get(ORDERS_FILE), Paths.get(ORDERS_SEED),
                   StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[setUp] Seed data System Test berhasil dibackup.");
    }

    @AfterAll
    static void tearDownData() throws Exception {
        System.out.println("[tearDown] System Test: mengembalikan data...");
        Files.copy(Paths.get(PRODUCTS_SEED), Paths.get(PRODUCTS_FILE),
                   StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(ORDERS_SEED), Paths.get(ORDERS_FILE),
                   StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(Paths.get(PRODUCTS_SEED));
        Files.deleteIfExists(Paths.get(ORDERS_SEED));
        System.out.println("[tearDown] Data System Test berhasil dikembalikan.");
    }

    @BeforeEach
    void setup() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");         // wajib untuk CI
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        driver = new ChromeDriver(options);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) driver.quit();
    }

    @Test
    void testCheckoutFlow() {
        driver.get("http://localhost:8000");

        Select bookSelect = new Select(driver.findElement(By.id("product_id")));
        bookSelect.selectByIndex(0);

        driver.findElement(By.id("quantity")).sendKeys("2");
        driver.findElement(By.id("address")).sendKeys("Madiun, Jawa Timur");
        driver.findElement(By.id("submitBtn")).click();

        WebElement result = driver.findElement(By.id("orderSummary"));
        assertTrue(result.isDisplayed());
        assertTrue(result.getText().contains("Pesanan Berhasil Diproses"));
    }
}