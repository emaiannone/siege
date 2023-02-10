package it.unisa.siege.core;

import org.evosuite.utils.StaticPath;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RootProximityEntryPointFinder extends EntryPointFinder {
    @Override
    public List<String> findEntryPoints(List<String> allowedClasses, Set<StaticPath> staticPaths) {
        Set<StaticPath> relevantPaths = selectPathsInvokingClasses(staticPaths, allowedClasses);
        if (relevantPaths.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> entryPoints = new LinkedHashSet<>();
        int maxLength = relevantPaths.stream()
                .mapToInt(p -> p.getCalledClasses().size())
                .max().getAsInt();
        // Sort classes by their proximity to the root of the paths
        for (int i = 0; i < maxLength; i++) {
            for (StaticPath staticPath : relevantPaths) {
                if (i < staticPath.length()) {
                    // Add only if this class is in the list of allowed classes
                    String classToAdd = staticPath.get(i).getClassName();
                    if (allowedClasses.contains(classToAdd)) {
                        entryPoints.add(classToAdd);
                    }
                }
            }
        }
        return new ArrayList<>(entryPoints);
    }
}
