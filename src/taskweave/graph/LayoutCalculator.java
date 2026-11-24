package taskweave.graph;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import taskweave.model.Workflow;

public class LayoutCalculator {

    // column for each task: 0 if it has no prerequisites,
    // otherwise one more than its deepest prerequisite.
    // order must be a valid execution order, so prerequisites come first
    public static Map<String, Integer> computeLevels(Workflow w, List<String> order) {
        Map<String, Integer> levels = new LinkedHashMap<>();
        for (String name : order) {
            int level = 0;
            for (String p : w.getTask(name).getPrerequisites()) {
                level = Math.max(level, levels.get(p) + 1);
            }
            levels.put(name, level);
        }
        return levels;
    }
}
