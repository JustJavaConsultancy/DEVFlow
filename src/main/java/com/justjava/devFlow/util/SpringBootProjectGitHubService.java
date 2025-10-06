package com.justjava.devFlow.util;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class SpringBootProjectGitHubService {

    private static final String SPRING_INITIALIZR_URL = "https://start.spring.io/starter.zip";
    private static final String GITHUB_API_BASE = "https://api.github.com";

    private final RestTemplate restTemplate;
    private final HttpClient httpClient;

    public SpringBootProjectGitHubService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Main method: Downloads Spring Boot project and pushes to GitHub
     */
    public GitHubRepositoryResult downloadAndPushToGitHub(
            String groupId, String artifactId, String javaVersion,
            String springBootVersion, String dependencies, String githubUsername,
            String githubToken, String repositoryDescription, boolean isPrivateRepo)
            throws ProjectDownloadException, GitHubPushException {

        artifactId = artifactId.replaceAll("\\s+","");
        if(repositoryDescription!=null&&repositoryDescription.length()>=350)
            repositoryDescription = repositoryDescription.substring(0,300);
        //repositoryDescription = repositoryDescription != null ? repositoryDescription.substring(0,300) : "Spring Boot project: " + artifactId;
        System.out.println(" The groupId==="+groupId
                +" the artifactId==="+artifactId
                +" the javaVersion==="+javaVersion
                +" the springBootVersion==="+springBootVersion
                +" the dependencies==="+dependencies
                +" the githubUsername==="+githubUsername
                +" the githubToken==="+githubToken
                +" the repositoryDescription==="+repositoryDescription+
                " the isPrivateRepo==="+isPrivateRepo);
        try {
            // Step 1: Download Spring Boot project WITHOUT baseDir parameter
            byte[] zipData = downloadSpringBootProject(
                    groupId, artifactId, javaVersion, springBootVersion, dependencies);

            // Step 2: Extract files from ZIP and remove the parent folder
            List<GitHubFile> projectFiles = extractFilesFromZip(zipData, artifactId);

            // Step 3: Push to GitHub repository
            String repositoryUrl = pushToGitHubRepository(
                    projectFiles, artifactId, repositoryDescription, isPrivateRepo,
                    githubUsername, githubToken);

            return new GitHubRepositoryResult(repositoryUrl, artifactId, projectFiles.size());

        } catch (IOException | InterruptedException e) {
            throw new GitHubPushException("Failed to push project to GitHub: " + e.getMessage(), e);
        }
    }

    /**
     * Downloads Spring Boot project from Spring Initializr WITHOUT baseDir
     */
    public byte[] downloadSpringBootProject(
            String groupId, String artifactId, String javaVersion,
            String springBootVersion, String dependencies) throws ProjectDownloadException {

        try {
            // Remove baseDir parameter to get files in root of ZIP
            String body = buildRequestPayload(groupId, artifactId, javaVersion, springBootVersion, dependencies);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    SPRING_INITIALIZR_URL, HttpMethod.POST, requestEntity, byte[].class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new ProjectDownloadException(
                        "Failed to download Spring Boot project. Status: " + response.getStatusCode());
            }

            byte[] zipData = response.getBody();
            validateZipFile(zipData);

            return zipData;

        } catch (Exception e) {
            throw new ProjectDownloadException(
                    "Error downloading Spring Boot project from Initializr: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts files from ZIP data and removes the parent folder structure
     */
    public List<GitHubFile> extractFilesFromZip(byte[] zipData, String artifactId) throws IOException {
        List<GitHubFile> files = new ArrayList<>();

        try (ByteArrayInputStream bais = new ByteArrayInputStream(zipData);
             ZipInputStream zis = new ZipInputStream(bais)) {

            ZipEntry entry;
            byte[] buffer = new byte[1024];

            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }

                    String originalFilePath = entry.getName();

                    // Remove the artifactId parent folder from the path
                    String cleanedFilePath = removeParentFolder(originalFilePath, artifactId);

                    // Skip if the path becomes empty (shouldn't happen with valid ZIP)
                    if (cleanedFilePath != null && !cleanedFilePath.isEmpty()) {
                        String content = baos.toString(StandardCharsets.UTF_8);
                        files.add(new GitHubFile(cleanedFilePath, content));
                        System.out.println("Extracted file: " + originalFilePath + " -> " + cleanedFilePath);
                    }
                }
                zis.closeEntry();
            }
        }

        return files;
    }

    /**
     * Removes the parent folder (artifactId) from the file path
     * Example: "JDocMan/pom.xml" -> "pom.xml"
     * Example: "JDocMan/src/main/java/com/example/App.java" -> "src/main/java/com/example/App.java"
     */
    private String removeParentFolder(String filePath, String artifactId) {
        // Handle cases where the file is directly in the artifactId folder
        if (filePath.startsWith(artifactId + "/")) {
            return filePath.substring(artifactId.length() + 1);
        }

        // Handle cases where there might be additional path components
        String prefix = artifactId + "/";
        if (filePath.startsWith(prefix)) {
            return filePath.substring(prefix.length());
        }

        // If the file doesn't start with artifactId/, return as-is (shouldn't happen with Spring Initializr)
        System.out.println("Warning: File path doesn't match expected pattern: " + filePath);
        return filePath;
    }

    /**
     * Builds the request payload for Spring Initializr WITHOUT baseDir
     */
    private String buildRequestPayload(String groupId, String artifactId, String javaVersion,
                                       String springBootVersion, String dependencies) {
        // Remove baseDir parameter to get files in root of ZIP
        return "type=maven-project" +
                "&language=java" +
                "&bootVersion=" + springBootVersion +
                "&javaVersion=" + javaVersion +
                // Remove: "&baseDir=" + artifactId +
                "&groupId=" + groupId +
                "&artifactId=" + artifactId +
                "&name=" + artifactId +
                "&packageName=" + groupId +
                "&dependencies=" + dependencies;
    }

    /**
     * Pushes files to GitHub repository using GitHub API
     */
    private String pushToGitHubRepository(List<GitHubFile> files, String repositoryName,
                                          String description, boolean isPrivate,
                                          String username, String token)
            throws IOException, InterruptedException {

        // Check if repository exists
        if (repositoryExists(username, repositoryName, token)) {
            System.out.println("Repository already exists: " + repositoryName);
            return updateExistingRepository(files, username, repositoryName, token);
        } else {
            System.out.println("Creating new repository: " + repositoryName);
            return createNewRepository(files, username, repositoryName, description, isPrivate, token);
        }
    }

    /**
     * Checks if GitHub repository exists using GitHub API
     */
    private boolean repositoryExists(String username, String repositoryName, String token)
            throws IOException, InterruptedException {

        String url = GITHUB_API_BASE + "/repos/" + username + "/" + repositoryName;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "token " + token)
                .header("Accept", "application/vnd.github.v3+json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 200;
    }

    /**
     * Creates new GitHub repository using GitHub API with auto_init: true
     */
    private String createNewRepository(List<GitHubFile> files, String username, String repositoryName,
                                       String description, boolean isPrivate, String token)
            throws IOException, InterruptedException {

        String url = GITHUB_API_BASE + "/user/repos";
        String payload = String.format(
                "{\"name\": \"%s\", \"description\": \"%s\", \"private\": %s, \"auto_init\": true}",
                repositoryName, description, isPrivate);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "token " + token)
                .header("Accept", "application/vnd.github.v3+json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 201) {
            throw new IOException("Failed to create repository: " + response.body());
        }

        // Wait a moment for GitHub to initialize the repository
        Thread.sleep(1000);

        // Create initial commit with all files
        createInitialCommit(files, username, repositoryName, token);

        return "https://github.com/" + username + "/" + repositoryName;
    }

    /**
     * Updates existing repository by creating individual file commits
     */
    private String updateExistingRepository(List<GitHubFile> files, String username,
                                            String repositoryName, String token)
            throws IOException, InterruptedException {

        // For existing repositories, create or update files individually
        for (GitHubFile file : files) {
            createOrUpdateFile(username, repositoryName, file, token);
        }

        return "https://github.com/" + username + "/" + repositoryName;
    }

    /**
     * Creates initial commit for a new repository using the contents API
     */
    private void createInitialCommit(List<GitHubFile> files, String username,
                                     String repositoryName, String token)
            throws IOException, InterruptedException {

        // For each file, use the contents API to create it
        for (GitHubFile file : files) {
            createOrUpdateFile(username, repositoryName, file, token);
        }
    }

    /**
     * Creates or updates a file using GitHub Contents API
     */
    private void createOrUpdateFile(String username, String repositoryName,
                                    GitHubFile file, String token)
            throws IOException, InterruptedException {

        String url = GITHUB_API_BASE + "/repos/" + username + "/" + repositoryName + "/contents/" + file.getFilePath();

        // First, check if file exists to get its SHA (for updates)
        String existingSha = getFileSha(username, repositoryName, file.getFilePath(), token);

        String payload;
        if (existingSha != null) {
            // Update existing file
            payload = String.format(
                    "{\"message\": \"Add %s\", \"content\": \"%s\", \"sha\": \"%s\"}",
                    file.getFilePath(),
                    java.util.Base64.getEncoder().encodeToString(file.getContent().getBytes(StandardCharsets.UTF_8)),
                    existingSha);
        } else {
            // Create new file
            payload = String.format(
                    "{\"message\": \"Add %s\", \"content\": \"%s\"}",
                    file.getFilePath(),
                    java.util.Base64.getEncoder().encodeToString(file.getContent().getBytes(StandardCharsets.UTF_8)));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "token " + token)
                .header("Accept", "application/vnd.github.v3+json")
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200 && response.statusCode() != 201) {
            System.err.println("Failed to create/update file " + file.getFilePath() + ": " + response.body());
            // Continue with other files instead of failing completely
        } else {
            System.out.println("Successfully created/updated file: " + file.getFilePath());
        }
    }

    /**
     * Gets the SHA of an existing file (returns null if file doesn't exist)
     */
    private String getFileSha(String username, String repositoryName, String filePath, String token)
            throws IOException, InterruptedException {

        String url = GITHUB_API_BASE + "/repos/" + username + "/" + repositoryName + "/contents/" + filePath;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "token " + token)
                .header("Accept", "application/vnd.github.v3+json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // Extract SHA from response
            String responseBody = response.body();
            int shaStart = responseBody.indexOf("\"sha\"") + 7;
            int shaEnd = responseBody.indexOf("\"", shaStart);
            return responseBody.substring(shaStart, shaEnd);
        }

        return null; // File doesn't exist
    }

    /**
     * Validates ZIP file signature
     */
    private void validateZipFile(byte[] zipData) throws ProjectDownloadException {
        if (zipData.length < 4 || zipData[0] != 'P' || zipData[1] != 'K') {
            throw new ProjectDownloadException("Invalid ZIP file received from Spring Initializr");
        }
    }

    // Supporting data classes and exceptions remain the same
    public static final class GitHubFile {
        private final String filePath;
        private final String content;

        public GitHubFile(String filePath, String content) {
            this.filePath = filePath;
            this.content = content;
        }

        public String getFilePath() { return filePath; }
        public String getContent() { return content; }
    }
    /**
     * Pushes existing files to GitHub repository (for use by ArtifactFileExtractor)
     */
    public GitHubRepositoryResult pushFilesToGitHubRepository(
            List<GitHubFile> files, String repositoryName, String description,
            boolean isPrivate, String username, String token)
            throws GitHubPushException {

        try {
            String repositoryUrl = pushToGitHubRepository(
                    files, repositoryName, description, isPrivate, username, token);

            return new GitHubRepositoryResult(repositoryUrl, repositoryName, files.size());

        } catch (IOException | InterruptedException e) {
            throw new GitHubPushException("Failed to push files to GitHub repository: " + e.getMessage(), e);
        }
    }

    public static final class GitHubRepositoryResult {
        private final String repositoryUrl;
        private final String repositoryName;
        private final int filesCount;

        public GitHubRepositoryResult(String repositoryUrl, String repositoryName, int filesCount) {
            this.repositoryUrl = repositoryUrl;
            this.repositoryName = repositoryName;
            this.filesCount = filesCount;
        }

        public String getRepositoryUrl() { return repositoryUrl; }
        public String getRepositoryName() { return repositoryName; }
        public int getFilesCount() { return filesCount; }
    }

    public static class ProjectDownloadException extends Exception {
        public ProjectDownloadException(String message) { super(message); }
        public ProjectDownloadException(String message, Throwable cause) { super(message, cause); }
    }

    public static class GitHubPushException extends Exception {
        public GitHubPushException(String message) { super(message); }
        public GitHubPushException(String message, Throwable cause) { super(message, cause); }
    }

}