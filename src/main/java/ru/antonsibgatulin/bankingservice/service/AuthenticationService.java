package ru.antonsibgatulin.bankingservice.service;

import ru.antonsibgatulin.bankingservice.dto.user.request.UserAuthenticationDto;
import ru.antonsibgatulin.bankingservice.dto.user.request.UserRegistrationDto;
import ru.antonsibgatulin.bankingservice.dto.user.response.UserAuthDto;

public interface AuthenticationService {
    UserAuthDto signIn(UserAuthenticationDto userAuthenticationDto);
    UserAuthDto singUp(UserRegistrationDto userRegistrationDto);
}
