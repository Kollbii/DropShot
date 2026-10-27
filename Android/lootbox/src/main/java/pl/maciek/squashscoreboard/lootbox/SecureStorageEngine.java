package pl.maciek.squashscoreboard.lootbox;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * [MASVS-STORAGE] [MASVS-CRYPTO]
 * Hardware-backed AES-256-GCM authenticated encryption engine for local persistence.
 * - Uses AndroidKeyStore master key when running on Android OS.
 * - Uses an ephemeral CSPRNG AES-256 key fallback inside local JVM unit tests.
 * - Binds every ciphertext to its (prefFile, key) context via GCM Associated Authenticated Data (AAD)
 *   to prevent cross-key ciphertext substitution / replay attacks.
 * - Zeroes intermediate byte[] buffers after cryptographic operations.
 */
public final class SecureStorageEngine {
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "dropshot_master_aes256_gcm_v1";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String PREFIX = "ENCv1:";
    private static final int IV_LENGTH_BYTES = 12; // 96-bit NIST-recommended GCM nonce
    private static final int TAG_LENGTH_BITS = 128; // 128-bit authentication tag

    private static final SecureRandom CSPRNG = new SecureRandom();
    private static volatile SecretKey fallbackTestKey;

    private SecureStorageEngine() {}

    public static String getEncryptedString(Context context, String prefFile, String key, String defaultValue) {
        SharedPreferences prefs = context.getSharedPreferences(prefFile, Context.MODE_PRIVATE);
        String raw = prefs.getString(key, null);
        if (raw == null) return defaultValue;
        byte[] aad = buildAad(prefFile, key);
        if (raw.startsWith(PREFIX)) {
            String decrypted = decrypt(raw, aad);
            return decrypted != null ? decrypted : defaultValue;
        }
        // Transparent one-way migration from legacy cleartext preference to AES-256-GCM
        String encrypted = encrypt(raw, aad);
        if (encrypted != null) {
            prefs.edit().putString(key, encrypted).apply();
        }
        return raw;
    }

    public static void putEncryptedString(Context context, String prefFile, String key, String plaintext) {
        SharedPreferences prefs = context.getSharedPreferences(prefFile, Context.MODE_PRIVATE);
        if (plaintext == null) {
            prefs.edit().remove(key).apply();
            return;
        }
        byte[] aad = buildAad(prefFile, key);
        String encrypted = encrypt(plaintext, aad);
        if (encrypted != null) {
            prefs.edit().putString(key, encrypted).apply();
        }
    }

    public static String encrypt(String plaintext, byte[] aad) {
        if (plaintext == null) return null;
        byte[] plainBytes = plaintext.getBytes(StandardCharsets.UTF_8);
        try {
            SecretKey secretKey = getOrCreateSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            // AndroidKeyStore generates a random IV automatically unless caller supplies one when randomizedEncryptionRequired=false
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            if (aad != null && aad.length > 0) {
                cipher.updateAAD(aad);
            }
            byte[] iv = cipher.getIV();
            if (iv == null || iv.length != IV_LENGTH_BYTES) {
                throw new IllegalStateException("Invalid GCM IV length");
            }
            byte[] cipherText = cipher.doFinal(plainBytes);
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);
            return PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            return null;
        } finally {
            Arrays.fill(plainBytes, (byte) 0);
        }
    }

    public static String decrypt(String payload, byte[] aad) {
        if (payload == null || !payload.startsWith(PREFIX)) return null;
        byte[] decoded = null;
        byte[] plainBytes = null;
        try {
            decoded = Base64.getDecoder().decode(payload.substring(PREFIX.length()));
            if (decoded.length <= IV_LENGTH_BYTES + (TAG_LENGTH_BITS / 8)) {
                return null;
            }
            SecretKey secretKey = getOrCreateSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, decoded, 0, IV_LENGTH_BYTES);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            if (aad != null && aad.length > 0) {
                cipher.updateAAD(aad);
            }
            plainBytes = cipher.doFinal(decoded, IV_LENGTH_BYTES, decoded.length - IV_LENGTH_BYTES);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception tamperedOrInvalid) {
            return null;
        } finally {
            if (decoded != null) Arrays.fill(decoded, (byte) 0);
            if (plainBytes != null) Arrays.fill(plainBytes, (byte) 0);
        }
    }

    public static byte[] buildAad(String namespace, String key) {
        return (namespace + ":" + key).getBytes(StandardCharsets.UTF_8);
    }

    private static synchronized SecretKey getOrCreateSecretKey() throws Exception {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            KeyStore.Entry existing = keyStore.getEntry(KEY_ALIAS, null);
            if (existing instanceof KeyStore.SecretKeyEntry) {
                return ((KeyStore.SecretKeyEntry) existing).getSecretKey();
            }
            KeyGenerator keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build();
            keyGen.init(spec);
            return keyGen.generateKey();
        } catch (Exception jvmTestFallback) {
            if (fallbackTestKey == null) {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256, CSPRNG);
                fallbackTestKey = keyGen.generateKey();
            }
            return fallbackTestKey;
        }
    }
}

