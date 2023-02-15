package it.unisa.siege.core.preprocessing;

import org.evosuite.utils.StaticPath;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RootProximityEntryPointFinder extends EntryPointFinder {
    @Override
    public Set<String> sortClasses(List<String> classesToSort, Set<StaticPath> relevantPaths) {
        Set<String> entryPoints = new LinkedHashSet<>();
        int maxLength = relevantPaths.stream()
                .mapToInt(p -> p.getCalledClasses().size())
                .max().orElse(0);
        // Sort classes (in classesToSort) by their proximity to the root in the relevantPaths
        for (int i = 0; i < maxLength; i++) {
            for (StaticPath staticPath : relevantPaths) {
                if (i < staticPath.length()) {
                    // Add only if this class is in the list of allowed classes
                    String classToAdd = staticPath.get(i).getClassName();
                    if (classesToSort.contains(classToAdd)) {
                        entryPoints.add(classToAdd);
                    }
                }
            }
        }
        return entryPoints;
    }
}
