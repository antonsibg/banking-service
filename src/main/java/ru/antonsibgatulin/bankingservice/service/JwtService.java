package ru.antonsibgatulin.bankingservice.service;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;
import ru.antonsibgatulin.bankingservice.dto.user.response.UserDto;

import java.security.Key;
import java.util.List;

public interface JwtService {
    String generateToken(UserDetails userDetails);
    boolean validateToken(String token);
    boolean isTokenExpired(String token, String secretKey);
    Claims getClaims(String token, String secretKey);
    String extractUsername(String token);
}
