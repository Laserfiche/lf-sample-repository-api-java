package sample;

import com.laserfiche.repository.api.RepositoryApiClient;
import com.laserfiche.repository.api.RepositoryApiClientImpl;
import com.laserfiche.repository.api.clients.impl.model.Entry;
import com.laserfiche.repository.api.clients.impl.model.ODataValueContextOfIListOfEntry;
import com.laserfiche.repository.api.clients.impl.model.RepositoryInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Sample {
    private static final int ROOTFOLDERENTRYID = 1;
    private static RepositoryApiClient client;

    public static void main(String[] args) {
        client = createRepositoryApiClient();
        CompletableFuture.allOf(getRepositoryName(), getRootFolder(), getFolderChildren(ROOTFOLDERENTRYID)).join();
    }

    public static CompletableFuture<String> getRepositoryName() {
        CompletableFuture<List<RepositoryInfo>> repositoryListResponse = client.getRepositoryClient().getRepositoryList();
        CompletableFuture<String> repositoryName = repositoryListResponse.thenApply(repositoryInfos -> repositoryInfos.get(0).getRepoName());
        CompletableFuture<String> repositoryId = repositoryListResponse.thenApply(repositoryInfoList -> repositoryInfoList.get(0).getRepoId());
        repositoryId.thenAccept(repositoryIdResult -> {
            System.out.println("Repository Name: " + repositoryIdResult);
        });
        return repositoryName;
    }

    public static CompletableFuture<Entry> getRootFolder() {
        CompletableFuture<Entry> entryResponse = client.getEntriesClient().getEntry(ServiceConfig.repositoryId, ROOTFOLDERENTRYID, null);
        CompletableFuture<Entry> entry = entryResponse.thenApply(entryResponseInfo -> entryResponseInfo);
        CompletableFuture<String> rootFolderName = entry.thenApply(entryName -> entryName.getName());
        rootFolderName.thenAccept(rootFolderNameResult -> {
            if (rootFolderNameResult.length() == 0) {
                rootFolderNameResult = "/";
            }
            System.out.println("Root Folder Name: " + rootFolderNameResult);
        });
        return entry;
    }

    public static CompletableFuture<List<Entry>> getFolderChildren(int folderEntryId) {
        CompletableFuture<ODataValueContextOfIListOfEntry> folderChildren = client.getEntriesClient().getEntryListing(ServiceConfig.repositoryId, folderEntryId, true, null, null, null, null, null, "name", null, null, null, null);
        CompletableFuture<List<Entry>> children = folderChildren.thenApply(folderChildrenInfo -> folderChildrenInfo.getValue());
        children.thenAccept(childrenResult -> {
            int i = 0;
            for (Entry child : childrenResult) {
                System.out.printf("%s:[%s id:%d] '%s'%n", i++, child.getEntryType(), child.getId(), child.getName());
            }
        });
        return children;
    }

    public static RepositoryApiClient createRepositoryApiClient() {
        ServiceConfig.setUp();
        return RepositoryApiClientImpl.CreateFromAccessKey(ServiceConfig.servicePrincipalKey, ServiceConfig.accessKey);
    }
}
