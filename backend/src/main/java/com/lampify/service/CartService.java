package com.lampify.service;

import com.lampify.dto.AddToCartRequest;
import com.lampify.dto.CartDto;
import com.lampify.dto.CartItemDto;
import com.lampify.dto.CartRecommendationDto;
import com.lampify.dto.UpdateCartItemRequest;
import com.lampify.entity.Cart;
import com.lampify.entity.CartItem;
import com.lampify.entity.Product;
import com.lampify.entity.ProductImage;
import com.lampify.entity.User;
import com.lampify.repository.CartItemRepository;
import com.lampify.repository.CartRepository;
import com.lampify.repository.ProductRepository;
import com.lampify.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    /**
     * @return guest token to set on the response cookie when a new guest cart was created
     *         (empty if no new guest token is needed)
     */
    public record CartResult(CartDto cart, Optional<String> newGuestToken, boolean clearGuestCookie) {}

    @Transactional
    public CartResult getCart(String userEmail, String guestToken) {
        if (userEmail != null) {
            User user = requireUser(userEmail);
            Optional<String> clearGuest = Optional.empty();
            if (guestToken != null && !guestToken.isBlank()) {
                mergeGuestIntoUser(user, guestToken);
                clearGuest = Optional.of(guestToken);
            }
            Cart cart = getOrCreateUserCart(user);
            return new CartResult(toDto(cart, false), Optional.empty(), clearGuest.isPresent());
        }

        if (guestToken != null && !guestToken.isBlank()) {
            Cart cart = cartRepository.findByGuestTokenWithItems(guestToken)
                    .orElseGet(() -> createGuestCart(guestToken));
            return new CartResult(toDto(cart, true), Optional.empty(), false);
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        Cart cart = createGuestCart(token);
        return new CartResult(toDto(cart, true), Optional.of(token), false);
    }

    @Transactional(readOnly = true)
    public List<CartRecommendationDto> getRecommendations(String userEmail, String guestToken, int limit) {
        CartResult cartResult = getCart(userEmail, guestToken);
        List<Long> excludeIds = cartResult.cart().getItems().stream()
                .map(CartItemDto::getProductId)
                .toList();
        List<Long> exclude = excludeIds.isEmpty() ? List.of(-1L) : excludeIds;
        int cappedLimit = Math.min(Math.max(limit, 1), 12);
        PageRequest page = PageRequest.of(0, cappedLimit);

        Set<Long> categoryIds = new HashSet<>();
        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail).orElse(null);
            if (user != null) {
                cartRepository.findByUserIdWithItems(user.getId()).ifPresent(cart ->
                        cart.getItems().forEach(item -> addCategoryId(categoryIds, item)));
            }
        } else if (guestToken != null && !guestToken.isBlank()) {
            cartRepository.findByGuestTokenWithItems(guestToken).ifPresent(cart ->
                    cart.getItems().forEach(item -> addCategoryId(categoryIds, item)));
        }

        List<Product> products = categoryIds.isEmpty()
                ? productRepository.findPopularExcluding(exclude, page)
                : productRepository.findRecommendationsByCategoryIds(categoryIds, exclude, page);

        return products.stream().map(this::toRecommendationDto).toList();
    }

    private void addCategoryId(Set<Long> categoryIds, CartItem item) {
        if (item.getProduct() != null && item.getProduct().getCategory() != null) {
            categoryIds.add(item.getProduct().getCategory().getId());
        }
    }

    private CartRecommendationDto toRecommendationDto(Product product) {
        CartRecommendationDto dto = new CartRecommendationDto();
        dto.setProductId(product.getId());
        dto.setName(product.getName());
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setImageUrl(resolvePrimaryImage(product));
        return dto;
    }

    @Transactional
    public CartResult addItem(String userEmail, String guestToken, AddToCartRequest request) {
        ResolvedCart resolved = resolveWritableCart(userEmail, guestToken);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        int addQty = Math.max(request.getQuantity(), 1);
        CartItem existing = cartItemRepository
                .findByCartIdAndProductId(resolved.cart().getId(), product.getId())
                .orElse(null);

        int currentQty = existing != null ? existing.getQuantity() : 0;
        int desired = currentQty + addQty;
        enforceStock(product, desired, currentQty);

        if (existing == null) {
            CartItem item = new CartItem();
            item.setCart(resolved.cart());
            item.setProduct(product);
            item.setQuantity(addQty);
            resolved.cart().getItems().add(item);
            cartItemRepository.save(item);
        } else {
            existing.setQuantity(desired);
            cartItemRepository.save(existing);
        }

        Cart refreshed = reload(resolved);
        return new CartResult(toDto(refreshed, resolved.guest()), resolved.newGuestToken(), false);
    }

    @Transactional
    public CartResult updateItem(String userEmail, String guestToken, Long productId, UpdateCartItemRequest request) {
        ResolvedCart resolved = resolveWritableCart(userEmail, guestToken);
        CartItem item = cartItemRepository.findByCartIdAndProductId(resolved.cart().getId(), productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not in cart"));

        if (request.getQuantity() <= 0) {
            resolved.cart().getItems().remove(item);
            cartItemRepository.delete(item);
        } else {
            Product product = item.getProduct();
            enforceStock(product, request.getQuantity(), 0);
            item.setQuantity(request.getQuantity());
            cartItemRepository.save(item);
        }

        Cart refreshed = reload(resolved);
        return new CartResult(toDto(refreshed, resolved.guest()), resolved.newGuestToken(), false);
    }

    @Transactional
    public CartResult removeItem(String userEmail, String guestToken, Long productId) {
        ResolvedCart resolved = resolveWritableCart(userEmail, guestToken);
        cartItemRepository.findByCartIdAndProductId(resolved.cart().getId(), productId)
                .ifPresent(item -> {
                    resolved.cart().getItems().remove(item);
                    cartItemRepository.delete(item);
                });
        Cart refreshed = reload(resolved);
        return new CartResult(toDto(refreshed, resolved.guest()), resolved.newGuestToken(), false);
    }

    @Transactional
    public CartResult clearCart(String userEmail, String guestToken) {
        ResolvedCart resolved = resolveWritableCart(userEmail, guestToken);
        resolved.cart().getItems().clear();
        cartRepository.save(resolved.cart());
        Cart refreshed = reload(resolved);
        return new CartResult(toDto(refreshed, resolved.guest()), resolved.newGuestToken(), false);
    }

    @Transactional
    public CartResult mergeGuestCart(String userEmail, String guestToken) {
        User user = requireUser(userEmail);
        if (guestToken != null && !guestToken.isBlank()) {
            mergeGuestIntoUser(user, guestToken);
        }
        Cart cart = getOrCreateUserCart(user);
        return new CartResult(toDto(cart, false), Optional.empty(), true);
    }

    private void mergeGuestIntoUser(User user, String guestToken) {
        Optional<Cart> guestCartOpt = cartRepository.findByGuestTokenWithItems(guestToken);
        if (guestCartOpt.isEmpty()) {
            return;
        }

        Cart guestCart = guestCartOpt.get();
        Cart userCart = getOrCreateUserCart(user);

        for (CartItem guestItem : guestCart.getItems()) {
            Product product = guestItem.getProduct();
            CartItem userItem = cartItemRepository
                    .findByCartIdAndProductId(userCart.getId(), product.getId())
                    .orElse(null);

            int current = userItem != null ? userItem.getQuantity() : 0;
            int merged = current + guestItem.getQuantity();
            if (product.getStockQuantity() <= 0) {
                continue;
            }
            int clamped = Math.min(merged, product.getStockQuantity());

            if (userItem == null) {
                CartItem item = new CartItem();
                item.setCart(userCart);
                item.setProduct(product);
                item.setQuantity(clamped);
                userCart.getItems().add(item);
                cartItemRepository.save(item);
            } else {
                userItem.setQuantity(clamped);
                cartItemRepository.save(userItem);
            }
        }

        cartRepository.delete(guestCart);
    }

    private record ResolvedCart(Cart cart, boolean guest, Optional<String> newGuestToken) {}

    private ResolvedCart resolveWritableCart(String userEmail, String guestToken) {
        if (userEmail != null) {
            User user = requireUser(userEmail);
            return new ResolvedCart(getOrCreateUserCart(user), false, Optional.empty());
        }

        if (guestToken != null && !guestToken.isBlank()) {
            Cart cart = cartRepository.findByGuestTokenWithItems(guestToken)
                    .orElseGet(() -> createGuestCart(guestToken));
            return new ResolvedCart(cart, true, Optional.empty());
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        return new ResolvedCart(createGuestCart(token), true, Optional.of(token));
    }

    private Cart getOrCreateUserCart(User user) {
        return cartRepository.findByUserIdWithItems(user.getId())
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }

    private Cart createGuestCart(String token) {
        Cart cart = new Cart();
        cart.setGuestToken(token);
        return cartRepository.save(cart);
    }

    private Cart reload(ResolvedCart resolved) {
        if (resolved.guest()) {
            return cartRepository.findByGuestTokenWithItems(resolved.cart().getGuestToken())
                    .orElse(resolved.cart());
        }
        return cartRepository.findByUserIdWithItems(resolved.cart().getUser().getId())
                .orElse(resolved.cart());
    }

    private void enforceStock(Product product, int desiredQty, int previousQty) {
        int stock = product.getStockQuantity();
        if (stock <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Out of stock");
        }
        if (desiredQty > stock) {
            String message = stock == 1
                    ? "Only 1 left in stock"
                    : "Only " + stock + " left in stock";
            if (previousQty >= stock) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private CartDto toDto(Cart cart, boolean guest) {
        CartDto dto = new CartDto();
        dto.setGuest(guest);

        var items = cart.getItems().stream()
                .sorted(Comparator.comparing(CartItem::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::toItemDto)
                .toList();
        dto.setItems(items);

        int totalItems = items.stream().mapToInt(CartItemDto::getQuantity).sum();
        BigDecimal totalPrice = items.stream()
                .map(CartItemDto::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalItems(totalItems);
        dto.setTotalPrice(totalPrice);
        return dto;
    }

    private CartItemDto toItemDto(CartItem item) {
        Product product = item.getProduct();
        CartItemDto dto = new CartItemDto();
        dto.setProductId(product.getId());
        dto.setName(product.getName());
        dto.setPrice(product.getPrice());
        dto.setQuantity(item.getQuantity());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setImageUrl(resolvePrimaryImage(product));
        dto.setLineTotal(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        return dto;
    }

    private String resolvePrimaryImage(Product product) {
        return product.getImages().stream()
                .filter(ProductImage::isPrimaryImage)
                .map(ProductImage::effectiveThumbPath)
                .findFirst()
                .orElseGet(() -> product.getImages().stream()
                        .map(ProductImage::effectiveThumbPath)
                        .findFirst()
                        .orElse(null));
    }
}
