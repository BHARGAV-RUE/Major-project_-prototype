package com.cloudtransfer.core.repository;

import com.cloudtransfer.core.entity.Contributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContributorRepository extends JpaRepository<Contributor, UUID> {

    @Query("SELECT c FROM Contributor c WHERE c.status = 'ACTIVE'")
    List<Contributor> findActiveContributors();
}