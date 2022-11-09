package sample;

import com.laserfiche.repository.api.RepositoryApiClient;
import com.laserfiche.repository.api.RepositoryApiClientImpl;
import com.laserfiche.repository.api.clients.impl.model.Entry;
import com.laserfiche.repository.api.clients.impl.model.EntryType;
import com.laserfiche.repository.api.clients.impl.model.RepositoryInfo;
import org.threeten.bp.OffsetDateTime;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Sample {
    private static final int ROOT_FOLDER_ENTRY_ID = 1;
    private static RepositoryApiClient client;
    private static ServiceConfig config;

    public static void main(String[] args) {
        config = new ServiceConfig();
        if (config.getAuthorizationType() == AuthorizationType.CLOUD_ACCESS_KEY) {
            client = RepositoryApiClientImpl.createFromAccessKey(config.getServicePrincipalKey(), config.getAccessKey());
        } else if (config.getAuthorizationType() == AuthorizationType.API_SERVER_USERNAME_PASSWORD) {
            client = RepositoryApiClientImpl.createFromUsernamePassword(config.getRepositoryId(), config.getUsername(), config.getPassword(), config.getBaseUrl());
        } else {
            throw new IllegalStateException("Environment variable '"+ config.AUTHORIZATION_TYPE + "' does not exist. It must be present and its value can only be '" + AuthorizationType.CLOUD_ACCESS_KEY + "' or '" + AuthorizationType.API_SERVER_USERNAME_PASSWORD + "'.");
        }
        CompletableFuture
                .allOf(getRepositoryInfo(), getRootFolder(), getFolderChildren(ROOT_FOLDER_ENTRY_ID))
                .join();
        System.exit(0);
    }

    public static CompletableFuture<RepositoryInfo[]> getRepositoryInfo() {
        return client
                .getRepositoryClient()
                .getRepositoryList()
                .thenApply(repositoryInfoArray -> {
                    for (RepositoryInfo repositoryInfo : repositoryInfoArray) {
                        System.out.printf("Repository Name: %s%nRepository ID: %s%n%n", repositoryInfo.getRepoName(),
                                repositoryInfo.getRepoId());
                    }
                    return repositoryInfoArray;
                });
    }

    public static CompletableFuture<Entry> getRootFolder() {
        return client
                .getEntriesClient()
                .getEntry(config.getRepositoryId(),
                        ROOT_FOLDER_ENTRY_ID, null)
                .thenApply(rootEntry -> {
                    EntryType entryType = rootEntry.getEntryType();
                    String creator = rootEntry.getCreator();
                    OffsetDateTime createdDate = rootEntry.getCreationTime();
                    System.out.printf("Root folder information:%nType: %s%nCreator: %s%nCreation Date: %s%n", entryType,
                            creator, createdDate);
                    return rootEntry;
                });
    }

    public static CompletableFuture<List<Entry>> getFolderChildren(int folderId) {
        return client
                .getEntriesClient()
                .getEntryListing(config.getRepositoryId(), folderId, true, null, null, null, null, null, "name",
                        null, null, null)
                .thenApply(entriesData -> {
                    List<Entry> entries = entriesData.getValue();
                    for (Entry entry : entries) {
                        System.out.printf("[%s id: %d] '%s'%n", entry.getEntryType(), entry.getId(), entry.getName());
                    }
                    return entries;
                });
    }
}
