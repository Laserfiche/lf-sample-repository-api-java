package sample;

import com.laserfiche.api.client.model.AccessKey;
import io.github.cdimascio.dotenv.Dotenv;

public class ServiceConfig {
    private String servicePrincipalKey;
    private final AccessKey accessKey;
    private String repositoryId;

    public String getServicePrincipalKey() {
        return servicePrincipalKey;
    }

    public AccessKey getAccessKey() {
        return accessKey;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public ServiceConfig() {
        // Try load from environment variable
        servicePrincipalKey = System.getenv("SERVICE_PRINCIPAL_KEY");
        String accessKeyBase64 = System.getenv("ACCESS_KEY");
        repositoryId = System.getenv("REPOSITORY_ID");

        if (servicePrincipalKey == null || accessKeyBase64 == null || repositoryId == null) {
            // Try load from file
            Dotenv dotenv = Dotenv
                    .configure()
                    .filename(".env")
                    .load();
            if (servicePrincipalKey == null) {
                servicePrincipalKey = dotenv.get("SERVICE_PRINCIPAL_KEY");
                if (servicePrincipalKey == null) {
                    throw new IllegalStateException("Environment variable SERVICE_PRINCIPAL_KEY does not exist.");
                }
            }
            if (accessKeyBase64 == null) {
                accessKeyBase64 = dotenv.get("ACCESS_KEY");
                if (accessKeyBase64 == null) {
                    throw new IllegalStateException("Environment variable ACCESS_KEY does not exist.");
                }
            }
            if (repositoryId == null) {
                repositoryId = dotenv.get("REPOSITORY_ID");
                if (repositoryId == null) {
                    throw new IllegalStateException("Environment variable REPOSITORY_ID does not exist.");
                }
            }
        }

        accessKey = AccessKey.createFromBase64EncodedAccessKey(accessKeyBase64);
    }
}
