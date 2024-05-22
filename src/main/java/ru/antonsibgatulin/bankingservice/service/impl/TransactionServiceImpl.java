package ru.antonsibgatulin.bankingservice.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.antonsibgatulin.bankingservice.dto.transaction.request.TransactionDto;
import ru.antonsibgatulin.bankingservice.dto.transaction.request.TransferManagerDto;
import ru.antonsibgatulin.bankingservice.dto.transaction.response.TransactionStatusDto;
import ru.antonsibgatulin.bankingservice.entity.transaction.Transaction;
import ru.antonsibgatulin.bankingservice.entity.transaction.TransactionStatus;
import ru.antonsibgatulin.bankingservice.entity.user.User;
import ru.antonsibgatulin.bankingservice.except.NotFoundException;

import ru.antonsibgatulin.bankingservice.repository.PhoneNumberRepository;
import ru.antonsibgatulin.bankingservice.repository.TransactionRepository;
import ru.antonsibgatulin.bankingservice.repository.UserRepository;
import ru.antonsibgatulin.bankingservice.repository.WalletRepository;
import ru.antonsibgatulin.bankingservice.service.TransactionService;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Service
public class TransactionServiceImpl implements TransactionService {


    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionServiceImpl.class);


    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PhoneNumberRepository phoneNumberRepository;

    private final SimpMessagingTemplate simpMessagingTemplate;

    @PostConstruct
    public void init() {
        LOGGER.info("Initializing TransactionService...");
    }


    @Override
    public TransactionStatusDto transfer(Authentication authentication, TransactionDto transactionDto) {
        var username = authentication.getName();
        var senderUser = userRepository.getUserByUsername(username);
        var receivedUser = getReceivedUser(transactionDto);

        LOGGER.debug("Sender user: {}, received user: {}",
                senderUser, receivedUser);
        return doTransaction(transactionDto.getAmount(), senderUser, receivedUser);
    }

    @Override
    public TransactionStatusDto transferByIds(TransferManagerDto transferManagerDto) {
        var senderUser = userRepository.getUserById(transferManagerDto.getSenderId());
        var receivedUser = userRepository.getUserById(transferManagerDto.getReceivedId());
        if (senderUser == null || receivedUser == null) {
            return new TransactionStatusDto("Not found received or sender", TransactionStatus.FAILED, null);
        }
        LOGGER.debug("Sender user: {}, received user: {}",
                senderUser, receivedUser);


        return doTransaction(transferManagerDto.getAmount(), senderUser, receivedUser);
    }


    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    public TransactionStatusDto doTransaction(BigDecimal amount, User senderUser, User receivedUser) {
        var senderWallet = walletRepository.getWalletByUser(senderUser);
        var receivedWallet = walletRepository.getWalletByUser(receivedUser);


        if (senderWallet.getBalance().compareTo(amount) >= 0) {
            senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
            receivedWallet.setBalance(receivedWallet.getBalance().add(amount));

            var transaction = new Transaction(senderUser, receivedUser, amount);


            transactionRepository.save(transaction);


            var resultSender = new TransactionStatusDto("Вы перевели " + amount.toString() + " ID получателя: " + receivedUser.getId(), TransactionStatus.COMPLETED, transaction.getId());
            var resultReceived = new TransactionStatusDto("Вам перевели " + amount.toString() + " ID отправителя: " + senderUser.getId(), TransactionStatus.COMPLETED, transaction.getId());

            simpMessagingTemplate.convertAndSendToUser(
                    String.valueOf(receivedUser.getId()),
                    "/queue/message",
                    resultReceived
            );
            simpMessagingTemplate.convertAndSendToUser(
                    String.valueOf(senderUser.getId()),
                    "/queue/message",
                    resultSender
            );

            LOGGER.debug("Sended user: {}, sender wallet: {}, received user: {}, received wallet: {}",
                    senderUser, receivedUser);


            ;
            return resultSender;

        } else {
            return new TransactionStatusDto("Не достаточно денег на счете", TransactionStatus.FAILED, null);
        }
    }


    private User getReceivedUser(TransactionDto transactionDto) {
        if (transactionDto.getPhoneReceived() != null) {
            var phoneNumber = phoneNumberRepository.getPhoneNumberByNumber(transactionDto.getPhoneReceived());
            if (phoneNumber != null) return userRepository.getUserById(phoneNumber.getUser().getId());

        }
        if (transactionDto.getReceivedUserId() != null) {
            var user = userRepository.getUserById(transactionDto.getReceivedUserId());
            if (user != null) return user;
        }
        if (transactionDto.getReceivedWalletId() != null) {
            var wallet = walletRepository.getWalletById(transactionDto.getReceivedWalletId());
            if (wallet != null) {
                return wallet.getUser();
            }
        }
        throw new NotFoundException("Received not found");
    }

}