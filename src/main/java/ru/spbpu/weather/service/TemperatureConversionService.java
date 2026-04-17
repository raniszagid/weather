package ru.spbpu.weather.service;

import org.springframework.stereotype.Service;

@Service
public class TemperatureConversionService {

    /**
     * Конвертирует температуру из Цельсия в Фаренгейт
     * @param celsius температура в градусах Цельсия
     * @return температура в градусах Фаренгейта (округлено до 1 знака)
     */
    public double celsiusToFahrenheit(double celsius) {
        return Math.round((celsius * 9.0 / 5.0 + 32) * 10) / 10.0;
    }

    /**
     * Конвертирует температуру из Фаренгейта в Цельсий
     * @param fahrenheit температура в градусах Фаренгейта
     * @return температура в градусах Цельсия (округлено до 1 знака)
     */
    public double fahrenheitToCelsius(double fahrenheit) {
        return Math.round(((fahrenheit - 32) * 5.0 / 9.0) * 10) / 10.0;
    }

    /**
     * Валидирует введенное значение
     * @param value строка с числом
     * @return true если значение валидно
     */
    public boolean isValidTemperature(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        try {
            double num = Double.parseDouble(value.trim());
            // Абсолютный ноль: -273.15°C = -459.67°F
            return num >= -500 && num <= 10000;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}