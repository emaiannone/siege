package it.unisa.siege.core;

public class ReachabilityTarget {
    private final String targetClass;
    private final String targetMethod;

    public ReachabilityTarget(String targetClass, String targetMethod) {
        if (targetClass == null || targetMethod == null) {
            throw new IllegalArgumentException("Class or method cannot be null.");
        }
        this.targetClass = targetClass;
        this.targetMethod = targetMethod;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public String getTargetMethod() {
        return targetMethod;
    }

    @Override
    public String toString() {
        return targetClass + "." + targetMethod;
    }
}
