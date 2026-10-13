// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

contract FileIntegrityLedger {

    struct FileRecord {
        string sessionId;    // The UUID from your Spring Boot Brain
        string merkleRoot;   // The final top hash of all encrypted chunks
        uint256 timestamp;   // Time of anchoring
    }

    // Maps a Session UUID to its Merkle Root record
    mapping(string => FileRecord) public fileRecords;

    // Event emitted to the blockchain logs when a new file is anchored
    event RootAnchored(string indexed sessionId, string merkleRoot, uint256 timestamp);

    /**
     * Called by the Spring Boot backend to permanently store the Merkle Root.
     */
    function anchorFileRoot(string memory _sessionId, string memory _merkleRoot) public {
        // Ensure the file hasn't already been anchored
        require(bytes(fileRecords[_sessionId].sessionId).length == 0, "Session ID already anchored on-chain");

        fileRecords[_sessionId] = FileRecord({
            sessionId: _sessionId,
            merkleRoot: _merkleRoot,
            timestamp: block.timestamp
        });

        emit RootAnchored(_sessionId, _merkleRoot, block.timestamp);
    }

    /**
     * Called by the frontend or backend during file download to verify data wasn't tampered with.
     */
    function verifyFileRoot(string memory _sessionId, string memory _providedRoot) public view returns (bool) {
        // Compare the stored root with the re-calculated root of the downloaded chunks
        return keccak256(abi.encodePacked(fileRecords[_sessionId].merkleRoot)) == keccak256(abi.encodePacked(_providedRoot));
    }
}