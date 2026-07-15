package com.cojac.storyteller.credit.entity;

import com.cojac.storyteller.credit.entity.enums.ChargeStatus;
import com.cojac.storyteller.profile.entity.ProfileEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditChargeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private ProfileEntity profile;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    private Integer chargeAmount;

    @Enumerated(EnumType.STRING)
    private ChargeStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static CreditChargeEntity createPending(ProfileEntity profile, String orderId, Integer amount, Integer chargeAmount) {
        return CreditChargeEntity.builder()
                .profile(profile)
                .orderId(orderId)
                .amount(amount)
                .chargeAmount(chargeAmount)
                .status(ChargeStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void markSuccess() {
        this.status = ChargeStatus.SUCCESS;
    }

    public void markFailed() {
        this.status = ChargeStatus.FAILED;
    }
}
