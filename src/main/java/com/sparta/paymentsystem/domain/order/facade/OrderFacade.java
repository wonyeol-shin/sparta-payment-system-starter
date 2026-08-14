package com.sparta.paymentsystem.domain.order.facade;

import com.sparta.paymentsystem.domain.cart.entity.CartItem;
import com.sparta.paymentsystem.domain.cart.service.CartService;
import com.sparta.paymentsystem.domain.order.dto.CheckoutResponse;
import com.sparta.paymentsystem.global.error.BusinessException;
import com.sparta.paymentsystem.global.error.ErrorCode;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderFacade {

    private final CartService cartService;

    public CheckoutResponse getCheckout(Long memberId, @Nullable List<Long> cartItemIds){
        // 주문서 미리보기 : 재고 차감/주문 생성 없는 읽기 전용
        // cartItemIds가 null/비어있으면 "전체 장바구니", 값이 있으면 "선택된 아이템만" 주문서에 담는다.
        List<CartItem> cartItems = getValidateCartItems(
                memberId, (cartItemIds != null) ? cartItemIds : List.of()
        );

        // 장바구니 아이템에서 상품 가격과 장바구니 수량을 곱해서 각 아이템의 총액을 구한다.
        List<CheckoutResponse.CheckoutItemResponse> items = cartItems.stream()
                .map(cartItem -> {
                    int price = cartItem.getProduct().getPrice();
                    int quantity = cartItem.getQuantity();
                    int subtotal = price * quantity;
                    return new CheckoutResponse.CheckoutItemResponse(
                            cartItem.getProductId(),
                            cartItem.getProduct().getName(),
                            cartItem.getProduct().getPrice(),
                            cartItem.getQuantity(),
                            subtotal
                    );

                }).toList();

        // 장바구니 주문 총액 구하기
        int totalPrice = items.stream()
                .mapToInt(CheckoutResponse.CheckoutItemResponse::subtotal)
                .sum();

        return new CheckoutResponse(items, totalPrice);

    }

    private List<CartItem> getValidateCartItems(Long memberId, List<Long> cartItemIds ){
        // cartItemIds가 비어있으면 "전체 장바구나", 아니면 "선택된 아이템만" 조회
        List<CartItem> cartItems = (cartItemIds.isEmpty())
                ? cartService.findCartEntities(memberId)
                : cartService.findCartEntitiesByIds(memberId, cartItemIds);

        // 1차 검증 : 주문할 아이템이 하나도 없으면 주문서 자체가 성립하지 않음
        // (전체조회 : 빈 바구니 / 선택 조회 : 넘긴 ID가 전부 남의 것/옶는 것)
        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        // 2차 검증 : 요청한 ID 개수와 조회된 개수가 다름 -> 일부가 "남의 것" 또는 "존재하지 않는 ID"
        // 일부만 주문되는 상황을 막음
        if ( cartItems.size() != cartItemIds.size()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        return cartItems;

    }

}
