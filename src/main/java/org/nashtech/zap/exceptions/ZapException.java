package org.nashtech.zap.exceptions;

public class ZapException extends RuntimeException {
    public ZapException(String message) {
        super(message);
    }

    public ZapException(String message, Throwable cause) {
        super(message, cause);
    }
    }
