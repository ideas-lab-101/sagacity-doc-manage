package com.sagacity.docs.base.exception;

public class IncorrectCredentialException extends AuthenticationException{
    public IncorrectCredentialException() {
    }

    public IncorrectCredentialException(String message) {
        super(message);
    }

    public IncorrectCredentialException(Throwable cause) {
        super(cause);
    }

    public IncorrectCredentialException(String message, Throwable cause) {
        super(message, cause);
    }
}
