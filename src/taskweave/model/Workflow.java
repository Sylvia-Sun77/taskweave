package taskweave.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Workflow {

    // LinkedHashMap remembers the order tasks were added
    private final Map<String, Task> tasks = new LinkedHashMap<>();

    public void addTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task can't be null");
        }
        if (tasks.containsKey(task.getName())) {
            throw new IllegalArgumentException("A task named " + task.getName() + " already exists");
        }
        tasks.put(task.getName(), task);
    }

    public void removeTask(String name) {
        // remove() returns null when the key is not in the map, so it checks and removes in one step
        if (tasks.remove(name) == null) {
            throw new IllegalArgumentException("No task named " + name);
        }
        // no task should still depend on the removed one
        // (otherwise DependencyChecker would report "depends on X, which doesn't exist")
        for (Task t : tasks.values()) {
            t.removePrerequisite(name);
        }
    }

    public Task getTask(String name) {
        return tasks.get(name);
    }

    public boolean contains(String name) {
        return tasks.containsKey(name);
    }

    // return a new list, so code outside can't add or remove tasks without going through addTask()
    public List<Task> getTasks() {
        return new ArrayList<>(tasks.values());
    }

    public int size() {
        return tasks.size();
    }

    public boolean isEmpty() {
        return tasks.isEmpty();
    }

    // taskName waits for prerequisite, e.g. addDependency("Chop", "Wash"): Chop starts after Wash
    public void addDependency(String taskName, String prerequisite) {
        Task task = requireTask(taskName);
        requireTask(prerequisite);  // only to check the prerequisite exists, the result is not needed
        task.addPrerequisite(prerequisite);
    }

    public void removeDependency(String taskName, String prerequisite) {
        requireTask(taskName).removePrerequisite(prerequisite);
    }

    // position in input order, -1 if there is no such task
    public int indexOf(String name) {
        int i = 0;
        for (String key : tasks.keySet()) {
            if (key.equals(name)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    // deep copy, every task is copied too
    // (a shallow copy would share the same Task objects, so changing the copy would also change
    // the original, and UndoManager's snapshots would change together with the current workflow)
    public Workflow copy() {
        Workflow c = new Workflow();
        for (Task t : tasks.values()) {
            c.addTask(t.copy());
        }
        return c;
    }

    private Task requireTask(String name) {
        Task t = tasks.get(name);
        if (t == null) {
            throw new IllegalArgumentException("No task named " + name);
        }
        return t;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Task t : tasks.values()) {
            sb.append(t).append(" <- ").append(t.getPrerequisites()).append("\n");
        }
        return sb.toString();
    }
}
