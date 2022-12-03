# SIEGE

SIEGE is an automated test case generator targeting any class method in the classpath based
on [EvoSuite](https://github.com/EvoSuite/evosuite).

# Requirements

- JDK 9 (higher version might not start the test generation)
- Maven

# Building SIEGE

Create the executable fat jar (if you want to run all the tests, remove the option `-DskipTests=true`):  
`mvn clean package -DskipTests=true`

# Running SIEGE

Run the executable fat JAR:  
`java -Dmaven.home=<MVN_DIR> -jar target/siege-cli<VERSION>-jar-with-dependencies.json <CLI_OPTIONS>`