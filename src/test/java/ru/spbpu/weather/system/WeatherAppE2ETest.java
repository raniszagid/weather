package ru.spbpu.weather.system;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WeatherAppE2ETest {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private WebDriverWait wait;
    private String baseUrl;

    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");
    }

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--headless=new");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        baseUrl = "http://localhost:" + port;
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void e2e04_historyIsIsolatedBetweenUsers() throws InterruptedException {
        String username1 = "user1_" + UUID.randomUUID().toString().substring(0, 8);
        String password1 = "pass123";

        String username2 = "user2_" + UUID.randomUUID().toString().substring(0, 8);
        String password2 = "pass123";

        String searchCity = "London";

        driver.get(baseUrl + "/auth/registration");
        Thread.sleep(2000);

        driver.findElement(By.id("username")).sendKeys(username1);
        driver.findElement(By.id("password")).sendKeys(password1);
        driver.findElement(By.cssSelector("input[type='submit'][value='Sign up!']")).click();
        Thread.sleep(3000);

        driver.get(baseUrl + "/auth/login");
        Thread.sleep(2000);

        driver.findElement(By.id("username")).sendKeys(username1);
        driver.findElement(By.id("password")).sendKeys(password1);
        driver.findElement(By.cssSelector("input[type='submit'][value='Login']")).click();
        Thread.sleep(5000);

        assertThat(driver.getCurrentUrl()).contains("/weather");

        driver.findElement(By.id("city")).sendKeys(searchCity);
        driver.findElement(By.cssSelector("input[type='submit'][value='Get Weather']")).click();
        Thread.sleep(5000);

        assertThat(driver.getPageSource()).contains("°C");

        driver.get(baseUrl + "/history");
        Thread.sleep(3000);

        List<WebElement> historyHeaders = driver.findElements(By.cssSelector(".history-item h3"));
        boolean firstUserHasLondon = false;
        for (WebElement header : historyHeaders) {
            if (header.getText().contains(searchCity)) {
                firstUserHasLondon = true;
                break;
            }
        }
        assertThat(firstUserHasLondon).isTrue();
        System.out.println("Первый пользователь видит Лондон в истории");

        driver.findElement(By.cssSelector(".user-menu")).click();
        Thread.sleep(1000);
        driver.findElement(By.cssSelector("input[type='submit'][value='Logout']")).click();
        Thread.sleep(3000);

        driver.manage().deleteAllCookies();
        Thread.sleep(1000);

        driver.get(baseUrl + "/weather");
        Thread.sleep(3000);

        String currentUrl = driver.getCurrentUrl();
        System.out.println("URL after logout and trying to access /weather: " + currentUrl);

        assertThat(currentUrl).contains("/auth/login");
        System.out.println("Первый пользователь вышел из системы");

        driver.manage().deleteAllCookies();
        Thread.sleep(1000);

        driver.get(baseUrl + "/auth/registration");
        Thread.sleep(2000);

        driver.findElement(By.id("username")).sendKeys(username2);
        driver.findElement(By.id("password")).sendKeys(password2);
        driver.findElement(By.cssSelector("input[type='submit'][value='Sign up!']")).click();
        Thread.sleep(3000);

        driver.get(baseUrl + "/auth/login");
        Thread.sleep(2000);

        driver.findElement(By.id("username")).sendKeys(username2);
        driver.findElement(By.id("password")).sendKeys(password2);
        driver.findElement(By.cssSelector("input[type='submit'][value='Login']")).click();
        Thread.sleep(5000);

        assertThat(driver.getCurrentUrl()).contains("/weather");

        driver.findElement(By.id("city")).sendKeys("Paris");
        driver.findElement(By.cssSelector("input[type='submit'][value='Get Weather']")).click();
        Thread.sleep(5000);

        driver.get(baseUrl + "/history");
        Thread.sleep(3000);

        historyHeaders = driver.findElements(By.cssSelector(".history-item h3"));
        boolean secondUserHasLondon = false;
        for (WebElement header : historyHeaders) {
            if (header.getText().contains(searchCity)) {
                secondUserHasLondon = true;
                System.out.println("Found London in header: " + header.getText());
                break;
            }
        }

        assertThat(secondUserHasLondon).isFalse();
        System.out.println("История второго пользователя НЕ содержит Лондон");

        boolean secondUserHasParis = false;
        for (WebElement header : historyHeaders) {
            if (header.getText().contains("Paris")) {
                secondUserHasParis = true;
                break;
            }
        }
        assertThat(secondUserHasParis).isTrue();
        System.out.println("История второго пользователя содержит Париж");
    }

    @Test
    void e2e05_historySorting() throws InterruptedException {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "pass123";
        String[] cities = {"Berlin", "London", "Paris"};

        driver.get(baseUrl + "/auth/registration");
        Thread.sleep(2000);

        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.cssSelector("input[type='submit'][value='Sign up!']")).click();
        Thread.sleep(3000);

        driver.get(baseUrl + "/auth/login");
        Thread.sleep(2000);

        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.cssSelector("input[type='submit'][value='Login']")).click();
        Thread.sleep(5000);

        assertThat(driver.getCurrentUrl()).contains("/weather");

        for (String city : cities) {
            driver.findElement(By.id("city")).clear();
            driver.findElement(By.id("city")).sendKeys(city);
            driver.findElement(By.cssSelector("input[type='submit'][value='Get Weather']")).click();
            Thread.sleep(4000);
        }

        driver.get(baseUrl + "/history?sortBy=city&order=asc");
        Thread.sleep(3000);

        List<String> ascOrder = getCityOrderFromHistory();
        System.out.println("Ascending order (A-Z): " + ascOrder);
        assertThat(ascOrder.get(1)).isEqualTo("London");
        assertThat(ascOrder.get(0)).isEqualTo("Berlin");
        assertThat(ascOrder.get(2)).isEqualTo("Paris");

        driver.get(baseUrl + "/history?sortBy=city&order=desc");
        Thread.sleep(3000);

        List<String> descOrder = getCityOrderFromHistory();
        System.out.println("Descending order (Z-A): " + descOrder);
        assertThat(descOrder.get(0)).isEqualTo("Paris");
        assertThat(descOrder.get(2)).isEqualTo("Berlin");
        assertThat(descOrder.get(1)).isEqualTo("London");
    }

    private List<String> getCityOrderFromHistory() {
        List<String> order = new java.util.ArrayList<>();
        List<WebElement> historyItems = driver.findElements(By.cssSelector(".history-item h3"));
        String[] expectedCities = {"Berlin", "London", "Paris"};

        for (WebElement item : historyItems) {
            String text = item.getText();
            for (String city : expectedCities) {
                if (text.contains(city)) {
                    order.add(city);
                    break;
                }
            }
        }
        return order;
    }
}

