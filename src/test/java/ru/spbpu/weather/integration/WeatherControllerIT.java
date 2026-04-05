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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@Testcontainers
class WeatherControllerIT {

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
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RequestService requestService;

	@MockBean  // Важно: используем @MockBean вместо @Mock
	private ApiService apiService;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private WeatherRepository weatherRepository;

	@Test
	@WithMockUser(username = "testuser")
	void shouldSaveRequestAndWeatherDataWhenValidCity() throws Exception {
		String city = "London";

		// Регистрируем пользователя
		User user = new User("testuser", "password");
		registrationService.register(user);

		// Создаем тестовые данные
		DayDto dayDto = DayDto.builder()
				.day("21")
				.wind("40 km/h")
				.temperature("20 °C")
				.build();

		WeatherDto weatherDto = WeatherDto.builder()
				.temperature("20 °C")
				.description("Sunny")
				.wind("40 km/h")
				.forecast(List.of(dayDto))
				.build();

		// Настраиваем мок - возвращаем данные для конкретного города
		when(apiService.makeRequest(city)).thenReturn(Optional.of(weatherDto));

		// Выполняем запрос
		mockMvc.perform(post("/weather")
						.with(csrf())
						.param("city", city))
				.andExpect(status().isOk())
				.andExpect(view().name("index"))
				.andExpect(model().attributeExists("result"));

		// Проверяем сохранение в БД
		List<RequestHistoryEntity> requests = requestService.findAll();
		assertThat(requests).hasSize(1);

		RequestHistoryEntity request = requests.getFirst();
		assertThat(request.getUser().getUsername()).isEqualTo("testuser");
		assertThat(request.getAddress()).isEqualTo(city);

		List<Weather> weatherData = weatherRepository.findAll();
		assertThat(weatherData).hasSize(1);
		assertThat(weatherData.getFirst().getRequest().getAddress()).isEqualTo(city);

		// Проверяем, что apiService был вызван ровно 1 раз
		verify(apiService, times(1)).makeRequest(city);
	}

	@Test
	@WithMockUser
	void shouldHandleCityNotFound() throws Exception {
		// Настраиваем мок - возвращаем empty для любого города
		when(apiService.makeRequest(anyString())).thenReturn(Optional.empty());

		mockMvc.perform(post("/weather")
						.with(csrf())
						.param("city", "NonExistentCity123"))
				.andExpect(status().isBadRequest())
				.andExpect(view().name("error"))
				.andExpect(model().attributeExists("errorMessage"))
				.andExpect(model().attribute("errorMessage", "City not found"))
				.andExpect(model().attributeDoesNotExist("result"));

		// Проверяем, что запрос не сохранился в БД
		assertThat(requestService.findAll()).isEmpty();

		// Проверяем, что apiService был вызван
		verify(apiService, times(1)).makeRequest("NonExistentCity123");
	}

	@Test
	@WithMockUser
	void shouldHandleApiServiceException() throws Exception {
		// Настраиваем мок - выбрасываем исключение
		when(apiService.makeRequest(anyString()))
				.thenThrow(new RuntimeException("API connection failed"));

		mockMvc.perform(post("/weather")
						.with(csrf())
						.param("city", "London"))
				.andExpect(status().isBadRequest())
				.andExpect(view().name("error"))
				.andExpect(model().attributeExists("errorMessage"))
				.andExpect(model().attribute("errorMessage", "API connection failed"));

		// Проверяем, что запрос не сохранился
		assertThat(requestService.findAll()).isEmpty();
	}

	@Test
	@WithMockUser
	void shouldNotSaveRequestWhenApiReturnsEmpty() throws Exception {
		// Настраиваем мок - город не найден
		when(apiService.makeRequest("Atlantis")).thenReturn(Optional.empty());

		mockMvc.perform(post("/weather")
						.with(csrf())
						.param("city", "Atlantis"))
				.andExpect(status().isBadRequest())
				.andExpect(view().name("error"));

		// Проверяем, что ничего не сохранилось в БД
		assertThat(requestService.findAll()).isEmpty();
		assertThat(weatherRepository.findAll()).isEmpty();
	}

	@Test
	@WithMockUser
	void shouldHandleMultipleRequestsForSameCity() throws Exception {
		String city = "Tokyo";

		WeatherDto weatherDto = WeatherDto.builder()
				.temperature("25 °C")
				.description("Clear")
				.wind("5 km/h")
				.forecast(List.of())
				.build();

		when(apiService.makeRequest(city)).thenReturn(Optional.of(weatherDto));

		// Делаем два одинаковых запроса подряд
		mockMvc.perform(post("/weather")
						.with(csrf())
						.param("city", city))
				.andExpect(status().isOk());

		mockMvc.perform(post("/weather")
						.with(csrf())
						.param("city", city))
				.andExpect(status().isOk());

		// Проверяем, что сохранилось два запроса (оба в истории)
		assertThat(requestService.findAll()).hasSize(2);

		// Проверяем, что apiService вызывался дважды
		verify(apiService, times(2)).makeRequest(city);
	}

	@Test
	@WithMockUser
	void shouldHandleNullCityParameter() throws Exception {
		mockMvc.perform(post("/weather")
						.with(csrf()))
				.andExpect(status().isBadRequest());
		// Не проверяем view, так как исключение возникает до вызова контроллера

		verify(apiService, never()).makeRequest(anyString());
	}

	@Test
	@WithMockUser
	void shouldHandleBlankCityParameter() throws Exception {
		String blankCity = "   ";
		when(apiService.makeRequest(blankCity)).thenReturn(Optional.empty());

		mockMvc.perform(post("/weather")
						.with(csrf())
						.param("city", blankCity))
				.andExpect(status().isBadRequest())
				.andExpect(view().name("error"));
	}
}
