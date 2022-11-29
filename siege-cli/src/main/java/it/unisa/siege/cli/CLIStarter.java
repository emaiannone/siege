package it.unisa.siege.cli;

import it.unisa.siege.core.RunConfiguration;
import it.unisa.siege.core.SiegeRunner;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CLIStarter {
    public static final String EXPORTS_DIR = "siege_report/";
    // TODO Leave the runner to depend on EvoSuite
    // TODO The runner returns the result and Siege will call SiegeIO
    // TODO Come scelgo il progetto client? Cioè io vorrei o dare la root del progetto (Maven) o una classe specifica di quel progetto, dove comunque devo compilarne tutto per fare il classpath completo
    // TODO Use SIEGE logger
    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) {
        try {
            RunConfiguration runConfiguration = CLIArgumentParser.parse(args);
            SiegeRunner siegeRunner = new SiegeRunner(runConfiguration);
            siegeRunner.run();
        } catch (Exception e) {
            LOGGER.error("\t* {}", ExceptionUtils.getStackTrace(e));
            System.exit(1);
        }
    }
}
