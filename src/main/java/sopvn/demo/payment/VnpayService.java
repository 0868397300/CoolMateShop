package sopvn.demo.payment;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import sopvn.demo.config.VnpayConfig;
import sopvn.demo.entity.Order;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VnpayService {

    private final VnpayConfig vnpayConfig;

    public VnpayService(VnpayConfig vnpayConfig) {
        this.vnpayConfig = vnpayConfig;
    }

    public String getClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return (ip != null && !ip.isEmpty()) ? ip : "127.0.0.1";
    }

    /**
     * API chuẩn hóa cho PaymentController
     */
    public String createPaymentUrl(Order order, String clientIp) {
        if (order == null) {
            throw new IllegalArgumentException("Đơn hàng không được null khi tạo URL thanh toán VNPAY");
        }
        long amount = order.getFinalAmount() != null ? order.getFinalAmount().longValue() : 0L;
        String orderInfo = "Thanh toan don hang Coolmate #" + order.getOrderCode();
        String txnRef = (order.getVnpayTxnRef() != null && !order.getVnpayTxnRef().isBlank())
                ? order.getVnpayTxnRef()
                : order.getOrderCode();
        return createPaymentUrl(amount, orderInfo, txnRef, vnpayConfig.getVnpReturnUrl(), clientIp);
    }

    public String createPaymentUrl(long amount, String orderInfo, String txnRef, String returnUrl, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        return createPaymentUrl(amount, orderInfo, txnRef, returnUrl, clientIp);
    }

    public String createPaymentUrl(long amount, String orderInfo, String txnRef, String returnUrl, String clientIp) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_OrderInfo = orderInfo != null ? orderInfo : "Thanh toan don hang Coolmate";
        String orderType = "other";
        String vnp_TxnRef = (txnRef != null && !txnRef.isBlank()) ? txnRef : String.valueOf(System.currentTimeMillis());
        String vnp_IpAddr = (clientIp != null && !clientIp.isBlank()) ? clientIp : "127.0.0.1";
        String vnp_TmnCode = vnpayConfig.getVnpTmnCode();

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount * 100)); // VNPAY tính theo đồng x 100
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", returnUrl != null ? returnUrl : vnpayConfig.getVnpReturnUrl());
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = VnpayConfig.hmacSHA512(vnpayConfig.getSecretKey(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        return vnpayConfig.getVnpPayUrl() + "?" + queryUrl;
    }

    public boolean validateSignature(Map<String, String> fields, String secureHash) {
        if (fields == null || secureHash == null) return false;
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }
        String calculatedHash = VnpayConfig.hmacSHA512(vnpayConfig.getSecretKey(), hashData.toString());
        return calculatedHash.equalsIgnoreCase(secureHash);
    }
}