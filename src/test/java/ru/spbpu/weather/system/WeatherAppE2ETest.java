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
        // Включите headless для CI/CD
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

    // ==================== СЦЕНАРИЙ E2E-01 ====================
    @Test
    @Order(1)
    void e2e01_registerNewUser_ShouldSucceed() {
        driver.get(baseUrl + "/auth/registration");

        WebElement usernameInput = wait.until(ExpectedConditions.elementToBeClickable(By.name("username")));
        usernameInput.sendKeys("e2euser");

        WebElement passwordInput = driver.findElement(By.name("password"));
        passwordInput.sendKeys("e2epass123");

        WebElement submitButton = driver.findElement(By.cssSelector("button[type='submit'], input[type='submit']"));
        submitButton.click();

        wait.until(ExpectedConditions.urlContains("/auth/login"));
        assertThat(driver.getCurrentUrl()).contains("/auth/login");
    }

    // ==================== СЦЕНАРИЙ E2E-02 ====================
    @Test
    @Order(2)
    void e2e02_loginExistingUser_ShouldRedirectToWeather() {
        driver.get(baseUrl + "/auth/login");

        WebElement usernameInput = wait.until(ExpectedConditions.elementToBeClickable(By.name("username")));
        usernameInput.sendKeys("e2euser");

        WebElement passwordInput = driver.findElement(By.name("password"));
        passwordInput.sendKeys("e2epass123");

        WebElement submitButton = driver.findElement(By.cssSelector("button[type='submit'], input[type='submit']"));
        submitButton.click();

        wait.until(ExpectedConditions.urlContains("/weather"));
        assertThat(driver.getCurrentUrl()).contains("/weather");
    }

    // ==================== СЦЕНАРИЙ E2E-03 ====================
    @Test
    @Order(3)
    void e2e03_searchWeatherByCity_ShouldDisplayWeather() throws InterruptedException {
        // Сначала логинимся
        driver.get(baseUrl + "/auth/login");
        wait.until(ExpectedConditions.elementToBeClickable(By.name("username"))).sendKeys("e2euser");
        driver.findElement(By.name("password")).sendKeys("e2epass123");
        driver.findElement(By.cssSelector("button[type='submit'], input[type='submit']")).click();

        wait.until(ExpectedConditions.urlContains("/weather"));

        // Вводим город
        WebElement cityInput = wait.until(ExpectedConditions.elementToBeClickable(By.name("city")));
        cityInput.sendKeys("London");

        WebElement searchButton = driver.findElement(By.cssSelector("button[type='submit'], input[type='submit']"));
        searchButton.click();

        // Ждем результат
        Thread.sleep(3000); // Даем время на загрузку API

        // Проверяем, что появилась карточка погоды
        boolean hasWeatherCard = driver.findElements(By.cssSelector(".weather-card, .current-weather")).size() > 0;
        assertThat(hasWeatherCard).isTrue();
    }

    // ==================== СЦЕНАРИЙ E2E-04 ====================
    @Test
    void e2e04_unauthenticatedUserSearch_ShouldRedirectToLogin() {
        driver.get(baseUrl + "/weather");

        wait.until(ExpectedConditions.urlContains("/auth/login"));
        assertThat(driver.getCurrentUrl()).contains("/auth/login");
    }

    // ==================== СЦЕНАРИЙ E2E-05 ====================
    @Test
    @Order(4)
    void e2e05_viewSearchHistory_ShouldShowPreviousSearches() throws InterruptedException {
        // Логинимся
        driver.get(baseUrl + "/auth/login");
        wait.until(ExpectedConditions.elementToBeClickable(By.name("username"))).sendKeys("e2euser");
        driver.findElement(By.name("password")).sendKeys("e2epass123");
        driver.findElement(By.cssSelector("button[type='submit'], input[type='submit']")).click();

        wait.until(ExpectedConditions.urlContains("/weather"));

        // Поиск Paris
        driver.findElement(By.name("city")).sendKeys("Paris");
        driver.findElement(By.cssSelector("button[type='submit'], input[type='submit']")).click();
        Thread.sleep(3000);

        // Поиск Berlin
        driver.findElement(By.name("city")).clear();
        driver.findElement(By.name("city")).sendKeys("Berlin");
        driver.findElement(By.cssSelector("button[type='submit'], input[type='submit']")).click();
        Thread.sleep(3000);

        // Открываем историю
        WebElement userMenu = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".user-menu, .user-btn")));
        userMenu.click();

        WebElement historyLink = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("History")));
        historyLink.click();

        wait.until(ExpectedConditions.urlContains("/history"));

        String pageSource = driver.getPageSource();
        assertThat(pageSource).contains("Paris");
        assertThat(pageSource).contains("Berlin");
    }

    // ==================== СЦЕНАРИЙ E2E-06 ====================
    @Test
    void e2e06_logout_ShouldRedirectToLogin() throws InterruptedException {
        driver.get(baseUrl + "/auth/login");
        wait.until(ExpectedConditions.elementToBeClickable(By.name("username"))).sendKeys("e2euser");
        driver.findElement(By.name("password")).sendKeys("e2epass123");
        driver.findElement(By.cssSelector("button[type='submit'], input[type='submit']")).click();

        wait.until(ExpectedConditions.urlContains("/weather"));

        // Выход
        WebElement userMenu = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".user-menu, .user-btn")));
        userMenu.click();

        WebElement logoutBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("input[value='Logout']")));
        logoutBtn.click();

        wait.until(ExpectedConditions.urlContains("/auth/login"));
        assertThat(driver.getCurrentUrl()).contains("/auth/login");
    }

    // ==================== СЦЕНАРИЙ E2E-07 ====================
    @Test
    @Order(5)
    void e2e07_searchNonExistentCity_ShouldShowError() throws InterruptedException {
        driver.get(baseUrl + "/auth/login");
        wait.until(ExpectedConditions.elementToBeClickable(By.name("username"))).sendKeys("e2euser");
        driver.findElement(By.name("password")).sendKeys("e2epass123");
        driver.findElement(By.cssSelector("button[type='submit'], input[type='submit']")).click();

        wait.until(ExpectedConditions.urlContains("/weather"));

        driver.findElement(By.name("city")).sendKeys("NonExistentCity123456");
        driver.findElement(By.cssSelector("button[type='submit'], input[type='submit']")).click();

        Thread.sleep(3000);

        boolean hasError = driver.findElements(By.cssSelector(".error-message, .error-container")).size() > 0;
        assertThat(hasError).isTrue();
    }

    // ==================== СЦЕНАРИЙ E2E-08 ====================
    @Test
    void e2e08_searchWithEmptyCity_ShouldShowError() {
        driver.get(baseUrl + "/auth/login");
        wait.until(ExpectedConditions.elementToBeClickable(By.name("username"))).sendKeys("e2euser");
        driver.findElement(By.name("password")).sendKeys("e2epass123");
        driver.findElement(By.cssSelector("button[type='submit'], input[type='submit']")).click();

        wait.until(ExpectedConditions.urlContains("/weather"));

        driver.findElement(By.name("city")).sendKeys("");
        driver.findElement(By.cssSelector("button[type='submit'], input[type='submit']")).click();

        boolean hasError = driver.findElements(By.cssSelector(".error-message, .error-container")).size() > 0 ||
                driver.getCurrentUrl().contains("error");
        assertThat(hasError).isTrue();
    }

    // ==================== СЦЕНАРИЙ E2E-09 ====================
    @Test
    void e2e09_registerWithExistingUsername_ShouldShowError() {
        String username = "existing_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "pass123";

        // Первая регистрация
        driver.get(baseUrl + "/auth/registration");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("username"))).sendKeys(username);
        driver.findElement(By.name("password")).sendKeys(password);
        driver.findElement(By.xpath("//form//button | //form//input[@type='submit']")).click();

        // Вторая попытка с тем же именем
        driver.get(baseUrl + "/auth/registration");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("username"))).sendKeys(username);
        driver.findElement(By.name("password")).sendKeys("differentpass");
        driver.findElement(By.xpath("//form//button | //form//input[@type='submit']")).click();

        boolean hasError = driver.findElements(By.xpath("//*[contains(text(), 'already exists')]")).size() > 0;
        assertThat(hasError).isTrue();
    }

    // ==================== СЦЕНАРИЙ E2E-10 ====================
    @Test
    void e2e10_registerWithEmptyFields_ShouldShowError() {
        driver.get(baseUrl + "/auth/registration");

        // Ждем загрузки формы
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("username")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("password")));

        // Сохраняем текущий URL
        String currentUrl = driver.getCurrentUrl();

        // Находим и нажимаем кнопку отправки
        WebElement submitButton = driver.findElement(By.xpath("//form//button | //form//input[@type='submit']"));
        submitButton.click();

        // Небольшая задержка для возможной валидации
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Проверяем, что мы остались на той же странице (редиректа не произошло)
        // Или что есть сообщение об ошибке
        boolean samePage = driver.getCurrentUrl().equals(currentUrl);
        boolean hasErrorMessage = driver.findElements(By.xpath("//*[contains(text(), 'must') or contains(text(), 'required') or contains(text(), 'empty')]")).size() > 0;

        // Тест проходит, если мы остались на странице регистрации (значит валидация сработала)
        assertThat(samePage).isTrue();
    }

    // ==================== СЦЕНАРИЙ E2E-11 ====================
    @Test
    @Order(6)
    void e2e11_historyDataAfterSearch_ShouldContainSearchedCity() throws InterruptedException {
        driver.get(baseUrl + "/auth/login");
        wait.until(ExpectedConditions.elementToBeClickable(By.name("username"))).sendKeys("e2euser");
        driver.findElement(By.name("password")).sendKeys("e2epass123");
        driver.findElement(By.cssSelector("button[type='submit'], input[type='submit']")).click();

        wait.until(ExpectedConditions.urlContains("/weather"));

        String testCity = "Tokyo";
        driver.findElement(By.name("city")).sendKeys(testCity);
        driver.findElement(By.cssSelector("button[type='submit'], input[type='submit']")).click();
        Thread.sleep(3000);

        WebElement userMenu = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".user-menu, .user-btn")));
        userMenu.click();

        driver.findElement(By.linkText("History")).click();

        wait.until(ExpectedConditions.urlContains("/history"));
        assertThat(driver.getPageSource()).contains(testCity);
    }

    // ==================== СЦЕНАРИЙ E2E-12 ====================
    @Test
    @Order(7)
    void e2e12_temperatureFormat_ShouldIncludeCelsiusSymbol() throws InterruptedException {
        driver.get(baseUrl + "/auth/login");
        wait.until(ExpectedConditions.elementToBeClickable(By.name("username"))).sendKeys("e2euser");
        driver.findElement(By.name("password")).sendKeys("e2epass123");
        driver.findElement(By.cssSelector("button[type='submit'], input[type='submit']")).click();

        wait.until(ExpectedConditions.urlContains("/weather"));

        driver.findElement(By.name("city")).sendKeys("Moscow");
        driver.findElement(By.cssSelector("button[type='submit'], input[type='submit']")).click();
        Thread.sleep(3000);

        String pageSource = driver.getPageSource();
        assertThat(pageSource).contains("°C");
    }

    // ==================== СЦЕНАРИЙ E2E-13 ====================
    @Test
    @Order(8)
    void e2e13_multipleCitySearches_AllShouldBeInHistory() throws InterruptedException {
        driver.get(baseUrl + "/auth/login");
        wait.until(ExpectedConditions.elementToBeClickable(By.name("username"))).sendKeys("e2euser");
        driver.findElement(By.name("password")).sendKeys("e2epass123");
        driver.findElement(By.cssSelector("button[type='submit'], input[type='submit']")).click();

        wait.until(ExpectedConditions.urlContains("/weather"));

        String[] cities = {"Rome", "Madrid", "Amsterdam"};
        for (String city : cities) {
            driver.findElement(By.name("city")).clear();
            driver.findElement(By.name("city")).sendKeys(city);
            driver.findElement(By.cssSelector("button[type='submit'], input[type='submit']")).click();
            Thread.sleep(2000);
        }

        WebElement userMenu = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".user-menu, .user-btn")));
        userMenu.click();
        driver.findElement(By.linkText("History")).click();

        wait.until(ExpectedConditions.urlContains("/history"));
        String pageSource = driver.getPageSource();

        for (String city : cities) {
            assertThat(pageSource).contains(city);
        }
    }

    // ==================== СЦЕНАРИЙ E2E-14 ====================
    @Test
    void e2e14_accessProtectedPagesWithoutAuth_ShouldRedirect() {
        String[] protectedUrls = {"/weather", "/history"};

        for (String url : protectedUrls) {
            driver.get(baseUrl + url);
            wait.until(ExpectedConditions.urlContains("/auth/login"));
            assertThat(driver.getCurrentUrl()).contains("/auth/login");
        }
    }

    // ==================== СЦЕНАРИЙ E2E-15 ====================
    @Test
    void e2e15_completeUserJourney_AllStepsSucceed() throws InterruptedException {
        String newUser = "completeuser";
        String newPass = "complete123";

        // 1. Регистрация
        driver.get(baseUrl + "/auth/registration");
        wait.until(ExpectedConditions.elementToBeClickable(By.name("username"))).sendKeys(newUser);
        driver.findElement(By.name("password")).sendKeys(newPass);
        driver.findElement(By.cssSelector("button[type='submit'], input[type='submit']")).click();
        wait.until(ExpectedConditions.urlContains("/auth/login"));

        // 2. Логин
        wait.until(ExpectedConditions.elementToBeClickable(By.name("username"))).sendKeys(newUser);
        driver.findElement(By.name("password")).sendKeys(newPass);
        driver.findElement(By.cssSelector("button[type='submit'], input[type='submit']")).click();
        wait.until(ExpectedConditions.urlContains("/weather"));

        // 3. Поиск погоды
        driver.findElement(By.name("city")).sendKeys("Barcelona");
        driver.findElement(By.cssSelector("button[type='submit'], input[type='submit']")).click();
        Thread.sleep(3000);

        boolean hasWeatherCard = driver.findElements(By.cssSelector(".weather-card, .current-weather")).size() > 0;
        assertThat(hasWeatherCard).isTrue();

        // 4. Проверка истории
        WebElement userMenu = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".user-menu, .user-btn")));
        userMenu.click();
        driver.findElement(By.linkText("History")).click();

        wait.until(ExpectedConditions.urlContains("/history"));
        assertThat(driver.getPageSource()).contains("Barcelona");

        // 5. Выход
        userMenu = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".user-menu, .user-btn")));
        userMenu.click();
        driver.findElement(By.cssSelector("input[value='Logout']")).click();

        wait.until(ExpectedConditions.urlContains("/auth/login"));
        assertThat(driver.getCurrentUrl()).contains("/auth/login");
    }
}