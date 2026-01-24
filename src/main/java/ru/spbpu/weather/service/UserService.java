package ru.spbpu.weather.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import ru.spbpu.weather.model.User;
import ru.spbpu.weather.repository.UserRepository;
import ru.spbpu.weather.security.UserDataDetails;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;

    public boolean loggedIn() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserDetails;
    }

    public Optional<User> getCurrentUser() {
        if (!loggedIn()) {
            return Optional.empty();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDataDetails userDataDetails) {
            return Optional.of(userDataDetails.getUser());
        } else if (principal instanceof org.springframework.security.core.userdetails.User springUser) {
            String username = springUser.getUsername();
            return userRepository.findByUsername(username);
        }

        return Optional.empty();
    }
}