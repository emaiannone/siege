# SIEGE

SIEGE is an automated test case generator targeting any class method in the classpath based
on [EvoSuite](https://github.com/EvoSuite/evosuite).

# Requirements

JDK 1.8 - As EvoSuite relies on some libraries in JDK 8 version only, e.g., `tools.jar`

# Building SIEGE

1. If you are building SIEGE from the terminal, ensure to set the default JDK to version 8:  
   `export JAVA_HOME=<PATH-TO-JDK-8>` (where `<PATH-TO-JDK-8>` is the directory of your JDK 8)
2. Run the compilation (if you want to run all the tests, remove the option `-DskipTests=true`):  
   `mvn clean package -DskipTests=true`

# Running SIEGE

1. If you are running SIEGE from the terminal, ensure to set the default JDK to version 8:
   `export JAVA_HOME=<PATH-TO-JDK-8>`
2. Run the executable fat JAR:  
   `java -jar siege-cli<VERSION>-jar-with-dependencies.json <ARGS>`