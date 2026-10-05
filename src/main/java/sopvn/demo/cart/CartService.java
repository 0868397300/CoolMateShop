package sopvn.demo.cart;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sopvn.demo.cart.dto.CartItemDto;
import sopvn.demo.cart.dto.CartResponse;
import sopvn.demo.core.exception.CustomException;
import sopvn.demo.entity.*;
import sopvn.demo.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ComboRuleRepository comboRuleRepository;

    private static final BigDecimal FREESHIP_THRESHOLD = BigDecimal.valueOf(200000);
    private static final BigDecimal DEFAULT_SHIPPING_FEE = BigDecimal.valueOf(25000);

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductVariantRepository productVariantRepository,
                       ComboRuleRepository comboRuleRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productVariantRepository = productVariantRepository;
        this.comboRuleRepository = comboRuleRepository;
    }

    @Transactional
    public Cart getOrCreateCart(User user, String sessionId) {
        if (user != null) {
            return cartRepository.findByUserId(user.getId())
                    .orElseGet(() -> {
                        Cart c = new Cart();
                        c.setUser(user);
                        c.setUpdatedAt(LocalDateTime.now());
                        return cartRepository.save(c);
                    });
        } else {
            return cartRepository.findBySessionId(sessionId)
                    .orElseGet(() -> {
                        Cart c = new Cart();
                        c.setSessionId(sessionId);
                        c.setUpdatedAt(LocalDateTime.now());
                        return cartRepository.save(c);
                    });
        }
    }

    @Transactional
    public void addToCart(User user, String sessionId, Long variantId, int quantity) {
        if (variantId == null) {
            throw new CustomException("Vui lòng chọn màu sắc và kích cỡ (Size) sản phẩm trước khi thêm vào giỏ hàng.");
        }
        if (quantity <= 0) {
            throw new CustomException("Số lượng sản phẩm đặt mua tối thiểu phải từ 1 trở lên.");
        }

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new CustomException("Phiên bản sản phẩm bạn chọn không tồn tại hoặc đã ngừng kinh doanh."));

        String pName = variant.getProduct() != null ? variant.getProduct().getName() : "Sản phẩm";
        String cName = variant.getColor() != null ? variant.getColor().getName() : "";
        String sName = variant.getSize() != null ? variant.getSize().getName() : "";

        if (variant.getStockQuantity() <= 0) {
            throw new CustomException("Sản phẩm '" + pName + "' (Màu: " + cName + ", Size: " + sName + ") hiện đã tạm hết hàng trong kho. Vui lòng chọn màu hoặc kích cỡ khác.");
        }

        if (variant.getStockQuantity() < quantity) {
            throw new CustomException("Số lượng bạn yêu cầu (" + quantity + " sản phẩm) vượt quá số lượng còn lại trong kho (chỉ còn " + variant.getStockQuantity() + " sản phẩm). Vui lòng giảm bớt số lượng.");
        }

        Cart cart = getOrCreateCart(user, sessionId);
        CartItem cartItem = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variantId)
                .orElse(null);

        if (cartItem == null) {
            cartItem = new CartItem(cart, variant, quantity);
        } else {
            int newQty = cartItem.getQuantity() + quantity;
            if (variant.getStockQuantity() < newQty) {
                throw new CustomException("Giỏ hàng của bạn đã có sẵn " + cartItem.getQuantity() + " sản phẩm này. Thêm tiếp " + quantity + " sản phẩm nữa sẽ vượt quá tồn kho hiện tại (chỉ còn " + variant.getStockQuantity() + " sản phẩm).");
            }
            cartItem.setQuantity(newQty);
        }
        cartItemRepository.save(cartItem);
    }

    @Transactional
    public void updateQuantity(Long itemId, int quantity) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException("Không tìm thấy sản phẩm này trong giỏ hàng của bạn."));

        if (quantity <= 0) {
            removeItem(itemId);
        } else {
            ProductVariant v = item.getVariant();
            if (v.getStockQuantity() < quantity) {
                throw new CustomException("Không thể cập nhật lên " + quantity + " sản phẩm do kho chỉ còn lại " + v.getStockQuantity() + " sản phẩm.");
            }
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }
    }

    @Transactional
    public void updateQuantity(User user, String sessionId, Long itemId, int quantity) {
        updateQuantity(itemId, quantity);
    }

    @Transactional
    public void removeItem(Long itemId) {
        CartItem item = cartItemRepository.findById(itemId).orElse(null);
        if (item != null) {
            Cart cart = item.getCart();
            if (cart != null && cart.getItems() != null) {
                cart.getItems().removeIf(ci -> ci.getId().equals(itemId));
                cartRepository.save(cart);
            }
            cartItemRepository.delete(item);
        }
        try {
            cartItemRepository.deleteDirectlyById(itemId);
            cartItemRepository.flush();
        } catch (Exception ignored) {}
    }

    @Transactional
    public void removeItem(User user, String sessionId, Long itemId) {
        removeItem(itemId);
    }

    @Transactional
    public void clearCart(User user, String sessionId) {
        Cart cart = getOrCreateCart(user, sessionId);
        if (cart != null) {
            if (cart.getItems() != null) {
                cart.getItems().clear();
                cartRepository.save(cart);
            }
            try {
                cartItemRepository.deleteAllByCartId(cart.getId());
                cartItemRepository.flush();
            } catch (Exception ignored) {}
        }
    }

    @Transactional(readOnly = true)
    public CartResponse getCartSummary(User user, String sessionId) {
        Cart cart = getOrCreateCart(user, sessionId);
        List<CartItem> items = cart.getItems() != null ? cart.getItems() : new ArrayList<>();

        List<CartItemDto> itemDtos = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalQty = 0;

        for (CartItem ci : items) {
            ProductVariant v = ci.getVariant();
            Product p = v.getProduct();

            BigDecimal price = v.getSalePrice() != null ? v.getSalePrice() : BigDecimal.ZERO;
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(ci.getQuantity()));
            subtotal = subtotal.add(lineTotal);
            totalQty += ci.getQuantity();

            CartItemDto dto = new CartItemDto();
            dto.setId(ci.getId());
            dto.setVariantId(v.getId());
            dto.setProductName(p != null ? p.getName() : "Sản phẩm Coolmate");
            dto.setProductSlug(p != null ? p.getSlug() : "");
            dto.setColorName(v.getColor() != null ? v.getColor().getName() : "");
            dto.setSizeName(v.getSize() != null ? v.getSize().getName() : "");
            dto.setPrice(price);
            dto.setSalePrice(price);
            dto.setQuantity(ci.getQuantity());
            dto.setItemSubtotal(lineTotal);
            dto.setLineTotal(lineTotal);
            dto.setImageUrl(p != null && p.getThumbnailUrl() != null ? p.getThumbnailUrl() : "/images/products/ao-thun-compact-den-1.jpg");
            dto.setStockQuantity(v.getStockQuantity());
            itemDtos.add(dto);
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        List<ComboRule> rules = comboRuleRepository.findByIsActiveTrue();
        if (rules != null && !rules.isEmpty()) {
            rules.sort((r1, r2) -> {
                BigDecimal p1 = r1.getDiscountPercentage() != null ? r1.getDiscountPercentage() : BigDecimal.ZERO;
                BigDecimal p2 = r2.getDiscountPercentage() != null ? r2.getDiscountPercentage() : BigDecimal.ZERO;
                return p2.compareTo(p1);
            });
            for (ComboRule rule : rules) {
                if (rule.getMinQuantity() != null && totalQty >= rule.getMinQuantity()) {
                    if (rule.getDiscountPercentage() != null) {
                        discountAmount = subtotal.multiply(rule.getDiscountPercentage())
                                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                        break;
                    }
                }
            }
        }

        boolean isFreeship = subtotal.compareTo(FREESHIP_THRESHOLD) >= 0;
        BigDecimal shippingFee = isFreeship ? BigDecimal.ZERO : DEFAULT_SHIPPING_FEE;
        BigDecimal finalTotal = subtotal.subtract(discountAmount).add(shippingFee);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        CartResponse resp = new CartResponse();
        resp.setItems(itemDtos);
        resp.setSubtotal(subtotal);
        resp.setDiscountAmount(discountAmount);
        resp.setShippingFee(shippingFee);
        resp.setFinalTotal(finalTotal);
        resp.setTotalQuantity(totalQty);
        resp.setFreeshipQualified(isFreeship);
        return resp;
    }
}
