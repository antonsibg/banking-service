package ru.antonsibgatulin.bankingservice.config.mapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.antonsibgatulin.bankingservice.mapper.impl.EmailAddressImpl;
import ru.antonsibgatulin.bankingservice.mapper.impl.PhoneNumberImpl;
import ru.antonsibgatulin.bankingservice.mapper.impl.UserMapperImpl;
import ru.antonsibgatulin.bankingservice.mapper.EmailAddressMapper;
import ru.antonsibgatulin.bankingservice.mapper.PhoneNumberMapper;
import ru.antonsibgatulin.bankingservice.mapper.UserMapper;


@Configuration
public class MapperConfigUser {
    @Bean
    public UserMapper userMapper() {
        return new UserMapperImpl();
    }
    @Bean
    public PhoneNumberMapper phoneNumberMapper() {
        return new PhoneNumberImpl();
    }
    @Bean
    public EmailAddressMapper emailAddressMapper() {
        return new EmailAddressImpl();
    }
}
