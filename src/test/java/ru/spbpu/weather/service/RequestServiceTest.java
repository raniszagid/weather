package ru.spbpu.weather.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.spbpu.weather.model.Day;
import ru.spbpu.weather.model.RequestHistoryEntity;
import ru.spbpu.weather.model.User;
import ru.spbpu.weather.model.Weather;
import ru.spbpu.weather.repository.DayRepository;
import ru.spbpu.weather.repository.RequestRepository;
import ru.spbpu.weather.repository.WeatherRepository;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private WeatherRepository weatherRepository;

    @Mock
    private DayRepository dayRepository;

    private RequestService requestService;

    @BeforeEach
    void setUp() {
        requestService = new RequestService(requestRepository, weatherRepository, dayRepository);
    }

    @Test
    void save_ShouldSaveRequestWeatherAndDays() {
        RequestHistoryEntity request = new RequestHistoryEntity();
        Weather weather = new Weather();
        List<Day> forecast = List.of(new Day(), new Day());

        requestService.save(request, weather, forecast);

        verify(requestRepository).save(request);
        verify(weatherRepository).save(weather);
        verify(dayRepository).saveAll(forecast);
    }

    @Test
    void save_ShouldSetRequestInWeather() {
        RequestHistoryEntity request = new RequestHistoryEntity();
        Weather weather = new Weather();
        List<Day> forecast = Collections.emptyList();

        requestService.save(request, weather, forecast);

        assertEquals(request, weather.getRequest());
    }

    @Test
    void save_WithEmptyForecast_ShouldSaveEmptyList() {
        RequestHistoryEntity request = new RequestHistoryEntity();
        Weather weather = new Weather();

        requestService.save(request, weather, Collections.emptyList());

        verify(dayRepository).saveAll(Collections.emptyList());
    }

    @Test
    void findAll_ShouldReturnAllRequests() {
        List<RequestHistoryEntity> expected = List.of(new RequestHistoryEntity());
        when(requestRepository.findAll()).thenReturn(expected);

        List<RequestHistoryEntity> result = requestService.findAll();

        assertEquals(expected, result);
        verify(requestRepository).findAll();
    }

    @Test
    void findAll_WhenNoRequests_ShouldReturnEmptyList() {
        when(requestRepository.findAll()).thenReturn(Collections.emptyList());

        List<RequestHistoryEntity> result = requestService.findAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void findCurrentUserRequests_ShouldReturnUserRequests() {
        User user = new User("user", "pass");
        List<RequestHistoryEntity> expected = List.of(new RequestHistoryEntity());
        when(requestRepository.findRequestHistoryEntitiesByUser(user)).thenReturn(expected);

        List<RequestHistoryEntity> result = requestService.findCurrentUserRequests(user);

        assertEquals(expected, result);
        verify(requestRepository).findRequestHistoryEntitiesByUser(user);
    }

    @Test
    void findCurrentUserRequests_WithNullUser_ShouldThrowException() {
        assertEquals(Collections.emptyList(),
                requestService.findCurrentUserRequests(null));
    }
}