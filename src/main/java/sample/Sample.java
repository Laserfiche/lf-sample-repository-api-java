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
    private static RepositoryApiClient client = createRepoApiClient();

    public static void main(String[] args) {
        String repoName = getRepoName().join();
        Entry rootFolder = getRootFolder().join();
        List<Entry> rootFolderChildren = getFolderChildren(ROOTFOLDERENTRYID).join();
        System.exit(0);
    }

    public static CompletableFuture<String> getRepoName() {
        CompletableFuture<List<RepositoryInfo>> repoListResponse = client.getRepositoryClient().getRepositoryList();
        CompletableFuture<String> repoName = repoListResponse.supplyAsync(() ->
                repoListResponse.join().get(0).getRepoName());
        CompletableFuture<String> repoId = repoListResponse.supplyAsync(() ->
                repoListResponse.join().get(0).getRepoId());
        repoId.thenAcceptAsync(repodIdAsync -> {
            System.out.println("Repository Name: " + repodIdAsync);
        });
        return repoName;
    }

    public static CompletableFuture<Entry> getRootFolder() {
        CompletableFuture<Entry> entryResponse = client.getEntriesClient().getEntry(ServiceConfig.repoId, ROOTFOLDERENTRYID, null);
        CompletableFuture<Entry> entry = entryResponse.supplyAsync(() ->
                entryResponse.join());
        CompletableFuture<String> rootFolderName = entry.supplyAsync(() -> entry.join().getName());
        rootFolderName.thenAcceptAsync(rootFolderNameAsync -> {
            if (rootFolderNameAsync.length() == 0) {
                rootFolderNameAsync = "/";
            }
            System.out.println("Root Folder Name: " + rootFolderNameAsync);
        });
        return entry;
    }

    public static CompletableFuture<List<Entry>> getFolderChildren(int folderEntryId) {
        CompletableFuture<ODataValueContextOfIListOfEntry> folderChildren = client.getEntriesClient().getEntryListing(ServiceConfig.repoId, folderEntryId, true, null, null, null, null, null, "name", null, null, null, null);
        CompletableFuture<List<Entry>> children = folderChildren.supplyAsync(() -> folderChildren.join().getValue());
        children.thenAcceptAsync(childrenAsync -> {
            int i = 0;
            for (Entry child : childrenAsync) {
                System.out.println(i++ + ":[" + child.getEntryType() + " id:" + child.getId() + "] " + "'" + child.getName() + "'");
            }
        });
        return children;
    }

    public static RepositoryApiClient createRepoApiClient() {
        ServiceConfig.setUp();
        return RepositoryApiClientImpl.CreateFromAccessKey(ServiceConfig.spKey, ServiceConfig.accessKey);
    }
}
