package com.bhargav.cloudtransfer.controller;

import com.bhargav.cloudtransfer.dto.ContributorRegistrationRequest;
import com.bhargav.cloudtransfer.entity.Contributor;
import com.bhargav.cloudtransfer.service.ContributorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contributors")
@RequiredArgsConstructor
public class ContributorController {

    private final ContributorService contributorService;

    @PostMapping("/register")
    public ResponseEntity<Contributor> register(@RequestBody ContributorRegistrationRequest request) {
        // Map the DTO to the Contributor entity
        Contributor contributor = new Contributor();
        contributor.setProviderType(request.getProviderType());
        contributor.setFolderId(request.getFolderId());
        contributor.setRefreshToken(request.getRefreshToken());
        contributor.setContributorEmail(request.getContributorEmail());

        // Map the newly added capacity field
        contributor.setTotalCapacityBytes(request.getTotalCapacityBytes());

        // Call the service
        Contributor savedContributor = contributorService.registerContributor(contributor);

        return ResponseEntity.ok(savedContributor);
    }


}