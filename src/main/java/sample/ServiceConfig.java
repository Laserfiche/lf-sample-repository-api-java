package sample;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.laserfiche.api.client.model.AccessKey;
import com.nimbusds.jose.jwk.JWK;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Base64;

public class ServiceConfig {
    protected static String spKey;
    protected static AccessKey accessKey;
    protected static String repoId;

    private ServiceConfig() {
        throw new IllegalStateException("Utility class");
    }

    public static void setUp() {
        spKey = System.getenv("SERVICE_PRINCIPAL_KEY");
        String accessKeyBase64 = System.getenv("ACCESS_KEY");
        repoId = System.getenv("REPOSITORY_ID");
        if (spKey == null && accessKeyBase64 == null && repoId == null) {
            // Load environment variables
            Dotenv dotenv = Dotenv
                    .configure()
                    .filename(".env")
                    .load();
            // Read env variable
            accessKeyBase64 = dotenv.get("ACCESS_KEY");
            repoId = dotenv.get("REPOSITORY_ID");
            spKey = dotenv.get("SERVICE_PRINCIPAL_KEY");
        }
        accessKey = AccessKey.CreateFromBase64EncodedAccessKey(accessKeyBase64);
    }
}
