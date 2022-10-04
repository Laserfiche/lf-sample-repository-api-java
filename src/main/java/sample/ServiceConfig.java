package sample;

import com.laserfiche.api.client.model.AccessKey;
import io.github.cdimascio.dotenv.Dotenv;

public class ServiceConfig {
    protected static String servicePrincipalKey;
    protected static AccessKey accessKey;
    protected static String repositoryId;

    private ServiceConfig() {
        throw new IllegalStateException("Utility class");
    }

    public static void setUp() {
        servicePrincipalKey = System.getenv("SERVICE_PRINCIPAL_KEY");
        String accessKeyBase64 = System.getenv("ACCESS_KEY");
        repositoryId = System.getenv("REPOSITORY_ID");
        if (servicePrincipalKey == null && accessKeyBase64 == null && repositoryId == null) {
            // Load environment variables
            Dotenv dotenv = Dotenv
                    .configure()
                    .filename(".env")
                    .load();
            // Read env variable
            accessKeyBase64 = dotenv.get("ACCESS_KEY");
            repositoryId = dotenv.get("REPOSITORY_ID");
            servicePrincipalKey = dotenv.get("SERVICE_PRINCIPAL_KEY");
        }
        accessKey = AccessKey.CreateFromBase64EncodedAccessKey(accessKeyBase64);
    }
}
