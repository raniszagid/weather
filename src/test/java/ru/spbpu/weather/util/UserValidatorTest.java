package ru.spbpu.weather.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import ru.spbpu.weather.model.User;
import ru.spbpu.weather.service.UserDataDetailsService;
import ru.spbpu.weather.util.UserValidator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserValidatorTest {

    @Mock
    private UserDataDetailsService userDataDetailsService;

    private UserValidator userValidator;

    @BeforeEach
    void setUp() {
        userValidator = new UserValidator(userDataDetailsService);
    }

    @Test
    void supports_ShouldSupportUserClass() {
        assertTrue(userValidator.supports(User.class));
    }

    @Test
    void supports_ShouldNotSupportOtherClasses() {
        assertFalse(userValidator.supports(String.class));
        assertFalse(userValidator.supports(Object.class));
    }

    @Test
    void validate_WhenUserExists_ShouldAddError() {
        User user = new User("existing", "pass");
        Errors errors = new BeanPropertyBindingResult(user, "user");

        when(userDataDetailsService.isExist("existing")).thenReturn(true);

        userValidator.validate(user, errors);

        assertTrue(errors.hasErrors());
        assertEquals(1, errors.getErrorCount());
        assertNotNull(errors.getFieldError("username"));
    }

    @Test
    void validate_WhenUserDoesNotExist_ShouldNotAddError() {
        User user = new User("newuser", "pass");
        Errors errors = new BeanPropertyBindingResult(user, "user");

        when(userDataDetailsService.isExist("newuser")).thenReturn(false);

        userValidator.validate(user, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    void validate_WithNullUsername_ShouldNotCheckExistence() {
        User user = new User(null, "pass");
        Errors errors = new BeanPropertyBindingResult(user, "user");

        userValidator.validate(user, errors);

        verify(userDataDetailsService, never()).isExist(anyString());
    }

    @Test
    void validate_WhenServiceThrowsException_ShouldPropagate() {
        User user = new User("testuser", "password");
        Errors errors = new BeanPropertyBindingResult(user, "user");

        when(userDataDetailsService.isExist("testuser"))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () ->
                userValidator.validate(user, errors));
    }

    @Test
    void validate_MultipleCallsWithSameUser_ShouldCheckExistenceEachTime() {
        User user = new User("repeated", "password");
        Errors errors1 = new BeanPropertyBindingResult(user, "user");
        Errors errors2 = new BeanPropertyBindingResult(user, "user");

        when(userDataDetailsService.isExist("repeated")).thenReturn(false);

        userValidator.validate(user, errors1);
        userValidator.validate(user, errors2);

        verify(userDataDetailsService, times(2)).isExist("repeated");
    }
}
