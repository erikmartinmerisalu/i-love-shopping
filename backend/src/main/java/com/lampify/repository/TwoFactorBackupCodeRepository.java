package com.lampify.repository;

import com.lampify.entity.TwoFactorBackupCode;
import com.lampify.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TwoFactorBackupCodeRepository extends JpaRepository<TwoFactorBackupCode, Long> {
    List<TwoFactorBackupCode> findByUserAndUsedFalse(User user);
    void deleteByUser(User user);
}
