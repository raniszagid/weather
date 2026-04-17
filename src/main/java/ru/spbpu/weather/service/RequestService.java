package ru.spbpu.weather.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.spbpu.weather.model.Day;
import ru.spbpu.weather.model.RequestHistoryEntity;
import ru.spbpu.weather.model.User;
import ru.spbpu.weather.model.Weather;
import ru.spbpu.weather.repository.DayRepository;
import ru.spbpu.weather.repository.RequestRepository;
import ru.spbpu.weather.repository.WeatherRepository;

import java.util.List;

@RequiredArgsConstructor
@Service
public class RequestService {
    private final RequestRepository requestRepository;
    private final WeatherRepository weatherRepository;
    private final DayRepository dayRepository;

    public void save(RequestHistoryEntity request, Weather weather, List<Day> forecast) {
        weather.setRequest(request);
        requestRepository.save(request);
        weatherRepository.save(weather);
        dayRepository.saveAll(forecast);
    }

    public List<RequestHistoryEntity> findAll() {
        return requestRepository.findAll();
    }

    public List<RequestHistoryEntity> findCurrentUserRequests(User user) {
        return requestRepository.findRequestHistoryEntitiesByUser(user);
    }

    // Новые методы для сортировки
    public List<RequestHistoryEntity> findByUserOrderByCityAsc(User user) {
        return requestRepository.findByUserOrderByAddressAsc(user);
    }

    public List<RequestHistoryEntity> findByUserOrderByCityDesc(User user) {
        return requestRepository.findByUserOrderByAddressDesc(user);
    }

    public List<RequestHistoryEntity> findByUserOrderByTimestampAsc(User user) {
        return requestRepository.findByUserOrderByTimestampAsc(user);
    }

    public List<RequestHistoryEntity> findByUserOrderByTimestampDesc(User user) {
        return requestRepository.findByUserOrderByTimestampDesc(user);
    }
}