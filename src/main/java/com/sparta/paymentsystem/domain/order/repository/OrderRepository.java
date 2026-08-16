package com.sparta.paymentsystem.domain.order.repository;

import com.sparta.paymentsystem.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 내 주문 목록 조회(최신순, memberId로 조회)
    // left left join 사용 이유 "상품명 외 N건"을 표시해야 되기 때문, n+1
    // 아이템이 하나도 없는 주문이 있더라고 주문만큼은 가져오기 위함
    // distinct : 컬렉션? fetch join은 root(Order)가 orderItem 수민큼 중복 엔티티 제거
    @Query("""
    SELECT DISTINCT o
    FROM Order o
    LEFT JOIN o.orderItems
    WHERE o.member.id = :memberId
    ORDER BY o.createdAt DESC
""")
    List<Order> findByMemberIdOrderByCreatedAtDesc(@Param("memberId") Long memberId);

    // 주문 단건 조회 (orderId 로 조회)
    @Query("""
    SELECT DISTINCT o
    FROM Order o
    LEFT JOIN o.orderItems
    WHERE o.id = :orderId
""")
    Optional<Order> findByIdWithOrderItems(@Param("orderId") Long orderId);

}
