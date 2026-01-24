package ru.spbpu.weather.dto;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
@Setter
@Builder
public class RequestDto {
    private String city;
    private LocalDateTime requestTimestamp;
    private WeatherDto result;

    public String getTimestamp() {
        return requestTimestamp.format(DateTimeFormatter.ofPattern("dd.MM HH:mm"));
    }

    public String getMainTemperature() {
        return result.getTemperature();
    }

    public String getMainWind() {
        return result.getWind();
    }

    public String getDescription() {
        return result.getDescription();
    }

    public List<DayDto> getForecast() {
        return result.getForecast();
    }
}
