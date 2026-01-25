package ru.spbpu.weather.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.ModelAndView;
import ru.spbpu.weather.UnitTestConfig;
import ru.spbpu.weather.dto.DayDto;
import ru.spbpu.weather.dto.WeatherDto;
import ru.spbpu.weather.model.Day;
import ru.spbpu.weather.model.RequestHistoryEntity;
import ru.spbpu.weather.model.User;
import ru.spbpu.weather.model.Weather;
import ru.spbpu.weather.security.SecurityConfig;
import ru.spbpu.weather.service.ApiService;
import ru.spbpu.weather.service.RequestService;
import ru.spbpu.weather.service.UserService;
import ru.spbpu.weather.util.WeatherMapper;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@WebMvcTest(WeatherController.class)
@Import({UnitTestConfig.class, SecurityConfig.class})
@AutoConfigureMockMvc
class WeatherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RequestService requestService;

    @MockBean
    private UserService userService;

    @MockBean
    private WeatherMapper weatherMapper;

    @MockBean
    private ApiService apiService;

    @Test
    void getIndex_ShouldReturnIndexView() throws Exception {
        mockMvc.perform(get("/weather"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser
    void makeSearch_WithValidCity_ShouldReturnIndexWithResult() throws Exception {
        WeatherDto weatherDto = createTestWeatherDto();
        when(apiService.makeRequest("London")).thenReturn(Optional.of(weatherDto));
        when(userService.getCurrentUser()).thenReturn(Optional.of(new User("user", "pass")));

        mockMvc.perform(post("/weather")
                        .with(csrf())
                        .param("city", "London"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("result"))
                .andExpect(model().attribute("result", equalTo(weatherDto)));

        verify(requestService, times(1)).save(any(), any(), any());
    }

    @Test
    @WithMockUser
    void makeSearch_WithEmptyCity_ShouldThrowException() throws Exception {
        mockMvc.perform(post("/weather")
                        .with(csrf())
                        .param("city", ""))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"));
    }

    @Test
    @WithMockUser
    void makeSearch_WhenApiServiceReturnsEmpty_ShouldThrowException() throws Exception {
        when(apiService.makeRequest("Unknown")).thenReturn(Optional.empty());

        mockMvc.perform(post("/weather")
                        .with(csrf())
                        .param("city", "Unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("errorMessage", "City not found"));
    }

    @Test
    @WithMockUser(username = "loggedUser")
    void makeSearch_ShouldSetUserWhenUserIsLoggedIn() throws Exception {
        User user = new User("loggedUser", "pass");
        WeatherDto weatherDto = createTestWeatherDto();

        when(apiService.makeRequest("London")).thenReturn(Optional.of(weatherDto));
        when(userService.getCurrentUser()).thenReturn(Optional.of(user));

        mockMvc.perform(post("/weather")
                .with(csrf())
                .param("city", "London"));

        ArgumentCaptor<RequestHistoryEntity> captor = ArgumentCaptor.forClass(RequestHistoryEntity.class);
        verify(requestService).save(captor.capture(), any(), any());

        assertEquals(user, captor.getValue().getUser());
    }

    @Test
    @WithMockUser
    void makeSearch_ShouldNotSetUserWhenUserNotLoggedIn() throws Exception {
        WeatherDto weatherDto = createTestWeatherDto();
        when(apiService.makeRequest("London")).thenReturn(Optional.of(weatherDto));
        when(userService.getCurrentUser()).thenReturn(Optional.empty());

        mockMvc.perform(post("/weather")
                .with(csrf())
                .param("city", "London"));

        ArgumentCaptor<RequestHistoryEntity> captor = ArgumentCaptor.forClass(RequestHistoryEntity.class);
        verify(requestService).save(captor.capture(), any(), any());

        assertNull(captor.getValue().getUser());
    }

    @Test
    @WithMockUser
    void makeSearch_ShouldSetCorrectAddress() throws Exception {
        WeatherDto weatherDto = createTestWeatherDto();
        when(apiService.makeRequest("Paris")).thenReturn(Optional.of(weatherDto));

        mockMvc.perform(post("/weather")
                .with(csrf())
                .param("city", "Paris"));

        ArgumentCaptor<RequestHistoryEntity> captor = ArgumentCaptor.forClass(RequestHistoryEntity.class);
        verify(requestService).save(captor.capture(), any(), any());

        assertEquals("Paris", captor.getValue().getAddress());
    }

    @Test
    @WithMockUser
    void makeSearch_ShouldSetRequestTimestamp() throws Exception {
        WeatherDto weatherDto = createTestWeatherDto();
        when(apiService.makeRequest("Berlin")).thenReturn(Optional.of(weatherDto));

        mockMvc.perform(post("/weather")
                .with(csrf())
                .param("city", "Berlin"));

        ArgumentCaptor<RequestHistoryEntity> captor = ArgumentCaptor.forClass(RequestHistoryEntity.class);
        verify(requestService).save(captor.capture(), any(), any());

        assertNotNull(captor.getValue().getRequestTimestamp());
        assertTrue(captor.getValue().getRequestTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    @WithMockUser
    void makeSearch_ShouldMapWeatherDtoToEntity() throws Exception {
        WeatherDto weatherDto = createTestWeatherDto();
        Weather weatherEntity = new Weather();

        when(apiService.makeRequest("London")).thenReturn(Optional.of(weatherDto));
        when(weatherMapper.toWeatherEntity(weatherDto)).thenReturn(weatherEntity);

        mockMvc.perform(post("/weather")
                .with(csrf())
                .param("city", "London"));

        verify(weatherMapper, times(1)).toWeatherEntity(weatherDto);
    }

    @Test
    @WithMockUser
    void makeSearch_ShouldGetForecastFromWeatherMapper() throws Exception {
        WeatherDto weatherDto = createTestWeatherDto();
        Weather weatherEntity = new Weather();
        List<Day> forecast = List.of(new Day(), new Day());

        when(apiService.makeRequest("London")).thenReturn(Optional.of(weatherDto));
        when(weatherMapper.toWeatherEntity(weatherDto)).thenReturn(weatherEntity);
        when(weatherMapper.getForecast(weatherDto, weatherEntity)).thenReturn(forecast);

        mockMvc.perform(post("/weather")
                .with(csrf())
                .param("city", "London"));

        verify(weatherMapper, times(1)).getForecast(weatherDto, weatherEntity);
    }

    @Test
    void handleException_ShouldReturnErrorViewWithCorrectAttributes() throws Exception {
        WeatherController controller = new WeatherController(requestService, userService, weatherMapper, apiService);

        ModelAndView mav = controller.handleException(new RuntimeException("Test error"));

        assertEquals("error", mav.getViewName());
        assertEquals("Test error", mav.getModel().get("errorMessage"));
        assertEquals(HttpStatus.BAD_REQUEST, mav.getModel().get("status"));
        assertEquals(HttpStatus.BAD_REQUEST, mav.getStatus());
    }

    @Test
    void handleException_WithDifferentExceptionMessage() throws Exception {
        WeatherController controller = new WeatherController(requestService, userService, weatherMapper, apiService);

        ModelAndView mav = controller.handleException(new RuntimeException("Another error"));

        assertEquals("Another error", mav.getModel().get("errorMessage"));
    }

    @Test
    @WithMockUser
    void makeSearch_WithoutCsrf_ShouldBeForbidden() throws Exception {
        mockMvc.perform(post("/weather")
                        .param("city", "London"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void makeSearch_WithNullCity_ShouldThrowException() throws Exception {
        mockMvc.perform(post("/weather")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void makeSearch_ShouldLogCityAndResult() throws Exception {
        WeatherDto weatherDto = createTestWeatherDto();
        when(apiService.makeRequest("Madrid")).thenReturn(Optional.of(weatherDto));

        // Проверка логирования будет через MockMvc, но можно добавить тест на Logger
        mockMvc.perform(post("/weather")
                        .with(csrf())
                        .param("city", "Madrid"))
                .andExpect(status().isOk());
    }

    private WeatherDto createTestWeatherDto() {
        DayDto dayDto = DayDto.builder()
                .day("21")
                .wind("40")
                .temperature("20")
                .build();
        return WeatherDto.builder()
                .temperature("20")
                .description("Description")
                .wind("40")
                .forecast(List.of(dayDto))
                .build();
    }
}
