package com.example.fruitshop.core;

public final class DomainException extends IllegalArgumentException {
    public final ErrorCode code;
    public DomainException(ErrorCode code, String message) { super(message); this.code = code; }
    public DomainException(ErrorCode code, String message, Throwable cause) { super(message, cause); this.code = code; }
}
