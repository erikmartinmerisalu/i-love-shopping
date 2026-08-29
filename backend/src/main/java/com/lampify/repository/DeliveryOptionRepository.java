package com.lampify.repository;

import com.lampify.entity.DeliveryOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryOptionRepository extends JpaRepository<DeliveryOption, Long> {
    List<DeliveryOption> findByActiveTrueOrderByPriceAsc();

    List<DeliveryOption> findAllByOrderByNameAsc();
}
