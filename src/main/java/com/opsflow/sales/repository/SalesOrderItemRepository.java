package com.opsflow.sales.repository;

import com.opsflow.sales.domain.SalesOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, UUID> {
}
