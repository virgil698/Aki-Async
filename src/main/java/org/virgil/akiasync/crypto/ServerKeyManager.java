package org.virgil.akiasync.crypto;

import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerKeyManager {
    
    private static final Logger LOGGER = Logger.getLogger("AkiAsync-QuantumSeed");
    private static final String KEY_FILE_NAME = "quantum-seed.key";
    private static final int KEY_SIZE = 64; 
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    private final Path keyFilePath;
    private byte[] serverKey;
    private volatile boolean initialized = false;
    
    public ServerKeyManager(Plugin plugin) {
        this.keyFilePath = plugin.getDataFolder().toPath().resolve(KEY_FILE_NAME);
    }
    
    public void initialize() {
        if (initialized) {
            return;
        }
        
        try {
            if (Files.exists(keyFilePath)) {
                loadKey();
                LOGGER.info("[QuantumSeed] Server key loaded successfully");
            } else {
                generateAndSaveKey();
                LOGGER.info("[QuantumSeed] New server key generated and saved");
            }
            
            initialized = true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[QuantumSeed] Failed to initialize server key", e);
            throw new RuntimeException("Failed to initialize QuantumSeed server key", e);
        }
    }
    
    public byte[] getServerKey() {
        if (!initialized) {
            throw new IllegalStateException("ServerKeyManager not initialized");
        }
        return serverKey.clone();
    }
    
    private void generateAndSaveKey() throws Exception {
        LOGGER.info("[QuantumSeed] Generating new server key...");
        
        long firstStartTime = System.currentTimeMillis();
        
        byte[] randomSalt = new byte[32];
        SECURE_RANDOM.nextBytes(randomSalt);
        
        String hardwareFingerprint = getHardwareFingerprint();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(longToBytes(firstStartTime));
        baos.write(randomSalt);
        baos.write(hardwareFingerprint.getBytes(StandardCharsets.UTF_8));
        
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        this.serverKey = digest.digest(baos.toByteArray());

        saveKey();
        
        LOGGER.info("[QuantumSeed] Server key generated: " + KEY_SIZE + " bytes");
        LOGGER.info("[QuantumSeed] Key fingerprint: " + getKeyFingerprint());
    }
    
    private void saveKey() throws IOException {
        Path parentDir = keyFilePath.getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }
        
        StringBuilder content = new StringBuilder();
        content.append("# AkiAsync QuantumSeed Server Key\n");
        content.append("# DO NOT SHARE THIS FILE!\n");
        content.append("# Generated: ").append(new java.util.Date()).append("\n");
        content.append("# Fingerprint: ").append(getKeyFingerprint()).append("\n");
        content.append("#\n");
        content.append("# WARNING: Deleting or modifying this file will make your world\n");
        content.append("# generate differently! Backup this file with your world data!\n");
        content.append("#\n");
        content.append("KEY=").append(Base64.getEncoder().encodeToString(serverKey)).append("\n");
        
        Files.write(keyFilePath, content.toString().getBytes(StandardCharsets.UTF_8));
        
        try {
            File keyFile = keyFilePath.toFile();
            boolean success = true;
            success &= keyFile.setReadable(false, false);
            success &= keyFile.setReadable(true, true);
            success &= keyFile.setWritable(false, false);
            success &= keyFile.setWritable(true, true);
            if (!success) {
                LOGGER.warning("[QuantumSeed] Failed to set some file permissions");
            }
        } catch (Exception e) {
            LOGGER.warning("[QuantumSeed] Could not set file permissions: " + e.getMessage());
        }
    }
    
    private void loadKey() throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(keyFilePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("KEY=")) {
                    String keyBase64 = line.substring(4);
                    serverKey = Base64.getDecoder().decode(keyBase64);
                    
                    if (serverKey.length < 32) {
                        throw new IOException("Invalid key length: " + serverKey.length);
                    }
                    
                    LOGGER.info("[QuantumSeed] Key fingerprint: " + getKeyFingerprint());
                    return;
                }
            }
            throw new IOException("No valid key found in file");
        }
    }
    
    private String getKeyFingerprint() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(serverKey);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02X", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
    
    private String getHardwareFingerprint() {
        StringBuilder fp = new StringBuilder();
        
        fp.append(System.getProperty("os.name", ""));
        fp.append(System.getProperty("os.version", ""));
        fp.append(System.getProperty("os.arch", ""));
        
        fp.append(System.getProperty("java.version", ""));
        fp.append(System.getProperty("java.vendor", ""));
        
        fp.append(System.getProperty("user.name", ""));
        fp.append(System.getProperty("user.home", ""));
        
        return fp.toString();
    }
    
    private byte[] longToBytes(long value) {
        byte[] bytes = new byte[8];
        for (int i = 7; i >= 0; i--) {
            bytes[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return bytes;
    }
    
    public void regenerateKey() throws Exception {
        LOGGER.warning("[QuantumSeed] Regenerating server key - this will change world generation!");
        
        if (Files.exists(keyFilePath)) {
            Path parentDir = keyFilePath.getParent();
            if (parentDir != null) {
                Path backupPath = parentDir.resolve(KEY_FILE_NAME + ".backup." + System.currentTimeMillis());
                Files.copy(keyFilePath, backupPath);
                LOGGER.info("[QuantumSeed] Old key backed up to: " + backupPath);
            }
        }
        
        generateAndSaveKey();
    }
    
    public boolean isInitialized() {
        return initialized;
    }
}
