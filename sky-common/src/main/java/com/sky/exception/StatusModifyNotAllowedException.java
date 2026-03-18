package com.sky.exception;

/**
 * 停售起售异常
 */
public class StatusModifyNotAllowedException extends BaseException {

    public StatusModifyNotAllowedException() {
    }

    public StatusModifyNotAllowedException(String msg) {
        super(msg);
    }

}
