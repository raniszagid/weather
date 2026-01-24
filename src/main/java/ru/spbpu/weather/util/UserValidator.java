package ru.spbpu.weather.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import ru.spbpu.weather.model.User;
import ru.spbpu.weather.service.UserDataDetailsService;

@Component
@RequiredArgsConstructor
public class UserValidator implements Validator {
    private final UserDataDetailsService service;

    @Override
    public boolean supports(Class<?> clazz) {
        return User.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        User person = (User) target;
        if (service.isExist(person.getUsername())) {
            errors.rejectValue("username", "",
                    "Person with current username already exists");
        }
    }
}
