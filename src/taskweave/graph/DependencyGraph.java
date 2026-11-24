package taskweave.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import taskweave.model.Task;
import taskweave.model.Workflow;

public class DependencyGraph {

    // task name -> tasks that are waiting for it
    private final Map<String, List<String>> dependents = new LinkedHashMap<>();
    // task name -> how many prerequisites it has
    private final Map<String, Integer> inDegree = new HashMap<>();

    public DependencyGraph(Workflow w) {
        for (Task t : w.getTasks()) {
            dependents.put(t.getName(), new ArrayList<>());
            inDegree.put(t.getName(), 0);
        }
        for (Task t : w.getTasks()) {
            for (String p : t.getPrerequisites()) {
                if (dependents.containsKey(p)) {
                    dependents.get(p).add(t.getName());
                    inDegree.put(t.getName(), inDegree.get(t.getName()) + 1);
                }
            }
        }
    }

    public List<String> getDependents(String name) {
        List<String> d = dependents.get(name);
        if (d == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(d);
    }

    public int getInDegree(String name) {
        return inDegree.getOrDefault(name, 0);
    }
}
