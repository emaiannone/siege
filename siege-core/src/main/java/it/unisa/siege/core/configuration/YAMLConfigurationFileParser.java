package it.unisa.siege.core.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
        try {
            List<ProjectConfiguration> projectConfigs = new ArrayList<>();
            YAMLConfiguration yamlConfig = mapper.readValue(configFile, YAMLConfiguration.class);
            List<YAMLProjectConfiguration> projects = yamlConfig.projects;
            for (YAMLProjectConfiguration yamlProjectConfig : projects) {
                // If values are not found in the YAML file, they are defaulted from BaseConfiguration
                // TODO Should log something when values are not in YAML file, but not too verbose
                String projectDir = yamlProjectConfig.path != null ? yamlProjectConfig.path : baseConfig.getProjectDir();
                String vulnerabilitiesFile = yamlProjectConfig.vulnerabilities != null ? yamlProjectConfig.vulnerabilities : baseConfig.getVulnerabilitiesFile();
                int chromosomeLength = yamlProjectConfig.chromosomeLength != null ? yamlProjectConfig.chromosomeLength : baseConfig.getChromosomeLength();
                boolean branchAwareness = yamlProjectConfig.branchAwareness != null ? yamlProjectConfig.branchAwareness : baseConfig.isBranchAwareness();
                int maxStringLength = yamlProjectConfig.maxStringLength != null ? yamlProjectConfig.maxStringLength : baseConfig.getMaxStringLength();
                double probabilityAddCallsBeforeEntryMethod = yamlProjectConfig.pAddCallsBeforeEntryMethod != null ? yamlProjectConfig.pAddCallsBeforeEntryMethod : baseConfig.getProbabilityAddCallsBeforeEntryMethod();
                double probabilityPrimitiveReuse = yamlProjectConfig.pPrimitiveReuse != null ? yamlProjectConfig.pPrimitiveReuse : baseConfig.getProbabilityPrimitiveReuse();
                double probabilityPrimitivePool = yamlProjectConfig.pPrimitivePool != null ? yamlProjectConfig.pPrimitivePool : baseConfig.getProbabilityPrimitivePool();
                double probabilityObjectReuse = yamlProjectConfig.pObjectReuse != null ? yamlProjectConfig.pObjectReuse : baseConfig.getProbabilityObjectReuse();
                double probabilityDynamicPool = yamlProjectConfig.pDynamicPool != null ? yamlProjectConfig.pDynamicPool : baseConfig.getProbabilityDynamicPool();
                double probabilityChangeParameter = yamlProjectConfig.pChangeParameter != null ? yamlProjectConfig.pChangeParameter : baseConfig.getProbabilityChangeParameter();
                boolean seedFromMethodsInGoals = yamlProjectConfig.seedFromMethodsInGoals != null ? yamlProjectConfig.seedFromMethodsInGoals : baseConfig.isSeedFromMethodsInGoals();
                boolean seedFromBranchesInGoals = yamlProjectConfig.seedFromBranchesInGoals != null ? yamlProjectConfig.seedFromBranchesInGoals : baseConfig.isSeedFromBranchesInGoals();
                String metaheuristic = yamlProjectConfig.metaheuristic != null ? yamlProjectConfig.metaheuristic : baseConfig.getMetaheuristic();
                String initialPopulationAlgorithm = yamlProjectConfig.initialPopulationAlgorithm != null ? yamlProjectConfig.initialPopulationAlgorithm : baseConfig.getInitialPopulationAlgorithm();
                String crossover = yamlProjectConfig.crossover != null ? yamlProjectConfig.crossover : baseConfig.getCrossover();
                boolean entryMethodMutation = yamlProjectConfig.entryMethodMutation != null ? yamlProjectConfig.entryMethodMutation : baseConfig.isEntryMethodMutation();
                boolean exceptionPointSampling = yamlProjectConfig.exceptionPointSampling != null ? yamlProjectConfig.exceptionPointSampling : baseConfig.isExceptionPointSampling();
                int searchBudget = yamlProjectConfig.searchBudget != null ? yamlProjectConfig.searchBudget : baseConfig.getSearchBudget();
                int populationSize = yamlProjectConfig.populationSize != null ? yamlProjectConfig.populationSize : baseConfig.getPopulationSize();
                try {
                    ProjectConfiguration projectConfig = new ProjectConfigurationBuilder()
                            .setProjectDir(projectDir)
                            .setVulnerabilitiesFile(vulnerabilitiesFile)
                            .setChromosomeLength(chromosomeLength)
                            .setBranchAwareness(branchAwareness)
                            .setMaxStringLength(maxStringLength)
                            .setProbabilityAddCallsBeforeEntryMethod(probabilityAddCallsBeforeEntryMethod)
                            .setProbabilityPrimitiveReuse(probabilityPrimitiveReuse)
                            .setProbabilityPrimitivePool(probabilityPrimitivePool)
                            .setProbabilityObjectReuse(probabilityObjectReuse)
                            .setProbabilityDynamicPool(probabilityDynamicPool)
                            .setProbabilityChangeParameter(probabilityChangeParameter)
                            .setSeedFromMethodsInGoals(seedFromMethodsInGoals)
                            .setSeedFromBranchesInGoals(seedFromBranchesInGoals)
                            .setMetaheuristic(metaheuristic)
                            .setInitialPopulationAlgorithm(initialPopulationAlgorithm)
                            .setCrossover(crossover)
                            .setEntryMethodMutation(entryMethodMutation)
                            .setExceptionPointSampling(exceptionPointSampling)
                            .setSearchBudget(searchBudget)
                            .setPopulationSize(populationSize)
                            .build();
                    projectConfigs.add(projectConfig);
                } catch (IllegalStateException e) {
                    // If we supply an invalid value, we log and skip the project
                    LOGGER.info("Project {} has an invalid parameter in the configuration file. Skipping this project.", projectDir);
                    LOGGER.warn("Details: {}", e.getMessage());
                    LOGGER.error("{}", ExceptionUtils.getStackTrace(e));
                }
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
        public Boolean branchAwareness;
        public Integer maxStringLength;
        public Double pAddCallsBeforeEntryMethod;
        public Double pPrimitiveReuse;
        public Double pPrimitivePool;
        public Double pObjectReuse;
        public Double pDynamicPool;
        public Double pChangeParameter;
        public Boolean seedFromMethodsInGoals;
        public Boolean seedFromBranchesInGoals;
        public String metaheuristic;
        public String initialPopulationAlgorithm;
        public String crossover;
        public Boolean entryMethodMutation;
        public Boolean exceptionPointSampling;
        public Integer searchBudget;
        public Integer populationSize;

        public YAMLProjectConfiguration() {
        }
    }
}
