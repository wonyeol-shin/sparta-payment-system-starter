package com.sparta.paymentsystem.domain.payment.repository;

import com.sparta.paymentsystem.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment,Long> {
    // 주문 단건조회 시 결제 ID를 얻기 위함
    // order <- payment 단반향으로 order는 payment를 참조하지 않아 order에서 payment로 직접 조회 못함
    // 그래서 역으로 payment쪽에서 역으로 찾기
    @Query("""
    SELECT p.id
    FROM Payment p
    WHERE p.order.id = :orderId
""")
    Optional<Long> findIdByOrderId(@Param("orderId") Long orderId);

    // 주문 목록 조회용
    // 각 주문마다 결제 ID가 필요한데 getIdByOrderId를 N번 반복하는 건 비효율적이라 IN 절을 통해 한번에 받아오기
    // Object[] 로 받는 이유는 JPA는 여러개를 지정하여 select를 하게되면 Object{ 값1, 값2 }로 전달
    @Query("""
    SELECT p.order.id, p.id
    FROM Payment p
    WHERE p.order.id IN :orderIds
""")
    List<Object[]> findIdsByOrderIds(@Param("orderIds") List<Long> orderIds);

}
