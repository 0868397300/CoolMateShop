package sopvn.demo.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sopvn.demo.core.exception.CustomException;
import sopvn.demo.core.service.NotificationService;
import sopvn.demo.entity.*;
import sopvn.demo.inventory.StockService;
import sopvn.demo.repository.OrderItemRepository;
import sopvn.demo.repository.OrderRepository;
import sopvn.demo.repository.OrderReturnRepository;
import sopvn.demo.repository.ProductVariantRepository;
import sopvn.demo.wallet.CoolCashService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ReturnService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderReturnRepository orderReturnRepository;
    private final ProductVariantRepository productVariantRepository;
    private final StockService stockService;
    private final CoolCashService coolCashService;
    private final NotificationService notificationService;

    public ReturnService(OrderRepository orderRepository,
                         OrderItemRepository orderItemRepository,
                         OrderReturnRepository orderReturnRepository,
                         ProductVariantRepository productVariantRepository,
                         StockService stockService,
                         CoolCashService coolCashService,
                         NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderReturnRepository = orderReturnRepository;
        this.productVariantRepository = productVariantRepository;
        this.stockService = stockService;
        this.coolCashService = coolCashService;
        this.notificationService = notificationService;
    }

    @Transactional
    public OrderReturn requestReturn(User user, Long orderId, Long orderItemId, 
                                     int quantity, String returnType, Long targetVariantId, 
                                     String reason, String evidenceImages) {
        if (user == null) {
            throw new CustomException("Vui lòng đăng nhập để gửi yêu cầu đổi trả.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Không tìm thấy đơn hàng #" + orderId));

        if (order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            throw new CustomException("Bạn không có quyền gửi yêu cầu đổi trả cho đơn hàng này.");
        }

        String st = order.getOrderStatus();
        if (!"DELIVERED".equalsIgnoreCase(st) && !"COMPLETED".equalsIgnoreCase(st)) {
            throw new CustomException("Chỉ những đơn hàng đã giao thành công mới được hỗ trợ đổi trả.");
        }

        if (order.getDeliveredAt() == null) {
            throw new CustomException("Đơn hàng chưa ghi nhận ngày giao thành công trên hệ thống.");
        }

        long daysPassed = ChronoUnit.DAYS.between(order.getDeliveredAt().toLocalDate(), LocalDate.now());
        if (daysPassed > 60) {
            throw new CustomException("Đơn hàng đã giao cách đây " + daysPassed + " ngày, đã vượt quá thời hạn đổi trả 60 ngày của Coolmate.");
        }

        if (orderItemId == null) {
            throw new CustomException("Vui lòng chọn sản phẩm cần đổi hoặc trả trong đơn hàng.");
        }

        OrderItem targetItem = null;
        if (order.getItems() != null) {
            for (OrderItem oi : order.getItems()) {
                if (oi.getId().equals(orderItemId)) {
                    targetItem = oi;
                    break;
                }
            }
        }

        if (targetItem == null) {
            throw new CustomException("Sản phẩm được chọn không thuộc đơn hàng #" + order.getOrderCode());
        }

        if (quantity <= 0) {
            throw new CustomException("Số lượng đổi trả phải lớn hơn 0.");
        }

        List<OrderReturn> prevReturns = orderReturnRepository.findByOrderItemId(orderItemId);
        int alreadyReturned = 0;
        for (OrderReturn r : prevReturns) {
            if (!"REJECTED".equalsIgnoreCase(r.getStatus())) {
                alreadyReturned += r.getQuantity();
            }
        }

        int remainingQty = targetItem.getQuantity() - alreadyReturned;
        if (quantity > remainingQty) {
            throw new CustomException("Số lượng yêu cầu đổi trả (" + quantity + ") vượt quá số lượng sản phẩm còn lại có thể đổi trả (" + remainingQty + ").");
        }

        ProductVariant targetVariant = null;
        if ("RETURN_SIZE".equalsIgnoreCase(returnType) || "RETURN_COLOR".equalsIgnoreCase(returnType)) {
            if (targetVariantId == null) {
                throw new CustomException("Vui lòng chọn biến thể (Màu / Size) muốn đổi sang.");
            }
            targetVariant = productVariantRepository.findById(targetVariantId)
                    .orElseThrow(() -> new CustomException("Biến thể muốn đổi không tồn tại."));

            if (!Boolean.TRUE.equals(targetVariant.getIsActive())) {
                throw new CustomException("Biến thể muốn đổi hiện đang tạm ngưng kinh doanh.");
            }

            if (targetVariant.getStockQuantity() < quantity) {
                throw new CustomException("Biến thể muốn đổi trong kho chỉ còn " + targetVariant.getStockQuantity() + " chiếc, không đủ số lượng " + quantity + ".");
            }
        }

        BigDecimal subtotal = order.getSubtotalAmount() != null && order.getSubtotalAmount().compareTo(BigDecimal.ZERO) > 0 
                ? order.getSubtotalAmount() : BigDecimal.ONE;

        BigDecimal totalDiscounts = (order.getVoucherDiscountAmount() != null ? order.getVoucherDiscountAmount() : BigDecimal.ZERO)
                .add(order.getComboDiscountAmount() != null ? order.getComboDiscountAmount() : BigDecimal.ZERO);

        BigDecimal itemTotalPrice = targetItem.getUnitPrice().multiply(BigDecimal.valueOf(quantity));
        BigDecimal itemRatio = itemTotalPrice.divide(subtotal, 4, RoundingMode.HALF_UP);
        BigDecimal itemAllocatedDiscount = totalDiscounts.multiply(itemRatio).setScale(0, RoundingMode.HALF_UP);

        BigDecimal refundAmount = itemTotalPrice.subtract(itemAllocatedDiscount);
        if (refundAmount.compareTo(BigDecimal.ZERO) < 0) refundAmount = BigDecimal.ZERO;

        OrderReturn req = new OrderReturn();
        req.setOrder(order);
        req.setOrderItem(targetItem);
        req.setUser(user);
        req.setReturnType(returnType);
        req.setTargetVariant(targetVariant);
        req.setQuantity(quantity);
        req.setReason(reason != null ? reason.trim() : "Khách yêu cầu đổi trả");
        req.setEvidenceImages(evidenceImages);
        req.setStatus("REQUESTED");
        req.setRefundAmount(refundAmount);

        OrderReturn saved = orderReturnRepository.save(req);
        notificationService.notifyReturnRequested(saved);
        return saved;
    }

    @Transactional
    public void processReturnApproval(Long returnId, String action, String refundMethod, String rejectReason, User staffUser) {
        OrderReturn req = orderReturnRepository.findById(returnId)
                .orElseThrow(() -> new CustomException("Không tìm thấy yêu cầu đổi trả #" + returnId));

        if (!"REQUESTED".equalsIgnoreCase(req.getStatus()) && !"PENDING".equalsIgnoreCase(req.getStatus()) && !"PROCESSING".equalsIgnoreCase(req.getStatus())) {
            throw new CustomException("Yêu cầu này đã được xử lý trước đó (" + req.getStatus() + ").");
        }

        if ("REJECT".equalsIgnoreCase(action)) {
            req.setStatus("REJECTED");
            req.setRejectionReason(rejectReason != null ? rejectReason.trim() : "Không đạt tiêu chuẩn kiểm định đổi trả.");
            req.setProcessedAt(LocalDateTime.now());
            orderReturnRepository.save(req);
            notificationService.notifyReturnRejected(req);
            return;
        }

        req.setStatus("COMPLETED");
        req.setProcessedAt(LocalDateTime.now());
        req.setRefundMethod(refundMethod != null ? refundMethod : "COOLCASH");
        req.setRefundStatus("COMPLETED");
        req.setRefundReference("REFUND-" + System.currentTimeMillis());

        if (req.getTargetVariant() != null) {
            stockService.restockFromReturn(req.getOrderItem().getVariant(), req.getQuantity(), req.getId());
            ProductVariant target = req.getTargetVariant();
            target.setStockQuantity(Math.max(0, target.getStockQuantity() - req.getQuantity()));
            productVariantRepository.save(target);
        } else {
            stockService.restockFromReturn(req.getOrderItem().getVariant(), req.getQuantity(), req.getId());
        }

        if (req.getRefundAmount() != null && req.getRefundAmount().compareTo(BigDecimal.ZERO) > 0) {
            if ("COOLCASH".equalsIgnoreCase(refundMethod)) {
                coolCashService.refundCoolCash(req.getUser(), req.getOrder(), req.getRefundAmount(), 
                        "Hoàn tiền vào ví CoolCash từ yêu cầu đổi trả #" + req.getId() + " (Đơn #" + req.getOrder().getOrderCode() + ")");
            }
        }

        orderReturnRepository.save(req);
        notificationService.notifyReturnApproved(req);
    }
}
