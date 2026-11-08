package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.ExamPackageItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 体检套餐-项目关联 Repository（P1-5b）
 */
@Repository
public interface ExamPackageItemRepository extends JpaRepository<ExamPackageItem, Long> {

    /** 查询某套餐下所有项目（按 sortOrder 升序） */
    List<ExamPackageItem> findByPackageIdOrderBySortOrderAsc(Long packageId);

    /** 删除某套餐下的所有关联（用于全量覆盖保存） */
    @Modifying
    @Transactional
    @Query("DELETE FROM ExamPackageItem e WHERE e.packageId = :packageId")
    void deleteByPackageId(Long packageId);

    /** 查询某项目被几个套餐引用 */
    long countByItemId(Integer itemId);
}
