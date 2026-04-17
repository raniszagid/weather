package ru.spbpu.weather.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.spbpu.weather.service.TemperatureConversionService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/temp-converter")
public class TemperatureConverterController {

    private final TemperatureConversionService temperatureConversionService;

    @GetMapping
    public String showConverterPage(Model model) {
        model.addAttribute("celsiusValue", "");
        model.addAttribute("fahrenheitValue", "");
        return "temperature-converter";
    }

    @PostMapping("/to-fahrenheit")
    public String convertCelsiusToFahrenheit(@RequestParam("celsius") String celsius, Model model) {
        if (temperatureConversionService.isValidTemperature(celsius)) {
            double celsiusValue = Double.parseDouble(celsius.trim());
            double fahrenheitValue = temperatureConversionService.celsiusToFahrenheit(celsiusValue);
            model.addAttribute("celsiusValue", celsiusValue);
            model.addAttribute("fahrenheitValue", fahrenheitValue);
            model.addAttribute("result", celsiusValue + " °C = " + fahrenheitValue + " °F");
        } else {
            model.addAttribute("error", "Пожалуйста, введите корректное число");
            model.addAttribute("celsiusValue", celsius);
            model.addAttribute("fahrenheitValue", "");
        }
        return "temperature-converter";
    }

    @PostMapping("/to-celsius")
    public String convertFahrenheitToCelsius(@RequestParam("fahrenheit") String fahrenheit, Model model) {
        if (temperatureConversionService.isValidTemperature(fahrenheit)) {
            double fahrenheitValue = Double.parseDouble(fahrenheit.trim());
            double celsiusValue = temperatureConversionService.fahrenheitToCelsius(fahrenheitValue);
            model.addAttribute("fahrenheitValue", fahrenheitValue);
            model.addAttribute("celsiusValue", celsiusValue);
            model.addAttribute("result", fahrenheitValue + " °F = " + celsiusValue + " °C");
        } else {
            model.addAttribute("error", "Пожалуйста, введите корректное число");
            model.addAttribute("fahrenheitValue", fahrenheit);
            model.addAttribute("celsiusValue", "");
        }
        return "temperature-converter";
    }
}