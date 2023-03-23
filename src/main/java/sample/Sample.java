package sample;

import com.laserfiche.repository.api.RepositoryApiClient;
import com.laserfiche.repository.api.RepositoryApiClientImpl;
import com.laserfiche.repository.api.clients.impl.model.Entry;
import com.laserfiche.repository.api.clients.impl.model.EntryType;
import com.laserfiche.repository.api.clients.impl.model.ODataValueContextOfIListOfEntry;
import com.laserfiche.repository.api.clients.impl.model.RepositoryInfo;
import com.laserfiche.repository.api.clients.params.ParametersForGetEntry;
import com.laserfiche.repository.api.clients.params.ParametersForGetEntryListing;
import org.threeten.bp.OffsetDateTime;

import java.util.List;

public class Sample {
    private static final int ROOT_FOLDER_ENTRY_ID = 1;
    private static RepositoryApiClient client;
    private static ServiceConfig config;

    public static void main(String[] args) {
        config = new ServiceConfig();
        if (AuthorizationType.CLOUD_ACCESS_KEY.name().equalsIgnoreCase(config.getAuthorizationType().toString())) {
            client = RepositoryApiClientImpl.createFromAccessKey(config.getServicePrincipalKey(), config.getAccessKey());
        } else {
            client = RepositoryApiClientImpl.createFromUsernamePassword(config.getRepositoryId(), config.getUsername(), config.getPassword(), config.getBaseUrl());
        }
        RepositoryInfo[] repositoryNames = getRepositoryInfo();
        Entry rootFolder = getRootFolder();
        List<Entry> folderChildren = getFolderChildren(ROOT_FOLDER_ENTRY_ID);
        client.close();
    }

    public static RepositoryInfo[] getRepositoryInfo() {
        RepositoryInfo[] repositoryInfoArray = client.getRepositoryClient().getRepositoryList();
        for (RepositoryInfo repositoryInfo : repositoryInfoArray) {
            System.out.printf("Repository Name: %s%nRepository ID: %s%n%n", repositoryInfo.getRepoName(),
                    repositoryInfo.getRepoId());
        }
        return repositoryInfoArray;
    }

    public static Entry getRootFolder() {
        Entry rootEntry = client.getEntriesClient().getEntry(new ParametersForGetEntry().setRepoId(config.getRepositoryId()).setEntryId(ROOT_FOLDER_ENTRY_ID));
        EntryType entryType = rootEntry.getEntryType();
        OffsetDateTime createdDate = rootEntry.getCreationTime();
        System.out.printf("Root folder information:%nType: %s%nCreation Date: %s%n", entryType,
                createdDate);
        return rootEntry;
    }

    public static List<Entry> getFolderChildren(int folderId) {
        ODataValueContextOfIListOfEntry entriesData = client.getEntriesClient().getEntryListing(new ParametersForGetEntryListing().setRepoId(config.getRepositoryId()).setEntryId(folderId).setGroupByEntryType(true).setOrderby("name"));
        List<Entry> entries = entriesData.getValue();
        for (Entry entry : entries) {
            System.out.printf("[%s id: %d] '%s'%n", entry.getEntryType(), entry.getId(), entry.getName());
        }
        return entries;
    }
}
