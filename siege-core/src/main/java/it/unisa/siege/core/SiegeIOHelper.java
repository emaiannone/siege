package it.unisa.siege.core;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.evosuite.Properties;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SiegeIOHelper {
    private static final String YML = "yml";
    private static final String YAML = "yaml";

    public static void writeToCsv(Path outFilePath, List<Map<String, String>> content) throws IOException {
        writeToCsv(outFilePath.getParent().toString(), outFilePath.getFileName().toString(), content);
    }

    // TODO Get rid of this method, rely on the one having Path as parameter only
    public static void writeToCsv(String dir, String filename, List<Map<String, String>> content) throws IOException {
        if (content.isEmpty()) {
            return;
        }
        File exportDir = new File(dir);
        if (!exportDir.exists()) {
            exportDir.mkdir();
        }
        String fullFilename = Paths.get(dir, filename).toString();
        File exportFile = new File(fullFilename);
        try (PrintWriter csvWriter = new PrintWriter(new FileOutputStream(exportFile, false))) {
            // Put the headers first
            Set<String> keys = content.get(0).keySet();
            csvWriter.println(String.join(",", keys));
            for (Map<String, String> line : content) {
                String csvLine = String.join(",", line.values());
                csvWriter.println(csvLine);
            }
        } catch (FileNotFoundException e) {
            throw new IOException("Could not write to " + fullFilename + ".");
        }
    }

    public static void deleteEmptyTestFiles(Path testsDirPath) throws IOException {
        if (!testsDirPath.toFile().exists()) {
            return;
        }
        List<Path> outputFiles;
        try (Stream<Path> stream = Files.walk(testsDirPath)) {
            outputFiles = stream.filter(Files::isRegularFile)
                    .filter(f -> FilenameUtils.getExtension(String.valueOf(f)).equals("java")).collect(Collectors.toList());
        }
        List<Path> emptyTestFiles = outputFiles.stream()
                .filter(f -> !f.getFileName().toString().contains(Properties.SCAFFOLDING_SUFFIX))
                .filter(SiegeIOHelper::isTestFileEmpty)
                .collect(Collectors.toList());
        List<Path> filesToDelete = new ArrayList<>();
        for (Path emptyTestFilePath : emptyTestFiles) {
            filesToDelete.add(emptyTestFilePath);
            String testFileName = emptyTestFilePath.toString();
            String testFileBaseName = testFileName.substring(0, testFileName.lastIndexOf("."));
            String scaffoldingFileName = testFileBaseName + "_" + Properties.SCAFFOLDING_SUFFIX + ".java";
            Path scaffoldingFilePath = Paths.get(scaffoldingFileName);
            if (outputFiles.contains(scaffoldingFilePath)) {
                filesToDelete.add(scaffoldingFilePath);
            }
        }
        for (Path path : filesToDelete) {
            path.toFile().delete();
        }
    }

    public static boolean isTestFileEmpty(Path testFilePath) {
        try {
            String content = FileUtils.readFileToString(testFilePath.toFile(), Charset.defaultCharset());
            return content.contains("public void notGeneratedAnyTest()");
        } catch (IOException e) {
            return true;
        }
    }

    public static boolean isYamlFile(File file) {
        String ext = FilenameUtils.getExtension(file.toPath().toString());
        return file.isFile() && ext.equalsIgnoreCase(YAML) || ext.equalsIgnoreCase(YML);
    }
}
