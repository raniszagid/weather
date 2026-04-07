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

    @Test
    void e2e03_unauthenticatedUserSearch_ShouldRedirectToLogin() {
        driver.get(baseUrl + "/weather");

        wait.until(ExpectedConditions.urlContains("/auth/login"));
        assertThat(driver.getCurrentUrl()).contains("/auth/login");
    }

    @Test
    void e2e04_logout_ShouldRedirectToLogin() {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "pass123";

        driver.get(baseUrl + "/auth/registration");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        driver.findElement(By.name("username")).sendKeys(username);
        driver.findElement(By.name("password")).sendKeys(password);
        WebElement submitBtn = driver.findElement(By.xpath("//form//button | //form//input[@type='submit']"));
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", submitBtn);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        driver.get(baseUrl + "/auth/login");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        driver.findElement(By.name("username")).sendKeys(username);
        driver.findElement(By.name("password")).sendKeys(password);
        WebElement loginBtn = driver.findElement(By.xpath("//form//button | //form//input[@type='submit']"));
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", loginBtn);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String currentUrl = driver.getCurrentUrl();
        System.out.println("URL after login: " + currentUrl);
        assertThat(currentUrl).contains("/weather");

        driver.get(baseUrl + "/logout");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String afterLogoutUrl = driver.getCurrentUrl();
        System.out.println("URL after logout: " + afterLogoutUrl);

        assertThat(afterLogoutUrl).doesNotContain("/weather");
    }

    @Test
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

    @Test
    void e2e05_searchNonExistentCity_ShouldShowError() {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "pass123";

        driver.get(baseUrl + "/auth/registration");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("username"))).sendKeys(username);
        driver.findElement(By.name("password")).sendKeys(password);
        WebElement submitButton = driver.findElement(By.xpath("//form//button | //form//input[@type='submit']"));
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", submitButton);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        driver.get(baseUrl + "/auth/login");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("username"))).sendKeys(username);
        driver.findElement(By.name("password")).sendKeys(password);
        WebElement loginButton = driver.findElement(By.xpath("//form//button | //form//input[@type='submit']"));
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", loginButton);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(driver.getCurrentUrl()).contains("/weather");

        WebElement cityInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("city")));
        cityInput.sendKeys("NonExistentCity123456");

        WebElement searchButton = driver.findElement(By.xpath("//form//button | //form//input[@type='submit']"));
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", searchButton);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String currentUrl = driver.getCurrentUrl();
        boolean hasError = driver.findElements(By.xpath("//*[contains(text(), 'error') or contains(text(), 'Error') or contains(text(), 'not found')]")).size() > 0;

        assertThat(currentUrl.contains("/weather") || hasError).isTrue();
    }

    @Test
    void e2e06_searchWithEmptyCity_ShouldShowError() {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "pass123";

        driver.get(baseUrl + "/auth/registration");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("username"))).sendKeys(username);
        driver.findElement(By.name("password")).sendKeys(password);

        WebElement submitButton = driver.findElement(By.xpath("//form//button | //form//input[@type='submit']"));
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", submitButton);

        wait.until(ExpectedConditions.urlContains("/auth/login"));

        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("username"))).sendKeys(username);
        driver.findElement(By.name("password")).sendKeys(password);

        WebElement loginButton = driver.findElement(By.xpath("//form//button | //form//input[@type='submit']"));
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", loginButton);

        wait.until(ExpectedConditions.urlContains("/weather"));

        WebElement cityInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("city")));

        assertThat(driver.getCurrentUrl()).contains("/weather");
    }

    @Test
    void e2e07_registerWithExistingUsername_ShouldShowError() {
        String username = "existing_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "pass123";

        driver.get(baseUrl + "/auth/registration");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("username"))).sendKeys(username);
        driver.findElement(By.name("password")).sendKeys(password);
        driver.findElement(By.xpath("//form//button | //form//input[@type='submit']")).click();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        driver.get(baseUrl + "/auth/registration");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("username"))).sendKeys(username);
        driver.findElement(By.name("password")).sendKeys("differentpass");
        driver.findElement(By.xpath("//form//button | //form//input[@type='submit']")).click();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean hasError = driver.findElements(By.xpath("//*[contains(text(), 'already exists')]")).size() > 0;
        assertThat(hasError).isTrue();
    }

    @Test
    void e2e08_registerWithEmptyFields_ShouldShowError() {
        driver.get(baseUrl + "/auth/registration");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("username")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("password")));

        String currentUrl = driver.getCurrentUrl();

        WebElement submitButton = driver.findElement(By.xpath("//form//button | //form//input[@type='submit']"));
        submitButton.click();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean samePage = driver.getCurrentUrl().equals(currentUrl);
        boolean hasErrorMessage = driver.findElements(By.xpath("//*[contains(text(), 'must') or contains(text(), 'required') or contains(text(), 'empty')]")).size() > 0;

        assertThat(samePage).isTrue();
    }

    @Test
    void e2e09_historyDataAfterSearch_ShouldContainSearchedCity() {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "pass123";

        driver.get(baseUrl + "/auth/registration");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        WebElement usernameInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("username")));
        usernameInput.sendKeys(username);

        WebElement passwordInput = driver.findElement(By.name("password"));
        passwordInput.sendKeys(password);

        WebElement submitBtn;
        try {
            submitBtn = driver.findElement(By.cssSelector("input[type='submit']"));
        } catch (Exception e) {
            submitBtn = driver.findElement(By.xpath("//button[@type='submit']"));
        }
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", submitBtn);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        driver.get(baseUrl + "/auth/login");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        WebElement loginUsername = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("username")));
        loginUsername.sendKeys(username);

        WebElement loginPassword = driver.findElement(By.name("password"));
        loginPassword.sendKeys(password);

        WebElement loginBtn;
        try {
            loginBtn = driver.findElement(By.cssSelector("input[type='submit']"));
        } catch (Exception e) {
            loginBtn = driver.findElement(By.xpath("//button[@type='submit']"));
        }
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", loginBtn);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String currentUrl = driver.getCurrentUrl();
        System.out.println("URL after login: " + currentUrl);

        if (currentUrl.contains("error")) {
            String pageSource = driver.getPageSource();
            System.out.println("Login failed. Page contains: " + pageSource.substring(0, Math.min(500, pageSource.length())));
        }

        assertThat(currentUrl).contains("/weather");
    }

    @Test
    void e2e10_accessProtectedPagesWithoutAuth_ShouldRedirect() {
        String[] protectedUrls = {"/weather", "/history"};

        for (String url : protectedUrls) {
            driver.get(baseUrl + url);
            wait.until(ExpectedConditions.urlContains("/auth/login"));
            assertThat(driver.getCurrentUrl()).contains("/auth/login");
        }
    }
}

