package com.sky.exception;

/**
 * 账号被锁定异常
 */
public class UserLocationException extends BaseException {

    public UserLocationException() {
    }

    public UserLocationException(String msg) {
        super(msg);
    }

}
