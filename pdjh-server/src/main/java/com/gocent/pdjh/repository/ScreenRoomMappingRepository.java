package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.ScreenRoomMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScreenRoomMappingRepository extends JpaRepository<ScreenRoomMapping, Integer> {
    List<ScreenRoomMapping> findByScreenId(String screenId);
    void deleteByScreenId(String screenId);
    List<ScreenRoomMapping> findByOfficeRoomId(String officeRoomId);
}
