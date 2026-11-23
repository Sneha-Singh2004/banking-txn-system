package com.bank.exception;

public class BankingException extends RuntimeException {
    private final String errorCategory;

    public BankingException(String errorCategory, String message) {
        super(message);
        this.errorCategory = errorCategory;
    }

    public String getErrorCategory() {
        return errorCategory;
    }
}