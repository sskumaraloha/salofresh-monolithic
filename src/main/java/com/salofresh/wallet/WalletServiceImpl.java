package com.salofresh.wallet;

import com.salofresh.common.enums.WalletTransactionSource;
import com.salofresh.common.enums.WalletTransactionType;
import com.salofresh.entity.User;
import com.salofresh.entity.Wallet;
import com.salofresh.entity.WalletTransaction;
import com.salofresh.event.WalletTransactionEvent;
import com.salofresh.exception.BadRequestException;
import com.salofresh.repository.WalletRepository;
import com.salofresh.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Wallet getOrCreateWallet(User user) {
        return walletRepository.findByUserId(user.getId())
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .user(user)
                        .balance(BigDecimal.ZERO)
                        .currency("INR")
                        .build()));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long userId) {
        return walletRepository.findByUserId(userId)
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public WalletTransaction credit(User user, BigDecimal amount, WalletTransactionSource source,
                                     String referenceId, String description) {
        validateAmount(amount);
        Wallet wallet = getOrCreateWallet(user);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        WalletTransaction transaction = walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .type(WalletTransactionType.CREDIT)
                .source(source)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .referenceId(referenceId)
                .description(description)
                .build());

        eventPublisher.publishEvent(new WalletTransactionEvent(user, transaction));
        return transaction;
    }

    @Override
    @Transactional
    public WalletTransaction debit(User user, BigDecimal amount, WalletTransactionSource source,
                                    String referenceId, String description) {
        validateAmount(amount);
        Wallet wallet = getOrCreateWallet(user);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient wallet balance");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        WalletTransaction transaction = walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .type(WalletTransactionType.DEBIT)
                .source(source)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .referenceId(referenceId)
                .description(description)
                .build());

        eventPublisher.publishEvent(new WalletTransactionEvent(user, transaction));
        return transaction;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletTransaction> getTransactionHistory(Long userId, Pageable pageable) {
        Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
        if (wallet == null) {
            return Page.empty(pageable);
        }
        return walletTransactionRepository.findAllByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }
    }
}
