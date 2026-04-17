package ru.spbpu.weather.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class WeatherDictionaryDto {
    private String englishTerm;
    private String russianTranslation;
}