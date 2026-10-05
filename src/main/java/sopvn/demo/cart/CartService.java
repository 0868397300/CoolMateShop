package sopvn.demo.cart;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sopvn.demo.cart.dto.CartItemDto;
import sopvn.demo.cart.dto.CartResponse;
import sopvn.demo.cart.dto.PricingSummaryDTO;
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
    private final ProductImageRepository productImageRepository;
    private final PricingService pricingService;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductVariantRepository productVariantRepository,
                       ProductImageRepository productImageRepository,
                       PricingService pricingService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
        this.pricingService = pricingService;
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
        } else if (sessionId != null && !sessionId.isBlank()) {
            return cartRepository.findBySessionId(sessionId)
                    .orElseGet(() -> {
                        Cart c = new Cart();
                        c.setSessionId(sessionId);
                        c.setUpdatedAt(LocalDateTime.now());
                        return cartRepository.save(c);
                    });
        }
        return null;
    }

    /**
     * Merge giỏ hàng vãng lai (Guest) vào tài khoản thành viên khi đăng nhập
     */
    @Transactional
    public void mergeGuestCartToUser(String sessionId, User user) {
        if (sessionId == null || user == null) return;
        Cart guestCart = cartRepository.findBySessionId(sessionId).orElse(null);
        if (guestCart == null || guestCart.getItems() == null || guestCart.getItems().isEmpty()) {
            return;
        }

        Cart userCart = getOrCreateCart(user, null);
        if (userCart.getItems() == null) {
            userCart.setItems(new ArrayList<>());
        }

        for (CartItem guestItem : guestCart.getItems()) {
            ProductVariant v = guestItem.getVariant();
            if (v == null || !Boolean.TRUE.equals(v.getIsActive())) continue;

            CartItem existing = null;
            for (CartItem ui : userCart.getItems()) {
                if (ui.getVariant() != null && ui.getVariant().getId().equals(v.getId())) {
                    existing = ui;
                    break;
                }
            }

            int availableStock = v.getStockQuantity() != null ? v.getStockQuantity() : 0;
            if (existing != null) {
                int combinedQty = existing.getQuantity() + guestItem.getQuantity();
                existing.setQuantity(Math.min(availableStock, combinedQty));
                cartItemRepository.save(existing);
            } else {
                int addQty = Math.min(availableStock, guestItem.getQuantity());
                if (addQty > 0) {
                    CartItem newItem = new CartItem();
                    newItem.setCart(userCart);
                    newItem.setVariant(v);
                    newItem.setQuantity(addQty);
                    userCart.getItems().add(newItem);
                    cartItemRepository.save(newItem);
                }
            }
        }

        // Xóa giỏ hàng guest sau khi merge thành công
        try {
            cartItemRepository.deleteAllByCartId(guestCart.getId());
            cartRepository.delete(guestCart);
        } catch (Exception ignored) {}
    }

    @Transactional
    public void addToCart(User user, String sessionId, Long variantId, int quantity) {
        if (variantId == null) {
            throw new CustomException("Vui lòng chọn màu sắc và kích cỡ sản phẩm.");
        }
        if (quantity <= 0) {
            throw new CustomException("Số lượng sản phẩm thêm vào giỏ phải lớn hơn 0.");
        }

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new CustomException("Phiên bản sản phẩm không tồn tại."));

        if (!Boolean.TRUE.equals(variant.getIsActive())) {
            throw new CustomException("Phiên bản này hiện tại đang tạm ngưng kinh doanh.");
        }

        Cart cart = getOrCreateCart(user, sessionId);
        if (cart == null) {
            throw new CustomException("Không thể khởi tạo phiên giỏ hàng.");
        }

        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }

        CartItem existingItem = null;
        for (CartItem ci : cart.getItems()) {
            if (ci.getVariant() != null && ci.getVariant().getId().equals(variantId)) {
                existingItem = ci;
                break;
            }
        }

        int currentQtyInCart = (existingItem != null) ? existingItem.getQuantity() : 0;
        int requestedTotal = currentQtyInCart + quantity;

        int stock = variant.getStockQuantity() != null ? variant.getStockQuantity() : 0;
        if (stock < requestedTotal) {
            throw new CustomException("Sản phẩm này trong kho chỉ còn " + stock + " chiếc. Bạn đã có " + currentQtyInCart + " trong giỏ hàng.");
        }

        if (existingItem != null) {
            existingItem.setQuantity(requestedTotal);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setVariant(variant);
            newItem.setQuantity(quantity);
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }

    /**
     * B4: Cập nhật số lượng với kiểm tra quyền sở hữu (IDOR protection)
     */
    @Transactional
    public void updateQuantity(User user, String sessionId, Long itemId, int quantity) {
        Cart cart = getOrCreateCart(user, sessionId);
        if (cart == null) {
            throw new CustomException("Giỏ hàng không tồn tại.");
        }

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException("Không tìm thấy sản phẩm này trong giỏ hàng của bạn."));

        // Verify Cart Ownership
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new CustomException("Bạn không có quyền chỉnh sửa mục giỏ hàng này.");
        }

        if (quantity <= 0) {
            removeItem(user, sessionId, itemId);
        } else {
            ProductVariant v = item.getVariant();
            int stock = v.getStockQuantity() != null ? v.getStockQuantity() : 0;
            if (stock < quantity) {
                throw new CustomException("Không thể cập nhật lên " + quantity + " sản phẩm do kho chỉ còn lại " + stock + " chiếc.");
            }
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }
    }

    /**
     * B4: Xóa sản phẩm với kiểm tra quyền sở hữu (IDOR protection)
     */
    @Transactional
    public void removeItem(User user, String sessionId, Long itemId) {
        Cart cart = getOrCreateCart(user, sessionId);
        if (cart == null) return;

        CartItem item = cartItemRepository.findById(itemId).orElse(null);
        if (item != null) {
            if (!item.getCart().getId().equals(cart.getId())) {
                throw new CustomException("Bạn không có quyền xóa mục giỏ hàng này.");
            }
            if (cart.getItems() != null) {
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
        return getCartSummary(user, sessionId, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public CartResponse getCartSummary(User user, String sessionId, String voucherCode, 
                                       BigDecimal requestedCoolCash, String provinceName, String districtName) {
        Cart cart = getOrCreateCart(user, sessionId);
        List<CartItem> items = (cart != null && cart.getItems() != null) ? cart.getItems() : new ArrayList<>();

        List<CartItemDto> itemDtos = new ArrayList<>();
        int totalQty = 0;

        for (CartItem ci : items) {
            ProductVariant v = ci.getVariant();
            Product p = v.getProduct();

            BigDecimal price = v.getSalePrice() != null ? v.getSalePrice() : BigDecimal.ZERO;
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(ci.getQuantity()));
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

            // Tìm chính xác ảnh theo Màu sắc
            String itemImg = null;
            if (p != null && v.getColor() != null) {
                try {
                    List<ProductImage> colorImgs = productImageRepository.findByProductIdAndColorIdOrderByDisplayOrderAsc(p.getId(), v.getColor().getId());
                    if (colorImgs != null && !colorImgs.isEmpty()) {
                        itemImg = colorImgs.get(0).getImageUrl();
                    }
                } catch (Exception ignored) {}
                if (itemImg == null || itemImg.isBlank()) {
                    itemImg = p.getImageUrlForColor(v.getColor().getId());
                }
            }

            if (itemImg == null || itemImg.isBlank()) {
                itemImg = (p != null && p.getThumbnailUrl() != null) ? p.getThumbnailUrl() : "/images/products/ao-thun-compact-den-1.jpg";
            }

            if (itemImg != null && itemImg.contains("/")) {
                String fileName = itemImg.substring(itemImg.lastIndexOf('/') + 1);
                if (fileName.endsWith(".jpg") || fileName.endsWith(".png")) {
                    dto.setImageUrl("/images/products/" + fileName);
                } else {
                    dto.setImageUrl(itemImg);
                }
            } else {
                dto.setImageUrl(itemImg);
            }

            dto.setStockQuantity(v.getStockQuantity());
            itemDtos.add(dto);
        }

        // Dùng PricingService chuẩn xác
        PricingSummaryDTO pricing = pricingService.calculatePricing(
                user, items, voucherCode, requestedCoolCash, provinceName, districtName, "COD"
        );

        CartResponse resp = new CartResponse();
        resp.setItems(itemDtos);
        resp.setSubtotal(pricing.getSubtotal());
        resp.setComboDiscount(pricing.getComboDiscount());
        resp.setVoucherDiscount(pricing.getVoucherDiscount());
        resp.setDiscountAmount(pricing.getComboDiscount().add(pricing.getVoucherDiscount()));
        resp.setCoolcashUsed(pricing.getCoolcashUsed());
        resp.setShippingFee(pricing.getShippingFee());
        resp.setFinalTotal(pricing.getFinalAmount());
        resp.setTotalQuantity(totalQty);
        resp.setFreeshipQualified(pricing.isFreeshipQualified());
        resp.setFreeshipThresholdRemaining(pricing.getRemainingToFreeShip());

        return resp;
    }
}