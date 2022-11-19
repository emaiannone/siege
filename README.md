# siege

SIEGE is an automated test case generator targeting any method in the classpath

# Requirements

JDK 1.8 - As EvoSuite relies on some libraries in JDK 8 version only, e.g., `tools.jar`

/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home

1. If you are building from the terminal, ensure to set the default JDK to version 8:
   `export JAVA_HOME=<PATH-TO-JDK-8>`
2. Run the compilation (if you want to run all the tests, remove the option `-DskipTests=true`):  
   `mvn clean package -DskipTests=true`