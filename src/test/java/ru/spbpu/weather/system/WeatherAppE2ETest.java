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
    void e2e01_baseSuccessfulScenario() throws InterruptedException {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "pass123";
        String[] cities = {"Paris", "Madrid", "London"};

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
            WebElement cityInput = driver.findElement(By.id("city"));
            cityInput.clear();
            cityInput.sendKeys(city);
            driver.findElement(By.cssSelector("input[type='submit'][value='Get Weather']")).click();
            Thread.sleep(4000);
            assertThat(driver.getPageSource()).contains("°C");
        }

        driver.get(baseUrl + "/history");
        Thread.sleep(3000);

        String historyPage = driver.getPageSource();
        for (String city : cities) {
            assertThat(historyPage).contains(city);
        }
    }

    @Test
    void e2e02_searchNonExistentCity_ShouldShowError() throws InterruptedException {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "pass123";
        String invalidCity = "NonExistentCity123456";

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

        driver.findElement(By.id("city")).sendKeys(invalidCity);
        driver.findElement(By.cssSelector("input[type='submit'][value='Get Weather']")).click();
        Thread.sleep(5000);

        String currentUrl = driver.getCurrentUrl();
        String pageSource = driver.getPageSource();

        boolean isOnErrorPage = currentUrl.contains("/error");
        boolean hasErrorMessage = pageSource.contains("error") ||
                pageSource.contains("Error") ||
                pageSource.contains("not found") ||
                pageSource.contains("City not found");

        assertThat(isOnErrorPage || hasErrorMessage).isTrue();
    }

    @Test
    void e2e03_searchWithEmptyCity_ShouldDoNothing() throws InterruptedException {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "pass123";

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

        driver.get(baseUrl + "/history");
        Thread.sleep(3000);

        String historyPage = driver.getPageSource();
        boolean isEmptyInitially = historyPage.contains("No search history yet");
        assertThat(isEmptyInitially).isTrue();
        System.out.println("Изначально история пуста");

        driver.get(baseUrl + "/weather");
        Thread.sleep(2000);

        driver.findElement(By.id("city")).sendKeys("");
        driver.findElement(By.cssSelector("input[type='submit'][value='Get Weather']")).click();
        Thread.sleep(3000);

        String currentUrl = driver.getCurrentUrl();
        assertThat(currentUrl).contains("/weather");
        System.out.println("После попытки поиска с пустым полем остались на /weather");

        driver.get(baseUrl + "/history");
        Thread.sleep(3000);

        historyPage = driver.getPageSource();
        boolean isEmptyAfterAttempt = historyPage.contains("No search history yet");
        assertThat(isEmptyAfterAttempt).isTrue();
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

    @Test
    void e2e09_temperatureConverter() throws InterruptedException {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "pass123";

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

        driver.get(baseUrl + "/temp-converter");
        Thread.sleep(2000);

        assertThat(driver.getPageSource()).contains("Temperature Converter");

        WebElement celsiusInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("celsius")));
        celsiusInput.clear();
        celsiusInput.sendKeys("100");

        WebElement convertToFahrenheitBtn = driver.findElement(By.cssSelector("form[action*='/to-fahrenheit'] button"));
        convertToFahrenheitBtn.click();
        Thread.sleep(2000);

        String pageSource = driver.getPageSource();
        boolean hasResult = pageSource.contains("100.0 °C = 212.0 °F") ||
                pageSource.contains("100 °C = 212 °F") ||
                pageSource.contains("100.0 °C = 212 °F") ||
                pageSource.contains("100 °C = 212.0 °F");
        assertThat(hasResult).isTrue();

        WebElement fahrenheitInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("fahrenheit")));
        fahrenheitInput.clear();
        fahrenheitInput.sendKeys("212");

        WebElement convertToCelsiusBtn = driver.findElement(By.cssSelector("form[action*='/to-celsius'] button"));
        convertToCelsiusBtn.click();
        Thread.sleep(2000);

        pageSource = driver.getPageSource();
        hasResult = pageSource.contains("212.0 °F = 100.0 °C") ||
                pageSource.contains("212 °F = 100 °C") ||
                pageSource.contains("212.0 °F = 100 °C") ||
                pageSource.contains("212 °F = 100.0 °C");
        assertThat(hasResult).isTrue();


        celsiusInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("celsius")));
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "arguments[0].setAttribute('type', 'number');", celsiusInput);
        celsiusInput.clear();
        celsiusInput.sendKeys("0");

        convertToFahrenheitBtn = driver.findElement(By.cssSelector("form[action*='/to-fahrenheit'] button"));
        convertToFahrenheitBtn.click();
        Thread.sleep(2000);

        pageSource = driver.getPageSource();
        hasResult = pageSource.contains("0.0 °C = 32.0 °F") ||
                pageSource.contains("0 °C = 32 °F");
        assertThat(hasResult).isTrue();
    }

    @Test
    void e2e10_weatherDictionary_ShouldSearchByPrefix() throws InterruptedException {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "pass123";

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

        driver.get(baseUrl + "/dictionary");
        Thread.sleep(2000);

        String pageSource = driver.getPageSource();
        assertThat(pageSource).contains("Weather Dictionary");
        assertThat(pageSource).contains("English-Russian weather terms");

        List<WebElement> tableRows = driver.findElements(By.cssSelector(".dictionary-table tbody tr"));
        assertThat(tableRows.isEmpty()).isFalse();
        System.out.println("Найдено терминов на странице: " + tableRows.size());

        WebElement searchInput = driver.findElement(By.name("search"));
        searchInput.clear();
        searchInput.sendKeys("sun");

        WebElement searchBtn = driver.findElement(By.cssSelector(".search-btn:not(.clear-btn)"));
        searchBtn.click();
        Thread.sleep(2000);

        pageSource = driver.getPageSource();
        assertThat(pageSource).contains("sunny");
        assertThat(pageSource).contains("солнечно");

        tableRows = driver.findElements(By.cssSelector(".dictionary-table tbody tr"));
        assertThat(tableRows.size()).isEqualTo(1);
        System.out.println("Поиск 'sun' нашел " + tableRows.size() + " термин(ов)");

        searchInput = driver.findElement(By.name("search"));
        searchInput.clear();
        searchInput.sendKeys("cl");

        searchBtn = driver.findElement(By.cssSelector(".search-btn:not(.clear-btn)"));
        searchBtn.click();
        Thread.sleep(2000);

        pageSource = driver.getPageSource();
        tableRows = driver.findElements(By.cssSelector(".dictionary-table tbody tr"));
        assertThat(tableRows.size()).isGreaterThan(0);

        boolean hasCloudy = pageSource.contains("cloudy");
        boolean hasClear = pageSource.contains("clear");
        assertThat(hasCloudy || hasClear).isTrue();
        System.out.println("Поиск 'cl' нашел " + tableRows.size() + " термин(ов)");

        searchInput = driver.findElement(By.name("search"));
        searchInput.clear();
        searchInput.sendKeys("rain");

        searchBtn = driver.findElement(By.cssSelector(".search-btn:not(.clear-btn)"));
        searchBtn.click();
        Thread.sleep(2000);

        pageSource = driver.getPageSource();
        boolean hasRain = pageSource.contains("rainy") || pageSource.contains("rain");
        assertThat(hasRain).isTrue();
        System.out.println("Поиск 'rain' работает");

        searchInput = driver.findElement(By.name("search"));
        searchInput.clear();
        searchInput.sendKeys("xyz123");

        searchBtn = driver.findElement(By.cssSelector(".search-btn:not(.clear-btn)"));
        searchBtn.click();
        Thread.sleep(2000);

        pageSource = driver.getPageSource();
        assertThat(pageSource).contains("No terms found");
        assertThat(pageSource).contains("xyz123");

        boolean noTable = driver.findElements(By.cssSelector(".dictionary-table")).isEmpty();
        assertThat(noTable).isTrue();
        System.out.println("Поиск несуществующего префикса показывает 'No terms found'");

        WebElement clearBtn = driver.findElement(By.cssSelector(".clear-btn"));
        clearBtn.click();
        Thread.sleep(2000);

        pageSource = driver.getPageSource();
        assertThat(pageSource).contains("Total terms:");

        tableRows = driver.findElements(By.cssSelector(".dictionary-table tbody tr"));
        assertThat(tableRows.isEmpty()).isFalse();
        System.out.println("Очистка поиска восстановила все термины");

        driver.get(baseUrl + "/dictionary?search=SUN");
        Thread.sleep(2000);

        pageSource = driver.getPageSource();
        assertThat(pageSource).contains("sunny");
        System.out.println("Поиск регистронезависимый");

        driver.get(baseUrl + "/dictionary?search=unny");
        Thread.sleep(2000);

        pageSource = driver.getPageSource();
        assertThat(pageSource).contains("No terms found");
        assertThat(pageSource).doesNotContain("sunny");
        System.out.println("Поиск работает по префиксу (первые буквы)");
    }
}

