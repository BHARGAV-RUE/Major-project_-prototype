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


## Merkle tree integration
http://localhost:8081/api/v1/stream/upload-chunk POST

{
    "chunkIndex": "0",
    "chunkHash": "99f2c80b2aa0228e85611f4d07fd0269e67bf92e9df409532c69906e288e91c2",
    "status": "success"
}

{
    "chunkIndex": "1",
    "chunkHash": "357681082e7370b47ef6992a2c17e38d4dd4c18a6ec28c3a6090911f2ff1e8f3",
    "status": "success"
}
{
    "chunkIndex": "2",
    "chunkHash": "0698c076dbb7b0e67373d4e6369d585428d2be3ea892f5377495cad08e440525",
    "status": "success"
}
{
    "chunkIndex": "3",
    "chunkHash": "75e5af9026b9718377c92f6db18c40266737c0c775873057cb4169057ff30124",
    "status": "success"
}
{
    "chunkIndex": "4",
    "chunkHash": "acd1e45b405b4c30a6cc7e9397f2f05b04d9abc65f238e79bace156e5f9bb08c",
    "status": "success"
}
{
    "chunkIndex": "5",
    "chunkHash": "2ebc34986942348a8bc6c969241ebda0ff8823ca6b2f8b5f15388e3acfa3db5b",
    "status": "success"
}
{
    "chunkIndex": "6",
    "chunkHash": "ae45a46f4a2e84ecc05671d4c119a735b9c5666f1b4eabcd412f6eeac5a01a54",
    "status": "success"
}
{
    "chunkIndex": "7",
    "chunkHash": "ecd84fd4e5eb9e7ac407f86ea07a46db2b88669ce899f8e2165d29705f67134d",
    "status": "success"
}
{
    "chunkIndex": "8",
    "chunkHash": "82f58ea6f5c1cf115587bfae8b797ad892408ddf2e330946dc903aaaf465e582",
    "status": "success"
}
{
    "chunkIndex": "9",
    "chunkHash": "15a93bdc0b9e844b80b8063ddc2d0e9ade7b1c49ab1e5370126aeef74dfa2077",
    "status": "success"
}
{
    "chunkIndex": "10",
    "chunkHash": "b3424f37122d25b6665da8a66594f7baffa5b5172865172bd1d8948dfc5f711a",
    "status": "success"
}

COPY PASTE THIS 10 TIMES 
Chunk 10 routed to cloud and hash queued successfully.
CONSOLE:
All 10 chunks received for Session: 123e4567-e89b-12d3-a456-426614174000
==================================================
MERKLE ROOT GENERATED: 40a722921439c0cc78383c8d2bf76ee832a4c6e074c8e3dee741a37c3be37a12
Ready for Blockchain Anchoring!
==================================================
