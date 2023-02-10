package it.unisa.siege.core;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.classpath.ResourceList;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProjectBuilder {
    public static List<Path> findClasspathFiles(Path startDirectory, String classpathFileName) {
        try (Stream<Path> stream = Files.walk(startDirectory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().equals(classpathFileName))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static List<String> readClasspathFiles(List<Path> classpathFiles) {
        Set<String> classpath = new LinkedHashSet<>();
        for (Path cpFile : classpathFiles) {
            try {
                for (String line : Files.readAllLines(cpFile)) {
                    List<String> pathElements = Arrays.stream(line.split(":"))
                            .filter(pe -> Paths.get(pe).toFile().exists())
                            .filter(pe -> FilenameUtils.getExtension(pe).equals("jar"))
                            .collect(Collectors.toList());
                    classpath.addAll(pathElements);
                }
            } catch (IOException ignored) {
            }
        }
        return new ArrayList<>(classpath);
    }

    public static List<String> findClasses(List<Path> directories, String classpath) {
        Set<String> classNames = new LinkedHashSet<>();
        for (Path dir : directories) {
            classNames.addAll(findClasses(dir, classpath));
        }
        return new ArrayList<>(classNames);
    }

    // https://maven.apache.org/plugins/maven-help-plugin/evaluate-mojo.html
    public static Path getMavenOutputDirectory(Path directory) {
        File tmpfile = null;
        try {
            tmpfile = File.createTempFile("tmp", ".txt");
            invokeMaven(Arrays.asList("help:evaluate", "-Dexpression=project.build.outputDirectory", "-q", "-B", "-Doutput=" + tmpfile.getAbsolutePath()), directory);
            return Paths.get(IOUtils.toString(Files.newInputStream(tmpfile.toPath()), StandardCharsets.UTF_8));
        } catch (MavenInvocationException | IOException e) {
            return null;
        } finally {
            if (tmpfile != null) {
                tmpfile.delete();
            }
        }
    }

    private static List<String> findClasses(Path dir, String classpath) {
        String oldPropertiesCP = Properties.CP;
        Properties.CP = classpath;
        ResourceList resourceList = ResourceList.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT());
        List<String> classNames = new ArrayList<>(resourceList.getAllClasses(dir.toString(), false));
        Properties.CP = oldPropertiesCP;
        return classNames;
    }

    private static void invokeMaven(List<String> goals, Path directory) throws IOException, MavenInvocationException {
        if (System.getProperty("maven.home") == null) {
            // Try to find mvn location
            ProcessBuilder whichMvn = new ProcessBuilder("which", "mvn");
            Process proc = whichMvn.start();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String mavenHome = stdInput.readLine();
            if (mavenHome == null) {
                throw new IOException("Could not find Maven. Must supply the directory where Maven can be found via -Dmaven.home JVM property");
            } else {
                System.setProperty("maven.home", mavenHome);
            }
        }
        InvocationRequest request = new DefaultInvocationRequest();
        request.setBaseDirectory(directory.toFile());
        request.setGoals(goals);
        request.setBatchMode(true);
        new DefaultInvoker().execute(request);
    }

    /*
    private static List<String> getAllMavenOutputDirectory(Path startDirectory) throws IOException, MavenInvocationException {
        List<Path> mavenDirectories = getMavenDirectories(startDirectory);
        List<String> allMavenOutputDirectory = new ArrayList<>();
        for (Path mavenDirectory : ProgressBar.wrap(mavenDirectories, new ProgressBarBuilder()
                .setTaskName("Finding Maven Output Directories: ")
                .setStyle(ProgressBarStyle.ASCII)
                .setMaxRenderedLength(150)
                .setConsumer(new ConsoleProgressBarConsumer(System.out, 141))
        )) {
            String outDir = getMavenOutputDirectory(mavenDirectory);
            if (outDir != null && Paths.get(outDir).toFile().exists()) {
                allMavenOutputDirectory.add(outDir);
            }
        }
        return allMavenOutputDirectory;
    }

    public static List<Path> getMavenDirectories(Path startDirectory) throws IOException {
        try (Stream<Path> walkStream = Files.walk(startDirectory)) {
            return walkStream
                    .filter(p -> p.toFile().isDirectory())
                    .filter(p -> Objects.requireNonNull(p.toFile().listFiles()).length > 0)
                    .filter(p -> Objects.requireNonNull(p.toFile().listFiles((dir, name) -> name.equals("pom.xml"))).length > 0)
                    .collect(Collectors.toList());
        }
    }
     */
}
