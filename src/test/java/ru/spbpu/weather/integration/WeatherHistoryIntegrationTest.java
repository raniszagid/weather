package ru.spbpu.weather.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.spbpu.weather.model.RequestHistoryEntity;
import ru.spbpu.weather.model.User;
import ru.spbpu.weather.model.Weather;
import ru.spbpu.weather.repository.UserRepository;
import ru.spbpu.weather.service.ApiService;
import ru.spbpu.weather.service.RequestService;
import ru.spbpu.weather.util.WeatherMapper;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class WeatherHistoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RequestService requestService;

    @Autowired
    private WeatherMapper weatherMapper;

    @MockBean
    private ApiService apiService;

    @Test
    @WithMockUser(username = "historyuser")
    void viewWeatherHistory_MultipleRequests_ShouldShowAllHistory() throws Exception {
        // Создаем пользователя
        User user = new User("historyuser", "password");
        userRepository.save(user);

        // Создаем несколько запросов в истории
        createWeatherRequest(user, "London");
        createWeatherRequest(user, "Paris");
        createWeatherRequest(user, "Berlin");

        // Просмотр истории
        mockMvc.perform(get("/history"))
                .andExpect(status().isOk())
                .andExpect(view().name("history"))
                .andExpect(model().attributeExists("history"))
                .andExpect(model().attribute("history", hasSize(3)));
    }

    @Test
    void viewHistory_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        // Попытка доступа к истории без авторизации
        mockMvc.perform(get("/history"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    private void createWeatherRequest(User user, String city) {
        RequestHistoryEntity request = new RequestHistoryEntity();
        request.setUser(user);
        request.setAddress(city);
        request.setRequestTimestamp(LocalDateTime.now());

        Weather weather = new Weather();
        weather.setTemperature(20);
        weather.setWind(15);
        weather.setDescription("Test");

        requestService.save(request, weather, Collections.emptyList());
    }
}
