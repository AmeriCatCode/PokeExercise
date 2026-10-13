import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Local-development helper: prints an HS256 JWT for the given username, signed with the
 * same secret the API uses (APP_JWT_SECRET, or the default from application.yml).
 * Token issuing is out of scope for the API itself; this only exists to try it out.
 *
 * Usage (JDK 21, no build needed): java tools/DevToken.java alice
 */
public class DevToken {

    private static final String DEFAULT_SECRET = "local-dev-secret-change-me-0123456789abcdef";
    private static final long VALIDITY_SECONDS = 3600;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java tools/DevToken.java <username>");
            System.exit(1);
        }
        String secret = System.getenv().getOrDefault("APP_JWT_SECRET", DEFAULT_SECRET);
        long now = Instant.now().getEpochSecond();

        String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = encode("{\"sub\":\"%s\",\"iat\":%d,\"exp\":%d}"
                .formatted(args[0], now, now + VALIDITY_SECONDS));
        String signingInput = header + "." + payload;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));

        System.out.println(signingInput + "." + signature);
    }

    private static String encode(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
