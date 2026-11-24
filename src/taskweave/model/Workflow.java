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
        if (tasks.remove(name) == null) {
            throw new IllegalArgumentException("No task named " + name);
        }
        // no task should still depend on the removed one
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

    public List<Task> getTasks() {
        return new ArrayList<>(tasks.values());
    }

    public int size() {
        return tasks.size();
    }

    public boolean isEmpty() {
        return tasks.isEmpty();
    }

    public void addDependency(String taskName, String prerequisite) {
        Task task = requireTask(taskName);
        requireTask(prerequisite);
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
