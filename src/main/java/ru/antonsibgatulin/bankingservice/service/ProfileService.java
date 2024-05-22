package ru.antonsibgatulin.bankingservice.service;

import org.springframework.security.core.Authentication;
import ru.antonsibgatulin.bankingservice.dto.user.request.*;

public interface ProfileService {
    void updateProfile(Authentication authentication, UserSetUpProfileDto userDto);
    void addPhoneNumber(Authentication authentication, ProfileAddPhoneDto profileAddPhoneDto);
    void addEmailAddress(Authentication authentication, ProfileAddEmailDto profileAddEmailDto);
    void deletePhone(Authentication authentication, Long phoneId);
    void deleteEmail(Authentication authentication, Long emailId);
    void changePhoneNumber(Authentication authentication, Long phoneId, ProfileChangePhoneDto profileChangePhoneDto);
    void changeEmailAddress(Authentication authentication, Long emailId, ProfileChangeEmailDto profileChangeEmailDto);

}
