package it.unisa.siege.cli;

import it.unisa.siege.core.RunConfiguration;
import it.unisa.siege.core.SiegeRunner;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CLIStarter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CLIStarter.class);

    public static void main(String[] args) {
        try {
            RunConfiguration runConfiguration = CLIArgumentParser.parse(args);
            SiegeRunner siegeRunner = new SiegeRunner(runConfiguration);
            siegeRunner.run();
        } catch (Exception e) {
            LOGGER.error(ExceptionUtils.getStackTrace(e));
            System.exit(1);
        }
    }
}
