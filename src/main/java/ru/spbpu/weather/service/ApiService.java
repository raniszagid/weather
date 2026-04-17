package ru.spbpu.weather.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import ru.spbpu.weather.dto.WeatherDto;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

@Service
@NoArgsConstructor
public class ApiService {
    public Optional<WeatherDto> makeRequest(String city) {
        /*try (HttpClient httpClient = HttpClient.newHttpClient()) {
            String url = "http://goweather.xyz/weather/" + city;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                return Optional.empty();
            }

            String json = response.body();
            System.out.println(json);
            ObjectMapper objectMapper = new ObjectMapper();
            return Optional.of(objectMapper.readValue(json, WeatherDto.class));
        } catch (IOException e) {
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }*/
        return mockJson(city);
    }

    private Optional<WeatherDto> mockJson(String city) {
        int temp = (int) (System.currentTimeMillis() % 10 + 10);
        int wind1 = (int) (System.currentTimeMillis() % 25);
        int wind2 = (int) (System.currentTimeMillis() % 25);
        int wind3 = (int) (System.currentTimeMillis() % 25);
        int wind4 = (int) (System.currentTimeMillis() % 25);
        String json = """
                {"temperature":"%d °C","wind":"%d km/h","description":"Partly cloudy","forecast":[{"day":"1","temperature":"%d °C","wind":"%d km/h"},{"day":"2","temperature":"%d °C","wind":"%d km/h"},{"day":"3","temperature":"%d °C","wind":"%d km/h"}]}
                """.formatted(temp, wind1, temp+1, wind2, temp, wind3, temp+2, wind4);
        List<String> validCities = List.of("London", "Paris", "Rome", "Madrid", "Moscow", "Amsterdam", "Berlin", "Tokyo");
        if (validCities.contains(city)) {
            try {
                return Optional.of(new ObjectMapper().readValue(json, WeatherDto.class));
            } catch (Exception e) {
                return Optional.empty();
            }
        }
        else
            return Optional.empty();
    }
}

