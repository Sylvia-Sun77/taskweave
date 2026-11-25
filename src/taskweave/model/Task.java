package taskweave.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// abstract: there is no "plain" task, every task is a FixedTask or a PerUnitTask
// (they only differ in how the duration is worked out, so getDuration() is abstract)
public abstract class Task {

    // the longest one task can take: 10080 min = 7 days
    // without a limit, e.g. 100000 units x 100000 min overflows int (> 2,147,483,647),
    // and a huge but valid number makes Simulator loop billions of minutes, so the window freezes
    public static final int MAX_MINUTES = 10080;

    // final: a task's name never changes after it is created (other tasks use the name to point to it)
    private final String name;
    // names of the tasks that must finish before this one can start
    // (store names, not Task objects, so a copied workflow doesn't point back to the old tasks)
    private final List<String> prerequisites = new ArrayList<>();

    // protected: only subclasses call this constructor (an abstract class can't be created with new)
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
        // validation: A waits for A can never start, so reject it here
        // (a longer loop like A -> B -> A is found later by DependencyChecker)
        if (prerequisite.equals(name)) {
            throw new IllegalArgumentException("A task can't depend on itself: " + name);
        }
        // skip duplicates, so linking the same two tasks twice doesn't add the same name twice
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
        // validation: trim() so a name of only spaces ("   ") is also treated as empty
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Task name can't be empty");
        }
        // validation: | and , are separators in the save file (see WorkflowFileIO),
        // a name with them would be split into wrong fields when the file is loaded again
        if (name.contains("|") || name.contains(",")) {
            throw new IllegalArgumentException("Task name can't contain | or , : " + name);
        }
    }

    static void checkPositive(int value, String label) {
        // validation: 0 or negative minutes/units make no sense for a task
        if (value <= 0) {
            throw new IllegalArgumentException(label + " must be greater than 0");
        }
    }

    // long: so units * minutesPerUnit can be checked before it overflows int
    static void checkMaxDuration(long minutes) {
        // validation: one task can't be longer than MAX_MINUTES (reason is at MAX_MINUTES above)
        if (minutes > MAX_MINUTES) {
            throw new IllegalArgumentException("A task can't be longer than " + MAX_MINUTES + " minutes (7 days)");
        }
    }

    @Override
    public String toString() {
        return name + " (" + getDuration() + " min)";
    }
}
