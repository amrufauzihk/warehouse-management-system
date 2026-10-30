package com.geli.warehouse.repository;

import com.geli.warehouse.domain.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    boolean existsByReference(String reference);

    Optional<Sale> findByReference(String reference);

    @Query("""
            select distinct s from Sale s
            left join fetch s.lines l
            left join fetch l.item
            left join fetch l.variant
            where s.id = :id
            """)
    Optional<Sale> findWithLinesById(@Param("id") Long id);

    @Query("""
            select distinct s from Sale s
            left join fetch s.lines l
            left join fetch l.item
            left join fetch l.variant
            where s.reference = :reference
            """)
    Optional<Sale> findWithLinesByReference(@Param("reference") String reference);
}
