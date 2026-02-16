package ru.spbpu.weather.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ru.spbpu.weather.UnitTestConfig;
import ru.spbpu.weather.dto.RequestDto;
import ru.spbpu.weather.model.RequestHistoryEntity;
import ru.spbpu.weather.model.User;
import ru.spbpu.weather.security.SecurityConfig;
import ru.spbpu.weather.service.RequestService;
import ru.spbpu.weather.service.UserService;
import ru.spbpu.weather.util.WeatherMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HistoryController.class)
@Import({UnitTestConfig.class, SecurityConfig.class})
@AutoConfigureMockMvc
class HistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RequestService requestService;

    @MockBean
    private UserService userService;

    @MockBean
    private WeatherMapper weatherMapper;

    @Test
    @WithMockUser(username = "testuser")
    void getHistory_WhenUserIsLoggedIn_ShouldReturnHistoryView() throws Exception {
        User user = new User("testuser", "password");
        when(userService.getCurrentUser()).thenReturn(Optional.of(user));
        when(requestService.findCurrentUserRequests(user)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/history"))
                .andExpect(status().isOk())
                .andExpect(view().name("history"))
                .andExpect(model().attributeExists("history"));
    }

    @Test
    @WithMockUser
    void getHistory_WhenUserServiceReturnsEmptyOptional_ShouldReturnEmptyList() throws Exception {
        when(userService.getCurrentUser()).thenReturn(Optional.empty());

        mockMvc.perform(get("/history"))
                .andExpect(model().attribute("history", hasSize(0)));
    }

    @Test
    @WithMockUser
    void getHistory_WhenRequestServiceReturnsEmptyList_ShouldReturnEmptyList() throws Exception {
        User user = new User("user", "pass");
        when(userService.getCurrentUser()).thenReturn(Optional.of(user));
        when(requestService.findCurrentUserRequests(user)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/history"))
                .andExpect(model().attribute("history", hasSize(0)));
    }

    @Test
    void getHistory_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/history"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser
    void getHistory_ModelShouldContainCorrectAttributeName() throws Exception {
        when(userService.getCurrentUser()).thenReturn(Optional.empty());

        mockMvc.perform(get("/history"))
                .andExpect(model().attributeExists("history"))
                .andExpect(model().attribute("history", instanceOf(List.class)));
    }

    @Test
    @WithMockUser
    void getHistory_ViewNameShouldBeCorrect() throws Exception {
        when(userService.getCurrentUser()).thenReturn(Optional.empty());

        mockMvc.perform(get("/history"))
                .andExpect(view().name("history"));
    }

    @Test
    @WithMockUser
    void getHistory_ShouldCallUserServiceGetCurrentUser() throws Exception {
        mockMvc.perform(get("/history"));

        verify(userService, times(1)).getCurrentUser();
    }

    @Test
    @WithMockUser
    void getHistory_ShouldCallRequestServiceFindCurrentUserRequests_WhenUserExists() throws Exception {
        User user = new User("user", "pass");
        when(userService.getCurrentUser()).thenReturn(Optional.of(user));

        mockMvc.perform(get("/history"));

        verify(requestService, times(1)).findCurrentUserRequests(user);
    }

    @Test
    @WithMockUser
    void getHistory_ShouldNotCallRequestService_WhenUserNotExists() throws Exception {
        when(userService.getCurrentUser()).thenReturn(Optional.empty());

        mockMvc.perform(get("/history"));

        verify(requestService, never()).findCurrentUserRequests(any());
    }
}
