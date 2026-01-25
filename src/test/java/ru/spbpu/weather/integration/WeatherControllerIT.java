package ru.spbpu.weather.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import ru.spbpu.weather.dto.DayDto;
import ru.spbpu.weather.dto.WeatherDto;
import ru.spbpu.weather.model.RequestHistoryEntity;
import ru.spbpu.weather.model.User;
import ru.spbpu.weather.model.Weather;
import ru.spbpu.weather.repository.WeatherRepository;
import ru.spbpu.weather.service.ApiService;
import ru.spbpu.weather.service.RegistrationService;
import ru.spbpu.weather.service.RequestService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@Disabled
class WeatherControllerIT {

	@Container
	private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.0")
					.withDatabaseName("test")
					.withUsername("test")
					.withPassword("test");

	@DynamicPropertySource
	private static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@BeforeAll
	static void initDB() {
		postgres.start();
	}

	@AfterAll
	static void closeDB() {
		postgres.close();
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RequestService requestService;

	@Mock
	private ApiService apiService = new ApiService();

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private WeatherRepository weatherRepository;

	@Test
	@WithMockUser(username = "testuser")
	void shouldSaveRequestAndWeatherDataWhenValidCity() throws Exception {
		User user = new User("testuser", "password");
		registrationService.register(user);
		DayDto dayDto = DayDto.builder()
						.day("21")
						.wind("40")
						.temperature("20")
						.build();

		WeatherDto weatherDto = WeatherDto.builder()
						.temperature("20")
						.description("Description")
						.wind("40")
						.forecast(List.of(dayDto))
						.build();

		when(apiService.makeRequest("London")).thenReturn(Optional.of(weatherDto));

		mockMvc.perform(post("/weather")
										.param("city", "London"))
						.andExpect(status().isOk())
						.andExpect(view().name("index"))
						.andExpect(model().attributeExists("result"));

		List<RequestHistoryEntity> requests = requestService.findAll();
		assertThat(requests).hasSize(1);

		RequestHistoryEntity request = requests.getFirst();
		assertThat(request.getUser().getUsername()).isEqualTo("testuser");
		assertThat(request.getAddress()).isEqualTo("London");

		List<Weather> weatherData = weatherRepository.findAll();
		assertThat(weatherData).hasSize(1);
		assertThat(weatherData.getFirst().getRequest().getAddress()).isEqualTo("London");
	}

	@Test
	@WithMockUser
	void shouldHandleCityNotFound() throws Exception {
		when(apiService.makeRequest(anyString())).thenReturn(Optional.empty());

		mockMvc.perform(post("/weather")
										.param("city", "InvalidCity"))
						.andExpect(status().isBadRequest())
						.andExpect(view().name("error"))
						.andExpect(model().attributeDoesNotExist("result"));
	}
}
