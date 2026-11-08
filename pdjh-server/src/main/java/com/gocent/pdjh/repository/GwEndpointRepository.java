package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.GwEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GwEndpointRepository extends JpaRepository<GwEndpoint, String> {
    List<GwEndpoint> findByConnectionId(String connectionId);
}
