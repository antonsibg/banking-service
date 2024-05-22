package ru.antonsibgatulin.bankingservice.schedule;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.logging.Log;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import ru.antonsibgatulin.bankingservice.entity.wallet.Wallet;
import ru.antonsibgatulin.bankingservice.repository.WalletRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

@RequiredArgsConstructor
@Component
public class WalletSchedule implements Runnable {
    private final WalletRepository walletRepository;


    private final Queue<BalanceDto> balanceDtoQueue = new LinkedBlockingQueue<>();

    private static final Logger log = Logger.getLogger(WalletSchedule.class.getName());

    private Thread balanceAdded = new Thread(this);

    @PostConstruct
    public void init() {
        balanceAdded.start();
    }


    @Scheduled(fixedDelay = 60000L)
    public void runBalanceIncrease() {
        log.info("Starting balance increase...");
        List<Wallet> wallets = walletRepository.findAll();
        for (Wallet wallet : wallets) {
            BigDecimal startDeposit = wallet.getStartDeposit();
            BigDecimal balance = wallet.getBalance();
            BigDecimal newBalance = balance.multiply(new BigDecimal("1.05")).setScale(2, RoundingMode.HALF_UP);
            if (newBalance.compareTo(startDeposit.multiply(new BigDecimal("2.07"))) > 0) {
                newBalance = startDeposit.multiply(new BigDecimal("2.07")).setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal dt = newBalance.subtract(balance);
            if (dt.stripTrailingZeros().equals(BigDecimal.ZERO)) {
                continue;

            }
            var walletId = wallet.getId();
            var balanceDto = new BalanceDto(walletId, dt);
            balanceDtoQueue.add(balanceDto);

        }
        log.info("Balance increase completed.");
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    public void doBalance(BalanceDto balanceDto) {
        try {

            var wallet = walletRepository.getWalletById(balanceDto.walletId);
            wallet.setBalance(wallet.getBalance().add(balanceDto.dtPlus));
            walletRepository.save(wallet);
        } catch (Exception e) {
            e.printStackTrace();
            balanceDtoQueue.add(balanceDto);
        }
    }


    @Override
    public void run() {
        while (true) {
            if (!balanceDtoQueue.isEmpty()) {
                var dto = balanceDtoQueue.poll();
                doBalance(dto);
            }
        }
    }


    private record BalanceDto(Long walletId, BigDecimal dtPlus) {

    }
}
