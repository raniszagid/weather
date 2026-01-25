package ru.spbpu.weather.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.spbpu.weather.dto.DayDto;
import ru.spbpu.weather.dto.RequestDto;
import ru.spbpu.weather.dto.WeatherDto;
import ru.spbpu.weather.model.Day;
import ru.spbpu.weather.model.RequestHistoryEntity;
import ru.spbpu.weather.model.Weather;
import ru.spbpu.weather.repository.DayRepository;
import ru.spbpu.weather.repository.WeatherRepository;
import ru.spbpu.weather.util.WeatherMapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherMapperTest {

    @Mock
    private WeatherRepository weatherRepository;

    @Mock
    private DayRepository dayRepository;

    private WeatherMapper weatherMapper;

    @BeforeEach
    void setUp() {
        weatherMapper = new WeatherMapper(weatherRepository, dayRepository);
    }

    @Test
    void toWeatherEntity_ShouldParsePositiveTemperature() {
        WeatherDto dto = WeatherDto.builder()
                .temperature("+15 °C")
                .wind("20 km/h")
                .description("Sunny")
                .build();

        Weather result = weatherMapper.toWeatherEntity(dto);

        assertEquals(15, result.getTemperature());
        assertEquals(20, result.getWind());
        assertEquals("Sunny", result.getDescription());
    }

    @Test
    void toWeatherEntity_ShouldParseNegativeTemperature() {
        WeatherDto dto = WeatherDto.builder()
                .temperature("-10 °C")
                .wind("5 km/h")
                .build();

        Weather result = weatherMapper.toWeatherEntity(dto);

        assertEquals(-10, result.getTemperature());
        assertEquals(5, result.getWind());
    }

    @Test
    void toWeatherEntity_ShouldParseTemperatureWithoutSign() {
        WeatherDto dto = WeatherDto.builder()
                .temperature("0 °C")
                .wind("0 km/h")
                .build();

        Weather result = weatherMapper.toWeatherEntity(dto);

        assertEquals(0, result.getTemperature());
        assertEquals(0, result.getWind());
    }

    @Test
    void toWeatherEntity_ShouldHandleInvalidTemperatureFormat() {
        WeatherDto dto = WeatherDto.builder()
                .temperature("invalid")
                .wind("10 km/h")
                .build();

        assertThrows(NumberFormatException.class, () ->
                weatherMapper.toWeatherEntity(dto));
    }

    @Test
    void toWeatherEntity_ShouldHandleInvalidWindFormat() {
        WeatherDto dto = WeatherDto.builder()
                .temperature("10 °C")
                .wind("invalid")
                .build();

        assertThrows(NumberFormatException.class, () ->
                weatherMapper.toWeatherEntity(dto));
    }

    @Test
    void getForecast_ShouldConvertDayDtosToDayEntities() {
        DayDto dayDto = DayDto.builder()
                .day("1")
                .temperature("+10 °C")
                .wind("15 km/h")
                .build();
        WeatherDto weatherDto = WeatherDto.builder()
                .forecast(List.of(dayDto))
                .build();
        Weather weather = new Weather();

        List<Day> result = weatherMapper.getForecast(weatherDto, weather);

        assertEquals(1, result.size());
        Day day = result.get(0);
        assertEquals(10, day.getTemperature());
        assertEquals(15, day.getWind());
        assertEquals(1, day.getDate());
        assertEquals(weather, day.getWeather());
    }

    @Test
    void getForecast_ShouldFilterNullDays() {
        DayDto validDay = DayDto.builder()
                .day("1")
                .temperature("10 °C")
                .wind("10 km/h")
                .build();
        DayDto invalidDay = DayDto.builder()
                .day("2")
                .temperature("invalid")
                .wind("10 km/h")
                .build();
        WeatherDto weatherDto = WeatherDto.builder()
                .forecast(List.of(validDay, invalidDay))
                .build();
        Weather weather = new Weather();

        List<Day> result = weatherMapper.getForecast(weatherDto, weather);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getDate());
    }

    @Test
    void getForecast_WithEmptyForecast_ShouldReturnEmptyList() {
        WeatherDto weatherDto = WeatherDto.builder()
                .forecast(Collections.emptyList())
                .build();
        Weather weather = new Weather();

        List<Day> result = weatherMapper.getForecast(weatherDto, weather);

        assertTrue(result.isEmpty());
    }

    @Test
    void getForecast_WithNullForecast_ShouldReturnEmptyList() {
        WeatherDto weatherDto = WeatherDto.builder()
                .forecast(null)
                .build();
        Weather weather = new Weather();

        List<Day> result = weatherMapper.getForecast(weatherDto, weather);

        assertTrue(result.isEmpty());
    }

    @Test
    void toWeatherDto_ShouldConvertEntityToDto() {
        Weather weather = new Weather();
        weather.setId(1);
        weather.setTemperature(20);
        weather.setWind(15);
        weather.setDescription("Cloudy");

        Day day = new Day();
        day.setDate(1);
        day.setTemperature(18);
        day.setWind(12);

        when(dayRepository.findDaysByWeatherId(1)).thenReturn(List.of(day));

        WeatherDto result = weatherMapper.toWeatherDto(weather);

        assertEquals("+20 °C", result.getTemperature());
        assertEquals("15 km/h", result.getWind());
        assertEquals("Cloudy", result.getDescription());
        assertEquals(1, result.getForecast().size());
        assertEquals("1", result.getForecast().get(0).getDay());
        assertEquals("+18 °C", result.getForecast().get(0).getTemperature());
        assertEquals("12 km/h", result.getForecast().get(0).getWind());
    }

    @Test
    void toWeatherDto_WithZeroTemperature_ShouldNotAddSign() {
        Weather weather = new Weather();
        weather.setTemperature(0);
        weather.setWind(0);
        when(dayRepository.findDaysByWeatherId(anyInt())).thenReturn(Collections.emptyList());

        WeatherDto result = weatherMapper.toWeatherDto(weather);

        assertEquals("0 °C", result.getTemperature());
    }

    @Test
    void toWeatherDto_WithNegativeTemperature_ShouldAddMinusSign() {
        Weather weather = new Weather();
        weather.setTemperature(-5);
        weather.setWind(10);
        when(dayRepository.findDaysByWeatherId(anyInt())).thenReturn(Collections.emptyList());

        WeatherDto result = weatherMapper.toWeatherDto(weather);

        assertEquals("-5 °C", result.getTemperature());
    }

    @Test
    void toWeatherDto_WithNoDays_ShouldReturnEmptyForecast() {
        Weather weather = new Weather();
        when(dayRepository.findDaysByWeatherId(anyInt())).thenReturn(Collections.emptyList());

        WeatherDto result = weatherMapper.toWeatherDto(weather);

        assertTrue(result.getForecast().isEmpty());
    }

    @Test
    void toRequestDto_WhenWeatherNotFound_ShouldReturnDtoWithoutResult() {
        RequestHistoryEntity entity = new RequestHistoryEntity();
        entity.setId(999);

        when(weatherRepository.findWeatherByRequestId(999)).thenReturn(Optional.empty());

        RequestDto result = weatherMapper.toRequestDto(entity);

        assertNull(result.getResult());
    }

    @Test
    void temperatureSymbol_PositiveValue_ShouldReturnPlus() {
        String result = weatherMapper.temperatureSymbol(10);
        assertEquals("+", result);
    }

    @Test
    void temperatureSymbol_NegativeValue_ShouldReturnMinus() {
        String result = weatherMapper.temperatureSymbol(-5);
        assertEquals("", result);
    }

    @Test
    void temperatureSymbol_ZeroValue_ShouldReturnEmptyString() {
        String result = weatherMapper.temperatureSymbol(0);
        assertEquals("", result);
    }

    @Test
    void toDayEntity_WithValidData_ShouldCreateDayEntity() {
        DayDto dto = DayDto.builder()
                .day("3")
                .temperature("+25 °C")
                .wind("30 km/h")
                .build();
        Weather weather = new Weather();

        Day result = weatherMapper.toDayEntity(dto, weather);

        assertNotNull(result);
        assertEquals(3, result.getDate());
        assertEquals(25, result.getTemperature());
        assertEquals(30, result.getWind());
        assertEquals(weather, result.getWeather());
    }

    @Test
    void toDayEntity_WithNegativeTemperature_ShouldParseCorrectly() {
        DayDto dto = DayDto.builder()
                .day("4")
                .temperature("-10 °C")
                .wind("15 km/h")
                .build();

        Day result = weatherMapper.toDayEntity(dto, new Weather());

        assertEquals(-10, result.getTemperature());
    }

    @Test
    void toDayEntity_WithInvalidTemperatureFormat_ShouldReturnNull() {
        DayDto dto = DayDto.builder()
                .day("5")
                .temperature("invalid temperature")
                .wind("10 km/h")
                .build();

        Day result = weatherMapper.toDayEntity(dto, new Weather());

        assertNull(result);
    }

    @Test
    void toDayEntity_WithInvalidWindFormat_ShouldReturnNull() {
        DayDto dto = DayDto.builder()
                .day("6")
                .temperature("20 °C")
                .wind("invalid wind")
                .build();

        Day result = weatherMapper.toDayEntity(dto, new Weather());

        assertNull(result);
    }

    @Test
    void toDayEntity_WithInvalidDayFormat_ShouldThrowException() {
        DayDto dto = DayDto.builder()
                .day("not-a-number")
                .temperature("20 °C")
                .wind("10 km/h")
                .build();

        assertThrows(NumberFormatException.class, () ->
                weatherMapper.toDayEntity(dto, new Weather()));
    }

    @Test
    void toDayDto_ShouldConvertDayEntityToDto() {
        Day entity = new Day();
        entity.setDate(7);
        entity.setTemperature(-5);
        entity.setWind(25);

        DayDto result = weatherMapper.toDayDto(entity);

        assertEquals("7", result.getDay());
        assertEquals("-5 °C", result.getTemperature());
        assertEquals("25 km/h", result.getWind());
    }

    @Test
    void toDayDto_WithZeroValues_ShouldFormatCorrectly() {
        Day entity = new Day();
        entity.setDate(0);
        entity.setTemperature(0);
        entity.setWind(0);

        DayDto result = weatherMapper.toDayDto(entity);

        assertEquals("0", result.getDay());
        assertEquals("0 °C", result.getTemperature());
        assertEquals("0 km/h", result.getWind());
    }

    @Test
    void testMapping() {

        DayDto day1 = DayDto.builder()
                .day("1")
                .wind("15 km/h")
                .temperature("+1 °C")
                .build();
        DayDto day2 = DayDto.builder()
                .day("2")
                .temperature("15 °C")
                .wind("0 km/h")
                .build();

        WeatherDto weatherDto = WeatherDto.builder()
                .temperature("-12 °C")
                .wind("20 km/h")
                .forecast(List.of(day1, day2))
                .build();

        WeatherMapper weatherMapper = new WeatherMapper(null, null);
        Weather entity = weatherMapper.toWeatherEntity(weatherDto);

        assertEquals(-12, entity.getTemperature());
        assertEquals(20, entity.getWind());
    }
}
