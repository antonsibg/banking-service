package ru.antonsibgatulin.bankingservice.service.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.antonsibgatulin.bankingservice.service.SearchService;
import ru.antonsibgatulin.bankingservice.dto.user.response.UserDto;
import ru.antonsibgatulin.bankingservice.entity.user.User;
import ru.antonsibgatulin.bankingservice.mapper.UserMapper;

import ru.antonsibgatulin.bankingservice.repository.UserRepository;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

@Service
@AllArgsConstructor
public class SearchServiceImpl implements SearchService {


    private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);


    private final UserRepository userRepository;

    private final UserMapper userMapper;

    @Override
    public List<UserDto> searchUsers(String dateOfBirth, String phone, String fullName,

                                     String email, int page, int size, String sortBy, String sortOrder) {

        logger.debug("Searching users with dateOfBirth={}, phone={}, fullName={}, email={}, page={}, size={}, sortBy={}, sortOrder={}",
                dateOfBirth, phone, fullName, email, page, size, sortBy, sortOrder);

        Pageable pageable = getPageable(page, size, sortBy, sortOrder);

        Specification<User> specification = Specification.where(null);

        if (!StringUtils.isEmpty(dateOfBirth)) {
            LocalDate searchDateOfBirth = LocalDate.parse(dateOfBirth);
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThan(root.get("birthDay"), searchDateOfBirth));
        }


        if (!StringUtils.isEmpty(phone)) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(root.get("phoneNumber"), "%" + phone + "%"));
        }

        if (!StringUtils.isEmpty(fullName)) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.or(
                            criteriaBuilder.like(root.get("firstname"), fullName + "%"),
                            criteriaBuilder.like(root.get("lastname"), fullName + "%"),
                            criteriaBuilder.like(root.get("secondname"), fullName + "%")
                    ));
        }


        if (!StringUtils.isEmpty(email)) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("email"), email));
        }


        var list = userRepository.findAll(specification, pageable);
        logger.debug("Found {} users", list.size());
        return userMapper.fromUserListToUserDtoList(list);
    }


    private Pageable getPageable(int page, int size, String sortBy, String sortOrder) {
        Sort sort = Sort.by(sortBy).ascending();
        if (sortOrder.equalsIgnoreCase("desc")) {
            sort = sort.descending();
        }
        return PageRequest.of(page, size, sort);
    }

}