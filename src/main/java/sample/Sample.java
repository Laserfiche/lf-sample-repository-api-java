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
        try {
            String repoName = getRepoName().join();
            Entry rootFolder = getRootFolder().join();
            List<Entry> rootFolderChildren = getFolderChildren(ROOTFOLDERENTRYID).join();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static CompletableFuture<String> getRepoName() {
        CompletableFuture<List<RepositoryInfo>> repoListResponse = client.getRepositoryClient().getRepositoryList();
        String repoName = repoListResponse.join().get(0).getRepoName();
        String repoId = repoListResponse.join().get(0).getRepoId();
        System.out.println("Repository Name: " + repoId);
        repoListResponse.cancel(true);
        return CompletableFuture.completedFuture(repoName);
    }

    public static CompletableFuture<Entry> getRootFolder() {
        CompletableFuture<Entry> entryResponse = client.getEntriesClient().getEntry(ServiceConfig.repoId, ROOTFOLDERENTRYID, null);
        Entry entry = entryResponse.join();
        String rootFolderName = entry.getName();
        if (rootFolderName.length() == 0) {
            rootFolderName = "/";
        }
        System.out.println("Root Folder Name: " + rootFolderName);
        entryResponse.cancel(true);
        return CompletableFuture.completedFuture(entry);
    }

    public static CompletableFuture<List<Entry>> getFolderChildren(int folderEntryId) {
        CompletableFuture<ODataValueContextOfIListOfEntry> folderChildren = client.getEntriesClient().getEntryListing(ServiceConfig.repoId, folderEntryId, true, null, null, null, null, null, "name", null, null, null, null);
        List<Entry> children = folderChildren.join().getValue();
        int i = 0;
        for (Entry child : children) {
            System.out.println(i++ + ":[" + child.getEntryType() + " id:" + child.getId() + "] " + "'" + child.getName() + "'");
        }
        folderChildren.cancel(true);
        return CompletableFuture.completedFuture(children);
    }

    public static RepositoryApiClient createRepoApiClient() {
        ServiceConfig.setUp();
        return RepositoryApiClientImpl.CreateFromAccessKey(ServiceConfig.spKey, ServiceConfig.accessKey);
    }
}
