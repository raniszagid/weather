package ru.spbpu.weather.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class AuthenticationDTO {
    @NotNull
    @NotEmpty
    private String username;
    private String password;
}