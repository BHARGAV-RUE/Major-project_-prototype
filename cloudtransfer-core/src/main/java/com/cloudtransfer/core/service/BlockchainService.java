package com.cloudtransfer.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class BlockchainService {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainService.class);

    @Value("${blockchain.rpc.url}")
    private String rpcUrl;

    @Value("${blockchain.wallet.private.key}")
    private String privateKey;

    @Value("${blockchain.contract.address}")
    private String contractAddress;

    public void anchorRootToBlockchain(String sessionId, String merkleRoot) {
        try {
            logger.info("Initiating blockchain anchoring for Session: {}", sessionId);

            // 1. Connect to Web3j
            Web3j web3j = Web3j.build(new HttpService(rpcUrl));
            Credentials credentials = Credentials.create(privateKey);

            // 2. Encode the Smart Contract Function call
            Function function = new Function(
                    "anchorFileRoot",
                    Arrays.asList(new Utf8String(sessionId), new Utf8String(merkleRoot)),
                    Collections.emptyList()
            );
            String encodedFunction = FunctionEncoder.encode(function);

            // 3. Send Transaction (Chain ID 80002 is Polygon Amoy)
            TransactionManager txManager = new RawTransactionManager(web3j, credentials, 80002);

            logger.info("Sending transaction to Polygon Amoy...");
            String txHash = txManager.sendTransaction(
                    DefaultGasProvider.GAS_PRICE,
                    DefaultGasProvider.GAS_LIMIT,
                    contractAddress,
                    encodedFunction,
                    BigInteger.ZERO
            ).getTransactionHash();

            logger.info("SUCCESS! Merkle Root anchored. Transaction Hash: {}", txHash);

        } catch (Exception e) {
            // This catches the error caused by dummy credentials so the Brain doesn't crash
            logger.warn("Blockchain anchoring skipped (Expected with dummy credentials). Error: {}", e.getMessage());
        }
    }

    public boolean verifyIntegrityOnChain(String sessionId, String recomputedRoot) {
        try {
            logger.info("Verifying file integrity on-chain for Session: {}", sessionId);

            Web3j web3j = Web3j.build(new HttpService(rpcUrl));

            // 1. Encode the 'verifyFileRoot(string, string)' smart contract read function
            Function function = new Function(
                    "verifyFileRoot",
                    Arrays.asList(new Utf8String(sessionId), new Utf8String(recomputedRoot)),
                    Collections.singletonList(new org.web3j.abi.TypeReference<org.web3j.abi.datatypes.Bool>() {})
            );

            String encodedFunction = FunctionEncoder.encode(function);

            // 2. Perform an eth_call (Read-only query, zero gas fees)
            org.web3j.protocol.core.methods.request.Transaction transaction =
                    org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                            null, contractAddress, encodedFunction
                    );

            String response = web3j.ethCall(transaction, org.web3j.protocol.core.DefaultBlockParameterName.LATEST)
                    .send()
                    .getValue();

            List<Type> results = FunctionReturnDecoder.decode(
                    response, function.getOutputParameters()
            );

            if (!results.isEmpty()) {
                boolean isValid = (Boolean) results.get(0).getValue();
                logger.info("Verification result for Session {}: {}", sessionId, isValid ? "AUTHENTIC" : "TAMPERED/INVALID");
                return isValid;
            }

        } catch (Exception e) {
            logger.warn("Verification query skipped (using dummy config). Error: {}", e.getMessage());
        }
        return false;
    }
}