package com.cojac.storyteller.credit.repository;

import com.cojac.storyteller.credit.entity.PaymentOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, Integer> {

    List<PaymentOutbox> findByProcessedAtIsNull();
}
