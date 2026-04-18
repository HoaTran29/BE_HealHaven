package backend.healhaven.controller;

import backend.healhaven.dto.request.SePayWebhookRequest;
import backend.healhaven.service.SePayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Payment Webhook APIs (SePay)")
public class SePayWebhookController {

    private final SePayService sePayService;

    @Value("${sepay.api-key}")
    private String sePayApiKey;

    /**
     * Endpoint nhận webhook từ SePay khi có giao dịch ngân hàng.
     * SePay gửi POST với JSON body chứa thông tin giao dịch.
     * Header: Authorization: Apikey {your-api-key}
     */
    @PostMapping("/sepay")
    @Operation(
            summary = "SePay Webhook",
            description = "Nhận thông báo giao dịch từ SePay. Endpoint này được gọi tự động bởi SePay, không cần gọi thủ công."
    )
    public ResponseEntity<Map<String, Boolean>> handleSePayWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody SePayWebhookRequest request) {

        // Validate API Key
        if (!isValidApiKey(authorization)) {
            log.warn("SePay webhook: Invalid API key from request");
            return ResponseEntity.status(401).body(Map.of("success", false));
        }

        // Xử lý webhook
        boolean success = sePayService.processWebhook(request);

        // SePay yêu cầu response {"success": true} với status 200
        return ResponseEntity.ok(Map.of("success", success));
    }

    private boolean isValidApiKey(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return false;
        }
        // SePay gửi header: "Apikey {key}"
        String prefix = "Apikey ";
        if (authorization.startsWith(prefix)) {
            String providedKey = authorization.substring(prefix.length()).trim();
            return sePayApiKey.equals(providedKey);
        }
        return false;
    }
}
