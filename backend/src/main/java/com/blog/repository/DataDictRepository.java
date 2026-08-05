package com.blog.repository;

import com.blog.entity.DataDict;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataDictRepository extends JpaRepository<DataDict, Long> {

    List<DataDict> findByStatusOrderBySortAsc(Integer status);

    List<DataDict> findByDictTypeAndStatusOrderBySortAsc(String dictType, Integer status);

    List<DataDict> findByDictTypeOrderBySortAsc(String dictType);

    long countByDictType(String dictType);
}
