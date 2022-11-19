package it.unisa.siege;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.evosuite.coverage.vulnerability.VulnerabilityDescription;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SiegeIO {
    public static List<Pair<String, VulnerabilityDescription>> readAndParseCsv(String file) throws CsvValidationException, FileNotFoundException {
        List<Pair<String, VulnerabilityDescription>> vulnerabilityList = new ArrayList<>();
        try (CSVReader reader = new CSVReaderBuilder(new FileReader(file)).withSkipLines(1).build()) {
            String[] values;
            while ((values = reader.readNext()) != null) {
                vulnerabilityList.add(new ImmutablePair<>(values[0], new VulnerabilityDescription(values[2], values[3])));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return vulnerabilityList;
    }

    public static void writeToCsv(String dir, String filename, List<Map<String, String>> content) throws IOException {
        if (content.isEmpty()) {
            return;
        }
        File exportDir = new File(dir);
        if (!exportDir.exists()) {
            exportDir.mkdir();
        }
        String fullFilename = Paths.get(dir, filename + ".csv").toString();
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
}
