package com.sagacity.docs.base.exception;

public class UnknownAccountException extends AuthenticationException{
    public UnknownAccountException() {
    }

    public UnknownAccountException(String message) {
        super(message);
    }

    public UnknownAccountException(Throwable cause) {
        super(cause);
    }

    public UnknownAccountException(String message, Throwable cause) {
        super(message, cause);
    }
}
