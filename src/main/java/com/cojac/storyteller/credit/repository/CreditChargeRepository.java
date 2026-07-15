package com.cojac.storyteller.credit.repository;

import com.cojac.storyteller.credit.entity.CreditChargeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditChargeRepository extends JpaRepository<CreditChargeEntity, Integer> {
}
