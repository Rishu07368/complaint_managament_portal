package com.complaints.util;

import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

public final class PasswordUtil {
    private static final Pattern BCRYPT_HASH = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
    private static final Pattern SHA256_HASH = Pattern.compile("^[0-9a-fA-F]{64}$");

    private PasswordUtil() {}

    public static String hash(char[] password) {
        return BCrypt.hashpw(new String(password), BCrypt.gensalt(12));
    }

    public static boolean matches(char[] password, String hash) {
        String stored = hash == null ? "" : hash.trim();
        if (BCRYPT_HASH.matcher(stored).matches()) {
            return BCrypt.checkpw(new String(password), stored);
        }
        if (SHA256_HASH.matcher(stored).matches()) {
            try {
                byte[] expected = HexFormat.of().parseHex(stored);
                byte[] actual = MessageDigest.getInstance("SHA-256")
                        .digest(new String(password).getBytes(StandardCharsets.UTF_8));
                return MessageDigest.isEqual(expected, actual);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 is unavailable in this Java runtime.", e);
            }
        }
        return false;
    }

    public static boolean needsUpgrade(String hash) {
        return hash != null && !BCRYPT_HASH.matcher(hash.trim()).matches()
                && SHA256_HASH.matcher(hash.trim()).matches();
    }
}
