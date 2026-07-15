package com.cojac.storyteller.common.toss.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TossConfirmResponseDto {
    private String paymentKey;
    private String orderId;
    private String status;
    private Integer totalAmount;
}
