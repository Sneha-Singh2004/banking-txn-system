package com.bank.exception;

public class DuplicateTransactionException extends BankingException {
    public DuplicateTransactionException(String message) {
        super("DUPLICATE_TRANSACTION", message);
    }
}