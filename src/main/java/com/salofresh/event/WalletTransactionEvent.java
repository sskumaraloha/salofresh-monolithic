package com.salofresh.event;

import com.salofresh.entity.User;
import com.salofresh.entity.WalletTransaction;

public record WalletTransactionEvent(User user, WalletTransaction walletTransaction) {
}
