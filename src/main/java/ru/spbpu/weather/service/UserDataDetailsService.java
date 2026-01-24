package ru.spbpu.weather.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.spbpu.weather.model.User;
import ru.spbpu.weather.repository.UserRepository;
import ru.spbpu.weather.security.UserDataDetails;

@RequiredArgsConstructor
@Service
public class UserDataDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(s)
                .orElseThrow(() -> new UsernameNotFoundException("User '%s' not found".formatted(s)));

        return new UserDataDetails(user);
    }

    public boolean isExist(String s) {
        return userRepository.findByUsername(s).isPresent();
    }
}
