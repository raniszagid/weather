package ru.spbpu.weather.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import ru.spbpu.weather.dto.WeatherDto;
import ru.spbpu.weather.repository.UserRepository;
import ru.spbpu.weather.service.ApiService;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WeatherAppE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private ApiService apiService;

    private String baseUrl;
    private HttpHeaders headers;

    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");
    }

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Очищаем репозиторий перед каждым тестом
        userRepository.deleteAll();
    }

    // ==================== СЦЕНАРИЙ E2E-01 ====================
    @Test
    @Order(1)
    void e2e01_registerNewUser_ShouldSucceed() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", "e2euser");
        formData.add("password", "e2epass123");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/auth/registration", request, String.class);

        assertThat(response.getStatusCode().is3xxRedirection()).isTrue();
        assertThat(response.getHeaders().getLocation().toString()).contains("/auth/login");
        assertThat(userRepository.findByUsername("e2euser")).isPresent();
    }

    // ==================== СЦЕНАРИЙ E2E-02 ====================
    @Test
    @Order(2)
    void e2e02_loginExistingUser_ShouldRedirectToWeather() {
        // Сначала создаем пользователя
        MultiValueMap<String, String> regData = new LinkedMultiValueMap<>();
        regData.add("username", "e2euser");
        regData.add("password", "e2epass123");

        HttpEntity<MultiValueMap<String, String>> regRequest = new HttpEntity<>(regData, headers);
        restTemplate.postForEntity(baseUrl + "/auth/registration", regRequest, String.class);

        // Теперь логинимся
        MultiValueMap<String, String> loginData = new LinkedMultiValueMap<>();
        loginData.add("username", "e2euser");
        loginData.add("password", "e2epass123");

        HttpEntity<MultiValueMap<String, String>> loginRequest = new HttpEntity<>(loginData, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/process_login", loginRequest, String.class);

        assertThat(response.getStatusCode().is3xxRedirection()).isTrue();
        assertThat(response.getHeaders().getLocation().toString()).contains("/weather");
    }

    // ==================== СЦЕНАРИЙ E2E-03 ====================
    @Test
    @Order(3)
    void e2e03_searchWeatherByCity_ShouldReturnWeather() {
        WeatherDto weatherDto = createTestWeatherDto();
        when(apiService.makeRequest("London")).thenReturn(Optional.of(weatherDto));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("city", "London");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/weather", request, String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("London");
    }

    // ==================== СЦЕНАРИЙ E2E-04 ====================
    @Test
    void e2e04_unauthenticatedUserSearch_ShouldRedirectToLogin() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/weather", String.class);
        assertThat(response.getStatusCode().is3xxRedirection()).isTrue();
    }

    // ==================== СЦЕНАРИЙ E2E-05 ====================
    @Test
    @Order(4)
    void e2e05_viewSearchHistory_ShouldShowPreviousSearches() {
        // Регистрируем и логинимся
        registerAndLogin("e2euser", "e2epass123");

        WeatherDto weatherDto = createTestWeatherDto();
        when(apiService.makeRequest(anyString())).thenReturn(Optional.of(weatherDto));

        // Выполняем поиски с сохранением сессии
        searchCityWithAuth("Paris");
        searchCityWithAuth("Berlin");

        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/history", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("Paris");
        assertThat(response.getBody()).contains("Berlin");
    }

    // ==================== СЦЕНАРИЙ E2E-06 ====================
    @Test
    void e2e06_logout_ShouldRedirectToLogin() {
        registerAndLogin("e2euser", "e2epass123");

        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/logout", String.class);
        assertThat(response.getStatusCode().is3xxRedirection()).isTrue();
    }

    // ==================== СЦЕНАРИЙ E2E-07 ====================
    @Test
    @Order(5)
    void e2e07_searchNonExistentCity_ShouldShowError() {
        registerAndLogin("e2euser", "e2epass123");

        when(apiService.makeRequest("NonExistentCity")).thenReturn(Optional.empty());

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("city", "NonExistentCity");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/weather", request, String.class);

        assertThat(response.getBody()).contains("error");
    }

    // ==================== СЦЕНАРИЙ E2E-08 ====================
    @Test
    void e2e08_searchWithEmptyCity_ShouldShowError() {
        registerAndLogin("e2euser", "e2epass123");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("city", "");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/weather", request, String.class);

        assertThat(response.getStatusCode().is4xxClientError() ||
                response.getBody().contains("error")).isTrue();
    }

    // ==================== СЦЕНАРИЙ E2E-09 ====================
    @Test
    void e2e09_registerWithExistingUsername_ShouldShowError() {
        // Создаем первого пользователя
        MultiValueMap<String, String> regData = new LinkedMultiValueMap<>();
        regData.add("username", "existinguser");
        regData.add("password", "pass123");

        HttpEntity<MultiValueMap<String, String>> regRequest = new HttpEntity<>(regData, headers);
        restTemplate.postForEntity(baseUrl + "/auth/registration", regRequest, String.class);

        // Пытаемся создать такого же
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", "existinguser");
        formData.add("password", "differentpass");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/auth/registration", request, String.class);

        assertThat(response.getBody()).contains("already exists");
    }

    // ==================== СЦЕНАРИЙ E2E-10 ====================
    @Test
    void e2e10_registerWithEmptyFields_ShouldShowError() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", "");
        formData.add("password", "");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/auth/registration", request, String.class);

        assertThat(response.getBody()).contains("error");
    }

    // ==================== СЦЕНАРИЙ E2E-11 ====================
    @Test
    @Order(6)
    void e2e11_historyDataAfterSearch_ShouldContainSearchedCity() {
        registerAndLogin("e2euser", "e2epass123");

        String testCity = "Tokyo";
        WeatherDto weatherDto = createTestWeatherDto();
        when(apiService.makeRequest(testCity)).thenReturn(Optional.of(weatherDto));

        searchCityWithAuth(testCity);

        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/history", String.class);
        assertThat(response.getBody()).contains(testCity);
    }

    // ==================== СЦЕНАРИЙ E2E-12 ====================
    @Test
    @Order(7)
    void e2e12_temperatureFormat_ShouldIncludeCelsiusSymbol() {
        registerAndLogin("e2euser", "e2epass123");

        WeatherDto weatherDto = WeatherDto.builder()
                .temperature("+25 °C")
                .wind("10 km/h")
                .description("Sunny")
                .forecast(Collections.emptyList())
                .build();
        when(apiService.makeRequest("Moscow")).thenReturn(Optional.of(weatherDto));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("city", "Moscow");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/weather", request, String.class);

        assertThat(response.getBody()).contains("°C");
    }

    // ==================== СЦЕНАРИЙ E2E-13 ====================
    @Test
    @Order(8)
    void e2e13_multipleCitySearches_AllShouldBeInHistory() {
        registerAndLogin("e2euser", "e2epass123");

        String[] cities = {"Rome", "Madrid", "Amsterdam"};
        WeatherDto weatherDto = createTestWeatherDto();
        when(apiService.makeRequest(anyString())).thenReturn(Optional.of(weatherDto));

        for (String city : cities) {
            searchCityWithAuth(city);
        }

        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/history", String.class);

        for (String city : cities) {
            assertThat(response.getBody()).contains(city);
        }
    }

    // ==================== СЦЕНАРИЙ E2E-14 ====================
    @Test
    void e2e14_accessProtectedPagesWithoutAuth_ShouldRedirect() {
        String[] protectedUrls = {"/weather", "/history"};

        for (String url : protectedUrls) {
            ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + url, String.class);
            assertThat(response.getStatusCode().is3xxRedirection()).isTrue();
        }
    }

    // ==================== СЦЕНАРИЙ E2E-15 ====================
    @Test
    void e2e15_completeUserJourney_AllStepsSucceed() {
        String newUser = "completeuser";
        String newPass = "complete123";

        // 1. Регистрация
        MultiValueMap<String, String> regData = new LinkedMultiValueMap<>();
        regData.add("username", newUser);
        regData.add("password", newPass);

        HttpEntity<MultiValueMap<String, String>> regRequest = new HttpEntity<>(regData, headers);
        ResponseEntity<String> regResponse = restTemplate.postForEntity(
                baseUrl + "/auth/registration", regRequest, String.class);
        assertThat(regResponse.getStatusCode().is3xxRedirection()).isTrue();

        // 2. Логин
        MultiValueMap<String, String> loginData = new LinkedMultiValueMap<>();
        loginData.add("username", newUser);
        loginData.add("password", newPass);

        HttpEntity<MultiValueMap<String, String>> loginRequest = new HttpEntity<>(loginData, headers);
        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                baseUrl + "/process_login", loginRequest, String.class);
        assertThat(loginResponse.getStatusCode().is3xxRedirection()).isTrue();

        // 3. Поиск погоды
        WeatherDto weatherDto = createTestWeatherDto();
        when(apiService.makeRequest("Barcelona")).thenReturn(Optional.of(weatherDto));

        MultiValueMap<String, String> searchData = new LinkedMultiValueMap<>();
        searchData.add("city", "Barcelona");

        HttpEntity<MultiValueMap<String, String>> searchRequest = new HttpEntity<>(searchData, headers);
        ResponseEntity<String> searchResponse = restTemplate.postForEntity(
                baseUrl + "/weather", searchRequest, String.class);
        assertThat(searchResponse.getStatusCode().is2xxSuccessful()).isTrue();

        // 4. Проверка истории
        ResponseEntity<String> historyResponse = restTemplate.getForEntity(baseUrl + "/history", String.class);
        assertThat(historyResponse.getBody()).contains("Barcelona");
    }

    // ==================== Helper Methods ====================

    private void registerAndLogin(String username, String password) {
        // Регистрация
        MultiValueMap<String, String> regData = new LinkedMultiValueMap<>();
        regData.add("username", username);
        regData.add("password", password);

        HttpEntity<MultiValueMap<String, String>> regRequest = new HttpEntity<>(regData, headers);
        restTemplate.postForEntity(baseUrl + "/auth/registration", regRequest, String.class);

        // Логин
        MultiValueMap<String, String> loginData = new LinkedMultiValueMap<>();
        loginData.add("username", username);
        loginData.add("password", password);

        HttpEntity<MultiValueMap<String, String>> loginRequest = new HttpEntity<>(loginData, headers);
        restTemplate.postForEntity(baseUrl + "/process_login", loginRequest, String.class);
    }

    private void searchCityWithAuth(String city) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("city", city);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
        restTemplate.postForEntity(baseUrl + "/weather", request, String.class);
    }

    private WeatherDto createTestWeatherDto() {
        return WeatherDto.builder()
                .temperature("+20 °C")
                .wind("15 km/h")
                .description("Sunny")
                .forecast(Collections.emptyList())
                .build();
    }
}