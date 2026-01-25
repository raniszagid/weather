package ru.spbpu.weather.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.validation.BindingResult;
import ru.spbpu.weather.UnitTestConfig;
import ru.spbpu.weather.model.User;
import ru.spbpu.weather.security.SecurityConfig;
import ru.spbpu.weather.service.RegistrationService;
import ru.spbpu.weather.util.UserValidator;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.any;

@WebMvcTest(AuthController.class)
@Import({UnitTestConfig.class, SecurityConfig.class})
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserValidator userValidator;

    @MockBean
    private RegistrationService registrationService;

    // Тесты для GET запросов
    @Test
    void loginPage_ShouldReturnLoginView() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void registrationPage_ShouldReturnRegistrationView() throws Exception {
        mockMvc.perform(get("/auth/registration"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/registration"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    void registrationPage_ShouldHaveEmptyUserInModel() throws Exception {
        mockMvc.perform(get("/auth/registration"))
                .andExpect(model().attribute("user", hasProperty("username", nullValue())))
                .andExpect(model().attribute("user", hasProperty("password", nullValue())));
    }

    // Тесты для POST регистрации (валидные данные)
    @Test
    void performRegistration_WithValidUser_ShouldRedirectToLogin() throws Exception {
        doNothing().when(userValidator).validate(any(User.class), any(BindingResult.class));
        doNothing().when(registrationService).register(any(User.class));

        mockMvc.perform(post("/auth/registration")
                        .with(csrf())
                        .param("username", "testuser")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));

        verify(registrationService, times(1)).register(any(User.class));
    }

    @Test
    void performRegistration_ShouldCaptureCorrectUsername() throws Exception {
        doNothing().when(userValidator).validate(any(User.class), any(BindingResult.class));
        doNothing().when(registrationService).register(any(User.class));

        mockMvc.perform(post("/auth/registration")
                .with(csrf())
                .param("username", "john_doe")
                .param("password", "securePass"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(registrationService).register(captor.capture());
        assertEquals("john_doe", captor.getValue().getUsername());
    }

    @Test
    void performRegistration_ShouldCaptureCorrectPassword() throws Exception {
        doNothing().when(userValidator).validate(any(User.class), any(BindingResult.class));
        doNothing().when(registrationService).register(any(User.class));

        mockMvc.perform(post("/auth/registration")
                .with(csrf())
                .param("username", "jane_doe")
                .param("password", "anotherPass"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(registrationService).register(captor.capture());
        assertEquals("anotherPass", captor.getValue().getPassword());
    }

    // Тесты для POST регистрации (ошибки валидации)
    @Test
    void performRegistration_WithValidationErrors_ShouldReturnRegistrationPage() throws Exception {
        doAnswer(invocation -> {
            BindingResult bindingResult = invocation.getArgument(1);
            bindingResult.rejectValue("username", "error", "User already exists");
            return null;
        }).when(userValidator).validate(any(User.class), any(BindingResult.class));

        mockMvc.perform(post("/auth/registration")
                        .with(csrf())
                        .param("username", "existing")
                        .param("password", "pass"))
                .andExpect(status().isOk())
                .andExpect(view().name("/auth/registration"));

        verify(registrationService, never()).register(any(User.class));
    }

    @Test
    void performRegistration_WithEmptyUsername_ShouldReturnError() throws Exception {
        mockMvc.perform(post("/auth/registration")
                        .with(csrf())
                        .param("username", "")
                        .param("password", "pass"))
                .andExpect(status().isOk())
                .andExpect(view().name("/auth/registration"));
    }

    @Test
    void performRegistration_WithNullUsername_ShouldReturnError() throws Exception {
        mockMvc.perform(post("/auth/registration")
                        .with(csrf())
                        .param("password", "pass"))
                .andExpect(status().isOk())
                .andExpect(view().name("/auth/registration"));
    }

    @Test
    void performRegistration_WithEmptyPassword_ShouldReturnError() throws Exception {
        mockMvc.perform(post("/auth/registration")
                        .with(csrf())
                        .param("username", "user")
                        .param("password", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/auth/login"));
    }

    @Test
    void performRegistration_WithoutCsrf_ShouldBeForbidden() throws Exception {
        mockMvc.perform(post("/auth/registration")
                        .param("username", "test")
                        .param("password", "test"))
                .andExpect(status().isForbidden());
    }

    // Тесты для CSRF токена
    @Test
    void loginPage_ShouldContainCsrfToken() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(content().string(containsString("_csrf")));
    }

    @Test
    void registrationPage_ShouldContainCsrfToken() throws Exception {
        mockMvc.perform(get("/auth/registration"))
                .andExpect(content().string(containsString("_csrf")));
    }

    // Тесты для логина
    @Test
    void loginForm_WithCorrectCredentials_ShouldRedirectToWeather() throws Exception {
        mockMvc.perform(formLogin("/process_login")
                        .user("user")
                        .password("password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/weather"));
    }

    @Test
    void loginForm_WithIncorrectPassword_ShouldRedirectWithError() throws Exception {
        mockMvc.perform(formLogin("/process_login")
                        .user("user")
                        .password("wrong"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?error"));
    }

    @Test
    void loginForm_WithNonExistentUser_ShouldRedirectWithError() throws Exception {
        mockMvc.perform(formLogin("/process_login")
                        .user("nonexistent")
                        .password("pass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?error"));
    }

    @Test
    void shouldRegisterNewUserAndRedirectToLoginPage() throws Exception {
        doNothing().when(userValidator).validate(any(User.class), any(BindingResult.class));
        doNothing().when(registrationService).register(any(User.class));
        String correct = "correct";
        MockHttpServletRequestBuilder request = post("/auth/registration")
                .with(csrf())
                .param("username", correct)
                .param("password", correct);
        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(registrationService).register(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals(correct, capturedUser.getUsername());
        assertEquals(correct, capturedUser.getPassword());
    }

    @Test
    void shouldRedirectAfterSuccessfulLogin() throws Exception {
        mockMvc.perform(formLogin("/process_login")
                        .user("user")
                        .password("password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/weather"));
    }

    @Test
    void shouldRedirectToLoginPageAfterUnsuccessfulLogin() throws Exception {
        mockMvc.perform(formLogin("/process_login")
                        .user("incorrect")
                        .password("incorrect"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?error"));
    }
}
