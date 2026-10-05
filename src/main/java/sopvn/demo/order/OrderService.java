package sopvn.demo.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sopvn.demo.core.exception.CustomException;
import sopvn.demo.entity.*;
import sopvn.demo.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        ProductVariantRepository productVariantRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productVariantRepository = productVariantRepository;
    }

    @Transactional
    public Order createOrder(User user, String recipientName, String phone, String email, String address, String note,
                            List<CartItem> cartItems, String paymentMethod) {
        return createOrder(user, recipientName, phone, email, address, note, cartItems, paymentMethod, null);
    }

    @Transactional
    public Order createOrder(User user, String recipientName, String phone, String email, String address, String note,
                            List<CartItem> cartItems, String paymentMethod, BigDecimal customShippingFee) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new CustomException("Giỏ hàng của bạn đang trống! Vui lòng chọn ít nhất 1 sản phẩm trước khi thanh toán.");
        }

        if (recipientName == null || recipientName.trim().isEmpty()) {
            throw new CustomException("Vui lòng nhập họ và tên người nhận hàng.");
        }

        if (phone == null || !phone.trim().matches("^(0[3|5|7|8|9])+([0-9]{8})$")) {
            throw new CustomException("Số điện thoại nhận hàng không hợp lệ. Vui lòng nhập số điện thoại gồm 10 chữ số (bắt đầu bằng 03, 05, 07, 08, 09).");
        }

        if (address == null || address.trim().length() < 6) {
            throw new CustomException("Vui lòng nhập địa chỉ nhận hàng chi tiết (số nhà, đường, phường/xã, quận/huyện) để đơn vị vận chuyển có thể giao đến tận nơi.");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        String orderCode = "CM" + System.currentTimeMillis();

        Order order = new Order();
        order.setOrderCode(orderCode);
        order.setUser(user);
        order.setRecipientName(recipientName);
        order.setRecipientPhone(phone);
        order.setRecipientEmail(email);
        order.setShippingAddress(address);
        order.setNote(note);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus("COD".equalsIgnoreCase(paymentMethod) ? "UNPAID" : "PAID");
        order.setOrderStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        order = orderRepository.save(order);

        for (CartItem ci : cartItems) {
            ProductVariant variant = ci.getVariant();
            String pName = variant.getProduct() != null ? variant.getProduct().getName() : "Sản phẩm";
            String cName = variant.getColor() != null ? variant.getColor().getName() : "";
            String sName = variant.getSize() != null ? variant.getSize().getName() : "";

            if (variant.getStockQuantity() <= 0) {
                throw new CustomException("Sản phẩm '" + pName + "' (Màu: " + cName + ", Size: " + sName + ") hiện đã hết hàng trong kho. Vui lòng xóa khỏi giỏ hoặc chọn phân loại khác.");
            }

            if (variant.getStockQuantity() < ci.getQuantity()) {
                throw new CustomException("Sản phẩm '" + pName + "' (Màu: " + cName + ", Size: " + sName + ") trong kho chỉ còn " + variant.getStockQuantity() + " sản phẩm, không đủ số lượng " + ci.getQuantity() + " mà bạn đã chọn. Vui lòng cập nhật lại giỏ hàng.");
            }

            variant.setStockQuantity(variant.getStockQuantity() - ci.getQuantity());
            productVariantRepository.save(variant);

            BigDecimal lineTotal = variant.getSalePrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
            subtotal = subtotal.add(lineTotal);

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setVariant(variant);
            oi.setProductNameSnapshot(pName);
            oi.setSkuSnapshot(variant.getSku());
            oi.setColorNameSnapshot(cName);
            oi.setSizeNameSnapshot(sName);
            oi.setQuantity(ci.getQuantity());
            oi.setUnitPrice(variant.getSalePrice());
            oi.setTotalPrice(lineTotal);
            oi.setIsReviewed(false);
            orderItemRepository.save(oi);
        }

        // Logic Freeship đơn từ 200.000đ
        BigDecimal shippingFee;
        if (subtotal.compareTo(BigDecimal.valueOf(200000)) >= 0) {
            shippingFee = BigDecimal.ZERO;
        } else if (customShippingFee != null && customShippingFee.compareTo(BigDecimal.ZERO) >= 0) {
            shippingFee = customShippingFee;
        } else {
            shippingFee = BigDecimal.valueOf(25000);
        }

        BigDecimal finalAmount = subtotal.add(shippingFee);

        order.setSubtotalAmount(subtotal);
        order.setShippingFee(shippingFee);
        order.setFinalAmount(finalAmount);
        order.setCoolcashEarned(finalAmount.multiply(BigDecimal.valueOf(0.05)));

        return orderRepository.save(order);
    }
}
