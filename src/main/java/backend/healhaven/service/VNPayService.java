package backend.healhaven.service;

import backend.healhaven.config.VNPayConfig;
import backend.healhaven.entity.Booking;
import backend.healhaven.exception.ResourceNotFoundException;
import backend.healhaven.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayService {

    private final VNPayConfig vnPayConfig;
    private final BookingRepository bookingRepository;

    /**
     * Tạo URL thanh toán VNPay từ bookingId và IP của client.
     *
     * @param bookingId ID booking cần thanh toán
     * @param ipAddress IP của client gọi API
     * @return URL đầy đủ để redirect sang trang VNPay
     */
    public String createPaymentUrl(Integer bookingId, String ipAddress) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        // VNPay yêu cầu amount * 100 (đơn vị: đồng → phải nhân 100)
        long amount = booking.getTotalPrice().longValue() * 100;

        Map<String, String> params = vnPayConfig.getVNPayParams();
        params.put("vnp_TxnRef", String.valueOf(bookingId));
        params.put("vnp_Amount", String.valueOf(amount));
        params.put("vnp_OrderInfo", "Thanh toan workshop HealHaven - Ma don: " + bookingId);
        params.put("vnp_OrderType", "other");
        params.put("vnp_IpAddr", ipAddress);
        params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());

        return buildPaymentUrl(params);
    }

    /**
     * Xác thực chữ ký (checksum) từ VNPay callback.
     * So sánh vnp_SecureHash VNPay gửi về với hash tính lại từ các params.
     *
     * @param params Toàn bộ query params từ VNPay (bao gồm cả vnp_SecureHash)
     * @return true nếu chữ ký hợp lệ
     */
    public boolean verifyPayment(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isEmpty()) {
            log.warn("VNPay callback missing vnp_SecureHash");
            return false;
        }

        // Tạo bản copy các params, loại bỏ vnp_SecureHash và vnp_SecureHashType
        Map<String, String> filteredParams = new TreeMap<>(params);
        filteredParams.remove("vnp_SecureHash");
        filteredParams.remove("vnp_SecureHashType");

        // Build hash data string (sort tự động nhờ TreeMap)
        StringBuilder hashData = new StringBuilder();
        for (Map.Entry<String, String> entry : filteredParams.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                if (hashData.length() > 0) {
                    hashData.append('&');
                }
                hashData.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII));
                hashData.append('=');
                hashData.append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII));
            }
        }

        String computedHash = vnPayConfig.hmacSHA512(
                vnPayConfig.getHashSecret(), hashData.toString());

        boolean valid = computedHash.equalsIgnoreCase(receivedHash);
        if (!valid) {
            log.warn("VNPay signature mismatch. Expected: {}, Received: {}", computedHash, receivedHash);
        }
        return valid;
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    /**
     * Build URL thanh toán với chữ ký HMAC-SHA512.
     * VNPay bắt buộc các params phải được sort alphabet và URL-encode (US_ASCII).
     */
    private String buildPaymentUrl(Map<String, String> params) {
        // TreeMap tự sort theo alphabet key
        Map<String, String> sortedParams = new TreeMap<>(params);

        StringBuilder hashData = new StringBuilder();
        StringBuilder queryStr = new StringBuilder();

        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                String encodedKey = URLEncoder.encode(key, StandardCharsets.US_ASCII);
                String encodedValue = URLEncoder.encode(value, StandardCharsets.US_ASCII);

                if (hashData.length() > 0) {
                    hashData.append('&');
                    queryStr.append('&');
                }
                hashData.append(encodedKey).append('=').append(encodedValue);
                queryStr.append(encodedKey).append('=').append(encodedValue);
            }
        }

        // Ký checksum
        String secureHash = vnPayConfig.hmacSHA512(
                vnPayConfig.getHashSecret(), hashData.toString());
        queryStr.append("&vnp_SecureHash=").append(secureHash);

        String paymentUrl = vnPayConfig.getPayUrl() + "?" + queryStr;
        log.info("Created VNPay payment URL for booking {}", params.get("vnp_TxnRef"));
        return paymentUrl;
    }
}
