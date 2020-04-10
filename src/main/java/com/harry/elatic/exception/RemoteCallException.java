package com.harry.elatic.exception;

/**
 * 远程调用异常
 */
public class RemoteCallException extends RuntimeException {

    public RemoteCallException(String message){
        super(message);
    }

    public RemoteCallException(String message, Throwable cause) {
        super(message, cause);
    }

}
