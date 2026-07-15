package com.cojac.storyteller.credit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmPaymentRequest {
    private String orderId;
    private String paymentKey;
    private Integer amount;
}
