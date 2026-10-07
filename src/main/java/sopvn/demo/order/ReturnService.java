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
import sopvn.demo.repository.UserRepository;
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
    private final UserRepository userRepository;

    public ReturnService(OrderRepository orderRepository,
                         OrderItemRepository orderItemRepository,
                         OrderReturnRepository orderReturnRepository,
                         ProductVariantRepository productVariantRepository,
                         StockService stockService,
                         CoolCashService coolCashService,
                         NotificationService notificationService,
                         UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderReturnRepository = orderReturnRepository;
        this.productVariantRepository = productVariantRepository;
        this.stockService = stockService;
        this.coolCashService = coolCashService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
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

        // P0-6: DB row locking trên OrderItem để chống 2 request đổi trả đồng thời vượt quá số lượng đã mua
        OrderItem targetItem = orderItemRepository.findByIdForUpdate(orderItemId)
                .orElseThrow(() -> new CustomException("Sản phẩm đổi trả không tồn tại: " + orderItemId));

        if (targetItem.getOrder() == null || !targetItem.getOrder().getId().equals(order.getId())) {
            throw new CustomException("Sản phẩm được chọn không thuộc đơn hàng #" + order.getOrderCode());
        }

        if (quantity <= 0) {
            throw new CustomException("Số lượng đổi trả phải lớn hơn 0.");
        }

        // Partial return check: chỉ tính các return đang active (loại trừ REJECTED)
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

        BigDecimal subtotal = order.getSubtotalAmount() != null && order.getSubtotalAmount().compareTo(BigDecimal.ZERO) > 0 
                ? order.getSubtotalAmount() : BigDecimal.ONE;

        BigDecimal totalDiscounts = (order.getVoucherDiscountAmount() != null ? order.getVoucherDiscountAmount() : BigDecimal.ZERO)
                .add(order.getComboDiscountAmount() != null ? order.getComboDiscountAmount() : BigDecimal.ZERO);

        BigDecimal itemTotalPrice = targetItem.getUnitPrice().multiply(BigDecimal.valueOf(quantity));
        BigDecimal itemRatio = itemTotalPrice.divide(subtotal, 4, RoundingMode.HALF_UP);
        BigDecimal itemAllocatedDiscount = totalDiscounts.multiply(itemRatio).setScale(0, RoundingMode.HALF_UP);

        BigDecimal effectiveItemValue = itemTotalPrice.subtract(itemAllocatedDiscount);
        if (effectiveItemValue.compareTo(BigDecimal.ZERO) < 0) effectiveItemValue = BigDecimal.ZERO;

        ProductVariant targetVariant = null;
        BigDecimal refundAmount = BigDecimal.ZERO;

        if ("RETURN_SIZE".equalsIgnoreCase(returnType) || "RETURN_COLOR".equalsIgnoreCase(returnType)) {
            if (targetVariantId == null) {
                throw new CustomException("Vui lòng chọn biến thể (Màu / Size) muốn đổi sang.");
            }
            targetVariant = productVariantRepository.findById(targetVariantId)
                    .orElseThrow(() -> new CustomException("Biến thể muốn đổi không tồn tại: " + targetVariantId));

            if (!Boolean.TRUE.equals(targetVariant.getIsActive())) {
                throw new CustomException("Biến thể muốn đổi hiện đang tạm ngưng kinh doanh.");
            }

            // Quy tắc bắt buộc: Biến thể đổi sang phải thuộc cùng sản phẩm với sản phẩm ban đầu
            Product origProduct = targetItem.getVariant() != null ? targetItem.getVariant().getProduct() : null;
            if (origProduct == null || targetVariant.getProduct() == null 
                    || !origProduct.getId().equals(targetVariant.getProduct().getId())) {
                throw new CustomException("Biến thể đổi sang phải thuộc cùng sản phẩm với sản phẩm ban đầu.");
            }

            if (targetVariant.getStockQuantity() < quantity) {
                throw new CustomException("Biến thể muốn đổi trong kho chỉ còn " + targetVariant.getStockQuantity() + " chiếc, không đủ số lượng " + quantity + ".");
            }

            // P0-4: Xử lý chênh lệch giá biến thể đổi
            BigDecimal origUnitPrice = targetItem.getUnitPrice() != null ? targetItem.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal newUnitPrice = targetVariant.getSalePrice() != null ? targetVariant.getSalePrice() : origUnitPrice;

            if (newUnitPrice.compareTo(origUnitPrice) > 0) {
                // Không hỗ trợ nâng cấp miễn phí
                throw new CustomException("Biến thể đổi '" + targetVariant.getSku() + "' có giá (" + newUnitPrice + "đ) cao hơn giá sản phẩm ban đầu (" + origUnitPrice + "đ). Hệ thống không hỗ trợ nâng cấp sản phẩm miễn phí. Vui lòng chọn biến thể ngang giá hoặc chọn Trả hàng - Hoàn tiền để đặt đơn hàng mới.");
            } else if (newUnitPrice.compareTo(origUnitPrice) < 0) {
                // Biến thể mới rẻ hơn: Tính khoản chênh lệch hoàn lại cho khách
                BigDecimal priceDifference = origUnitPrice.subtract(newUnitPrice).multiply(BigDecimal.valueOf(quantity));
                refundAmount = priceDifference.min(effectiveItemValue);
            } else {
                refundAmount = BigDecimal.ZERO;
            }
        } else {
            // Hoàn tiền sản phẩm (Return & Refund)
            refundAmount = effectiveItemValue;
        }

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
    public void approveReturn(Long returnId, User staffUser) {
        OrderReturn req = orderReturnRepository.findById(returnId)
                .orElseThrow(() -> new CustomException("Không tìm thấy yêu cầu đổi trả #" + returnId));
        if (!"REQUESTED".equalsIgnoreCase(req.getStatus())) {
            throw new CustomException("Chỉ có thể phê duyệt yêu cầu ở trạng thái REQUESTED (hiện tại: " + req.getStatus() + ").");
        }
        req.setStatus("APPROVED");
        req.setProcessedAt(LocalDateTime.now());
        orderReturnRepository.save(req);
        notificationService.notifyReturnApproved(req);
    }

    @Transactional
    public void rejectReturn(Long returnId, String rejectReason, User staffUser) {
        OrderReturn req = orderReturnRepository.findById(returnId)
                .orElseThrow(() -> new CustomException("Không tìm thấy yêu cầu đổi trả #" + returnId));
        if ("COMPLETED".equalsIgnoreCase(req.getStatus()) || "REJECTED".equalsIgnoreCase(req.getStatus())) {
            throw new CustomException("Yêu cầu này đã kết thúc (" + req.getStatus() + "), không thể từ chối.");
        }
        req.setStatus("REJECTED");
        req.setRejectionReason(rejectReason != null ? rejectReason.trim() : "Không đạt tiêu chuẩn kiểm định đổi trả.");
        req.setProcessedAt(LocalDateTime.now());
        orderReturnRepository.save(req);
        notificationService.notifyReturnRejected(req);
    }

    @Transactional
    public void startProcessingReturn(Long returnId, User staffUser) {
        OrderReturn req = orderReturnRepository.findById(returnId)
                .orElseThrow(() -> new CustomException("Không tìm thấy yêu cầu đổi trả #" + returnId));
        if (!"APPROVED".equalsIgnoreCase(req.getStatus())) {
            throw new CustomException("Yêu cầu phải ở trạng thái APPROVED mới có thể tiếp nhận kiểm định (PROCESSING).");
        }
        req.setStatus("PROCESSING");
        orderReturnRepository.save(req);
    }

    @Transactional
    public void completeReturn(Long returnId, String refundMethod, User staffUser) {
        OrderReturn req = orderReturnRepository.findById(returnId)
                .orElseThrow(() -> new CustomException("Không tìm thấy yêu cầu đổi trả #" + returnId));

        // Idempotency: Nếu đã COMPLETED thì bỏ qua
        if ("COMPLETED".equalsIgnoreCase(req.getStatus())) {
            return;
        }

        // P0-5: Workflow nghiêm ngặt: REQUESTED -> APPROVED -> PROCESSING -> COMPLETED (không skip)
        if (!"PROCESSING".equalsIgnoreCase(req.getStatus())) {
            throw new CustomException("Yêu cầu đổi trả #" + returnId + " phải ở trạng thái PROCESSING (đang kiểm định) mới có thể hoàn tất (trạng thái hiện tại: " + req.getStatus() + ").");
        }

        req.setStatus("COMPLETED");
        req.setProcessedAt(LocalDateTime.now());
        req.setRefundMethod(refundMethod != null ? refundMethod : "COOLCASH");
        req.setRefundStatus("COMPLETED");
        req.setRefundReference("REFUND-" + req.getId() + "-" + System.currentTimeMillis());

        // 1. Nhập kho lại sản phẩm đổi trả (Idempotent theo returnId qua StockService)
        if (req.getOrderItem() != null && req.getOrderItem().getVariant() != null) {
            stockService.restockFromReturn(req.getOrderItem().getVariant(), req.getQuantity(), req.getId());
        }

        // 2. Nếu là đổi hàng (Exchange) -> Xuất kho biến thể mới qua StockService (Idempotent theo returnId)
        if (req.getTargetVariant() != null) {
            stockService.dispatchExchangeVariant(req.getTargetVariant(), req.getQuantity(), req.getId());
        }

        // 3. Hoàn tiền nếu có (Idempotent theo returnId)
        if (req.getRefundAmount() != null && req.getRefundAmount().compareTo(BigDecimal.ZERO) > 0) {
            if ("COOLCASH".equalsIgnoreCase(refundMethod) || refundMethod == null) {
                coolCashService.refundCoolCash(req.getUser(), req.getOrder(), req.getRefundAmount(), 
                        "Hoàn tiền ví CoolCash từ yêu cầu đổi trả #" + req.getId() + " (Đơn #" + req.getOrder().getOrderCode() + ")",
                        "RETURN_REFUND:" + req.getId());
            }
        }

        // 4. Thu hồi cashback đã trao thưởng trước đó và giảm tổng chi tiêu nếu có (Idempotent theo returnId)
        Order order = req.getOrder();
        if (order != null && order.getCoolcashEarned() != null && order.getCoolcashEarned().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal orderFinal = order.getFinalAmount() != null && order.getFinalAmount().compareTo(BigDecimal.ZERO) > 0 ? order.getFinalAmount() : BigDecimal.ONE;
            BigDecimal cashbackToRevoke = order.getCoolcashEarned()
                    .multiply(req.getRefundAmount())
                    .divide(orderFinal, 0, RoundingMode.HALF_UP);
            if (cashbackToRevoke.compareTo(BigDecimal.ZERO) > 0 && req.getUser() != null) {
                coolCashService.revokeOrderCashback(req.getUser(), order, cashbackToRevoke, 
                        "Thu hồi hoàn tiền do đổi trả sản phẩm đơn #" + order.getOrderCode(), 
                        "RETURN_CASHBACK_REVOKE:" + req.getId());
            }
        }

        if (req.getUser() != null && req.getRefundAmount() != null && req.getRefundAmount().compareTo(BigDecimal.ZERO) > 0) {
            User u = req.getUser();
            BigDecimal currentSpent = u.getTotalSpent() != null ? u.getTotalSpent() : BigDecimal.ZERO;
            BigDecimal newSpent = currentSpent.subtract(req.getRefundAmount()).max(BigDecimal.ZERO);
            u.setTotalSpent(newSpent);

            if (newSpent.compareTo(BigDecimal.valueOf(6000000)) >= 0) {
                u.setMembershipTier("PLATINUM");
            } else if (newSpent.compareTo(BigDecimal.valueOf(3000000)) >= 0) {
                u.setMembershipTier("GOLD");
            } else if (newSpent.compareTo(BigDecimal.valueOf(1000000)) >= 0) {
                u.setMembershipTier("SILVER");
            } else {
                u.setMembershipTier("NEW");
            }
            userRepository.save(u);
        }

        orderReturnRepository.save(req);
        notificationService.notifyReturnCompleted(req);
    }

    @Transactional
    public void processReturnApproval(Long returnId, String action, String refundMethod, String rejectReason, User staffUser) {
        if (action == null || action.isBlank()) {
            throw new CustomException("Hành động xử lý đổi trả không được để trống.");
        }

        String act = action.trim().toUpperCase();
        switch (act) {
            case "APPROVE":
                approveReturn(returnId, staffUser);
                break;
            case "REJECT":
                rejectReturn(returnId, rejectReason, staffUser);
                break;
            case "PROCESS":
                startProcessingReturn(returnId, staffUser);
                break;
            case "COMPLETE":
                completeReturn(returnId, refundMethod, staffUser);
                break;
            default:
                throw new CustomException("Hành động xử lý đổi trả không hợp lệ: '" + action + "'. Chỉ chấp nhận: APPROVE, REJECT, PROCESS, COMPLETE.");
        }
    }
}
