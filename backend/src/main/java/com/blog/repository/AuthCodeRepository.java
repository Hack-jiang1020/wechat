package com.blog.repository;

import com.blog.entity.AuthCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthCodeRepository extends JpaRepository<AuthCode, Long> {

    Optional<AuthCode> findByTypeAndCode(String type, String code);

    Optional<AuthCode> findByCode(String code);
}