package com.cojac.storyteller.credit.service;

import com.cojac.storyteller.common.toss.TossPaymentService;
import com.cojac.storyteller.credit.dto.ConfirmPaymentRequest;
import com.cojac.storyteller.credit.dto.CreditDTO;
import com.cojac.storyteller.credit.dto.CreditOrderDTO;
import com.cojac.storyteller.credit.entity.CreditChargeEntity;
import com.cojac.storyteller.credit.entity.enums.ChargeStatus;
import com.cojac.storyteller.credit.exception.PaymentAmountMismatchException;
import com.cojac.storyteller.credit.exception.PaymentFailedException;
import com.cojac.storyteller.credit.exception.PaymentOrderNotFoundException;
import com.cojac.storyteller.credit.repository.CreditChargeRepository;
import com.cojac.storyteller.profile.entity.ProfileEntity;
import com.cojac.storyteller.profile.exception.ProfileNotFoundException;
import com.cojac.storyteller.profile.repository.ProfileRepository;
import com.cojac.storyteller.response.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreditService {

    // 실제 가격 정책은 범위 밖이라 크레딧 1개당 100원으로 단순 고정
    private static final int CREDIT_UNIT_PRICE = 100;

    private final ProfileRepository profileRepository;
    private final CreditChargeRepository creditChargeRepository;
    private final TossPaymentService tossPaymentService;

    /**
     * 크레딧 잔액 조회
     */
    public CreditDTO getBalance(Integer profileId) {
        ProfileEntity profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException(ErrorCode.PROFILE_NOT_FOUND));

        return CreditDTO.toDto(profile);
    }

    /**
     * 결제 주문 생성 - 토스 결제위젯 호출 전, 서버가 먼저 주문 금액을 기록해둔다
     * (승인 시점에 클라이언트가 보낸 금액과 대조해 위변조를 막기 위함)
     */
    @Transactional
    public CreditOrderDTO createOrder(Integer profileId, Integer creditAmount) {
        ProfileEntity profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException(ErrorCode.PROFILE_NOT_FOUND));

        int amount = creditAmount * CREDIT_UNIT_PRICE;
        String orderId = UUID.randomUUID().toString();

        CreditChargeEntity order = CreditChargeEntity.createPending(profile, orderId, amount, creditAmount);
        creditChargeRepository.save(order);

        return CreditOrderDTO.builder()
                .orderId(orderId)
                .amount(amount)
                .creditAmount(creditAmount)
                .build();
    }

    /**
     * 결제 승인 확정 - 토스 결제위젯 완료 후 전달받은 paymentKey/orderId/amount로 승인 API 호출
     * 같은 orderId로 중복 호출돼도(클라이언트 재시도, PG 웹훅 재전송 등) 크레딧이 한 번만 지급되도록,
     * PENDING -> SUCCESS 전이를 원자적 CAS UPDATE로 단 하나의 요청만 통과시킨다 (Issue 2)
     */
    @Transactional
    public CreditDTO confirmCharge(ConfirmPaymentRequest request) {
        CreditChargeEntity order = creditChargeRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new PaymentOrderNotFoundException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));

        if (!order.getAmount().equals(request.getAmount())) {
            creditChargeRepository.compareAndSetStatus(request.getOrderId(), ChargeStatus.PENDING, ChargeStatus.FAILED);
            throw new PaymentAmountMismatchException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        int claimed = creditChargeRepository.compareAndSetStatus(request.getOrderId(), ChargeStatus.PENDING, ChargeStatus.SUCCESS);
        if (claimed == 0) {
            // 이미 다른 요청이 먼저 처리한 주문 - 재처리 없이 기존 결과를 그대로 반환 (멱등 재생)
            CreditChargeEntity existing = creditChargeRepository.findByOrderId(request.getOrderId())
                    .orElseThrow(() -> new PaymentOrderNotFoundException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));
            if (existing.getStatus() == ChargeStatus.SUCCESS) {
                return CreditDTO.toDto(existing.getProfile());
            }
            throw new PaymentFailedException(ErrorCode.PAYMENT_FAILED);
        }

        // 이 요청이 PENDING -> SUCCESS 전이에 성공한 유일한 요청 - CAS로 영속성 컨텍스트가 비워졌으므로 재조회
        CreditChargeEntity claimedOrder = creditChargeRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new PaymentOrderNotFoundException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));

        try {
            tossPaymentService.confirmPayment(request.getPaymentKey(), request.getOrderId(), request.getAmount());
        } catch (PaymentFailedException e) {
            creditChargeRepository.compareAndSetStatus(request.getOrderId(), ChargeStatus.SUCCESS, ChargeStatus.FAILED);
            throw e;
        }

        ProfileEntity profile = claimedOrder.getProfile();
        profile.chargeCredit(claimedOrder.getChargeAmount());

        return CreditDTO.toDto(profile);
    }
}
