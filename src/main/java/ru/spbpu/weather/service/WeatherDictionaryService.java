package ru.spbpu.weather.service;

import org.springframework.stereotype.Service;
import ru.spbpu.weather.dto.WeatherDictionaryDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WeatherDictionaryService {

    // Словарь будет заполнен вами
    private final Map<String, String> weatherTerms;

    public WeatherDictionaryService() {
        this.weatherTerms = new HashMap<>();
        weatherTerms.putAll(Map.of(
                "sunny", "солнечно",
                "cloudy", "облачно",
                "rainy", "дождливо",
                "snowy", "снежно",
                "windy", "ветрено",
                "stormy", "штормово",
                "foggy", "туманно"));
        weatherTerms.putAll(Map.of(
                "clear", "ясно",
                "partly cloudy", "переменная облачность",
                "overcast", "пасмурно",
                "thunderstorm", "гроза",
                "shower", "ливень",
                "breeze", "бриз",
                "gale", "штормовой ветер"));
        weatherTerms.putAll(Map.of(
                "hurricane", "ураган",
                "humidity", "влажность",
                "precipitation", "осадки",
                "pressure", "давление"));

    }

    /**
     * Поиск терминов по первым буквам английского слова
     * @param prefix префикс для поиска (регистронезависимый)
     * @return список найденных терминов
     */
    public List<WeatherDictionaryDto> searchByPrefix(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return getAllTerms();
        }

        String searchPrefix = prefix.trim().toLowerCase();

        return weatherTerms.entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase().startsWith(searchPrefix))
                .map(entry -> WeatherDictionaryDto.builder()
                        .englishTerm(entry.getKey())
                        .russianTranslation(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Получить все термины
     */
    public List<WeatherDictionaryDto> getAllTerms() {
        return weatherTerms.entrySet().stream()
                .map(entry -> WeatherDictionaryDto.builder()
                        .englishTerm(entry.getKey())
                        .russianTranslation(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }
}