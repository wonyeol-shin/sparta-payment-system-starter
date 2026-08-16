package com.sparta.paymentsystem.domain.cart.repository;

import com.sparta.paymentsystem.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @Query("SELECT ci FROM CartItem ci JOIN FETCH ci.product WHERE ci.member.id = :memberId")
    List<CartItem> findByMemberId(@Param("memberId") Long memberId);

    Optional<CartItem> findByMember_IdAndProduct_Id(Long memberId, Long productId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.id = :id AND ci.member.id = :memberId")
    int deleteByIdAndMember_Id(@Param("id") Long id, @Param("memberId") Long memberId);

    // 주문서에 담을 선택된 장바구니 아이템을 상품 정보와 함께 조회
    // MemberId 조건으로 찾기 : 프론트에서 다른 회원의 cartItemId를 넘겨도 조회되지 않도록 소유권 검증
    @Query(""" 
    SELECT ci
    FROM CartItem ci
    JOIN fetch ci.product
    WHERE ci.id IN :ids AND ci.member.id = :memberId
    """)
    List<CartItem> findByIdInAndMember_IdWithProduct(
            @Param("ids") List<Long> ids, @Param("memberId") Long memberId );

    // 주문 생성 완료 직후 "주문한 장바구니 아이템을" 일괄 생성
    // member.id 조건 : cartItemId를 남의것으로 보내는 경우를 방지하기 위한 소유권 검사
    // 반환은 int로 하는데 몇 개를 지웠는지 알려줌
    // modifying은 1차캐시를 지워주는 역할을 하여 삭제를 하고나서 혹시나 조회가 되는 상황을 방지한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    DELETE FROM CartItem c
    WHERE c.product.id IN :ids AND c.member.id = :memberId
""")
    int deleteAllByIdInAndMemberId(@Param("ids") List<Long> ids , @Param("memberId") Long memberId);

}
