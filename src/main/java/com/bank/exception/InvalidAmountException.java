package com.bank.exception;

public class InvalidAmountException extends BankingException {
    public InvalidAmountException(String message) {
        super("INVALID_AMOUNT", message);
    }
}