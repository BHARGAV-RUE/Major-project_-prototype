```python
# Your code goes inside the box
GET http://localhost:8080/api/v1/transfers/blueprint

{
    "senderEmail": "bhargav.user@example.com",
    "filename": "medium-video.mp4",
    "totalSizeBytes": 524288000, 
    "expiryDays": 7
}

{
    "sessionId": "223d99c9-c85a-4e8d-a2a1-2997cf74fa17",
    "totalSizeBytes": 524288000,
    "totalChunks": 10,
    "chunkAssignments": [
        {
            "chunkIndex": 0,
            "primaryContributorId": "66389a48-7b50-49ea-8319-0bddc3f55c99",
            "primaryProvider": "GOOGLE_DRIVE",
            "primaryRefreshToken": "fake_oauth_token_volunteer1@gmail.com",
            "replicaContributorId": "673a156f-dd3a-415f-b8ba-989cb84cfb6c",
            "replicaProvider": "GOOGLE_DRIVE",
            "replicaRefreshToken": "fake_oauth_token_volunteer2@gmail.com"
        },
        {
            "chunkIndex": 1,
            "primaryContributorId": "673a156f-dd3a-415f-b8ba-989cb84cfb6c",
            "primaryProvider": "GOOGLE_DRIVE",
            "primaryRefreshToken": "fake_oauth_token_volunteer2@gmail.com",
            "replicaContributorId": "5eb0e0c2-628d-49f9-aef6-fdcc71cfa263",
            "replicaProvider": "DROPBOX",
            "replicaRefreshToken": "fake_oauth_token_volunteer3@yahoo.com"
        },
        {
            "chunkIndex": 2,
            "primaryContributorId": "5eb0e0c2-628d-49f9-aef6-fdcc71cfa263",
            "primaryProvider": "DROPBOX",
            "primaryRefreshToken": "fake_oauth_token_volunteer3@yahoo.com",
            "replicaContributorId": "ebbda4e1-97ee-4c42-80b9-5e209f635313",
            "replicaProvider": "GOOGLE_DRIVE",
            "replicaRefreshToken": "fake_oauth_token_volunteer4@gmail.com"
        },
        {
            "chunkIndex": 3,
            "primaryContributorId": "ebbda4e1-97ee-4c42-80b9-5e209f635313",
            "primaryProvider": "GOOGLE_DRIVE",
            "primaryRefreshToken": "fake_oauth_token_volunteer4@gmail.com",
            "replicaContributorId": "316e16f4-14f0-401c-b922-a16fcb03f6a6",
            "replicaProvider": "DROPBOX",
            "replicaRefreshToken": "fake_oauth_token_volunteer5@outlook.com"
        },
        {
            "chunkIndex": 4,
            "primaryContributorId": "316e16f4-14f0-401c-b922-a16fcb03f6a6",
            "primaryProvider": "DROPBOX",
            "primaryRefreshToken": "fake_oauth_token_volunteer5@outlook.com",
            "replicaContributorId": "66389a48-7b50-49ea-8319-0bddc3f55c99",
            "replicaProvider": "GOOGLE_DRIVE",
            "replicaRefreshToken": "fake_oauth_token_volunteer1@gmail.com"
        },
        {
            "chunkIndex": 5,
            "primaryContributorId": "66389a48-7b50-49ea-8319-0bddc3f55c99",
            "primaryProvider": "GOOGLE_DRIVE",
            "primaryRefreshToken": "fake_oauth_token_volunteer1@gmail.com",
            "replicaContributorId": "673a156f-dd3a-415f-b8ba-989cb84cfb6c",
            "replicaProvider": "GOOGLE_DRIVE",
            "replicaRefreshToken": "fake_oauth_token_volunteer2@gmail.com"
        },
        {
            "chunkIndex": 6,
            "primaryContributorId": "673a156f-dd3a-415f-b8ba-989cb84cfb6c",
            "primaryProvider": "GOOGLE_DRIVE",
            "primaryRefreshToken": "fake_oauth_token_volunteer2@gmail.com",
            "replicaContributorId": "5eb0e0c2-628d-49f9-aef6-fdcc71cfa263",
            "replicaProvider": "DROPBOX",
            "replicaRefreshToken": "fake_oauth_token_volunteer3@yahoo.com"
        },
        {
            "chunkIndex": 7,
            "primaryContributorId": "5eb0e0c2-628d-49f9-aef6-fdcc71cfa263",
            "primaryProvider": "DROPBOX",
            "primaryRefreshToken": "fake_oauth_token_volunteer3@yahoo.com",
            "replicaContributorId": "ebbda4e1-97ee-4c42-80b9-5e209f635313",
            "replicaProvider": "GOOGLE_DRIVE",
            "replicaRefreshToken": "fake_oauth_token_volunteer4@gmail.com"
        },
        {
            "chunkIndex": 8,
            "primaryContributorId": "ebbda4e1-97ee-4c42-80b9-5e209f635313",
            "primaryProvider": "GOOGLE_DRIVE",
            "primaryRefreshToken": "fake_oauth_token_volunteer4@gmail.com",
            "replicaContributorId": "316e16f4-14f0-401c-b922-a16fcb03f6a6",
            "replicaProvider": "DROPBOX",
            "replicaRefreshToken": "fake_oauth_token_volunteer5@outlook.com"
        },
        {
            "chunkIndex": 9,
            "primaryContributorId": "316e16f4-14f0-401c-b922-a16fcb03f6a6",
            "primaryProvider": "DROPBOX",
            "primaryRefreshToken": "fake_oauth_token_volunteer5@outlook.com",
            "replicaContributorId": "66389a48-7b50-49ea-8319-0bddc3f55c99",
            "replicaProvider": "GOOGLE_DRIVE",
            "replicaRefreshToken": "fake_oauth_token_volunteer1@gmail.com"
        }
    ]
}
-- MASSIVE
{
    "senderEmail": "bhargav.user@example.com",
    "filename": "massive-project-video.mp4",
    "totalSizeBytes": 3221225472, 
    "expiryDays": 7
}

{
    "error": "Capacity Limit Exceeded",
    "message": "Network capacity limit reached. Please try uploading a smaller file or try again later."
}
