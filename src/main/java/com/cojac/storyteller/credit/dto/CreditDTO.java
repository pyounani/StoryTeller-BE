package com.cojac.storyteller.credit.dto;

import com.cojac.storyteller.profile.entity.ProfileEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreditDTO {
    private Integer profileId;
    private Integer credit;

    public static CreditDTO toDto(ProfileEntity profile) {
        return CreditDTO.builder()
                .profileId(profile.getId())
                .credit(profile.getCredit())
                .build();
    }
}
