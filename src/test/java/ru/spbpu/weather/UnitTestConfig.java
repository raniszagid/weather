package ru.spbpu.weather;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import ru.spbpu.weather.service.RegistrationService;
import ru.spbpu.weather.util.UserValidator;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class UnitTestConfig {

	@Bean
	public RegistrationService registrationService() {
		return mock(RegistrationService.class);
	}

	@Bean
	public UserValidator userValidator() {
		return mock(UserValidator.class);
	}

	@Bean
	@Primary
	public UserDetailsService userDetailsService() {
		UserDetails user = User.builder()
						.username("user")
						.password("{noop}password")
						.roles("USER")
						.build();
		return new InMemoryUserDetailsManager(user);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
