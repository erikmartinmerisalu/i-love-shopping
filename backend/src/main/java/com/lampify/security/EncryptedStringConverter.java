package com.lampify.security;

import com.lampify.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        EncryptionService encryptionService = EncryptionService.getInstance();
        if (encryptionService == null || !encryptionService.isEnabled()) {
            return attribute;
        }
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        EncryptionService encryptionService = EncryptionService.getInstance();
        if (encryptionService == null || !encryptionService.isEnabled()) {
            return dbData;
        }
        return encryptionService.decrypt(dbData);
    }
}
