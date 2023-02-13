package it.unisa.siege.core.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import it.unisa.siege.core.BaseConfiguration;
import it.unisa.siege.core.SiegeIOHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class YAMLConfigurationFileParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(YAMLConfigurationFileParser.class);

    public static List<ProjectConfiguration> parseConfigFile(BaseConfiguration baseConfig) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        File configFile = Paths.get(baseConfig.getConfigurationFile()).toFile();
        if (!SiegeIOHelper.isYamlFile(configFile)) {
            throw new IllegalStateException("The target configuration file does not exist or is not a YAML file.");
        }
        try {
            List<ProjectConfiguration> projectConfigs = new ArrayList<>();
            YAMLConfiguration yamlConfigs = mapper.readValue(configFile, YAMLConfiguration.class);
            for (YAMLProjectConfiguration yamlProjectConfig : yamlConfigs.projects) {
                ProjectConfigurationBuilder projectConfigBuilder = new ProjectConfigurationBuilder();
                String projectDir;
                if (yamlProjectConfig.path == null) {
                    LOGGER.info("An entry has no specific project to analyze. Using default.");
                    projectDir = baseConfig.getProject();
                } else {
                    projectDir = yamlProjectConfig.path;
                }
                String vulnerabilitiesFile;
                if (yamlProjectConfig.vulnerabilities == null) {
                    LOGGER.info("An entry has no specific vulnerabilities to analyze. Using default from CLI.");
                    vulnerabilitiesFile = baseConfig.getVulnerabilitiesFileName();
                } else {
                    vulnerabilitiesFile = yamlProjectConfig.vulnerabilities;
                }
                // TODO Supply all the other YAML data here
                ProjectConfiguration projectConfig = projectConfigBuilder
                        .setProjectDir(projectDir)
                        .setVulnerabilitiesFile(vulnerabilitiesFile)
                        .setSearchBudget(yamlProjectConfig.searchBudget)
                        .setPopulationSize(yamlProjectConfig.populationSize)
                        .build();
                projectConfigs.add(projectConfig);
            }
            return projectConfigs;
        } catch (IOException e) {
            throw new RuntimeException("Could not read file " + configFile + " likely because it has an invalid structure", e);
        }
    }

    private static class YAMLConfiguration {
        public List<YAMLProjectConfiguration> projects;

        public YAMLConfiguration() {
        }
    }

    private static class YAMLProjectConfiguration {
        public String path;
        public String vulnerabilities;
        public Integer chromosomeLength;
        public Boolean reachabilityBranchAwareness;
        public Integer maxStringLength;
        public Double probabilityAddCallsBeforeEntryMethod;
        public Double probabilityPrimitiveReuse;
        public Double probabilityPrimitivePool;
        public Double probabilityObjectReuse;
        public Double probabilityDynamicPool;
        public Double probabilityChangeParameter;
        public Boolean reachabilitySeedFromMethodsInGoals;
        public Boolean reachabilitySeedFromBranchesInGoals;
        public String gaType;
        public String initialPopulationGenerationAlgorithm;
        public String crossoverAlgorithm;
        public Boolean reachabilityEntryMethodMutation;
        public Boolean exceptionPointSampling;
        public Integer searchBudget;
        public Integer populationSize;

        public YAMLProjectConfiguration() {
        }
    }
}
