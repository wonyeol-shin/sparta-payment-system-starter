package com.sparta.paymentsystem.infra.portone.client;

import com.sparta.paymentsystem.domain.payment.port.PaymentGateway;
import com.sparta.paymentsystem.domain.payment.port.PaymentGatewayResponse;
import com.sparta.paymentsystem.infra.portone.config.PortOneProperties;
import com.sparta.paymentsystem.infra.portone.dto.PortOneCancelRequest;
import com.sparta.paymentsystem.infra.portone.dto.PortOnePaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PortOneClient implements PaymentGateway {

    private final RestClient portOneRestClient;
    private final PortOneProperties portOneProperties;

    @Override
    public PaymentGatewayResponse getPayment(String paymentId) {
        // paymentId logging
        log.info("PortOne 결제 조회: {}", paymentId);

        // portOneRestClient https://api.portone.io/payments/{paymnetId}?storeId={storeId}
        PortOnePaymentResponse response = portOneRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/payments/{paymentId}")
                        .queryParam("storeId", portOneProperties.getStoreId())
                        .build(paymentId))
                .retrieve()
                .body(PortOnePaymentResponse.class);

        // PortOne 응답 결과인 PortOnePaymentResponse를 PaymentGatewayResponse로 변환
        return new PaymentGatewayResponse(
                response.id(),
                response.status(),
                response.amount().total()
        );
    }

//    @Override
//    public void cancelPayment(String paymentId, String reason) {
//        // paymentId logging
//        log.info("PortOne 결제 취소 요청: paymentId={}, reason={}", paymentId, reason);
//
//        // portOneRestClient https://api.portone.io/payments/{paymnetId}/cancel body: {reason, storeId}
//        portOneRestClient.post()
//                .uri("/payments/{paymentId}/cancel", paymentId)
//                .body(new PortOneCancelRequest(reason, portOneProperties.getStoreId()))
//                .retrieve()
//                .toBodilessEntity();
//    }

    /**
     * 결제 전액 취소
     * 교육용, 개념 설명을 위한 코드
     *
     * Idempotency-Key가 있으면 PortOne이
     * "아, 아까 그 요청이구나" 하고 이전 성공 응답을 그대로 반환해준다.
     */
    @Override
    public void cancelPayment(String paymentId, String reason) {
        String idempotencyKey = UUID.randomUUID().toString();
        log.info("PortOne 결제 취소 요청: paymentId={}, reason={}, idempotencyKey={}", paymentId, reason, idempotencyKey);

        int maxRetries = 3;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // portOneRestClient https://api.portone.io/payments/{paymnetId}/cancel body: {reason, storeId}
                portOneRestClient.post()
                        .uri("/payments/{paymentId}/cancel", paymentId)
                        .header("Idempotency-Key", "\"" + idempotencyKey + "\"") // 같은 키 유지
                        .body(new PortOneCancelRequest(reason, portOneProperties.getStoreId()))
                        .retrieve()
                        .toBodilessEntity();
                return;  // 성공하면 종료
            } catch (ResourceAccessException e) {
                // 네트워크 타임아웃 -> 재시도
                log.warn("PortOne 취소 요청 타임아웃 (시도 {}/{})", attempt, maxRetries);
                if (attempt == maxRetries) throw e;
            }
        }
    }
}