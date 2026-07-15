package com.cojac.storyteller.common.toss;

import com.cojac.storyteller.common.toss.dto.TossConfirmResponseDto;
import com.cojac.storyteller.credit.exception.PaymentFailedException;
import com.cojac.storyteller.response.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TossPaymentService {

    private final RestTemplate tossRestTemplate;
    private final HttpHeaders tossHttpHeaders;

    /**
     * 토스페이먼츠 결제 승인(confirm) API 호출
     * @param paymentKey 토스 결제위젯에서 발급된 결제 키
     * @param orderId 서버가 사전에 발급한 주문 ID
     * @param amount 결제 금액(원) — 서버가 사전에 기록한 주문 금액과 일치해야 함
     */
    public TossConfirmResponseDto confirmPayment(String paymentKey, String orderId, Integer amount) {
        String url = "https://api.tosspayments.com/v1/payments/confirm";

        Map<String, Object> requestBody = Map.of(
                "paymentKey", paymentKey,
                "orderId", orderId,
                "amount", amount
        );

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, tossHttpHeaders);

        try {
            ResponseEntity<TossConfirmResponseDto> response =
                    tossRestTemplate.exchange(url, HttpMethod.POST, requestEntity, TossConfirmResponseDto.class);
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[TossPaymentService] 결제 승인 실패 - orderId: {}, status: {}, body: {}",
                    orderId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaymentFailedException(ErrorCode.PAYMENT_FAILED);
        }
    }
}
