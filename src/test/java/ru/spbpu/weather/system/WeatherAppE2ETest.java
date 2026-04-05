package ru.spbpu.weather.system;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import ru.spbpu.weather.system.pages_model.LoginPage;
import ru.spbpu.weather.system.pages_model.RegistrationPage;
import ru.spbpu.weather.system.pages_model.WeatherPage;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WeatherAppE2ETest extends BaseE2ETest {

    private static final String TEST_USER = "e2euser";
    private static final String TEST_PASS = "e2epass123";

    // ==================== СЦЕНАРИЙ E2E-01 ====================
    @Test
    @Order(1)
    void e2e01_registerNewUser_ShouldSucceed() {
        driver.get(baseUrl + "/auth/registration");
        RegistrationPage registrationPage = new RegistrationPage(driver);

        registrationPage.register(TEST_USER, TEST_PASS);

        assertThat(driver.getCurrentUrl()).contains("/auth/login");
    }

    // ==================== СЦЕНАРИЙ E2E-02 ====================
    @Test
    @Order(2)
    void e2e02_loginExistingUser_ShouldRedirectToWeather() {
        driver.get(baseUrl + "/auth/login");
        LoginPage loginPage = new LoginPage(driver);

        loginPage.login(TEST_USER, TEST_PASS);

        assertThat(driver.getCurrentUrl()).contains("/weather");
    }

    // ==================== СЦЕНАРИЙ E2E-03 ====================
    @Test
    @Order(3)
    void e2e03_searchWeatherByCity_ShouldDisplayWeather() {
        driver.get(baseUrl + "/weather");
        WeatherPage weatherPage = new WeatherPage(driver);

        weatherPage.searchCity("London");

        assertThat(weatherPage.isWeatherDisplayed()).isTrue();
        assertThat(weatherPage.getForecastCount()).isGreaterThan(0);
    }

    // ==================== СЦЕНАРИЙ E2E-04 ====================
    @Test
    void e2e04_unauthenticatedUserSearch_ShouldRedirectToLogin() {
        // Выходим если залогинены
        if (driver.getCurrentUrl().contains("/weather")) {
            new WeatherPage(driver).logout();
        }

        driver.get(baseUrl + "/weather");

        assertThat(driver.getCurrentUrl()).contains("/auth/login");
    }

    // ==================== СЦЕНАРИЙ E2E-05 ====================
    @Test
    @Order(4)
    void e2e05_viewSearchHistory_ShouldShowPreviousSearches() {
        driver.get(baseUrl + "/weather");
        WeatherPage weatherPage = new WeatherPage(driver);

        // Выполняем несколько поисков
        weatherPage.searchCity("Paris");

        weatherPage.searchCity("Berlin");


        weatherPage.openHistory();

        assertThat(weatherPage.getHistoryItemsCount()).isGreaterThanOrEqualTo(2);
        assertThat(weatherPage.isHistoryContainsCity("Paris")).isTrue();
        assertThat(weatherPage.isHistoryContainsCity("Berlin")).isTrue();
    }

    // ==================== СЦЕНАРИЙ E2E-06 ====================
    @Test
    void e2e06_logout_ShouldRedirectToLogin() {
        driver.get(baseUrl + "/auth/login");
        new LoginPage(driver).login(TEST_USER, TEST_PASS);

        WeatherPage weatherPage = new WeatherPage(driver);
        weatherPage.logout();

        assertThat(driver.getCurrentUrl()).contains("/auth/login");
    }

    // ==================== СЦЕНАРИЙ E2E-07 ====================
    @Test
    @Order(5)
    void e2e07_searchNonExistentCity_ShouldShowError() {
        driver.get(baseUrl + "/weather");
        WeatherPage weatherPage = new WeatherPage(driver);

        weatherPage.searchCity("NonExistentCity123456");

        assertThat(weatherPage.isErrorDisplayed()).isTrue();
    }

    // ==================== СЦЕНАРИЙ E2E-08 ====================
    @Test
    @Order(6)
    void e2e08_searchWithEmptyCity_ShouldShowError() {
        driver.get(baseUrl + "/weather");
        WeatherPage weatherPage = new WeatherPage(driver);

        weatherPage.searchCity("");

        // Пустой ввод может вызвать ошибку валидации или редирект
        assertThat(weatherPage.isErrorDisplayed() || driver.getCurrentUrl().contains("error")).isTrue();
    }

    // ==================== СЦЕНАРИЙ E2E-09 ====================
    @Test
    void e2e09_registerWithExistingUsername_ShouldShowError() {
        // Сначала создаем пользователя
        driver.get(baseUrl + "/auth/registration");
        RegistrationPage registrationPage = new RegistrationPage(driver);

        // Регистрируем первого пользователя
        registrationPage.register(TEST_USER, TEST_PASS);

        // Ждем редиректа на страницу логина
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Снова переходим на страницу регистрации
        driver.get(baseUrl + "/auth/registration");

        // Пытаемся зарегистрировать того же пользователя
        registrationPage.register(TEST_USER, "differentpass");

        // Проверяем наличие ошибки с ожиданием
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Получаем текст страницы для отладки
        String pageText = driver.getPageSource();
        System.out.println("Page text contains error: " + pageText.contains("already exists"));

        assertThat(registrationPage.isErrorDisplayed()).isTrue();
    }

    // ==================== СЦЕНАРИЙ E2E-10 ====================
    @Test
    void e2e10_registerWithEmptyFields_ShouldShowError() {
        driver.get(baseUrl + "/auth/registration");
        RegistrationPage registrationPage = new RegistrationPage(driver);

        registrationPage.register("", "");

        assertThat(registrationPage.isErrorDisplayed()).isTrue();
    }

    // ==================== СЦЕНАРИЙ E2E-11 ====================
    @Test
    @Order(7)
    void e2e11_historyDataAfterSearch_ShouldContainSearchedCity() {
        String testCity = "Tokyo";
        driver.get(baseUrl + "/weather");
        WeatherPage weatherPage = new WeatherPage(driver);

        weatherPage.searchCity(testCity);
        waitForPageLoad();
        weatherPage.openHistory();

        assertThat(weatherPage.isHistoryContainsCity(testCity)).isTrue();
    }

    // ==================== СЦЕНАРИЙ E2E-12 ====================
    @Test
    @Order(8)
    void e2e12_temperatureFormat_ShouldIncludeCelsiusSymbol() {
        driver.get(baseUrl + "/weather");
        WeatherPage weatherPage = new WeatherPage(driver);

        weatherPage.searchCity("Moscow");

        String temperature = weatherPage.getCurrentTemperature();
        assertThat(temperature).contains("°C");
    }

    // ==================== СЦЕНАРИЙ E2E-13 ====================
    @Test
    @Order(9)
    void e2e13_multipleCitySearches_AllShouldBeInHistory() {
        String[] cities = {"Rome", "Madrid", "Amsterdam"};
        driver.get(baseUrl + "/weather");
        WeatherPage weatherPage = new WeatherPage(driver);

        for (String city : cities) {
            weatherPage.searchCity(city);
            waitForPageLoad();
        }

        weatherPage.openHistory();

        for (String city : cities) {
            assertThat(weatherPage.isHistoryContainsCity(city)).isTrue();
        }
        assertThat(weatherPage.getHistoryItemsCount()).isGreaterThanOrEqualTo(cities.length);
    }

    // ==================== СЦЕНАРИЙ E2E-14 ====================
    @Test
    void e2e14_accessProtectedPagesWithoutAuth_ShouldRedirect() {
        String[] protectedUrls = {"/weather", "/history"};

        for (String url : protectedUrls) {
            driver.get(baseUrl + url);
            assertThat(driver.getCurrentUrl()).contains("/auth/login");
        }
    }

    // ==================== СЦЕНАРИЙ E2E-15 ====================
    @Test
    void e2e15_completeUserJourney_AllStepsSucceed() {
        String newUser = "completeuser";
        String newPass = "complete123";

        // Шаг 1: Регистрация
        driver.get(baseUrl + "/auth/registration");
        RegistrationPage registrationPage = new RegistrationPage(driver);
        registrationPage.register(newUser, newPass);
        assertThat(driver.getCurrentUrl()).contains("/auth/login");

        // Шаг 2: Логин
        LoginPage loginPage = new LoginPage(driver);
        loginPage.login(newUser, newPass);
        assertThat(driver.getCurrentUrl()).contains("/weather");

        // Шаг 3: Поиск погоды
        WeatherPage weatherPage = new WeatherPage(driver);
        weatherPage.searchCity("Barcelona");
        assertThat(weatherPage.isWeatherDisplayed()).isTrue();

        // Шаг 4: Проверка истории
        weatherPage.openHistory();
        assertThat(weatherPage.isHistoryContainsCity("Barcelona")).isTrue();

        // Шаг 5: Выход
        weatherPage.logout();
        assertThat(driver.getCurrentUrl()).contains("/auth/login");
    }
}
