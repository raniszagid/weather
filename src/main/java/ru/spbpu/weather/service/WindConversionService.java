package ru.spbpu.weather.service;

import org.springframework.stereotype.Service;

@Service
public class WindConversionService {

    private static final double KMH_TO_MS = 3.6;

    /**
     * Конвертирует скорость ветра из км/ч в м/с
     * @param kmh скорость в километрах в час
     * @return скорость в метрах в секунду (округлено до 1 знака)
     */
    public double convertKmhToMs(double kmh) {
        return Math.round((kmh / KMH_TO_MS) * 10) / 10.0;
    }

    /**
     * Конвертирует скорость ветра из м/с в км/ч
     * @param ms скорость в метрах в секунду
     * @return скорость в километрах в час (округлено до 1 знака)
     */
    public double convertMsToKmh(double ms) {
        return Math.round((ms * KMH_TO_MS) * 10) / 10.0;
    }

    /**
     * Валидирует введенное значение
     * @param value строка с числом
     * @return true если значение валидно
     */
    public boolean isValidWindSpeed(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        try {
            double num = Double.parseDouble(value.trim());
            return num >= 0 && num <= 500; // разумные пределы
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
