package ru.spbpu.weather.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.spbpu.weather.dto.WeatherDto;
import ru.spbpu.weather.model.RequestHistoryEntity;
import ru.spbpu.weather.model.User;
import ru.spbpu.weather.repository.RequestRepository;
import ru.spbpu.weather.repository.UserRepository;
import ru.spbpu.weather.service.ApiService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@Testcontainers
class UserRegistrationAndWeatherSearchIT {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Включаем Hibernate auto DDL для создания таблиц
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // Отключаем Liquibase
        registry.add("spring.liquibase.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RequestRepository requestRepository;

    @MockBean
    private ApiService apiService;

    @Test
    void completeUserJourney_RegistrationLoginWeatherSearch() throws Exception {
        String username = "journeyuser";
        String password = "journeypass";

        // Шаг 1: Регистрация нового пользователя
        mockMvc.perform(post("/auth/registration")
                        .with(csrf())
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));

        // Проверяем, что пользователь создан
        assertTrue(userRepository.findByUsername(username).isPresent());

        // Шаг 2 и 3: Логин и поиск погоды в одном запросе с аутентификацией
        User user = userRepository.findByUsername(username).orElseThrow();

        WeatherDto weatherDto = createTestWeatherDto();
        when(apiService.makeRequest("Paris")).thenReturn(Optional.of(weatherDto));

        // Выполняем поиск погоды с аутентификацией пользователя
        mockMvc.perform(post("/weather")
                        .with(csrf())
                        .with(user(user.getUsername()).password(password))
                        .param("city", "Paris"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("result"));

        // Проверяем, что запрос сохранился в истории
        List<RequestHistoryEntity> requests = requestRepository.findRequestHistoryEntitiesByUser(user);
        assertEquals(1, requests.size());
        assertEquals("Paris", requests.get(0).getAddress());
    }

    @Test
    @WithMockUser(username = "journeyuser")
    void searchWeather_WithMockUser_ShouldWork() throws Exception {
        // Альтернативный подход - использовать @WithMockUser
        WeatherDto weatherDto = createTestWeatherDto();
        when(apiService.makeRequest("London")).thenReturn(Optional.of(weatherDto));

        mockMvc.perform(post("/weather")
                        .with(csrf())
                        .param("city", "London"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("result"));
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