package sample;

import com.laserfiche.api.client.model.AccessKey;
import io.github.cdimascio.dotenv.Dotenv;

public class ServiceConfig {
    private String servicePrincipalKey;
    private AccessKey accessKey;
    private String repositoryId;
    private String username;
    private String password;
    private String baseUrl;
    private String authorizationType;


    public String getServicePrincipalKey() {
        return servicePrincipalKey;
    }

    public AccessKey getAccessKey() {
        return accessKey;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getAuthorizationType() {
        return authorizationType;
    }

    public ServiceConfig() {
        // Try load from environment variable
        servicePrincipalKey = System.getenv("SERVICE_PRINCIPAL_KEY");
        String accessKeyBase64 = System.getenv("ACCESS_KEY");
        repositoryId = System.getenv("REPOSITORY_ID");
        authorizationType = System.getenv("AUTHORIZATION_TYPE");
        username = System.getenv("APISERVER_USERNAME");
        password = System.getenv("APISERVER_PASSWORD");
        baseUrl = System.getenv("APISERVER_REPOSITORY_API_BASE_URL");
        if (authorizationType == null && repositoryId == null) {
            Dotenv dotenv = Dotenv
                    .configure()
                    .filename(".env")
                    .load();
            authorizationType = dotenv.get("AUTHORIZATION_TYPE");
            if (authorizationType == null) {
                throw new IllegalStateException("Environment variable 'AUTHORIZATION_TYPE' does not exist. It must be present and its value can only be 'CloudAccessKey' or 'APIServerUsernamePassword'.");
            }
            if (repositoryId == null) {
                repositoryId = dotenv.get("REPOSITORY_ID");
                if (repositoryId == null) {
                    throw new IllegalStateException("Environment variable REPOSITORY_ID does not exist.");
                }
            }
            if (authorizationType.equalsIgnoreCase("CloudAccessKey")) {
                if (servicePrincipalKey == null || accessKeyBase64 == null) {
                    // Try load from file
                    if (servicePrincipalKey == null) {
                        servicePrincipalKey = dotenv.get("SERVICE_PRINCIPAL_KEY");
                        if (servicePrincipalKey == null) {
                            throw new IllegalStateException("Environment variable SERVICE_PRINCIPAL_KEY does not exist.");
                        }
                    }
                    if (accessKeyBase64 == null) {
                        accessKeyBase64 = dotenv.get("ACCESS_KEY");
                        accessKey = AccessKey.createFromBase64EncodedAccessKey(accessKeyBase64);
                        if (accessKeyBase64 == null) {
                            throw new IllegalStateException("Environment variable ACCESS_KEY does not exist.");
                        }
                    }
                }
            } else if (authorizationType.equalsIgnoreCase("APIServerUsernamePassword")) {
                if (username == null || password == null || baseUrl == null) {
                    if (username == null) {
                        username = dotenv.get("APISERVER_USERNAME");
                        if (username == null) {
                            throw new IllegalStateException("Environment variable APISERVER_USERNAME does not exist.");
                        }
                    }
                    if (password == null) {
                        password = dotenv.get("APISERVER_PASSWORD");
                        if (password == null) {
                            throw new IllegalStateException("Environment variable APISERVER_PASSWORD does not exist.");
                        }
                    }
                    if (baseUrl == null) {
                        baseUrl = dotenv.get("APISERVER_REPOSITORY_API_BASE_URL");
                        if (baseUrl == null) {
                            throw new IllegalStateException("Environment variable APISERVER_REPOSITORY_API_BASE_URL does not exist.");
                        }
                    }
                }
            }
        }
        //accessKey = AccessKey.createFromBase64EncodedAccessKey(accessKeyBase64);
    }
}
