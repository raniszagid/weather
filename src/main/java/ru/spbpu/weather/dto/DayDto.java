package ru.spbpu.weather.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayDto {
    @JsonProperty("day")
    private String day;

    @JsonProperty("temperature")
    private String temperature;

    @JsonProperty("wind")
    private String wind;

    @Override
    public String toString() {
        return "{" +
                "day='" + day + '\'' +
                ", temperature='" + temperature + '\'' +
                ", wind='" + wind + '\'' +
                '}';
    }
}