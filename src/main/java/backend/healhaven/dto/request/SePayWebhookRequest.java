package backend.healhaven.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO nhận webhook từ SePay khi có giao dịch ngân hàng.
 * Docs: https://my.sepay.vn/userguide/docs/webhooks-api
 */
@Data
public class SePayWebhookRequest {

    private Integer id;             // Mã giao dịch SePay

    private String gateway;         // Tên ngân hàng (MBBank, VietinBank, ...)

    @JsonProperty("accountNumber")
    private String accountNumber;   // Số tài khoản nhận tiền

    private String code;            // Mã tham chiếu (SePay tự extract từ content)

    private String content;         // Nội dung chuyển khoản

    @JsonProperty("transferAmount")
    private Long transferAmount;    // Số tiền chuyển (VNĐ)

    @JsonProperty("transferType")
    private String transferType;    // "in" = tiền vào, "out" = tiền ra

    @JsonProperty("transactionDate")
    private String transactionDate; // Ngày giao dịch

    private String subAccount;      // Sub-account (nếu có)

    @JsonProperty("referenceCode")
    private String referenceCode;   // Mã tham chiếu ngân hàng
}
