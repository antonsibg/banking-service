package ru.antonsibgatulin.bankingservice.service;

import org.junit.jupiter.api.Test;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.Rollback;

import org.springframework.test.context.junit4.SpringRunner;
import ru.antonsibgatulin.bankingservice.controller.auth.AuthenticationController;
import ru.antonsibgatulin.bankingservice.controller.transaction.TransactionController;
import ru.antonsibgatulin.bankingservice.service.impl.TransactionServiceImpl;
import ru.antonsibgatulin.bankingservice.dto.transaction.request.TransactionDto;
import ru.antonsibgatulin.bankingservice.dto.user.request.UserRegistrationDto;
import ru.antonsibgatulin.bankingservice.entity.transaction.Transaction;
import ru.antonsibgatulin.bankingservice.entity.user.User;
import ru.antonsibgatulin.bankingservice.repository.TransactionRepository;
import ru.antonsibgatulin.bankingservice.repository.UserRepository;
import ru.antonsibgatulin.bankingservice.repository.WalletRepository;


import java.math.BigDecimal;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@RunWith(SpringRunner.class)
@SpringBootTest
public class TransactionServiceImplTest {

    @Autowired
    private AuthenticationController authenticationController;


    @Autowired
    private TransactionServiceImpl transactionServiceImpl;

    @Autowired
    private TransactionController transactionController;

    @Autowired
    private TransactionRepository transactionRepository;


    @Autowired
    private UserRepository userRepository;


    @Autowired
    private WalletRepository walletRepository;

    @Test
    @Rollback(true)
    public void testRunTransactionThreadSafety() throws InterruptedException {

        String usernameSender = "TestUser1";
        String phoneSender = "79879999999";

        String usernameReceived = "TestUser2";
        String phoneReceived = "79879999998";

        transactionRepository.deleteAll();

        try {
            authenticationController.signUp(new UserRegistrationDto(usernameSender, "TestPassword", "test1@mail.ru", phoneSender, new BigDecimal(2000)));
            authenticationController.signUp(new UserRegistrationDto(usernameReceived, "TestPassword", "test2@mail.ru", phoneReceived, new BigDecimal(2000)));
        } catch (Exception e) {
            System.out.println("Ignore");
        }

        var senderUserTest = userRepository.getUserByUsername(usernameSender);
        var receivedUserTest = userRepository.getUserByUsername(usernameReceived);
        var sendeUserWallet = walletRepository.getWalletByUser(senderUserTest);
        var receivedUserWallet = walletRepository.getWalletByUser(receivedUserTest);
        sendeUserWallet.setBalance(new BigDecimal(2000));
        receivedUserWallet.setBalance(new BigDecimal(2000));
        walletRepository.save(sendeUserWallet);
        walletRepository.save(receivedUserWallet);
        Thread.sleep(2000L);


        BigDecimal amount = BigDecimal.TEN;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);

        System.out.println("Pool of threads");
        for (int i = 0; i < 10; i++) {

            executorService.submit(() -> {

                try {

                    var result = transactionController.trans(new TransactionDto(null, phoneReceived, null, amount),
                            new Authentication() {
                                @Override
                                public Collection<? extends GrantedAuthority> getAuthorities() {
                                    return List.of(new SimpleGrantedAuthority("USER"));
                                }

                                @Override
                                public Object getCredentials() {
                                    return null;
                                }

                                @Override
                                public Object getDetails() {
                                    return null;
                                }

                                @Override
                                public Object getPrincipal() {
                                    return userRepository.getUserByUsername(usernameSender);
                                }

                                @Override
                                public boolean isAuthenticated() {
                                    return false;
                                }

                                @Override
                                public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

                                }

                                @Override
                                public String getName() {
                                    return usernameSender;
                                }
                            });
                    System.out.println(result);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }

            });

        }

        System.out.println("Just threads in queue");

        for (int i = 0; i < 10; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {

                        var result = transactionController.trans(new TransactionDto(null, phoneReceived, null, amount),
                                new Authentication() {
                                    @Override
                                    public Collection<? extends GrantedAuthority> getAuthorities() {
                                        return List.of(new SimpleGrantedAuthority("USER"));
                                    }

                                    @Override
                                    public Object getCredentials() {
                                        return null;
                                    }

                                    @Override
                                    public Object getDetails() {
                                        return null;
                                    }

                                    @Override
                                    public Object getPrincipal() {
                                        return null;//userRepository.getUserByUsername(usernameSender);
                                    }

                                    @Override
                                    public boolean isAuthenticated() {
                                        return false;
                                    }

                                    @Override
                                    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

                                    }

                                    @Override
                                    public String getName() {
                                        return usernameSender;
                                    }
                                });
                        System.out.println(result);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }
            }).start();
        }
        Thread.sleep(5000L);

        //latch.await(20, TimeUnit.SECONDS);

        executorService.shutdown();
        List<Transaction> transactions = transactionRepository.findAll();
        System.out.println(transactions.size());
        senderUserTest = userRepository.getUserByUsername(usernameSender);
        receivedUserTest = userRepository.getUserByUsername(usernameReceived);
        var walletReciver = walletRepository.getWalletByUser(receivedUserTest);
        var walletSender = walletRepository.getWalletByUser(senderUserTest);
        System.out.println(walletSender.getBalance());
        System.out.println(walletReciver.getBalance());


        BigDecimal expectedBalanceSender = new BigDecimal(2000);
        BigDecimal expectedBalanceReciver = new BigDecimal(2000);
        BigDecimal differenceSender = expectedBalanceSender.subtract(walletSender.getBalance()).stripTrailingZeros();
        BigDecimal differenceReciver = walletReciver.getBalance().subtract(expectedBalanceReciver).stripTrailingZeros();

        BigDecimal difference = differenceSender.subtract(differenceReciver).stripTrailingZeros();


        assertTrue(difference.equals(BigDecimal.ZERO));


        assertEquals(20, Integer.valueOf(transactions.size()));
        User finalSenderUserTest = senderUserTest;
        User finalReceivedUserTest = receivedUserTest;
        transactions.forEach(transaction -> {
            assertEquals(finalSenderUserTest.getId(), transaction.getSender().getId());
            assertEquals(finalReceivedUserTest.getId(), transaction.getReceived().getId());
            assertTrue(amount.subtract(transaction.getAmount()).stripTrailingZeros().equals(BigDecimal.ZERO));

        });

    }


}