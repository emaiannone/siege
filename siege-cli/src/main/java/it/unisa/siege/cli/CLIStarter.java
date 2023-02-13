package it.unisa.siege.cli;

import it.unisa.siege.core.BaseConfiguration;
import it.unisa.siege.core.SiegeLauncher;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CLIStarter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CLIStarter.class);

    public static void main(String[] args) {
        try {
            BaseConfiguration baseConfig = CLIArgumentReader.read(args);
            if (baseConfig == null) {
                System.exit(0);
            }
            SiegeLauncher siegeLauncher = new SiegeLauncher(baseConfig);
            siegeLauncher.launch();
            System.exit(0);
        } catch (Exception e) {
            LOGGER.error(ExceptionUtils.getStackTrace(e));
            System.exit(1);
        }
    }
}
