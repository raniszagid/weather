package ru.spbpu.weather.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;


@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherDto {
    @JsonProperty("temperature")
    private String temperature;

    @JsonProperty("wind")
    private String wind;

    @JsonProperty("description")
    private String description;

    @JsonProperty("forecast")
    private List<DayDto> forecast;

    @Override
    public String toString() {
        return "{" +
                "temperature='" + temperature + '\'' +
                ", wind='" + wind + '\'' +
                ", description='" + description + '\'' +
                ", forecast=" + forecast +
                '}';
    }
}