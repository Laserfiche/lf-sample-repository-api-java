package sample;

import com.laserfiche.repository.api.RepositoryApiClient;
import com.laserfiche.repository.api.RepositoryApiClientImpl;
import com.laserfiche.repository.api.clients.impl.model.*;
import com.laserfiche.repository.api.clients.params.*;
import org.threeten.bp.OffsetDateTime;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sample {
    private static final int ROOT_FOLDER_ENTRY_ID = 1;
    private static String sampleProjectEdocName = "Java Sample Project GetDocumentContent";
    private static RepositoryApiClient client;
    private static ServiceConfig config;

    public static void main(String[] args) {
        config = new ServiceConfig();
        // Scope(s) requested by the app
        String scope = "repository.Read,repository.Write";
        if (AuthorizationType.CLOUD_ACCESS_KEY.name().equalsIgnoreCase(config.getAuthorizationType().toString())) {
            client = createCloudRepositoryApiClient(scope);
        } else {
            client = createSelfHostedRepositoryApiClient();
        }
        RepositoryInfo[] repositoryNames = getRepositoryInfo(); // Print repository name
        Entry rootFolder = getFolder(ROOT_FOLDER_ENTRY_ID); // Print root folder name
        List<Entry> folderChildren = getFolderChildren(ROOT_FOLDER_ENTRY_ID); // Print root folder children
        Entry createFolder = createFolder(); // Creates a sample project folder
        int tempEdocEntryId = importDocument(createFolder.getId(), sampleProjectEdocName); // Imports a document inside the sample project folder
        Entry setEntryFields = setEntryFields(createFolder.getId()); // Set Entry Fields
        Entry sampleProjectRootFolder = getFolder(createFolder.getId()); // Print root folder name
        List<Entry> sampleProjectRootFolderChildren = getFolderChildren(createFolder.getId()); // Print root folder children
        ODataValueContextOfIListOfFieldValue entryFields = getEntryFields(setEntryFields.getId()); // Print entry Fields
        Map<String, String> entryContentType = getEntryContentType(tempEdocEntryId); // Print Edoc Information
        searchForImportedDocument(sampleProjectEdocName); // Search for the imported document inside the sample project folder
        deleteSampleProjectFolder(createFolder.getId()); // Deletes sample project folder and its contents inside it
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

    public static Entry getFolder(int folderEntryId) {
        Entry rootEntry = client.getEntriesClient().getEntry(new ParametersForGetEntry().setRepoId(config.getRepositoryId()).setEntryId(folderEntryId));
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

    public static Entry createFolder() {
        final String newEntryName = "Java sample project folder";
        final PostEntryChildrenRequest request = new PostEntryChildrenRequest();
        request.setEntryType(PostEntryChildrenEntryType.FOLDER);
        request.setName(newEntryName);
        System.out.println("\nCreating sample project folder...");
        final Entry result = client.getEntriesClient().createOrCopyEntry(new ParametersForCreateOrCopyEntry().setRepoId(config.getRepositoryId()).setEntryId(ROOT_FOLDER_ENTRY_ID).setRequestBody(request).setAutoRename(true));
        return result;
    }

    public static int importDocument(int folderEntryId, String sampleProjectFileName) {
        CreateEntryResult result = null;
        String fileContent = "This is the file content";
        InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());
        System.out.println("\nImporting a document into the sample project folder...");
        result = client.getEntriesClient().importDocument(new ParametersForImportDocument()
                .setRepoId(config.getRepositoryId())
                .setParentEntryId(folderEntryId)
                .setFileName(sampleProjectFileName)
                .setAutoRename(true)
                .setInputStream(inputStream)
                .setRequestBody(new PostEntryWithEdocMetadataRequest()));
        int edocEntryId = result.getOperations().getEntryCreate().getEntryId();
        return edocEntryId;
    }

    public static Entry setEntryFields(int sampleProjectFolderEntryId) {
        WFieldInfo field = null;
        final String fieldValue = "Java sample project set entry value";
        final ODataValueContextOfIListOfWFieldInfo fieldDefinitionsResponse = client.getFieldDefinitionsClient().getFieldDefinitions(new ParametersForGetFieldDefinitions().setRepoId(config.getRepositoryId()));
        final WFieldInfo[] fieldDefinitions = fieldDefinitionsResponse.getValue().toArray(new WFieldInfo[0]);
        for (int i = 0; i < fieldDefinitions.length; i++) {
            if (fieldDefinitions[i].getFieldType() == WFieldType.STRING && (fieldDefinitions[i].getConstraint() == "" || fieldDefinitions[i].getConstraint() == null) &&
                    (((fieldDefinitions[i].getLength() != null) ? fieldDefinitions[i].getLength() : -1) >= 1)) {
                field = fieldDefinitions[i];
                break;
            }
        }
        if (field.getName() == null) {
            throw new Error("field is undefined");
        }
        Map<String, FieldToUpdate> requestBody = new HashMap<>();
        FieldToUpdate fieldToUpdate = new FieldToUpdate();
        requestBody.put(field.getName(), fieldToUpdate);

        fieldToUpdate.setValues(new ArrayList<>());
        ValueToUpdate valueToUpdate = new ValueToUpdate();
        fieldToUpdate.getValues().add(valueToUpdate);

        valueToUpdate.setPosition(1);
        valueToUpdate.setValue(fieldValue);

        Entry entry = createEntry(
                client, "RepositoryApiClientIntegrationTest Java SetFields", sampleProjectFolderEntryId, true);
        Integer entryId = entry.getId();
        System.out.println("\nSetting Entry Fields in the sample project folder...\n");
        ODataValueOfIListOfFieldValue assignFieldValuesResponse = client
                .getEntriesClient()
                .assignFieldValues(new ParametersForAssignFieldValues()
                        .setRepoId(config.getRepositoryId())
                        .setEntryId(entryId)
                        .setRequestBody(requestBody));
        return entry;
    }

    public static ODataValueContextOfIListOfFieldValue getEntryFields(int setFieldsEntryId) {
        final ODataValueContextOfIListOfFieldValue entryFieldResponse = client.getEntriesClient().getFieldValues(
                new ParametersForGetFieldValues().setRepoId(config.getRepositoryId()).setEntryId(setFieldsEntryId));
        final FieldValue[] fieldDefinitions = entryFieldResponse.getValue().toArray(new FieldValue[0]);
        System.out.println("Entry Field Name:" + fieldDefinitions[0].getFieldName());
        System.out.println("Entry Field Type:" + fieldDefinitions[0].getFieldType());
        System.out.println("Entry Field ID:" + fieldDefinitions[0].getFieldId());
        System.out.println("Entry Field Value:" + fieldDefinitions[0].getValues());
        return entryFieldResponse;
    }

    public static Map<String, String> getEntryContentType(int tempEdocEntryId) {
        final Map<String, String> documentContentTypeResponse = client.getEntriesClient().getDocumentContentType(
                new ParametersForGetDocumentContentType().setRepoId(config.getRepositoryId()).setEntryId(tempEdocEntryId));
        System.out.println("Electronic Document Content Type:" + documentContentTypeResponse.get("Content-Type"));
        System.out.println("Electronic Document Content Length:" + documentContentTypeResponse.get("Content-Length"));
        return documentContentTypeResponse;
    }

    public static void searchForImportedDocument(String sampleProjectFileName) {
        final SimpleSearchRequest searchRequest = new SimpleSearchRequest();
        String searchCommand = "({LF:Basic ~= \"" + sampleProjectFileName + "\", option=\"DFANLT\"})";
        searchRequest.setSearchCommand(searchCommand);
        System.out.println("\nSearching for imported document...");
        final ODataValueContextOfIListOfEntry simpleSearchResponse = client.getSimpleSearchesClient().createSimpleSearchOperation(
                new ParametersForCreateSimpleSearchOperation().setRepoId(config.getRepositoryId()).setRequestBody(searchRequest));
        System.out.println("\nSearch Results");
        final Entry[] searchResults = simpleSearchResponse.getValue().toArray(new Entry[0]);
        for (int i = 0; i < searchResults.length; i++) {
            final Entry child = searchResults[i];
            System.out.print(i + ":[" + child.getEntryType() + "id:" + child.getId() + "]" + child.getName());
        }
        System.out.println();
    }

    public static void deleteSampleProjectFolder(int sampleProjectFolderEntryId) {
        System.out.println("\nDeleting all sample project entries...");
        client.getEntriesClient().deleteEntryInfo(new ParametersForDeleteEntryInfo().setRepoId(config.getRepositoryId()).setEntryId(sampleProjectFolderEntryId));
        System.out.println("\nDeleted all sample project entries");
    }

    public static RepositoryApiClient createCloudRepositoryApiClient(String scope) {
        final RepositoryApiClient repositoryApiClient = RepositoryApiClientImpl.createFromAccessKey(config.getServicePrincipalKey(), config.getAccessKey(), scope);
        return repositoryApiClient;
    }

    public static RepositoryApiClient createSelfHostedRepositoryApiClient() {
        final RepositoryApiClient repositoryApiClient = RepositoryApiClientImpl.createFromUsernamePassword(config.getRepositoryId(), config.getUsername(), config.getPassword(), config.getBaseUrl());
        return repositoryApiClient;
    }

    public static Entry createEntry(
            RepositoryApiClient client, String entryName, Integer parentEntryId, Boolean autoRename) {
        PostEntryChildrenRequest request = new PostEntryChildrenRequest();
        request.setEntryType(PostEntryChildrenEntryType.FOLDER);
        request.setName(entryName);

        return client.getEntriesClient()
                .createOrCopyEntry(new ParametersForCreateOrCopyEntry()
                        .setRepoId(config.getRepositoryId())
                        .setEntryId(parentEntryId)
                        .setRequestBody(request)
                        .setAutoRename(autoRename));
    }
}
