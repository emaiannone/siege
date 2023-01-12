package it.unisa.siege.core;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.evosuite.coverage.reachability.ReachabilityTarget;

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
    public static List<Pair<String, ReachabilityTarget>> readAndParseCsv(Path csvFilePath) throws IOException {
        List<Pair<String, ReachabilityTarget>> vulnerabilityList = new ArrayList<>();
        try (CSVReader reader = new CSVReaderBuilder(new FileReader(csvFilePath.toFile())).withSkipLines(1).build()) {
            String[] values;
            while ((values = reader.readNext()) != null) {
                vulnerabilityList.add(new ImmutablePair<>(values[0], new ReachabilityTarget(values[2], values[3])));
            }
        } catch (CsvValidationException e) {
            throw new IOException(e);
        }
        return vulnerabilityList;
    }

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
                .filter(f -> !f.getFileName().toString().contains("scaffolding"))
                .filter(SiegeIOHelper::isTestFileEmpty)
                .collect(Collectors.toList());
        List<Path> filesToDelete = new ArrayList<>();
        for (Path emptyTestFilePath : emptyTestFiles) {
            filesToDelete.add(emptyTestFilePath);
            String testFileName = emptyTestFilePath.toString();
            String testFileBaseName = testFileName.substring(0, testFileName.lastIndexOf("."));
            String scaffoldingFileName = testFileBaseName + "_scaffolding.java";
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
}
