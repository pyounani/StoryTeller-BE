package com.cojac.storyteller.unit.credit;

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
import com.cojac.storyteller.credit.service.CreditService;
import com.cojac.storyteller.profile.entity.ProfileEntity;
import com.cojac.storyteller.profile.exception.ProfileNotFoundException;
import com.cojac.storyteller.profile.repository.ProfileRepository;
import com.cojac.storyteller.response.code.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditServiceUnitTest {

    @InjectMocks
    private CreditService creditService;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private CreditChargeRepository creditChargeRepository;

    @Mock
    private TossPaymentService tossPaymentService;

    private ProfileEntity profile;

    @BeforeEach
    void setUp() {
        profile = ProfileEntity.builder().id(1).birthDate(LocalDate.of(2015, 1, 1)).credit(5).build();
    }

    @Test
    @DisplayName("크레딧 잔액 조회 단위 테스트 - 성공")
    void testGetBalance_Success() {
        // given
        when(profileRepository.findById(profile.getId())).thenReturn(Optional.of(profile));

        // when
        CreditDTO result = creditService.getBalance(profile.getId());

        // then
        assertNotNull(result);
        assertEquals(5, result.getCredit());
        verify(profileRepository, times(1)).findById(profile.getId());
    }

    @Test
    @DisplayName("크레딧 잔액 조회 단위 테스트 - 프로필 없음 예외")
    void testGetBalance_ProfileNotFound() {
        // given
        when(profileRepository.findById(profile.getId())).thenReturn(Optional.empty());

        // when & then
        assertThrows(ProfileNotFoundException.class, () -> creditService.getBalance(profile.getId()));
    }

    @Test
    @DisplayName("결제 주문 생성 단위 테스트 - 성공")
    void testCreateOrder_Success() {
        // given
        when(profileRepository.findById(profile.getId())).thenReturn(Optional.of(profile));

        // when
        CreditOrderDTO result = creditService.createOrder(profile.getId(), 10);

        // then
        assertNotNull(result);
        assertNotNull(result.getOrderId());
        assertEquals(10, result.getCreditAmount());
        assertEquals(1000, result.getAmount());
        verify(creditChargeRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("결제 주문 생성 단위 테스트 - 프로필 없음 예외")
    void testCreateOrder_ProfileNotFound() {
        // given
        when(profileRepository.findById(profile.getId())).thenReturn(Optional.empty());

        // when & then
        assertThrows(ProfileNotFoundException.class, () -> creditService.createOrder(profile.getId(), 10));
        verify(creditChargeRepository, never()).save(any());
    }

    @Test
    @DisplayName("결제 승인 확정 단위 테스트 - 성공")
    void testConfirmCharge_Success() {
        // given
        CreditChargeEntity order = CreditChargeEntity.createPending(profile, "order-1", 1000, 10);
        when(creditChargeRepository.findByOrderId("order-1")).thenReturn(Optional.of(order));
        when(creditChargeRepository.compareAndSetStatus("order-1", ChargeStatus.PENDING, ChargeStatus.SUCCESS)).thenReturn(1);

        ConfirmPaymentRequest request = ConfirmPaymentRequest.builder()
                .orderId("order-1")
                .paymentKey("payment-key-1")
                .amount(1000)
                .build();

        // when
        CreditDTO result = creditService.confirmCharge(request);

        // then
        assertNotNull(result);
        assertEquals(15, result.getCredit());
        verify(tossPaymentService, times(1)).confirmPayment("payment-key-1", "order-1", 1000);
    }

    @Test
    @DisplayName("결제 승인 확정 단위 테스트 - 중복 요청 멱등 재생")
    void testConfirmCharge_DuplicateRequest_IdempotentReplay() {
        // given
        CreditChargeEntity order = CreditChargeEntity.createPending(profile, "order-1", 1000, 10);
        order.markSuccess(); // 이미 다른 요청이 먼저 처리해 SUCCESS로 종결된 상태를 가정
        when(creditChargeRepository.findByOrderId("order-1")).thenReturn(Optional.of(order));
        when(creditChargeRepository.compareAndSetStatus("order-1", ChargeStatus.PENDING, ChargeStatus.SUCCESS)).thenReturn(0);

        ConfirmPaymentRequest request = ConfirmPaymentRequest.builder()
                .orderId("order-1")
                .paymentKey("payment-key-1")
                .amount(1000)
                .build();

        // when
        CreditDTO result = creditService.confirmCharge(request);

        // then
        assertNotNull(result);
        assertEquals(5, result.getCredit(), "이미 처리된 요청이므로 크레딧이 추가로 지급되지 않고 기존 잔액 그대로여야 합니다.");
        verify(tossPaymentService, never()).confirmPayment(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("결제 승인 확정 단위 테스트 - 주문 없음 예외")
    void testConfirmCharge_OrderNotFound() {
        // given
        when(creditChargeRepository.findByOrderId("unknown-order")).thenReturn(Optional.empty());

        ConfirmPaymentRequest request = ConfirmPaymentRequest.builder()
                .orderId("unknown-order")
                .paymentKey("payment-key-1")
                .amount(1000)
                .build();

        // when & then
        assertThrows(PaymentOrderNotFoundException.class, () -> creditService.confirmCharge(request));
        verify(tossPaymentService, never()).confirmPayment(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("결제 승인 확정 단위 테스트 - 금액 불일치 예외")
    void testConfirmCharge_AmountMismatch() {
        // given
        CreditChargeEntity order = CreditChargeEntity.createPending(profile, "order-1", 1000, 10);
        when(creditChargeRepository.findByOrderId("order-1")).thenReturn(Optional.of(order));

        ConfirmPaymentRequest request = ConfirmPaymentRequest.builder()
                .orderId("order-1")
                .paymentKey("payment-key-1")
                .amount(999)
                .build();

        // when & then
        assertThrows(PaymentAmountMismatchException.class, () -> creditService.confirmCharge(request));
        verify(tossPaymentService, never()).confirmPayment(anyString(), anyString(), any());
        assertEquals(5, profile.getCredit());
    }

    @Test
    @DisplayName("결제 승인 확정 단위 테스트 - 토스 승인 실패 예외")
    void testConfirmCharge_TossPaymentFailed() {
        // given
        CreditChargeEntity order = CreditChargeEntity.createPending(profile, "order-1", 1000, 10);
        when(creditChargeRepository.findByOrderId("order-1")).thenReturn(Optional.of(order));
        when(creditChargeRepository.compareAndSetStatus("order-1", ChargeStatus.PENDING, ChargeStatus.SUCCESS)).thenReturn(1);
        when(tossPaymentService.confirmPayment("payment-key-1", "order-1", 1000))
                .thenThrow(new PaymentFailedException(ErrorCode.PAYMENT_FAILED));

        ConfirmPaymentRequest request = ConfirmPaymentRequest.builder()
                .orderId("order-1")
                .paymentKey("payment-key-1")
                .amount(1000)
                .build();

        // when & then
        assertThrows(PaymentFailedException.class, () -> creditService.confirmCharge(request));
        assertEquals(5, profile.getCredit());
    }
}
