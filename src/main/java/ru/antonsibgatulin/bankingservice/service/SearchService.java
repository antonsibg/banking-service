package ru.antonsibgatulin.bankingservice.service;

import ru.antonsibgatulin.bankingservice.dto.user.response.UserDto;

import java.util.List;

public interface SearchService {
    List<UserDto> searchUsers(String dateOfBirth, String phone, String fullName, String email, int page, int size, String sortBy, String sortOrder);
}
