package com.lampify.controller;

import com.lampify.dto.ContactRequest;
import com.lampify.dto.ValidationErrorResponse;
import com.lampify.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/contact")
public class ContactController {

    private final EmailService emailService;

    public ContactController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> submitContact(@Valid @RequestBody ContactRequest request) {
        emailService.sendContactEmail(request.getName(), request.getEmail(), request.getSubject(), request.getMessage());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Thank you for your message. We will get back to you soon."
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ValidationErrorResponse body = new ValidationErrorResponse(false, "Please fix the highlighted fields", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }
}
