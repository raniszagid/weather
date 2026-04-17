package ru.spbpu.weather.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.spbpu.weather.service.WeatherDictionaryService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/dictionary")
public class WeatherDictionaryController {

    private final WeatherDictionaryService dictionaryService;

    @GetMapping
    public String showDictionary(
            Model model,
            @RequestParam(required = false, defaultValue = "") String search) {

        var terms = dictionaryService.searchByPrefix(search);

        model.addAttribute("terms", terms);
        model.addAttribute("searchQuery", search);
        model.addAttribute("totalCount", terms.size());

        return "dictionary";
    }
}