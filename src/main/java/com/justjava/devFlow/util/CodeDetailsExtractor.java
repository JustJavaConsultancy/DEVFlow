package com.justjava.devFlow.util;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CodeDetailsExtractor {

    private final SpringBootProjectGitHubService projectGitHubService;

    public CodeDetailsExtractor(SpringBootProjectGitHubService projectGitHubService) {
        this.projectGitHubService = projectGitHubService;
    }

    public static class ExtractedCodeFile {
        private final String filePath;
        private final String content;
        private final FileType fileType;

        public ExtractedCodeFile(String filePath, String content, FileType fileType) {
            this.filePath = filePath;
            this.content = content;
            this.fileType = fileType;
        }

        public String getFilePath() { return filePath; }
        public String getContent() { return content; }
        public FileType getFileType() { return fileType; }
    }

    public enum FileType {
        JAVA_MAIN,
        JAVA_TEST,
        HTML_TEMPLATE,
        SQL_SCHEMA,
        YAML_CONFIG,
        UNKNOWN
    }

    /**
     * Main method: Extracts code files from codeDetails and pushes them to GitHub
     */
    public GitHubPushResult extractAndPushToGitHub(String codeDetails, String repositoryName,
                                                   String githubUsername, String githubToken,
                                                   String repositoryDescription, boolean isPrivateRepo)
            throws CodeExtractionException, GitHubPushException {

        try {
            System.out.println("🚀 Starting code extraction from codeDetails and pushing to GitHub...");

            // Extract code files
            List<ExtractedCodeFile> extractedFiles = extractCodeFiles(codeDetails);

            if (extractedFiles.isEmpty()) {
                throw new CodeExtractionException("No code files were extracted from the codeDetails");
            }

            System.out.println("✅ Successfully extracted " + extractedFiles.size() + " code files");

            // Convert to GitHub files and push to repository
            List<SpringBootProjectGitHubService.GitHubFile> githubFiles = convertToGitHubFiles(extractedFiles);

            // Use SpringBootProjectGitHubService to push to GitHub
            SpringBootProjectGitHubService.GitHubRepositoryResult result =
                    projectGitHubService.pushFilesToGitHubRepository(
                            githubFiles, repositoryName, repositoryDescription, isPrivateRepo,
                            githubUsername, githubToken
                    );

            return new GitHubPushResult(
                    result.getRepositoryUrl(),
                    result.getRepositoryName(),
                    result.getFilesCount(),
                    extractedFiles.size(),
                    getFileTypeBreakdown(extractedFiles)
            );

        } catch (SpringBootProjectGitHubService.GitHubPushException e) {
            throw new GitHubPushException("Failed to push code files to GitHub: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new CodeExtractionException("Error during code extraction: " + e.getMessage(), e);
        }
    }

    /**
     * Converts internal ExtractedCodeFile to SpringBootProjectGitHubService.GitHubFile
     */
    private List<SpringBootProjectGitHubService.GitHubFile> convertToGitHubFiles(List<ExtractedCodeFile> extractedFiles) {
        List<SpringBootProjectGitHubService.GitHubFile> githubFiles = new ArrayList<>();

        for (ExtractedCodeFile extractedFile : extractedFiles) {
            githubFiles.add(new SpringBootProjectGitHubService.GitHubFile(
                    extractedFile.getFilePath(),
                    extractedFile.getContent()
            ));
        }

        return githubFiles;
    }

    /**
     * Gets breakdown of file types for reporting
     */
    private FileTypeBreakdown getFileTypeBreakdown(List<ExtractedCodeFile> files) {
        FileTypeBreakdown breakdown = new FileTypeBreakdown();

        for (ExtractedCodeFile file : files) {
            switch (file.getFileType()) {
                case JAVA_MAIN:
                    breakdown.javaMainFiles++;
                    break;
                case JAVA_TEST:
                    breakdown.javaTestFiles++;
                    break;
                case HTML_TEMPLATE:
                    breakdown.htmlFiles++;
                    break;
                case SQL_SCHEMA:
                    breakdown.sqlFiles++;
                    break;
                case YAML_CONFIG:
                    breakdown.yamlFiles++;
                    break;
                default:
                    breakdown.otherFiles++;
            }
        }

        return breakdown;
    }

    // All existing extraction methods remain exactly the same
    public List<ExtractedCodeFile> extractCodeFiles(String codeDetails) {
        List<ExtractedCodeFile> extractedFiles = new ArrayList<>();

        System.out.println("Starting code extraction from codeDetails...");

        // Extract Java main source files
        extractedFiles.addAll(extractJavaMainFiles(codeDetails));

        // Extract Java test files
        extractedFiles.addAll(extractJavaTestFiles(codeDetails));

        // Extract HTML templates
        extractedFiles.addAll(extractHtmlTemplates(codeDetails));

        // Extract SQL files
        extractedFiles.addAll(extractSqlFiles(codeDetails));

        // Extract YAML/OpenAPI files
        extractedFiles.addAll(extractYamlFiles(codeDetails));

        System.out.println("Successfully extracted " + extractedFiles.size() + " code files");

        // Debug: Print extracted file paths
        for (ExtractedCodeFile file : extractedFiles) {
            System.out.println("Found: " + file.getFilePath() + " (" + file.getFileType() + ")");
        }

        return extractedFiles;
    }

    private List<ExtractedCodeFile> extractJavaMainFiles(String codeDetails) {
        List<ExtractedCodeFile> files = new ArrayList<>();

        // Pattern for Java main source files (domain, application, infrastructure, presentation layers)
        Pattern javaPattern = Pattern.compile(
                "<pre><code class=\"language-java\">\\s*package\\s+([^;]+);\\s*" +
                        "([\\s\\S]*?)</code></pre>",
                Pattern.DOTALL
        );

        Matcher matcher = javaPattern.matcher(codeDetails);

        while (matcher.find()) {
            String packageName = matcher.group(1).trim();
            String content = matcher.group(2).trim();

            // Extract class name from content
            String className = extractClassName(content);
            if (className != null && !isTestClass(content, className)) {
                String filePath = buildJavaFilePath(packageName, className, false);
                files.add(new ExtractedCodeFile(filePath, formatJavaContent(packageName, content), FileType.JAVA_MAIN));
                System.out.println("Extracted Java main file: " + filePath);
            }
        }

        return files;
    }

    private List<ExtractedCodeFile> extractJavaTestFiles(String codeDetails) {
        List<ExtractedCodeFile> files = new ArrayList<>();

        // Pattern for Java test files - enhanced to capture complete test classes
        Pattern testPattern = Pattern.compile(
                "<pre><code class=\"language-java\">\\s*" +
                        "([\\s\\S]*?)</code></pre>",
                Pattern.DOTALL
        );

        Matcher matcher = testPattern.matcher(codeDetails);

        while (matcher.find()) {
            String content = matcher.group(1).trim();

            // Extract package name from content
            String packageName = extractPackageName(content);
            String className = extractClassName(content);

            // Enhanced test detection - specifically look for DocumentEditingE2ETest and other test indicators
            if (isTestClass(content, className)) {
                if (packageName == null) {
                    // If no package found, use default test package
                    packageName = "tech.justjava.dms.selenium";
                }
                String filePath = buildJavaFilePath(packageName, className, true);
                files.add(new ExtractedCodeFile(filePath, formatJavaContent(packageName, content), FileType.JAVA_TEST));
                System.out.println("Extracted Java test file: " + filePath + " (Class: " + className + ")");
            }
        }

        return files;
    }

    private List<ExtractedCodeFile> extractHtmlTemplates(String codeDetails) {
        List<ExtractedCodeFile> files = new ArrayList<>();

        // Pattern for complete HTML files
        Pattern htmlPattern = Pattern.compile(
                "<code>\\s*&lt;!DOCTYPE html&gt;\\s*" +
                        "([\\s\\S]*?)</code>",
                Pattern.DOTALL
        );

        Matcher matcher = htmlPattern.matcher(codeDetails);

        if (matcher.find()) {
            String content = matcher.group(1).trim();
            content = unescapeHtml(content);
            files.add(new ExtractedCodeFile("src/main/resources/templates/documents.html", content, FileType.HTML_TEMPLATE));
            System.out.println("Extracted HTML template: documents.html");
        }

        // Pattern for HTML fragments
        Pattern fragmentPattern = Pattern.compile(
                "<code>\\s*&lt;!--\\s*([^>]+)\\s*--&gt;\\s*" +
                        "([\\s\\S]*?)</code>",
                Pattern.DOTALL
        );

        Matcher fragmentMatcher = fragmentPattern.matcher(codeDetails);

        while (fragmentMatcher.find()) {
            String fragmentName = fragmentMatcher.group(1).trim();
            String content = fragmentMatcher.group(2).trim();
            content = unescapeHtml(content);

            String filePath = "src/main/resources/templates/" + fragmentName;
            files.add(new ExtractedCodeFile(filePath, content, FileType.HTML_TEMPLATE));
            System.out.println("Extracted HTML fragment: " + fragmentName);
        }

        return files;
    }

    private List<ExtractedCodeFile> extractSqlFiles(String codeDetails) {
        List<ExtractedCodeFile> files = new ArrayList<>();

        // Pattern for SQL files
        Pattern sqlPattern = Pattern.compile(
                "<pre><code class=\"language-sql\">\\s*--[\\s\\S]*?" +
                        "([\\s\\S]*?)</code></pre>",
                Pattern.DOTALL
        );

        Matcher matcher = sqlPattern.matcher(codeDetails);

        while (matcher.find()) {
            String content = matcher.group(1).trim();
            content = unescapeHtml(content);
            files.add(new ExtractedCodeFile("src/main/resources/schema.sql", content, FileType.SQL_SCHEMA));
            System.out.println("Extracted SQL schema file");
        }

        return files;
    }

    private List<ExtractedCodeFile> extractYamlFiles(String codeDetails) {
        List<ExtractedCodeFile> files = new ArrayList<>();

        // Pattern for YAML/OpenAPI files
        Pattern yamlPattern = Pattern.compile(
                "<pre><code class=\"language-yaml\">\\s*" +
                        "([\\s\\S]*?)</code></pre>",
                Pattern.DOTALL
        );

        Matcher matcher = yamlPattern.matcher(codeDetails);

        while (matcher.find()) {
            String content = matcher.group(1).trim();
            content = unescapeHtml(content);
            files.add(new ExtractedCodeFile("src/main/resources/api/openapi.yaml", content, FileType.YAML_CONFIG));
            System.out.println("Extracted OpenAPI YAML file");
        }

        return files;
    }

    private String extractPackageName(String javaContent) {
        Pattern packagePattern = Pattern.compile("package\\s+([^;]+);");
        Matcher matcher = packagePattern.matcher(javaContent);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String extractClassName(String javaContent) {
        Pattern classPattern = Pattern.compile("(?:public\\s+)?(?:class|interface|@?interface|enum)\\s+(\\w+)");
        Matcher matcher = classPattern.matcher(javaContent);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String buildJavaFilePath(String packageName, String className, boolean isTest) {
        String basePath = isTest ? "src/test/java/" : "src/main/java/";
        String packagePath = packageName.replace('.', '/');
        return basePath + packagePath + "/" + className + ".java";
    }

    private boolean isTestClass(String content, String className) {
        // Enhanced test detection with multiple indicators
        boolean hasTestAnnotations = content.contains("@Test") ||
                content.contains("@BeforeAll") ||
                content.contains("@AfterAll") ||
                content.contains("@BeforeEach") ||
                content.contains("@AfterEach");

        boolean hasTestImports = content.contains("import org.junit") ||
                content.contains("import org.testng") ||
                content.contains("import org.openqa.selenium") ||
                content.contains("import org.selenium");

        boolean hasTestClassName = className != null &&
                (className.endsWith("Test") ||
                        className.endsWith("Tests") ||
                        className.endsWith("TestCase") ||
                        className.contains("E2E") ||
                        className.contains("IntegrationTest"));

        boolean hasSeleniumContent = content.contains("WebDriver") ||
                content.contains("ChromeDriver") ||
                content.contains("findElement") ||
                content.contains("By.") ||
                content.contains(".get(") ||
                content.contains(".click()");

        // Specific detection for DocumentEditingE2ETest
        boolean isDocumentEditingTest = "DocumentEditingE2ETest".equals(className);

        // It's a test class if it has any of these strong indicators
        return hasTestAnnotations ||
                hasTestImports ||
                hasSeleniumContent ||
                isDocumentEditingTest ||
                (hasTestClassName && (hasTestAnnotations || hasSeleniumContent));
    }

    private String formatJavaContent(String packageName, String content) {
        // Ensure proper package declaration and clean up HTML entities
        content = unescapeHtml(content);

        // Remove any leading/trailing whitespace and ensure proper structure
        content = content.trim();

        // Check if content already has package declaration
        boolean hasPackage = content.startsWith("package ");

        if (!hasPackage) {
            // Add package declaration if missing
            content = "package " + packageName + ";\n\n" + content;
        } else {
            // Ensure the package matches what we expect
            Pattern existingPackagePattern = Pattern.compile("^package\\s+([^;]+);");
            Matcher matcher = existingPackagePattern.matcher(content);
            if (matcher.find()) {
                String existingPackage = matcher.group(1);
                if (!existingPackage.equals(packageName)) {
                    System.out.println("Warning: Package mismatch. Expected: " + packageName + ", Found: " + existingPackage);
                }
            }
        }

        return content;
    }

    private String unescapeHtml(String content) {
        return content
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ");
    }

    /**
     * @deprecated Use extractAndPushToGitHub instead for GitHub integration
     */
    @Deprecated
    public void writeCodeFiles(String appPath, List<ExtractedCodeFile> extractedFiles) throws IOException {
        System.out.println("⚠️ Using local filesystem fallback (deprecated)...");

        Path rootPath = Paths.get(appPath);

        if (!Files.exists(rootPath)) {
            Files.createDirectories(rootPath);
            System.out.println("Created application root directory: " + rootPath.toAbsolutePath());
        }

        for (ExtractedCodeFile file : extractedFiles) {
            Path fullPath = rootPath.resolve(file.getFilePath());
            Path parentDir = fullPath.getParent();

            System.out.println("Writing code file to: " + fullPath.toAbsolutePath());

            if (!Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                System.out.println("Created parent directory: " + parentDir.toAbsolutePath());
            }

            Files.writeString(fullPath, file.getContent());
            System.out.println("Successfully written: " + fullPath.toAbsolutePath() +
                    " (" + file.getContent().length() + " characters)");
        }
    }

    // Main extraction method that handles the complete codeDetails
    public List<ExtractedCodeFile> extractAllCodeComponents(String codeDetails) {
        return extractCodeFiles(codeDetails);
    }

    // New result class for GitHub push operations
    public static class GitHubPushResult {
        private final String repositoryUrl;
        private final String repositoryName;
        private final int filesPushed;
        private final int filesExtracted;
        private final FileTypeBreakdown fileTypeBreakdown;

        public GitHubPushResult(String repositoryUrl, String repositoryName,
                                int filesPushed, int filesExtracted,
                                FileTypeBreakdown fileTypeBreakdown) {
            this.repositoryUrl = repositoryUrl;
            this.repositoryName = repositoryName;
            this.filesPushed = filesPushed;
            this.filesExtracted = filesExtracted;
            this.fileTypeBreakdown = fileTypeBreakdown;
        }

        public String getRepositoryUrl() { return repositoryUrl; }
        public String getRepositoryName() { return repositoryName; }
        public int getFilesPushed() { return filesPushed; }
        public int getFilesExtracted() { return filesExtracted; }
        public FileTypeBreakdown getFileTypeBreakdown() { return fileTypeBreakdown; }
    }

    // File type breakdown for detailed reporting
    public static class FileTypeBreakdown {
        public int javaMainFiles = 0;
        public int javaTestFiles = 0;
        public int htmlFiles = 0;
        public int sqlFiles = 0;
        public int yamlFiles = 0;
        public int otherFiles = 0;

        @Override
        public String toString() {
            return String.format(
                    "Java Main: %d, Java Test: %d, HTML: %d, SQL: %d, YAML: %d, Other: %d",
                    javaMainFiles, javaTestFiles, htmlFiles, sqlFiles, yamlFiles, otherFiles
            );
        }
    }

    // Custom exceptions
    public static class CodeExtractionException extends Exception {
        public CodeExtractionException(String message) { super(message); }
        public CodeExtractionException(String message, Throwable cause) { super(message, cause); }
    }

    public static class GitHubPushException extends Exception {
        public GitHubPushException(String message) { super(message); }
        public GitHubPushException(String message, Throwable cause) { super(message, cause); }
    }
}