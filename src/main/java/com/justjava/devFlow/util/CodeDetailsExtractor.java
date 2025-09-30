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

    public void writeCodeFiles(String appPath, List<ExtractedCodeFile> extractedFiles) throws IOException {
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
}