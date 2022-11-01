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

    public static void main(String[] args) {
        client = createRepositoryApiClient();
        CompletableFuture
                .allOf(getRepositoryInfo(), getRootFolder(), getFolderChildren(ROOT_FOLDER_ENTRY_ID))
                .join();
    }

    public static CompletableFuture<RepositoryInfo[]> getRepositoryInfo() {
        return client
                .getRepositoryClient()
                .getRepositoryList()
                .thenApply(repositories -> {
                    for (RepositoryInfo repository : repositories) {
                        System.out.printf("Repository Name: %s\nRepository ID: %s\n\n", repository.getRepoName(),
                                repository.getRepoId());
                    }
                    return repositories;
                });
    }

    public static CompletableFuture<Entry> getRootFolder() {
        return client
                .getEntriesClient()
                .getEntry(ServiceConfig.repositoryId,
                        ROOT_FOLDER_ENTRY_ID, null)
                .thenApply(rootEntry -> {
                    EntryType entryType = rootEntry.getEntryType();
                    String creator = rootEntry.getCreator();
                    OffsetDateTime createdDate = rootEntry.getCreationTime();
                    System.out.printf("Root folder information:\nType: %s\nCreator: %s\nCreation Date: %s\n", entryType,
                            creator, createdDate);
                    return rootEntry;
                });
    }

    public static CompletableFuture<List<Entry>> getFolderChildren(int folderId) {
        return client
                .getEntriesClient()
                .getEntryListing(ServiceConfig.repositoryId, folderId, true, null, null, null, null, null, "name",
                        null, null, null)
                .thenApply(entriesData -> {
                    List<Entry> entries = entriesData.getValue();
                    for (Entry entry : entries) {
                        System.out.printf("[%s id:%d] '%s'%n", entry.getEntryType(), entry.getId(), entry.getName());
                    }
                    return entries;
                });
    }

    public static RepositoryApiClient createRepositoryApiClient() {
        ServiceConfig.setUp();
        return RepositoryApiClientImpl.CreateFromAccessKey(ServiceConfig.servicePrincipalKey, ServiceConfig.accessKey);
    }
}
