package com.bhargav.cloudtransfer.repository;

import com.bhargav.cloudtransfer.entity.Contributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContributorRepository extends JpaRepository<Contributor, UUID> {
    Optional<Contributor> findByContributorEmail(String email);
}