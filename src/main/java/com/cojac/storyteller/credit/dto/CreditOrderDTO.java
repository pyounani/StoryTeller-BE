package com.cojac.storyteller.credit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreditOrderDTO {
    private String orderId;
    private Integer amount;
    private Integer creditAmount;
}
