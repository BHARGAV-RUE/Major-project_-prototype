package com.bhargav.cloudtransfer.service;

import com.bhargav.cloudtransfer.dto.ContributorRegistrationRequest;
import com.bhargav.cloudtransfer.entity.Contributor;
import com.bhargav.cloudtransfer.entity.ProviderType;
import com.bhargav.cloudtransfer.repository.ContributorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContributorService {

    private final ContributorRepository contributorRepository;

    public Contributor registerContributor(Contributor request) {
        log.info("Registering new contributor for provider: {}", request.getProviderType());

        Contributor contributor = new Contributor();
        contributor.setProviderType(request.getProviderType());
        contributor.setFolderId(request.getFolderId());
        contributor.setRefreshToken(request.getRefreshToken());
        contributor.setContributorEmail(request.getContributorEmail());

        // Save the storage limit provided by the user (default to 0 if not provided)
        contributor.setTotalCapacityBytes(request.getTotalCapacityBytes() != null ? request.getTotalCapacityBytes() : 0L);
        contributor.setUsedCapacityBytes(0L); // Starts at zero usage

        contributor.setActive(true);

        return contributorRepository.save(contributor);
    }
}