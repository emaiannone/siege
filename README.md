# SIEGE

SIEGE is an automated test case generator targeting any class method in the classpath based
on [EvoSuite](https://github.com/EvoSuite/evosuite).

# Requirements

- JDK version 8+
- Maven

# Building SIEGE

Create the executable fat jar (if you want to run all the tests, remove the option `-DskipTests=true`):  
`mvn clean package -DskipTests=true`

# Running SIEGE

Run the executable fat JAR:  
`java -jar target/siege-cli<VERSION>-jar-with-dependencies.json <ARGS>`