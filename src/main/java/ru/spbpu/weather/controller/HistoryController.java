package ru.spbpu.weather.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.spbpu.weather.model.RequestHistoryEntity;
import ru.spbpu.weather.model.User;
import ru.spbpu.weather.service.RequestService;
import ru.spbpu.weather.service.UserService;
import ru.spbpu.weather.util.WeatherMapper;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Controller
@RequestMapping("/history")
public class HistoryController {
    private final RequestService requestService;
    private final UserService userService;
    private final WeatherMapper mapper;
    private static final int PAGE_SIZE = 3; // Количество записей на странице

    @GetMapping
    public String getHistory(
            Model model,
            @RequestParam(required = false, defaultValue = "") String sortBy,
            @RequestParam(required = false, defaultValue = "") String order,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "1") int page) {

        Optional<User> optionalUser = userService.getCurrentUser();

        if (optionalUser.isEmpty()) {
            model.addAttribute("history", Collections.emptyList());
            model.addAttribute("currentSortBy", "timestamp");
            model.addAttribute("currentOrder", "desc");
            model.addAttribute("searchQuery", "");
            model.addAttribute("currentPage", 1);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0);
            return "history";
        }

        User user = optionalUser.get();

        // Получаем все запросы пользователя
        List<RequestHistoryEntity> allRequests;

        // Применяем сортировку (если параметры переданы)
        if (sortBy != null && !sortBy.isEmpty() && order != null && !order.isEmpty()) {
            switch (sortBy) {
                case "city":
                    if ("asc".equals(order)) {
                        allRequests = requestService.findByUserOrderByCityAsc(user);
                    } else {
                        allRequests = requestService.findByUserOrderByCityDesc(user);
                    }
                    break;
                case "timestamp":
                default:
                    if ("asc".equals(order)) {
                        allRequests = requestService.findByUserOrderByTimestampAsc(user);
                    } else {
                        allRequests = requestService.findByUserOrderByTimestampDesc(user);
                    }
                    break;
            }
        } else {
            // Без параметров сортировки - сортировка по умолчанию (новые сверху)
            allRequests = requestService.findCurrentUserRequests(user);
        }

        // Применяем фильтрацию по поиску (если есть)
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.trim().toLowerCase();
            allRequests = allRequests.stream()
                    .filter(r -> r.getAddress() != null &&
                            r.getAddress().toLowerCase().startsWith(searchLower))
                    .collect(Collectors.toList());
        }

        // Пагинация
        int totalElements = allRequests.size();
        int totalPages = (int) Math.ceil((double) totalElements / PAGE_SIZE);

        // Корректируем номер страницы
        int currentPage = page;
        if (currentPage < 1) currentPage = 1;
        if (currentPage > totalPages && totalPages > 0) currentPage = totalPages;

        // Вычисляем индексы для подсписка
        int startIndex = (currentPage - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalElements);

        List<RequestHistoryEntity> pagedRequests = Collections.emptyList();
        if (startIndex < totalElements) {
            pagedRequests = allRequests.subList(startIndex, endIndex);
        }

        model.addAttribute("history", pagedRequests.stream()
                .map(mapper::toRequestDto)
                .collect(Collectors.toList()));
        model.addAttribute("currentSortBy", sortBy != null && !sortBy.isEmpty() ? sortBy : "timestamp");
        model.addAttribute("currentOrder", order != null && !order.isEmpty() ? order : "desc");
        model.addAttribute("searchQuery", search != null ? search : "");
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", totalElements);
        model.addAttribute("pageSize", PAGE_SIZE);

        return "history";
    }
}
