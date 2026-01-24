package ru.spbpu.weather.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import ru.spbpu.weather.dto.WeatherDto;
import ru.spbpu.weather.model.RequestHistoryEntity;
import ru.spbpu.weather.model.User;
import ru.spbpu.weather.model.Weather;
import ru.spbpu.weather.service.ApiService;
import ru.spbpu.weather.service.RequestService;
import ru.spbpu.weather.service.UserService;
import ru.spbpu.weather.util.WeatherMapper;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/weather")
public class WeatherController {
    private final RequestService requestService;
    private final UserService userService;
    private final WeatherMapper weatherMapper;
    private final ApiService apiService;

    @GetMapping
    public String getIndex() {
        return "index";
    }

    @PostMapping
    public String makeSearch(Model model, @RequestParam("city") String city) {
        log.info("City: {}", city);
        RequestHistoryEntity request = new RequestHistoryEntity();
        request.setAddress(city);
        request.setRequestTimestamp(LocalDateTime.now());
        Optional<User> optionalUser = userService.getCurrentUser();
        optionalUser.ifPresent(request::setUser);
        WeatherDto weatherDto = apiService.makeRequest(city)
                .orElseThrow(() -> new NoSuchElementException("City not found"));
        log.info("Result: {}", weatherDto);
        Weather weather = weatherMapper.toWeatherEntity(weatherDto);
        requestService.save(request, weather, weatherMapper.getForecast(weatherDto, weather));
        model.addAttribute("result", weatherDto);

        return "index";
    }

    @ExceptionHandler
    public ModelAndView handleException(RuntimeException e) {
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorMessage", e.getMessage());
        mav.addObject("status", HttpStatus.BAD_REQUEST);
        mav.setStatus(HttpStatus.BAD_REQUEST);
        return mav;
    }
}
