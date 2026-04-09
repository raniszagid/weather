package ru.spbpu.weather.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.spbpu.weather.dto.DayDto;
import ru.spbpu.weather.dto.WeatherDto;
import ru.spbpu.weather.model.Day;
import ru.spbpu.weather.model.RequestHistoryEntity;
import ru.spbpu.weather.model.User;
import ru.spbpu.weather.model.Weather;
import ru.spbpu.weather.repository.DayRepository;
import ru.spbpu.weather.repository.RequestRepository;
import ru.spbpu.weather.repository.UserRepository;
import ru.spbpu.weather.repository.WeatherRepository;
import ru.spbpu.weather.service.ApiService;
import ru.spbpu.weather.service.RequestService;
import ru.spbpu.weather.util.WeatherMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
public class WeatherIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ApiService apiService;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private WeatherRepository weatherRepository;

    @Autowired
    private DayRepository dayRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RequestService requestService;

    @Autowired
    private WeatherMapper weatherMapper;

    private WeatherDto sampleWeatherDto;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "password");
        userRepository.save(testUser);

        sampleWeatherDto = WeatherDto.builder()
                .temperature("15 °C")
                .wind("10 km/h")
                .description("Sunny")
                .forecast(List.of(
                        DayDto.builder().day("1").temperature("16 °C").wind("12 km/h").build(),
                        DayDto.builder().day("2").temperature("14 °C").wind("8 km/h").build()
                ))
                .build();
        when(apiService.makeRequest(anyString())).thenReturn(Optional.of(sampleWeatherDto));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getIndex_shouldReturnIndexPage() throws Exception {
        mockMvc.perform(get("/weather"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void makeSearch_validCity_shouldSaveAndReturnIndex() throws Exception {
        mockMvc.perform(post("/weather")
                        .param("city", "London")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("result"))
                .andExpect(model().attribute("result", sampleWeatherDto));

        List<RequestHistoryEntity> requests = requestRepository.findAll();
        assertThat(requests).hasSize(1);
        RequestHistoryEntity savedRequest = requests.get(0);
        assertThat(savedRequest.getAddress()).isEqualTo("London");
        assertThat(savedRequest.getUser()).isNotNull();
        assertThat(savedRequest.getUser().getUsername()).isEqualTo("testuser");

        Optional<Weather> weatherOpt = weatherRepository.findWeatherByRequestId(savedRequest.getId());
        assertThat(weatherOpt).isPresent();
        Weather weather = weatherOpt.get();
        assertThat(weather.getTemperature()).isEqualTo(15);
        assertThat(weather.getWind()).isEqualTo(10);
        assertThat(weather.getDescription()).isEqualTo("Sunny");

        List<Day> days = dayRepository.findDaysByWeatherId(weather.getId());
        assertThat(days).hasSize(2);
    }

    @Test
    void requestService_save_shouldPersistFullHierarchy() {
        RequestHistoryEntity request = new RequestHistoryEntity();
        request.setAddress("Moscow");
        request.setRequestTimestamp(LocalDateTime.now());
        request.setUser(testUser);

        Weather weather = new Weather();
        weather.setTemperature(20);
        weather.setWind(5);
        weather.setDescription("Cloudy");

        Day day1 = new Day();
        day1.setDate(1);
        day1.setTemperature(18);
        day1.setWind(7);
        day1.setWeather(weather);

        Day day2 = new Day();
        day2.setDate(2);
        day2.setTemperature(22);
        day2.setWind(4);
        day2.setWeather(weather);

        requestService.save(request, weather, List.of(day1, day2));

        RequestHistoryEntity savedRequest = requestRepository.findById(request.getId()).orElseThrow();
        assertThat(savedRequest.getAddress()).isEqualTo("Moscow");

        Weather savedWeather = weatherRepository.findWeatherByRequestId(savedRequest.getId()).orElseThrow();
        assertThat(savedWeather.getTemperature()).isEqualTo(20);

        List<Day> savedDays = dayRepository.findDaysByWeatherId(savedWeather.getId());
        assertThat(savedDays).hasSize(2);
        assertThat(savedDays).extracting(Day::getDate).containsExactly(1, 2);
    }
}