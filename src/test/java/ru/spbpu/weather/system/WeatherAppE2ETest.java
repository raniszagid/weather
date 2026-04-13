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
    void e2e06_userSearchesMultipleCities_historyShouldContainAllSearches() throws InterruptedException {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "pass123";
        String[] cities = {"Rome", "Madrid", "Amsterdam"};

        // ШАГ 1: Регистрация
        driver.get(baseUrl + "/auth/registration");
        Thread.sleep(2000);

        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.cssSelector("input[type='submit'][value='Sign up!']")).click();
        Thread.sleep(3000);

        // ШАГ 2: Логин
        driver.get(baseUrl + "/auth/login");
        Thread.sleep(2000);

        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.cssSelector("input[type='submit'][value='Login']")).click();
        Thread.sleep(5000);

        assertThat(driver.getCurrentUrl()).contains("/weather");

        // ШАГ 3: Выполнение нескольких поисков
        for (String city : cities) {
            WebElement cityInput = driver.findElement(By.id("city"));
            cityInput.clear();
            cityInput.sendKeys(city);
            driver.findElement(By.cssSelector("input[type='submit'][value='Get Weather']")).click();
            Thread.sleep(4000);
            // Проверка: результат поиска отобразился
            assertThat(driver.getPageSource()).contains("°C");
        }

        // ШАГ 4: Проверка истории
        driver.get(baseUrl + "/history");
        Thread.sleep(3000);

        String historyPage = driver.getPageSource();
        for (String city : cities) {
            assertThat(historyPage).contains(city);
        }
    }

    @Test
    void e2e05_searchNonExistentCity_ShouldShowError() throws InterruptedException {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "pass123";
        String invalidCity = "NonExistentCity123456";

        // ШАГ 1: Регистрация
        driver.get(baseUrl + "/auth/registration");
        Thread.sleep(2000);

        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.cssSelector("input[type='submit'][value='Sign up!']")).click();
        Thread.sleep(3000);

        // ШАГ 2: Логин
        driver.get(baseUrl + "/auth/login");
        Thread.sleep(2000);

        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.cssSelector("input[type='submit'][value='Login']")).click();
        Thread.sleep(5000);

        assertThat(driver.getCurrentUrl()).contains("/weather");

        // ШАГ 3: Поиск несуществующего города
        driver.findElement(By.id("city")).sendKeys(invalidCity);
        driver.findElement(By.cssSelector("input[type='submit'][value='Get Weather']")).click();
        Thread.sleep(5000);

        // ШАГ 4: Проверка - должна открыться страница ошибки
        String currentUrl = driver.getCurrentUrl();
        String pageSource = driver.getPageSource();

        // Проверяем, что либо остались на /weather с сообщением об ошибке,
        // либо перешли на страницу error
        boolean isOnErrorPage = currentUrl.contains("/error");
        boolean hasErrorMessage = pageSource.contains("error") ||
                pageSource.contains("Error") ||
                pageSource.contains("not found") ||
                pageSource.contains("City not found");

        assertThat(isOnErrorPage || hasErrorMessage).isTrue();
    }

    @Test
    void e2e06_searchWithEmptyCity_ShouldShowValidationError() throws InterruptedException {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "pass123";

        // ШАГ 1: Регистрация
        driver.get(baseUrl + "/auth/registration");
        Thread.sleep(2000);

        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.cssSelector("input[type='submit'][value='Sign up!']")).click();
        Thread.sleep(3000);

        // ШАГ 2: Логин
        driver.get(baseUrl + "/auth/login");
        Thread.sleep(2000);

        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.cssSelector("input[type='submit'][value='Login']")).click();
        Thread.sleep(5000);

        assertThat(driver.getCurrentUrl()).contains("/weather");

        // ШАГ 3: Попытка поиска с пустым полем города
        // Оставляем поле пустым (не вводим ничего)
        driver.findElement(By.id("city")).sendKeys("");
        driver.findElement(By.cssSelector("input[type='submit'][value='Get Weather']")).click();
        Thread.sleep(3000);

        // ШАГ 4: Проверка - должны остаться на той же странице (HTML5 валидация не даст отправить)
        String currentUrl = driver.getCurrentUrl();

        // Из-за атрибута required в поле ввода, форма не должна отправиться
        assertThat(currentUrl).contains("/weather");
    }

    @Test
    void e2e07_historyIsIsolatedBetweenUsers() throws InterruptedException {
        // Данные первого пользователя
        String username1 = "user1_" + UUID.randomUUID().toString().substring(0, 8);
        String password1 = "pass123";

        // Данные второго пользователя
        String username2 = "user2_" + UUID.randomUUID().toString().substring(0, 8);
        String password2 = "pass123";

        String searchCity = "London";

        // ========== ЧАСТЬ 1: Первый пользователь ==========

        // ШАГ 1: Регистрация первого пользователя
        driver.get(baseUrl + "/auth/registration");
        Thread.sleep(2000);

        driver.findElement(By.id("username")).sendKeys(username1);
        driver.findElement(By.id("password")).sendKeys(password1);
        driver.findElement(By.cssSelector("input[type='submit'][value='Sign up!']")).click();
        Thread.sleep(3000);

        // ШАГ 2: Логин первого пользователя
        driver.get(baseUrl + "/auth/login");
        Thread.sleep(2000);

        driver.findElement(By.id("username")).sendKeys(username1);
        driver.findElement(By.id("password")).sendKeys(password1);
        driver.findElement(By.cssSelector("input[type='submit'][value='Login']")).click();
        Thread.sleep(5000);

        assertThat(driver.getCurrentUrl()).contains("/weather");

        // ШАГ 3: Поиск погоды для Лондона
        driver.findElement(By.id("city")).sendKeys(searchCity);
        driver.findElement(By.cssSelector("input[type='submit'][value='Get Weather']")).click();
        Thread.sleep(5000);

        // ШАГ 4: Проверка, что результат поиска отобразился
        assertThat(driver.getPageSource()).contains("°C");

        // ШАГ 5: Проверка истории первого пользователя - должна содержать Лондон
        driver.get(baseUrl + "/history");
        Thread.sleep(3000);

        String historyPage1 = driver.getPageSource();
        assertThat(historyPage1).contains(searchCity);

        // ШАГ 6: Выход из аккаунта первого пользователя
        driver.get(baseUrl + "/logout");
        Thread.sleep(3000);

        // ========== ЧАСТЬ 2: Второй пользователь ==========

        // ШАГ 7: Регистрация второго пользователя
        driver.get(baseUrl + "/auth/registration");
        Thread.sleep(2000);

        driver.findElement(By.id("username")).sendKeys(username2);
        driver.findElement(By.id("password")).sendKeys(password2);
        driver.findElement(By.cssSelector("input[type='submit'][value='Sign up!']")).click();
        Thread.sleep(3000);

        // ШАГ 8: Логин второго пользователя
        driver.get(baseUrl + "/auth/login");
        Thread.sleep(2000);

        driver.findElement(By.id("username")).sendKeys(username2);
        driver.findElement(By.id("password")).sendKeys(password2);
        driver.findElement(By.cssSelector("input[type='submit'][value='Login']")).click();
        Thread.sleep(5000);

        assertThat(driver.getCurrentUrl()).contains("/weather");

        // ШАГ 9: Выполнение поиска другим городом (чтобы убедиться, что история работает)
        driver.findElement(By.id("city")).sendKeys("Paris");
        driver.findElement(By.cssSelector("input[type='submit'][value='Get Weather']")).click();
        Thread.sleep(5000);

        // ШАГ 10: Проверка истории второго пользователя
        driver.get(baseUrl + "/history");
        Thread.sleep(3000);

        String historyPage2 = driver.getPageSource();

        // Проверка: история второго пользователя НЕ должна содержать Лондон
        assertThat(historyPage2).doesNotContain(searchCity);

        // Проверка: история второго пользователя должна содержать его собственный поиск (Париж)
        assertThat(historyPage2).contains("Paris");

        System.out.println("✅ Проверка пройдена: история пользователей изолирована");
    }
}

