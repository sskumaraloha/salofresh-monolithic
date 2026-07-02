package com.salofresh.wallet;

import com.salofresh.common.enums.WalletTransactionSource;
import com.salofresh.entity.User;
import com.salofresh.entity.Wallet;
import com.salofresh.entity.WalletTransaction;

import java.math.BigDecimal;

public interface WalletService {

    Wallet getOrCreateWallet(User user);

    BigDecimal getBalance(Long userId);

    WalletTransaction credit(User user, BigDecimal amount, WalletTransactionSource source,
                              String referenceId, String description);

    WalletTransaction debit(User user, BigDecimal amount, WalletTransactionSource source,
                             String referenceId, String description);
}
