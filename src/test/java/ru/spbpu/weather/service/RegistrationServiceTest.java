package ru.spbpu.weather.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.spbpu.weather.model.User;
import ru.spbpu.weather.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(userRepository, passwordEncoder);
    }

    @Test
    void register_ShouldEncodePasswordBeforeSaving() {
        User user = new User("testuser", "plainpassword");
        when(passwordEncoder.encode("plainpassword")).thenReturn("encodedpassword");

        registrationService.register(user);

        assertEquals("encodedpassword", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void register_ShouldSaveUserWithEncodedPassword() {
        User user = new User("user", "pass");
        when(passwordEncoder.encode("pass")).thenReturn("encoded");

        registrationService.register(user);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        assertEquals("encoded", captor.getValue().getPassword());
        assertEquals("user", captor.getValue().getUsername());
    }

    @Test
    void register_ShouldCallPasswordEncoderOnce() {
        User user = new User("user", "pass");

        registrationService.register(user);

        verify(passwordEncoder, times(1)).encode("pass");
    }

    @Test
    void register_ShouldCallRepositorySaveOnce() {
        User user = new User("user", "pass");
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        registrationService.register(user);

        verify(userRepository, times(1)).save(user);
    }

    @Test
    void register_WithNullUser_ShouldThrowException() {
        assertThrows(NullPointerException.class, () ->
                registrationService.register(null));
    }

    @Test
    void register_WithNullPassword_ShouldEncodeNull() {
        User user = new User("testuser", null);
        when(passwordEncoder.encode(null)).thenReturn("encodedNull");

        registrationService.register(user);

        assertEquals("encodedNull", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void register_WithEmptyPassword_ShouldEncodeEmptyString() {
        User user = new User("testuser", "");
        when(passwordEncoder.encode("")).thenReturn("encodedEmpty");

        registrationService.register(user);

        assertEquals("encodedEmpty", user.getPassword());
    }
}