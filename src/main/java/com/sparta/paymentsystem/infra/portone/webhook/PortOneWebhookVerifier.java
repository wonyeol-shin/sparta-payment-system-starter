package com.sparta.paymentsystem.infra.portone.webhook;

import com.sparta.paymentsystem.infra.portone.config.PortOneProperties;
import io.portone.sdk.server.errors.WebhookVerificationException;
import io.portone.sdk.server.webhook.Webhook;
import io.portone.sdk.server.webhook.WebhookVerifier;
import org.springframework.stereotype.Component;

/**
 * PortOne 공식 Server SDK를 이용한 웹훅 시그니처 검증기
 *
 * SDK가 HMAC-SHA256 검증·타임스탬프 검증·역직렬화를 모두 담당하므로,
 * 성공 시 타입 세이프한 {@link Webhook} 객체를 반환해 핸들러가 instanceof로 분기할 수 있게 한다.
 */
@Component
public class PortOneWebhookVerifier {

    private final WebhookVerifier webhookVerifier;

    public PortOneWebhookVerifier(PortOneProperties properties) {
        this.webhookVerifier = new WebhookVerifier(properties.getWebhookSecret());
    }

    /**
     * 웹훅 메시지를 검증하고 파싱된 {@link Webhook} 객체를 반환한다.
     *
     * @throws WebhookVerificationException 시그니처·타임스탬프 검증 실패 시
     */
    public Webhook verify(String body, String webhookId, String signature, String timestamp) throws WebhookVerificationException {
        return webhookVerifier.verify(body, webhookId, signature, timestamp);
    }

}
