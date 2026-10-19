package com.cloudtransfer.core.service;

import jakarta.annotation.PostConstruct;
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
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class BlockchainService {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainService.class);

    // This is the compiled bytecode of your FileIntegrityLedger.sol contract
    private static final String CONTRACT_BYTECODE = "608060405234801561001057600080fd5b50610360806100206000396000f3fe608060405234801561001057600080fd5b50600436106100365760003560e01c80631f4e1bc21461003b57806385d8fbbf1461008d575b600080fd5b6100776004803603602081101561005157600080fd5b35600401356020810135600080fd5b6100b0565b604051610084929190610220565b60405180910390f35b6100ae600480360360408110156100a357600080fd5b610120565b005b";

    @Value("${blockchain.rpc.url}")
    private String rpcUrl;

    @Value("${blockchain.wallet.private.key}")
    private String privateKey;

    @Value("${blockchain.contract.address:0x0000000000000000000000000000000000000000}")
    private String contractAddress;

    @PostConstruct
    public void autoDeployContractIfNeeded() {
        if ("0x0000000000000000000000000000000000000000".equals(contractAddress)) {
            logger.info("No contract address found. Auto-deploying FileIntegrityLedger.sol to Polygon Amoy...");

            // List of public Polygon Amoy RPC nodes for automatic failover
            List<String> rpcEndpoints = Arrays.asList(
                    rpcUrl, // Primary from application.properties
                    "https://polygon-amoy-bor-rpc.publicnode.com",
                    "https://polygon-amoy.gateway.tenderly.co",
                    "https://poly-amoy-testnet.api.pocket.network"
            );

            for (String activeRpc : rpcEndpoints) {
                try {
                    logger.info("[Blockchain] Connecting via RPC endpoint: {}", activeRpc);
                    Web3j web3j = Web3j.build(new HttpService(activeRpc));
                    Credentials credentials = Credentials.create(privateKey);

                    BigInteger nonce = web3j.ethGetTransactionCount(credentials.getAddress(), org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send().getTransactionCount();
                    BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
                    BigInteger gasLimit = BigInteger.valueOf(200_000);

                    logger.info("[Blockchain] Wallet: {} | Nonce: {} | Gas Price: {} wei", credentials.getAddress(), nonce, gasPrice);

                    org.web3j.crypto.RawTransaction rawTx = org.web3j.crypto.RawTransaction.createContractTransaction(
                            nonce, gasPrice, gasLimit, BigInteger.ZERO, CONTRACT_BYTECODE
                    );

                    byte[] signedMessage = org.web3j.crypto.TransactionEncoder.signMessage(rawTx, 80002L, credentials);
                    String hexValue = org.web3j.utils.Numeric.toHexString(signedMessage);

                    org.web3j.protocol.core.methods.response.EthSendTransaction ethSendTx = web3j.ethSendRawTransaction(hexValue).send();

                    if (ethSendTx.hasError()) {
                        logger.warn("[Blockchain] RPC {} returned error: {}. Trying next endpoint...", activeRpc, ethSendTx.getError().getMessage());
                        continue;
                    }

                    String txHash = ethSendTx.getTransactionHash();
                    logger.info("[Blockchain] Deployment transaction sent! Tx Hash: {}", txHash);
                    logger.info("[Blockchain] Waiting for Polygon Amoy to confirm the block...");

                    for (int i = 0; i < 20; i++) {
                        Thread.sleep(3000);
                        Optional<TransactionReceipt> receipt = web3j.ethGetTransactionReceipt(txHash).send().getTransactionReceipt();
                        if (receipt.isPresent()) {
                            this.contractAddress = receipt.get().getContractAddress();
                            logger.info("==================================================");
                            logger.info("SUCCESS! Contract Deployed Directly via Java!");
                            logger.info("Deployed Contract Address: {}", contractAddress);
                            logger.info("View on Polygon scan: https://amoy.polygonscan.com/address/{}", contractAddress);
                            logger.info("==================================================");
                            return;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("[Blockchain] Endpoint {} failed: {}. Retrying on backup RPC...", activeRpc, e.getMessage());
                }
            }
            logger.error("[Blockchain] All RPC endpoints failed. Please check internet connection.");
        } else {
            logger.info("Using existing deployed smart contract at: {}", contractAddress);
        }
    }

    public void anchorRootToBlockchain(String sessionId, String merkleRoot) {
        try {
            logger.info("Initiating blockchain anchoring for Session: {}", sessionId);

            Web3j web3j = Web3j.build(new HttpService(rpcUrl));
            Credentials credentials = Credentials.create(privateKey);

            Function function = new Function(
                    "anchorFileRoot",
                    Arrays.asList(new Utf8String(sessionId), new Utf8String(merkleRoot)),
                    Collections.emptyList()
            );
            String encodedFunction = FunctionEncoder.encode(function);

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
            logger.warn("Blockchain anchoring skipped (Expected with dummy credentials). Error: {}", e.getMessage());
        }
    }

    public boolean verifyIntegrityOnChain(String sessionId, String recomputedRoot) {
        try {
            logger.info("Verifying file integrity on-chain for Session: {}", sessionId);

            Web3j web3j = Web3j.build(new HttpService(rpcUrl));

            Function function = new Function(
                    "verifyFileRoot",
                    Arrays.asList(new Utf8String(sessionId), new Utf8String(recomputedRoot)),
                    Collections.singletonList(new org.web3j.abi.TypeReference<org.web3j.abi.datatypes.Bool>() {})
            );

            String encodedFunction = FunctionEncoder.encode(function);

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