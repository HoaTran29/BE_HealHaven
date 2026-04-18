package backend.healhaven.service;

import backend.healhaven.dto.request.SePayWebhookRequest;
import backend.healhaven.entity.Booking;
import backend.healhaven.enums.BookingStatus;
import backend.healhaven.enums.NotificationType;
import backend.healhaven.enums.PaymentStatus;
import backend.healhaven.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SePayService {

    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    // Regex tìm mã booking trong nội dung chuyển khoản: HH123, HH 123, hh123
    private static final Pattern BOOKING_CODE_PATTERN = Pattern.compile("HH\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    /**
     * Xử lý webhook từ SePay khi có giao dịch ngân hàng.
     * Tìm mã booking trong nội dung chuyển khoản (HH{bookingId}),
     * kiểm tra số tiền, và tự động xác nhận thanh toán.
     */
    @Transactional
    public boolean processWebhook(SePayWebhookRequest request) {
        log.info("SePay webhook received: id={}, gateway={}, amount={}, content='{}'",
                request.getId(), request.getGateway(), request.getTransferAmount(), request.getContent());

        // Chỉ xử lý tiền VÀO
        if (!"in".equalsIgnoreCase(request.getTransferType())) {
            log.info("Skipping outgoing transaction");
            return true; // Vẫn return true để SePay không retry
        }

        // Tìm mã booking từ nội dung chuyển khoản
        Integer bookingId = extractBookingId(request.getContent());
        if (bookingId == null) {
            log.warn("Could not extract booking ID from content: '{}'", request.getContent());
            return true; // Không match booking nào → skip, không retry
        }

        // Tìm booking trong DB
        Optional<Booking> optBooking = bookingRepository.findById(bookingId);
        if (optBooking.isEmpty()) {
            log.warn("Booking #{} not found in database", bookingId);
            return true;
        }

        Booking booking = optBooking.get();

        // Idempotency: nếu đã PAID rồi thì skip
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            log.info("Booking #{} already PAID, skipping", bookingId);
            return true;
        }

        // Kiểm tra số tiền (transferAmount >= totalPrice)
        long requiredAmount = booking.getTotalPrice().longValue();
        if (request.getTransferAmount() < requiredAmount) {
            log.warn("Booking #{}: insufficient amount. Required={}, received={}",
                    bookingId, requiredAmount, request.getTransferAmount());
            // Gửi notification cho user biết thiếu tiền
            notificationService.sendNotification(
                    booking.getAttendee().getUserId(),
                    "Thanh toán chưa đủ",
                    "Booking #" + bookingId + " nhận được " + formatVND(request.getTransferAmount())
                            + " nhưng cần " + formatVND(requiredAmount)
                            + ". Vui lòng chuyển thêm phần thiếu.",
                    NotificationType.SEPAY_PAYMENT_INSUFFICIENT,
                    bookingId
            );
            return true;
        }

        // ✅ Thanh toán thành công!
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setBookingStatus(BookingStatus.PAID);
        bookingRepository.save(booking);

        // Gửi notification cho User
        notificationService.sendNotification(
                booking.getAttendee().getUserId(),
                "Thanh toán thành công! 🎉",
                "Booking #" + bookingId + " (" + booking.getWorkshop().getTitle()
                        + ") đã được xác nhận thanh toán " + formatVND(request.getTransferAmount())
                        + " qua " + request.getGateway() + ". Chúc bạn tham gia vui vẻ!",
                NotificationType.SEPAY_PAYMENT_SUCCESS,
                bookingId
        );

        log.info("✅ Booking #{} payment confirmed via SePay. Amount: {}", bookingId, request.getTransferAmount());
        return true;
    }

    /**
     * Trích xuất bookingId từ nội dung chuyển khoản.
     * Tìm pattern: HH123, HH 123, hh123, ...
     */
    private Integer extractBookingId(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        Matcher matcher = BOOKING_CODE_PATTERN.matcher(content);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String formatVND(long amount) {
        return String.format("%,d VNĐ", amount);
    }
}
