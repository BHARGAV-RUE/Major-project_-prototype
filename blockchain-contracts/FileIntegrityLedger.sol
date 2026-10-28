// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @title FileIntegrityLedger
 * @dev Stores and verifies Merkle Roots for multi-cloud transfer sessions on Polygon Amoy.
 */
contract FileIntegrityLedger {

    struct FileRecord {
        string merkleRoot;
        uint256 timestamp;
        address anchoredBy;
    }

    // Mapping from Session ID -> FileRecord
    mapping(string => FileRecord) private records;

    event FileAnchored(string indexed sessionId, string merkleRoot, uint256 timestamp);

    /**
     * @dev Anchors a Merkle Root for a specific transfer session on-chain.
     */
    function anchorFileRoot(string calldata sessionId, string calldata merkleRoot) external {
        require(bytes(records[sessionId].merkleRoot).length == 0, "Session ID already anchored");

        records[sessionId] = FileRecord({
            merkleRoot: merkleRoot,
            timestamp: block.timestamp,
            anchoredBy: msg.sender
        });

        emit FileAnchored(sessionId, merkleRoot, block.timestamp);
    }

    /**
     * @dev Verifies if a given recomputed Merkle Root matches the on-chain ledger.
     */
    function verifyFileRoot(string calldata sessionId, string calldata recomputedRoot) external view returns (bool) {
        if (bytes(records[sessionId].merkleRoot).length == 0) {
            return false;
        }
        return keccak256(abi.encodePacked(records[sessionId].merkleRoot)) == keccak256(abi.encodePacked(recomputedRoot));
    }

    /**
     * @dev Retrieves the recorded Merkle Root for a session.
     */
    function getMerkleRoot(string calldata sessionId) external view returns (string memory) {
        return records[sessionId].merkleRoot;
    }
}