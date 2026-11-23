package com.bank.exception;

public class InvalidAccountStateException extends BankingException {
    public InvalidAccountStateException(String message) {
        super("INVALID_ACCOUNT_STATE", message);
    }
}