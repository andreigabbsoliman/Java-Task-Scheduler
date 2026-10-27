import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Handles creating accounts and signing in.
 * Passwords are hashed with PBKDF2 and a random salt, so the database never
 * contains the real password.
 */
public class AuthManager {

    private static final int ITERATIONS = 65536;
    private static final int KEY_BITS = 256;

    private final TaskDatabase database;
    private final SecureRandom random = new SecureRandom();

    public AuthManager(TaskDatabase database) {
        this.database = database;
    }

    public boolean isDatabaseReady() {
        return database.isConnected();
    }

    /** Returns null when the account was created, otherwise a message explaining what is wrong. */
    public String register(String username, char[] password, char[] confirm) {
        String name = username == null ? "" : username.trim();
        if (!name.matches("[A-Za-z0-9_.]{3,20}")) {
            return "Username must be 3-20 characters (letters, numbers, underscore or dot).";
        }
        if (password.length < 6) {
            return "Password must be at least 6 characters.";
        }
        if (!java.util.Arrays.equals(password, confirm)) {
            return "Passwords do not match.";
        }
        if (database.findUser(name) != null) {
            return "That username is already taken.";
        }

        byte[] salt = new byte[16];
        random.nextBytes(salt);
        String hash = hash(password, salt);
        if (!database.createUser(name, Base64.getEncoder().encodeToString(salt), hash)) {
            return "Could not create the account. Please try again.";
        }
        return null;
    }

    /** Returns the username as it was registered if the login is correct, otherwise null. */
    public String login(String username, char[] password) {
        String name = username == null ? "" : username.trim();
        if (name.isEmpty() || password.length == 0) return null;

        String[] user = database.findUser(name);
        if (user == null) return null;

        byte[] salt = Base64.getDecoder().decode(user[1]);
        String attempt = hash(password, salt);
        boolean matches = MessageDigest.isEqual(attempt.getBytes(), user[2].getBytes());
        return matches ? user[0] : null;
    }

    private String hash(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing is not available", e);
        }
    }
}
