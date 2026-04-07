package ru.spbpu.weather.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import ru.spbpu.weather.model.User;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class UserDataDetailsTest {

    @Test
    void getUser_ShouldReturnProvidedUser() {
        User user = new User("testuser", "password");
        UserDataDetails details = new UserDataDetails(user);

        assertEquals(user, details.getUser());
    }

    @Test
    void getAuthorities_ShouldAlwaysReturnEmptyList() {
        UserDataDetails details = new UserDataDetails(new User("user", "pass"));

        Collection<? extends GrantedAuthority> authorities = details.getAuthorities();

        assertNotNull(authorities);
        assertTrue(authorities.isEmpty());
    }

    @Test
    void isAccountNonExpired_ShouldAlwaysReturnTrue() {
        UserDataDetails details = new UserDataDetails(new User("user", "pass"));

        assertTrue(details.isAccountNonExpired());
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.isCredentialsNonExpired());
        assertTrue(details.isEnabled());
    }
}