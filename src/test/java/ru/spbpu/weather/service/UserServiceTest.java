package ru.spbpu.weather.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import ru.spbpu.weather.model.User;
import ru.spbpu.weather.repository.UserRepository;
import ru.spbpu.weather.security.UserDataDetails;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void loggedIn_WhenAuthenticatedWithUserDetails_ShouldReturnTrue() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mock(UserDetails.class));

        assertTrue(userService.loggedIn());
    }

    @Test
    void loggedIn_WhenNotAuthenticated_ShouldReturnFalse() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        assertFalse(userService.loggedIn());
    }

    @Test
    void loggedIn_WhenAuthenticationIsNull_ShouldReturnFalse() {
        when(securityContext.getAuthentication()).thenReturn(null);

        assertFalse(userService.loggedIn());
    }

    @Test
    void loggedIn_WhenPrincipalIsNotUserDetails_ShouldReturnFalse() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("anonymous");

        assertFalse(userService.loggedIn());
    }

    @Test
    void getCurrentUser_WhenLoggedInWithUserDataDetails_ShouldReturnUser() {
        User user = new User("testuser", "pass");
        UserDataDetails userDetails = new UserDataDetails(user);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        Optional<User> result = userService.getCurrentUser();

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    void getCurrentUser_WhenLoggedInWithSpringUser_ShouldReturnUserFromRepository() {
        org.springframework.security.core.userdetails.User springUser =
                new org.springframework.security.core.userdetails.User(
                        "springuser", "pass", Collections.emptyList());
        User user = new User("springuser", "pass");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(springUser);
        when(userRepository.findByUsername("springuser")).thenReturn(Optional.of(user));

        Optional<User> result = userService.getCurrentUser();

        assertTrue(result.isPresent());
        assertEquals("springuser", result.get().getUsername());
    }

    @Test
    void getCurrentUser_WhenNotLoggedIn_ShouldReturnEmpty() {
        when(securityContext.getAuthentication()).thenReturn(null);

        Optional<User> result = userService.getCurrentUser();

        assertTrue(result.isEmpty());
    }

    @Test
    void getCurrentUser_WhenSpringUserNotFoundInRepository_ShouldReturnEmpty() {
        org.springframework.security.core.userdetails.User springUser =
                new org.springframework.security.core.userdetails.User(
                        "nonexistent", "pass", Collections.emptyList());

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(springUser);
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        Optional<User> result = userService.getCurrentUser();

        assertTrue(result.isEmpty());
    }
}
