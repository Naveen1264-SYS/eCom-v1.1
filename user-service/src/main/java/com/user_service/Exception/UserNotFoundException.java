package com.user_service.Exception;

public class UserNotFoundException  extends  RuntimeException{
    public UserNotFoundException(String message) {
        super(message);
    }
}
