package com.cloudtransfer.core.config;

import com.cloudtransfer.core.entity.Contributor;
import com.cloudtransfer.core.repository.ContributorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final ContributorRepository contributorRepository;

    @Override
    public void run(String... args) throws Exception {
        if (contributorRepository.count() == 0) {
            System.out.println("No volunteers found. Seeding 5 fake volunteers for testing...");

            long defaultSpace = 5L * 1024 * 1024 * 1024; // 5 GB simulated space per volunteer

            Contributor c1 = createFake("volunteer1@gmail.com", "GOOGLE_DRIVE", defaultSpace);
            Contributor c2 = createFake("volunteer2@gmail.com", "GOOGLE_DRIVE", defaultSpace);
            Contributor c3 = createFake("volunteer3@yahoo.com", "DROPBOX", defaultSpace);
            Contributor c4 = createFake("volunteer4@gmail.com", "GOOGLE_DRIVE", defaultSpace);
            Contributor c5 = createFake("volunteer5@outlook.com", "DROPBOX", defaultSpace);

            contributorRepository.saveAll(List.of(c1, c2, c3, c4, c5));
            System.out.println("Database seeding complete!");
        }
    }

    private Contributor createFake(String email, String provider, long space) {
        Contributor c = new Contributor();
        c.setEmail(email);
        c.setCloudProvider(provider);
        c.setRefreshToken("fake_oauth_token_" + email);
        c.setMaxStorageQuotaBytes(space);
        c.setStatus("ACTIVE");
        return c;
    }
}