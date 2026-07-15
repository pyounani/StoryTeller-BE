package com.cojac.storyteller.credit.exception;

import com.cojac.storyteller.response.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PaymentAmountMismatchException extends RuntimeException {
    private final ErrorCode errorCode;
}
