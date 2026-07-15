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

    @Column(nullable = false)
    private Integer chargeAmount;

    @Enumerated(EnumType.STRING)
    private ChargeStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static CreditChargeEntity createSuccess(ProfileEntity profile, Integer chargeAmount) {
        return CreditChargeEntity.builder()
                .profile(profile)
                .chargeAmount(chargeAmount)
                .status(ChargeStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
