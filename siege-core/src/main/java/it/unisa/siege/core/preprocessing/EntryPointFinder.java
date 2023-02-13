package it.unisa.siege.core.preprocessing;

import org.evosuite.utils.StaticPath;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class EntryPointFinder {
    public abstract List<String> findEntryPoints(List<String> allowedClasses, Set<StaticPath> staticPaths);

    protected Set<StaticPath> selectPathsInvokingClasses(Set<StaticPath> staticPaths, List<String> classes) {
        Set<StaticPath> relevantPaths = new LinkedHashSet<>();
        for (StaticPath staticPath : staticPaths) {
            Set<String> calledClasses = new LinkedHashSet<>(classes);
            calledClasses.retainAll(staticPath.getCalledClasses());
            if (!calledClasses.isEmpty()) {
                relevantPaths.add(staticPath);
            }
        }
        return relevantPaths;
    }
}
