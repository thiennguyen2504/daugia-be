package com.example.daugia.user.repository;

import com.example.daugia.user.entity.UserAccountLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountLogRepository extends JpaRepository<UserAccountLog, Long> {
    Page<UserAccountLog> findAllByTargetUser_Id(Long userId, Pageable pageable);
}
