package com.sagacity.docs.base.exception;

public class IncorrectRoleException extends AuthenticationException{
    public IncorrectRoleException() {
    }

    public IncorrectRoleException(String message) {
        super(message);
    }

    public IncorrectRoleException(Throwable cause) {
        super(cause);
    }

    public IncorrectRoleException(String message, Throwable cause) {
        super(message, cause);
    }
}
