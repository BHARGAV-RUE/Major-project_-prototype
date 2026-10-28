package com.cloudtransfer.core.config;

import com.cloudtransfer.core.repository.ContributorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final ContributorRepository contributorRepository;

    @Value("${google.oauth.refresh-token:}")
    private String googleRefreshToken;

    @Value("${dropbox.oauth.refresh-token:}")
    private String dropboxRefreshToken;

    @Value("${contributor.primary.email:bhargavkallepally9@gmail.com}")
    private String primaryContributorEmail;

    @Override
    public void run(String... args) {
        log.info("[DatabaseSeeder] Automatic seeding of fake volunteers is disabled. Database relies solely on real volunteer registrations.");
    }
}