package it.unisa.siege.core.preprocessing;

import org.evosuite.setup.callgraph.CallGraphEntry;
import org.evosuite.utils.StaticPath;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TargetDistanceEntryPointFinder extends EntryPointFinder {
    @Override
    protected Set<String> sortClasses(List<String> classesToSort, Set<StaticPath> relevantPaths) {
        Set<String> entryPoints = new LinkedHashSet<>();
        List<StaticPath> sortedPaths = relevantPaths.stream()
                .sorted(Comparator.comparing(StaticPath::length)
                        .thenComparing(sp -> sp.getCalledClasses().size()))
                .collect(Collectors.toList());
        // Sort classes (in classesToSort) based on the shortest static paths. If equals, the number of called classes is checked
        for (StaticPath sortedPath : sortedPaths) {
            for (CallGraphEntry callGraphEntry : sortedPath) {
                // Add only if this class is in the list of allowed classes
                String classToAdd = callGraphEntry.getClassName();
                if (classesToSort.contains(classToAdd)) {
                    entryPoints.add(classToAdd);
                }
            }
        }
        return entryPoints;
    }
}
