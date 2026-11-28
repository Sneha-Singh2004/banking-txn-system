package com.bank.service;

import com.bank.dto.AccountSummary;
import com.bank.dto.TransactionRequest;
import com.bank.dto.TransactionResult;
import com.bank.model.AccountStatus;
import com.bank.model.TransactionStatus;
import com.bank.model.TransactionType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountServiceTest {

    private static AccountService accountService;
    private static BankingService bankingService;
    private static String tag;

    @BeforeAll
    static void setUp() {
        accountService = new AccountService();
        bankingService = new BankingService();
        tag = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @Test
    @DisplayName("1. Opening a new account should create customer and active account atomically")
    void testOpenAndFetchAccount() throws Exception {
        String accId = "ACC-" + tag + "-01";
        String email = "user." + tag.toLowerCase() + "@bank.in";

        AccountSummary created = accountService.openAccount(
                accId, "Sneha Test User", email, "9876543210", new BigDecimal("5000.00")
        );

        assertTrue(created.customerId() > 0);
        assertEquals(AccountStatus.ACTIVE, created.status());

        AccountSummary fetched = accountService.getAccountSummary(accId);
        assertEquals("Sneha Test User", fetched.customerName());
        assertEquals(0, new BigDecimal("5000.00").compareTo(fetched.balance()));
    }

    @Test
    @DisplayName("2. Blocked account must reject transactions until reactivated")
    void testBlockAndUnblockAccount() throws Exception {
        String accId = "ACC-" + tag + "-02";
        String email = "block." + tag.toLowerCase() + "@bank.in";

        accountService.openAccount(accId, "Freeze Test User", email, "9876500001", new BigDecimal("2000.00"));
        accountService.updateAccountStatus(accId, AccountStatus.BLOCKED);

        TransactionResult blockedResult = bankingService.processTransaction(
                new TransactionRequest("TXN-" + tag + "-BLK", TransactionType.WITHDRAWAL,
                        new BigDecimal("100.00"), accId, null, "ATM")
        );
        assertEquals(TransactionStatus.REJECTED, blockedResult.getStatus());

        accountService.updateAccountStatus(accId, AccountStatus.ACTIVE);
        TransactionResult activeResult = bankingService.processTransaction(
                new TransactionRequest("TXN-" + tag + "-ACT", TransactionType.WITHDRAWAL,
                        new BigDecimal("100.00"), accId, null, "ATM")
        );
        assertEquals(TransactionStatus.SUCCESS, activeResult.getStatus());
    }

    @Test
    @DisplayName("3. Account with positive balance cannot be closed until balance reaches zero")
    void testCloseAccountValidation() throws Exception {
        String accId = "ACC-" + tag + "-03";
        String email = "close." + tag.toLowerCase() + "@bank.in";

        accountService.openAccount(accId, "Close Test User", email, "9876500002", new BigDecimal("500.00"));

        assertThrows(IllegalStateException.class, () ->
                accountService.updateAccountStatus(accId, AccountStatus.CLOSED)
        );

        bankingService.processTransaction(
                new TransactionRequest("TXN-" + tag + "-EMPTY", TransactionType.WITHDRAWAL,
                        new BigDecimal("500.00"), accId, null, "WEB")
        );

        accountService.updateAccountStatus(accId, AccountStatus.CLOSED);
        assertEquals(AccountStatus.CLOSED, accountService.getAccountSummary(accId).status());
    }
}