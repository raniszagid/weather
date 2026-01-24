package ru.spbpu.weather.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.spbpu.weather.model.Day;

import java.util.List;

public interface DayRepository extends JpaRepository<Day, Integer> {
    List<Day> findDaysByWeatherId(int id);
}
