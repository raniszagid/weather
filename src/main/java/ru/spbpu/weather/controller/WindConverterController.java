package ru.spbpu.weather.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.spbpu.weather.service.WindConversionService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/wind-converter")
public class WindConverterController {

    private final WindConversionService windConversionService;

    @GetMapping
    public String showConverterPage(Model model) {
        model.addAttribute("kmhValue", "");
        model.addAttribute("msValue", "");
        return "wind-converter";
    }

    @PostMapping("/to-ms")
    public String convertKmhToMs(@RequestParam("kmh") String kmh, Model model) {
        if (windConversionService.isValidWindSpeed(kmh)) {
            double kmhValue = Double.parseDouble(kmh.trim());
            double msValue = windConversionService.convertKmhToMs(kmhValue);
            model.addAttribute("kmhValue", kmhValue);
            model.addAttribute("msValue", msValue);
            model.addAttribute("result", kmhValue + " km/h = " + msValue + " m/s");
        } else {
            model.addAttribute("error", "Пожалуйста, введите корректное число от 0 до 500");
            model.addAttribute("kmhValue", kmh);
            model.addAttribute("msValue", "");
        }
        return "wind-converter";
    }

    @PostMapping("/to-kmh")
    public String convertMsToKmh(@RequestParam("ms") String ms, Model model) {
        if (windConversionService.isValidWindSpeed(ms)) {
            double msValue = Double.parseDouble(ms.trim());
            double kmhValue = windConversionService.convertMsToKmh(msValue);
            model.addAttribute("msValue", msValue);
            model.addAttribute("kmhValue", kmhValue);
            model.addAttribute("result", msValue + " m/s = " + kmhValue + " km/h");
        } else {
            model.addAttribute("error", "Пожалуйста, введите корректное число от 0 до 500");
            model.addAttribute("msValue", ms);
            model.addAttribute("kmhValue", "");
        }
        return "wind-converter";
    }
}