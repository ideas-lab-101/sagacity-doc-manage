package com.sagacity.docs.base.exception;

public class LockedAccountException extends AuthenticationException{
    public LockedAccountException() {
    }

    public LockedAccountException(String message) {
        super(message);
    }

    public LockedAccountException(Throwable cause) {
        super(cause);
    }

    public LockedAccountException(String message, Throwable cause) {
        super(message, cause);
    }
}
