package sample;

import com.laserfiche.api.client.model.ProblemDetails;
import com.laserfiche.repository.api.RepositoryApiClient;
import com.laserfiche.repository.api.RepositoryApiClientImpl;
import com.laserfiche.repository.api.clients.impl.model.*;
import com.laserfiche.repository.api.clients.params.*;
import org.threeten.bp.OffsetDateTime;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Sample {
    private static final int ROOT_FOLDER_ENTRY_ID = 1;
    private static final String sampleProjectDocumentName = "Java Sample Project Document";
    private static RepositoryApiClient client;
    private static ServiceConfig config;

    public static void main(String[] args) {
        config = new ServiceConfig();
        String requiredScopes = "repository.Read repository.Write";
        if (AuthorizationType.CLOUD_ACCESS_KEY.name().equalsIgnoreCase(config.getAuthorizationType().toString())) {
            client = createCloudRepositoryApiClient(requiredScopes);
        } else {
            client = createSelfHostedRepositoryApiClient();
        }
        Entry sampleFolderEntry = null;
        try {
            printAllRepositoryNames();
            printFolderName(ROOT_FOLDER_ENTRY_ID);
            printFolderChildrenInformation(ROOT_FOLDER_ENTRY_ID);
            sampleFolderEntry = createSampleProjectFolder();
            int importedEntryId = importDocument(sampleFolderEntry.getId(), sampleProjectDocumentName);
            setEntryFields(importedEntryId);
            printEntryFields(importedEntryId);
            printEntryContentType(importedEntryId);
            searchForImportedDocument(sampleProjectDocumentName);
            importLargeDocument(sampleFolderEntry.getId());
        } finally {
            if (sampleFolderEntry != null) {
                deleteSampleProjectFolder(sampleFolderEntry.getId());
            }
            client.close();
        }
    }

    /**
     * Prints the information of all the available repositories.
     */
    public static void printAllRepositoryNames() {
        RepositoryCollectionResponse collectionResponse = client.getRepositoryClient().listRepositories();
        for (Repository repository : collectionResponse.getValue()) {
            System.out.printf("Repository Name: '%s' Repository ID: %s%n", repository.getName(),
                    repository.getId());
        }
    }

    /**
     * Prints the name of the folder for the given folder's entry Id.
     */
    public static Entry printFolderName(int folderEntryId) {
        Entry entry = client.getEntriesClient().getEntry(new ParametersForGetEntry()
                .setRepositoryId(config.getRepositoryId()).setEntryId(folderEntryId));
        OffsetDateTime createdDate = entry.getCreationTime();
        System.out.printf("Folder Name: '%s' Full Path: '%s' Creation Date: %s%n", entry.getName(), entry.getFullPath(),
                createdDate);
        return entry;
    }

    /**
     * Prints the information of the child entries of the given folder's entry Id.
     */
    public static void printFolderChildrenInformation(int folderId) {
        EntryCollectionResponse collectionResponse = client.getEntriesClient().listEntries(new ParametersForListEntries()
                .setRepositoryId(config.getRepositoryId()).setEntryId(folderId).setGroupByEntryType(true).setOrderby("name"));
        List<Entry> entries = collectionResponse.getValue();
        for (Entry entry : entries) {
            System.out.printf("Id: %d Name: '%s' Type: '%s'%n", entry.getId(), entry.getName(), entry.getEntryType());
        }
    }

    /**
     * Creates a sample folder in the root folder.
     */
    public static Entry createSampleProjectFolder() {
        final String newEntryName = "Java sample project folder";
        final CreateEntryRequest request = new CreateEntryRequest();
        request.setEntryType(CreateEntryRequestEntryType.FOLDER);
        request.setName(newEntryName);
        request.setAutoRename(true);
        System.out.println("Creating sample project folder...");
        final Entry newEntry = client.getEntriesClient().createEntry(new ParametersForCreateEntry()
                .setRepositoryId(config.getRepositoryId()).setEntryId(ROOT_FOLDER_ENTRY_ID).setRequestBody(request));
        System.out.printf("Done! Entry Id: %d%n", newEntry.getId());
        return newEntry;
    }

    /**
     * Imports a document into the folder specified by the given entry Id.
     */
    public static int importDocument(int folderEntryId, String sampleProjectFileName) {
        Entry importedEntry = null;
        String fileContent = "This is the file content";
        InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());
        System.out.println("Importing a document into the sample project folder...");
        ImportEntryRequest request = new ImportEntryRequest();
        request.setName(sampleProjectFileName);
        request.setAutoRename(true);
        importedEntry = client.getEntriesClient().importEntry(new ParametersForImportEntry()
                .setRepositoryId(config.getRepositoryId())
                .setEntryId(folderEntryId)
                .setInputStream(inputStream)
                .setContentType("text/plain")
                .setRequestBody(request));
        System.out.printf("Done! Entry Id: %d%n", importedEntry.getId());
        return importedEntry.getId();
    }

    /**
     * Sets a string field on the entry specified by the given entry Id.
     */
    public static void setEntryFields(int entryId) {
        final String fieldValue = "Java sample project set entry value";
        final FieldDefinitionCollectionResponse collectionResponse = client.getFieldDefinitionsClient().listFieldDefinitions(
                new ParametersForListFieldDefinitions().setRepositoryId(config.getRepositoryId()));
        if (collectionResponse == null || collectionResponse.getValue().isEmpty()) {
            System.out.println("There is no FieldDefinition available.");
            return;
        }
        List<FieldDefinition> fieldDefinitions = collectionResponse.getValue();
        FieldDefinition field = null;
        for (FieldDefinition fieldDefinition : fieldDefinitions) {
            if (fieldDefinition.getFieldType() == FieldType.STRING
                    && (fieldDefinition.getConstraint() == null || fieldDefinition.getConstraint().isEmpty())
                    && (((fieldDefinition.getLength() != null) ? fieldDefinition.getLength() : -1) >= 1)
            ) {
                field = fieldDefinition;
                break;
            }
        }
        if (field == null || field.getName() == null || field.getName().isEmpty()) {
            System.out.println("The FieldDefinition's name is undefined.");
            return;
        }

        SetFieldsRequest requestBody = new SetFieldsRequest();

        FieldToUpdate fieldToUpdate = new FieldToUpdate();
        fieldToUpdate.setName(field.getName());
        List<String> values = new ArrayList<>();
        values.add(fieldValue);
        fieldToUpdate.setValues(values);
        List<FieldToUpdate> fieldsToUpdate = new ArrayList<>();
        fieldsToUpdate.add(fieldToUpdate);
        requestBody.setFields(fieldsToUpdate);

        System.out.println("Setting Entry Fields in the sample project folder...");
        FieldCollectionResponse fieldCollectionResponse = client
                .getEntriesClient()
                .setFields(new ParametersForSetFields()
                        .setRepositoryId(config.getRepositoryId())
                        .setEntryId(entryId)
                        .setRequestBody(requestBody));
        if (fieldCollectionResponse.getValue() != null) {
            System.out.printf("Number of fields set on the entry: %d%n", fieldCollectionResponse.getValue().size());
        }
    }

    /**
     * Prints the fields assigned to the entry specified by the given entry Id.
     */
    public static void printEntryFields(int entryId) {
        final FieldCollectionResponse collectionResponse = client.getEntriesClient().listFields(
                new ParametersForListFields().setRepositoryId(config.getRepositoryId()).setEntryId(entryId));
        List<Field> fields = collectionResponse.getValue();
        for (Field field : fields) {
            System.out.printf("Id: %d Name: '%s' Type: %s Value: %s%n", field.getId(), field.getName(), field.getFieldType(), field.getValues().stream().collect(Collectors.joining(", ")));
        }
    }

    /**
     * Prints the content-type of the electronic document associated with the given entry Id.
     */
    public static void printEntryContentType(int entryId) {
        ExportEntryRequest request = new ExportEntryRequest();
        request.setPart(ExportEntryRequestPart.EDOC);
        int exportAuditReasonId = getAuditReasonIdForExport();
        if (exportAuditReasonId != -1) {
            request.setAuditReasonId(exportAuditReasonId);
        }
        ExportEntryResponse response = client.getEntriesClient().exportEntry(new ParametersForExportEntry()
                .setRepositoryId(config.getRepositoryId())
                .setEntryId(entryId)
                .setRequestBody(request));
        String uri = response.getValue();
        try {
            URLConnection connection = new URL(uri).openConnection();
            String mimeType = connection.getContentType();
            System.out.printf("Electronic Document Content Type: %s%n", mimeType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Performs a simple search for the given file name, and prints out the search results.
     */
    public static void searchForImportedDocument(String sampleProjectFileName) {
        final SearchEntryRequest searchRequest = new SearchEntryRequest();
        String searchCommand = "({LF:Basic ~= \"" + sampleProjectFileName + "\", option=\"DFANLT\"})";
        searchRequest.setSearchCommand(searchCommand);
        System.out.println("Searching for imported document...");
        final EntryCollectionResponse collectionResponse = client.getSimpleSearchesClient().searchEntry(
                new ParametersForSearchEntry().setRepositoryId(config.getRepositoryId()).setRequestBody(searchRequest));
        System.out.println("Search Results:");
        List<Entry> searchResults = collectionResponse.getValue();
        for (Entry entry : searchResults) {
            System.out.printf("Id: %d, Name: '%s', Type: %s%n", entry.getId(), entry.getName(), entry.getEntryType());
        }
    }

    /**
     * Deletes the sample project folder.
     */
    public static void deleteSampleProjectFolder(int sampleProjectFolderEntryId) {
        System.out.println("Deleting all sample project entries...");
        StartTaskResponse response = client.getEntriesClient().startDeleteEntry(new ParametersForStartDeleteEntry().setRepositoryId(config.getRepositoryId()).setEntryId(sampleProjectFolderEntryId));
        String taskId = response.getTaskId();
        System.out.printf("Task Id: %s%n", taskId);
        TaskCollectionResponse collectionResponse = client.getTasksClient().listTasks(new ParametersForListTasks().setRepositoryId(config.getRepositoryId()).setTaskIds(taskId));
        TaskProgress taskProgress = collectionResponse.getValue().get(0);
        TaskStatus taskStatus = taskProgress.getStatus();
        System.out.printf("Task Status: %s%n", taskStatus);
    }

    /**
     * Uses the asynchronous import API to import a large file into the specified folder.
     */
    public static void importLargeDocument(int folderEntryId) {
        File file = new File("testFiles", "sample.pdf");
        String mimeType = "application/pdf";

        // Step 1: Get upload URLs
        int parts = 2;
        int partSizeInMB = 5;
        CreateMultipartUploadUrlsRequest requestBody = new CreateMultipartUploadUrlsRequest();
        requestBody.setFileName(file.getName());
        requestBody.setMimeType(mimeType);
        requestBody.setNumberOfParts(parts);

        System.out.println("Requesting upload URLs...");
        CreateMultipartUploadUrlsResponse response = client.getEntriesClient().createMultipartUploadUrls(new ParametersForCreateMultipartUploadUrls()
                .setRepositoryId(config.getRepositoryId()).setRequestBody(requestBody));

        String uploadId = response.getUploadId();

        // Step 2: Write file part into upload URLs
        System.out.println("Writing file parts to upload URLs...");
        List<String> eTags = writeFile(file.getPath(), response.getUrls(), partSizeInMB);

        // Step 3: Call ImportUploadedParts API
        System.out.println("Starting the import task...");
        StartImportUploadedPartsRequest requestBody2 = new StartImportUploadedPartsRequest();
        requestBody2.setUploadId(uploadId);
        requestBody2.setAutoRename(true);
        requestBody2.setPartETags(eTags);
        requestBody2.setName(file.getName());
        ImportEntryRequestPdfOptions pdfOptions = new ImportEntryRequestPdfOptions();
        pdfOptions.setGeneratePages(true);
        pdfOptions.setKeepPdfAfterImport(true);
        requestBody2.setPdfOptions(pdfOptions);
        StartTaskResponse response2 = client.getEntriesClient().startImportUploadedParts(new ParametersForStartImportUploadedParts()
                .setRepositoryId(config.getRepositoryId())
                .setEntryId(folderEntryId)
                .setRequestBody(requestBody2));

        String taskId = response2.getTaskId();
        System.out.printf("Task Id: %s%n", taskId);

        // Check/print the status of the import task.
        TaskCollectionResponse tasks = client.getTasksClient().listTasks(new ParametersForListTasks().setRepositoryId(config.getRepositoryId()).setTaskIds(taskId));
        TaskProgress taskProgress = tasks.getValue().get(0);
        System.out.printf("Task Status: %s%n", taskProgress.getStatus());
        switch (taskProgress.getStatus()) {
            case COMPLETED:
                System.out.printf("Entry Id: %d%n", taskProgress.getResult().getEntryId());
                break;
            case FAILED:
                for (ProblemDetails problemDetails : taskProgress.getErrors()) {
                    printProblemDetails(problemDetails);
                }
                break;
        }
    }

    /**
     * Splits the given file into parts of the given size, and writes them into the given URLs. Finally, returns the eTags of the written parts.
     */
    private static List<String> writeFile(String filePath, List<String> urls, int partSizeInMB) {
        File file = new File(filePath);
        List<String> eTags = new ArrayList<>(urls.size());
        try (FileInputStream inputStream = new FileInputStream(file)) {
            for (String uploadUrl : urls) {
                byte[] buffer = new byte[partSizeInMB * 1024 * 1024];
                int numberOfBytesRead = inputStream.read(buffer);

                URL url = new URL(uploadUrl);
                HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
                httpCon.setDoOutput(true);
                httpCon.setRequestMethod("PUT");
                httpCon.setRequestProperty("Content-Type", "application/octet-stream");

                DataOutputStream outputStream = new DataOutputStream(
                        httpCon.getOutputStream());
                outputStream.write(buffer, 0, numberOfBytesRead);
                outputStream.flush();
                outputStream.close();
                if (httpCon.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    String eTag = httpCon.getHeaderField("ETag");
                    eTags.add(eTag);
                }
                httpCon.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return eTags;
    }

    /**
     * Creates RepositoryApiClient for cloud, from an access key.
     */
    public static RepositoryApiClient createCloudRepositoryApiClient(String scope) {
        return RepositoryApiClientImpl.createFromAccessKey(config.getServicePrincipalKey(), config.getAccessKey(), scope);
    }

    /**
     * Creates RepositoryApiClient for self-hosted mode, from a username/password.
     */
    public static RepositoryApiClient createSelfHostedRepositoryApiClient() {
        return RepositoryApiClientImpl.createFromUsernamePassword(config.getRepositoryId(), config.getUsername(), config.getPassword(), config.getBaseUrl());
    }

    /**
     * Searches for the audit reason for export operation, and if found, returns its Id. Otherwise, returns -1.
     */
    private static int getAuditReasonIdForExport() {
        int exportAuditReasonId = -1;
        AuditReasonCollectionResponse auditReasons = client.getAuditReasonsClient().listAuditReasons(
                new ParametersForListAuditReasons().setRepositoryId(config.getRepositoryId()));
        Optional<AuditReason> exportAuditReason = auditReasons.getValue().stream().filter(auditReason -> auditReason.getAuditEventType() == AuditEventType.EXPORT_DOCUMENT).findFirst();
        if (exportAuditReason.isPresent()) {
            exportAuditReasonId = exportAuditReason.get().getId();
        }
        return exportAuditReasonId;
    }

    /**
     * Prints the information of the given ProblemDetails object.
     */
    private static void printProblemDetails(ProblemDetails problemDetails) {
        System.out.printf("ProblemDetails: (Title: %s, Status: %d, Detail: %s, Type: %s, Instance: %s, ErrorCode: %d, ErrorSource: %s)%n",
                problemDetails.getTitle(),
                problemDetails.getStatus(),
                problemDetails.getDetail(),
                problemDetails.getType(),
                problemDetails.getInstance(),
                problemDetails.getErrorCode(),
                problemDetails.getErrorSource());
    }
}
