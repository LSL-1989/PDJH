package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.DictExamItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DictExamItemRepository extends JpaRepository<DictExamItem, Integer> {
    List<DictExamItem> findByIsEnabled(Integer isEnabled);
}
