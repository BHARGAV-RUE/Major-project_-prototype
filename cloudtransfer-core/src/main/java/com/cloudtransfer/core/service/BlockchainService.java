package com.cloudtransfer.core.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class BlockchainService {

    public enum OnChainResult {
        VERIFIED,
        MISMATCH,
        UNAVAILABLE
    }

    @Value("${web3.rpc-url:https://polygon-amoy.drpc.org}")
    private String primaryRpcUrl;

    @Value("${web3.rpc-fallback-urls:https://rpc.ankr.com/polygon_amoy,https://polygon-amoy-bor-rpc.publicnode.com,https://80002.rpc.thirdweb.com}")
    private String fallbackRpcUrls;

    @Value("${web3.private-key:${blockchain.wallet.private.key:}}")
    private String privateKey;

    @Value("${web3.contract-address:${blockchain.contract.address:}}")
    private String contractAddress;

    private static final long AMOY_CHAIN_ID = 80002L;
    private static final String BROKEN_RPC_HOST = "rpc-amoy.polygon.technology";
    private static final BigInteger MIN_GAS_PRICE = BigInteger.valueOf(30_000_000_000L); // 30 Gwei
    private static final BigInteger DEFAULT_GAS_LIMIT = BigInteger.valueOf(500_000L);

    private final List<String> rpcEndpoints = new ArrayList<>();
    private OkHttpClient okHttpClient;

    @PostConstruct
    public void init() {
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build();

        addRpcEndpoint(primaryRpcUrl);

        if (fallbackRpcUrls != null && !fallbackRpcUrls.isBlank()) {
            Arrays.stream(fallbackRpcUrls.split(","))
                    .map(String::trim)
                    .forEach(this::addRpcEndpoint);
        }

        if (privateKey == null || privateKey.isBlank() || contractAddress == null || contractAddress.isBlank()) {
            log.warn("[Blockchain] Web3 configuration missing. On-chain anchoring disabled.");
            return;
        }

        try {
            Credentials credentials = Credentials.create(privateKey);
            log.info("[Blockchain] Enabled. Wallet: {} | Contract: {} | Active RPC Pool: {}",
                    credentials.getAddress(), contractAddress, rpcEndpoints);
        } catch (Exception e) {
            log.error("[Blockchain] Invalid Web3 private key provided: {}", e.getMessage());
        }
    }

    private void addRpcEndpoint(String url) {
        if (url == null || url.isBlank()) return;
        String cleanUrl = url.trim();
        if (cleanUrl.contains(BROKEN_RPC_HOST)) {
            log.warn("[Blockchain] Skipping deprecated/unreliable RPC endpoint: {}", cleanUrl);
            return;
        }
        if (!rpcEndpoints.contains(cleanUrl)) {
            rpcEndpoints.add(cleanUrl);
        }
    }

    /**
     * Primary anchoring method used by MerkleTreeService
     */
    public String anchorRootToBlockchain(String sessionId, String merkleRoot) {
        if (rpcEndpoints.isEmpty() || privateKey == null || privateKey.isBlank()) {
            log.warn("[Blockchain] On-chain anchoring skipped: missing Web3 credentials or valid RPC endpoints.");
            return null;
        }

        Credentials credentials;
        try {
            credentials = Credentials.create(privateKey);
        } catch (Exception e) {
            log.error("[Blockchain] Cannot anchor: Invalid private key.");
            return null;
        }

        for (String rpcUrl : rpcEndpoints) {
            Web3j web3j = null;
            try {
                log.info("[Blockchain] Connecting to RPC: {} for session {}", rpcUrl, sessionId);

                HttpService httpService = new HttpService(rpcUrl, okHttpClient, false);
                web3j = Web3j.build(httpService);

                BigInteger blockNumber = web3j.ethBlockNumber().send().getBlockNumber();
                log.info("[Blockchain] Connected to RPC: {} (Latest Block: {})", rpcUrl, blockNumber);

                TransactionManager txManager = new RawTransactionManager(web3j, credentials, AMOY_CHAIN_ID);

                String txHash = executeContractAnchor(web3j, txManager, sessionId, merkleRoot);

                log.info("[Blockchain] Successfully anchored session {} to Polygon Amoy! Tx Hash: {}", sessionId, txHash);
                return txHash;

            } catch (Exception e) {
                log.warn("[Blockchain] RPC endpoint {} failed for session {}: {}", rpcUrl, sessionId, e.getMessage());
            } finally {
                if (web3j != null) {
                    try {
                        web3j.shutdown();
                    } catch (Exception ignored) {}
                }
            }
        }

        log.error("[Blockchain] All RPC endpoints in failover pool failed for session {}.", sessionId);
        return null;
    }

    /**
     * Alias anchoring method used by TransferService
     */
    public String anchorMerkleRoot(String sessionId, String merkleRoot) {
        return anchorRootToBlockchain(sessionId, merkleRoot);
    }

    /**
     * Used by AuditController (returns boolean)
     */
    public boolean verifyIntegrityOnChain(String sessionId, String recomputedRoot) {
        OnChainResult result = checkRootOnChain(sessionId, recomputedRoot);
        return result == OnChainResult.VERIFIED;
    }

    /**
     * Used by DownloadIntegrityService (returns OnChainResult enum)
     */
    public OnChainResult checkRootOnChain(String sessionId, String recomputedMerkleRoot) {
        if (rpcEndpoints.isEmpty() || contractAddress == null || contractAddress.isBlank()) {
            return OnChainResult.UNAVAILABLE;
        }

        for (String rpcUrl : rpcEndpoints) {
            Web3j web3j = null;
            try {
                HttpService httpService = new HttpService(rpcUrl, okHttpClient, false);
                web3j = Web3j.build(httpService);

                // Matches Solidity method: getMerkleRoot(string)
                Function function = new Function(
                        "getMerkleRoot",
                        List.of(new Utf8String(sessionId)),
                        List.of(new TypeReference<Utf8String>() {})
                );

                String encodedFunction = FunctionEncoder.encode(function);

                Transaction ethCallTransaction = Transaction.createEthCallTransaction(null, contractAddress, encodedFunction);
                EthCall response = web3j.ethCall(ethCallTransaction, DefaultBlockParameterName.LATEST).send();

                if (response.hasError() || response.getValue() == null) {
                    continue;
                }

                List<Type> decoded = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
                if (decoded.isEmpty()) {
                    continue;
                }

                String onChainRoot = (String) decoded.get(0).getValue();
                if (onChainRoot == null || onChainRoot.isBlank()) {
                    return OnChainResult.UNAVAILABLE;
                }

                if (onChainRoot.trim().equalsIgnoreCase(recomputedMerkleRoot.trim())) {
                    return OnChainResult.VERIFIED;
                } else {
                    return OnChainResult.MISMATCH;
                }

            } catch (Exception e) {
                log.warn("[Blockchain] Could not query RPC {} for root check: {}", rpcUrl, e.getMessage());
            } finally {
                if (web3j != null) {
                    try {
                        web3j.shutdown();
                    } catch (Exception ignored) {}
                }
            }
        }

        return OnChainResult.UNAVAILABLE;
    }

    private String executeContractAnchor(Web3j web3j, TransactionManager txManager, String sessionId, String merkleRoot) throws Exception {
        BigInteger gasPrice;
        try {
            gasPrice = web3j.ethGasPrice().send().getGasPrice();
            if (gasPrice == null || gasPrice.compareTo(MIN_GAS_PRICE) < 0) {
                gasPrice = MIN_GAS_PRICE;
            }
        } catch (Exception e) {
            gasPrice = MIN_GAS_PRICE;
        }

        log.info("[Blockchain] Executing anchorFileRoot with Gas Price: {} Gwei", gasPrice.divide(BigInteger.valueOf(1_000_000_000L)));

        // Matches Solidity method: anchorFileRoot(string,string)
        Function function = new Function(
                "anchorFileRoot",
                Arrays.asList(
                        new Utf8String(sessionId),
                        new Utf8String(merkleRoot)
                ),
                List.of()
        );

        String encodedFunction = FunctionEncoder.encode(function);

        EthSendTransaction ethSendTransaction = txManager.sendTransaction(
                gasPrice,
                DEFAULT_GAS_LIMIT,
                contractAddress,
                encodedFunction,
                BigInteger.ZERO
        );

        if (ethSendTransaction.hasError()) {
            throw new RuntimeException("EVM Transaction failed: " + ethSendTransaction.getError().getMessage());
        }

        String txHash = ethSendTransaction.getTransactionHash();
        if (txHash == null || txHash.isBlank()) {
            throw new RuntimeException("EVM Transaction returned null transaction hash.");
        }

        return txHash;
    }
}