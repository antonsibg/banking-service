package ru.antonsibgatulin.bankingservice.service;

import org.springframework.security.core.Authentication;
import ru.antonsibgatulin.bankingservice.dto.transaction.request.TransactionDto;
import ru.antonsibgatulin.bankingservice.dto.transaction.request.TransferManagerDto;
import ru.antonsibgatulin.bankingservice.dto.transaction.response.TransactionStatusDto;
import ru.antonsibgatulin.bankingservice.entity.transaction.Transaction;
import ru.antonsibgatulin.bankingservice.entity.user.User;

public interface TransactionService {
    TransactionStatusDto transfer(Authentication authentication, TransactionDto transactionDto);
    TransactionStatusDto transferByIds(TransferManagerDto transferManagerDto);
}
