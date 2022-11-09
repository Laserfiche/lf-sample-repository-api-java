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
    private AuthorizationType authorizationType;
    public static final String ACCESS_KEY = "ACCESS_KEY";
    public static final String SERVICE_PRINCIPAL_KEY = "SERVICE_PRINCIPAL_KEY";
    public static final String REPOSITORY_ID = "REPOSITORY_ID";
    public static final String USERNAME = "APISERVER_USERNAME";
    public static final String PASSWORD = "APISERVER_PASSWORD";
    public static final String BASE_URL = "APISERVER_REPOSITORY_API_BASE_URL";
    public static final String AUTHORIZATION_TYPE = "AUTHORIZATION_TYPE";

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

    public AuthorizationType getAuthorizationType() {
        return authorizationType;
    }

    public ServiceConfig() {
        Dotenv dotenv = Dotenv
                .configure()
                .filename(".env")
                .systemProperties()
                .ignoreIfMissing()
                .load();
        try {
            authorizationType = AuthorizationType.valueOf(getEnvironmentVariable(AUTHORIZATION_TYPE));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Environment variable '" + AUTHORIZATION_TYPE + "' does not exist. It must be present and its value can only be '" + AuthorizationType.CLOUD_ACCESS_KEY + "' or '" + AuthorizationType.API_SERVER_USERNAME_PASSWORD + "'.");
        }
        repositoryId = getEnvironmentVariable(REPOSITORY_ID);
        if (authorizationType == AuthorizationType.CLOUD_ACCESS_KEY) {
            servicePrincipalKey = getEnvironmentVariable(SERVICE_PRINCIPAL_KEY);
            String accessKeyBase64 = getEnvironmentVariable(ACCESS_KEY);
            accessKey = AccessKey.createFromBase64EncodedAccessKey(accessKeyBase64);
        } else if (authorizationType == AuthorizationType.API_SERVER_USERNAME_PASSWORD) {
            username = getEnvironmentVariable(USERNAME);
            password = getEnvironmentVariable(PASSWORD);
            baseUrl = getEnvironmentVariable(BASE_URL);
        }
    }

    private String getEnvironmentVariable(String environmentVariableName) {
        String environmentVariable = System.getenv(environmentVariableName);
        if (nullOrEmpty(environmentVariable)) {
            environmentVariable = System.getProperty(environmentVariableName);
            if (nullOrEmpty(environmentVariable))
                throw new IllegalStateException("Environment variable '" + environmentVariableName + "' does not exist.");
        }
        return environmentVariable;
    }

    public static boolean nullOrEmpty(String str) {
        return str == null || str.length() == 0;
    }
}
