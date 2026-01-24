package ru.spbpu.weather.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.spbpu.weather.model.User;
import ru.spbpu.weather.service.RequestService;
import ru.spbpu.weather.service.UserService;
import ru.spbpu.weather.util.WeatherMapper;
import java.util.Collections;
import java.util.Optional;

@RequiredArgsConstructor
@Controller
@RequestMapping("/history")
public class HistoryController {
    private final RequestService requestService;
    private final UserService userService;
    private final WeatherMapper mapper;
    @GetMapping
    public String getHistory(Model model) {
        Optional<User> optionalUser = userService.getCurrentUser();
        model.addAttribute("history",
                optionalUser.map(user -> requestService.findCurrentUserRequests(user).stream()
                        .map(mapper::toRequestDto).toList()).orElse(Collections.emptyList()));
        return "history";
    }
}
