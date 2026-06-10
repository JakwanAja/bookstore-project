package backend;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.Select;

import static org.junit.jupiter.api.Assertions.*;

public class BookStoreSystemTest {

    private WebDriver driver;

    @BeforeEach
    void setup() {
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
    }

    @AfterEach
    void tearDown() {
        if(driver != null){
            driver.quit();
        }
    }

    @Test
    void testCheckoutFlow() {

        driver.get("http://localhost:8000");

        Select bookSelect =
                new Select(driver.findElement(By.id("product_id")));

        bookSelect.selectByIndex(0);

        driver.findElement(By.id("quantity"))
                .sendKeys("2");

        driver.findElement(By.id("address"))
                .sendKeys("Madiun, Jawa Timur");

        driver.findElement(By.id("submitBtn"))
                .click();

        WebElement result =
                driver.findElement(By.id("orderSummary"));

        assertTrue(result.isDisplayed());

        String text = result.getText();

        assertTrue(
            text.contains("Pesanan Berhasil Diproses")
        );
    }
}
