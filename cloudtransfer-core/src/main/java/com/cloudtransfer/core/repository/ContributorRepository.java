package com.cloudtransfer.core.repository;

import com.cloudtransfer.core.entity.Contributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContributorRepository extends JpaRepository<Contributor, UUID> {
    Optional<Contributor> findByEmailAndCloudProvider(String email, String cloudProvider);
    List<Contributor> findAllByEmail(String email);
    List<Contributor> findByStatus(String status);
}