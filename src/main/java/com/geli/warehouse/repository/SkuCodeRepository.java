package com.geli.warehouse.repository;

import com.geli.warehouse.domain.SkuCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkuCodeRepository extends JpaRepository<SkuCode, String> {
}
