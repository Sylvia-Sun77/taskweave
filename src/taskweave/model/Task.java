package taskweave.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Task {

    private final String name;
    private final List<String> prerequisites = new ArrayList<>();

    protected Task(String name) {
        checkName(name);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // read-only view, use addPrerequisite / removePrerequisite to change it
    public List<String> getPrerequisites() {
        return Collections.unmodifiableList(prerequisites);
    }

    public void addPrerequisite(String prerequisite) {
        checkName(prerequisite);
        if (prerequisite.equals(name)) {
            throw new IllegalArgumentException("A task can't depend on itself: " + name);
        }
        if (!prerequisites.contains(prerequisite)) {
            prerequisites.add(prerequisite);
        }
    }

    public boolean removePrerequisite(String prerequisite) {
        return prerequisites.remove(prerequisite);
    }

    public abstract int getDuration();

    public abstract String getType();

    public abstract Task copy();

    // subclasses call this inside copy() so the copy gets the same prerequisites
    protected void copyPrerequisitesTo(Task other) {
        for (String p : prerequisites) {
            other.prerequisites.add(p);
        }
    }

    static void checkName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Task name can't be empty");
        }
        if (name.contains("|") || name.contains(",")) {
            throw new IllegalArgumentException("Task name can't contain | or , : " + name);
        }
    }

    static void checkPositive(int value, String label) {
        if (value <= 0) {
            throw new IllegalArgumentException(label + " must be greater than 0");
        }
    }

    @Override
    public String toString() {
        return name + " (" + getDuration() + " min)";
    }
}
