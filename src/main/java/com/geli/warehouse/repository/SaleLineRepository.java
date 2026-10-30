package com.geli.warehouse.repository;

import com.geli.warehouse.domain.SaleLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SaleLineRepository extends JpaRepository<SaleLine, Long> {

    @Query("select l.sale.id, count(l) from SaleLine l where l.sale.id in :ids group by l.sale.id")
    List<Object[]> countBySaleIds(@Param("ids") Collection<Long> ids);
}
