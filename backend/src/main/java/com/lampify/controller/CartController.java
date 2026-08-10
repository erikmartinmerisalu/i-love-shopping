package com.lampify.controller;

import com.lampify.dto.AddToCartRequest;
import com.lampify.dto.ApiErrorResponse;
import com.lampify.dto.CartDto;
import com.lampify.dto.CartRecommendationDto;
import com.lampify.dto.UpdateCartItemRequest;
import com.lampify.service.CartService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {

    public static final String GUEST_CART_COOKIE = "guestCartToken";

    private final CartService cartService;

    @Value("${app.cookie.secure:false}")
    private boolean secureCookie;

    @Value("${app.cookie.http-only:true}")
    private boolean httpOnlyCookie;

    @Value("${app.cookie.same-site:Lax}")
    private String sameSiteCookie;

    @Value("${app.cookie.max-age:604800}")
    private int cookieMaxAge;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartDto> getCart(HttpServletRequest request, HttpServletResponse response) {
        CartService.CartResult result = cartService.getCart(currentUserEmail(), guestToken(request));
        applyCookieUpdates(response, result);
        return ResponseEntity.ok(result.cart());
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<CartRecommendationDto>> recommendations(
            HttpServletRequest request,
            @RequestParam(defaultValue = "4") int limit) {
        return ResponseEntity.ok(
                cartService.getRecommendations(currentUserEmail(), guestToken(request), limit));
    }

    @PostMapping("/items")
    public ResponseEntity<?> addItem(
            @Valid @RequestBody AddToCartRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            CartService.CartResult result = cartService.addItem(currentUserEmail(), guestToken(request), body);
            applyCookieUpdates(response, result);
            return ResponseEntity.ok(result.cart());
        } catch (ResponseStatusException ex) {
            return error(ex);
        }
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<?> updateItem(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            CartService.CartResult result = cartService.updateItem(
                    currentUserEmail(), guestToken(request), productId, body);
            applyCookieUpdates(response, result);
            return ResponseEntity.ok(result.cart());
        } catch (ResponseStatusException ex) {
            return error(ex);
        }
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<?> removeItem(
            @PathVariable Long productId,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            CartService.CartResult result = cartService.removeItem(
                    currentUserEmail(), guestToken(request), productId);
            applyCookieUpdates(response, result);
            return ResponseEntity.ok(result.cart());
        } catch (ResponseStatusException ex) {
            return error(ex);
        }
    }

    @DeleteMapping
    public ResponseEntity<CartDto> clearCart(HttpServletRequest request, HttpServletResponse response) {
        CartService.CartResult result = cartService.clearCart(currentUserEmail(), guestToken(request));
        applyCookieUpdates(response, result);
        return ResponseEntity.ok(result.cart());
    }

    @PostMapping("/merge")
    public ResponseEntity<?> merge(HttpServletRequest request, HttpServletResponse response) {
        String email = currentUserEmail();
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiErrorResponse(false, "Login required to merge cart"));
        }
        CartService.CartResult result = cartService.mergeGuestCart(email, guestToken(request));
        applyCookieUpdates(response, result);
        return ResponseEntity.ok(result.cart());
    }

    private ResponseEntity<ApiErrorResponse> error(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }
        String message = ex.getReason() != null ? ex.getReason() : "Cart request failed";
        return ResponseEntity.status(status).body(new ApiErrorResponse(false, message));
    }

    private void applyCookieUpdates(HttpServletResponse response, CartService.CartResult result) {
        result.newGuestToken().ifPresent(token -> setGuestCookie(response, token));
        if (result.clearGuestCookie()) {
            clearGuestCookie(response);
        }
    }

    private String currentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String name = authentication.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            return null;
        }
        return name;
    }

    private String guestToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (GUEST_CART_COOKIE.equals(cookie.getName())
                    && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void setGuestCookie(HttpServletResponse response, String token) {
        String cookieValue = String.format(
                "%s=%s; Path=/; Max-Age=%d; %s%s%s",
                GUEST_CART_COOKIE,
                token,
                cookieMaxAge,
                secureCookie ? "Secure; " : "",
                httpOnlyCookie ? "HttpOnly; " : "",
                "SameSite=" + sameSiteCookie
        );
        response.addHeader("Set-Cookie", cookieValue);
    }

    private void clearGuestCookie(HttpServletResponse response) {
        String clearCookie = String.format(
                "%s=; Path=/; Max-Age=0; %s%sSameSite=%s",
                GUEST_CART_COOKIE,
                secureCookie ? "Secure; " : "",
                httpOnlyCookie ? "HttpOnly; " : "",
                sameSiteCookie
        );
        response.addHeader("Set-Cookie", clearCookie);
    }
}
