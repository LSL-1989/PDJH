package com.gocent.pdjh.exception;

public class TerminalBindingException extends RuntimeException {
    private final int code;

    public TerminalBindingException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
