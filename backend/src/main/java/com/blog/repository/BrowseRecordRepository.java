package com.blog.repository;

import com.blog.entity.BrowseRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrowseRecordRepository extends JpaRepository<BrowseRecord, Long> {

    Optional<BrowseRecord> findByUserIdAndArticleId(Long userId, Long articleId);

    List<BrowseRecord> findByUserIdOrderByCreateTimeDesc(Long userId);

    void deleteByUserId(Long userId);

    long countByUserId(Long userId);
}
