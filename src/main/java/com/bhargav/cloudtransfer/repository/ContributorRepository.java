package com.bhargav.cloudtransfer.repository;

import com.bhargav.cloudtransfer.entity.Contributor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ContributorRepository extends JpaRepository<Contributor, UUID> {
}