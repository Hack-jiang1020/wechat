package com.blog.repository;

import com.blog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByDeletedOrderBySortAscIdAsc(Integer deleted);

    long countByDeleted(Integer deleted);
}
