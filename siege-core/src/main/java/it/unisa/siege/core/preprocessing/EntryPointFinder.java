package it.unisa.siege.core.preprocessing;

import org.evosuite.utils.StaticPath;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class EntryPointFinder {

    public List<String> findEntryPoints(List<String> allowedClasses, Set<StaticPath> staticPaths) {
        Set<StaticPath> relevantPaths = new LinkedHashSet<>();
        // Select paths that call at least one class in allowedClasses
        for (StaticPath staticPath : staticPaths) {
            Set<String> calledClasses = new LinkedHashSet<>(allowedClasses);
            calledClasses.retainAll(staticPath.getCalledClasses());
            if (!calledClasses.isEmpty()) {
                relevantPaths.add(staticPath);
            }
        }
        if (relevantPaths.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(sortClasses(allowedClasses, relevantPaths));
    }

    protected abstract Set<String> sortClasses(List<String> classesToSort, Set<StaticPath> relevantPaths);
}
