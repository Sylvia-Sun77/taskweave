package taskweave.model;

public class ModelDemo {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        Workflow w = SampleWorkflows.cooking();
        System.out.println(w);

        check("cooking example has 5 tasks", w.size() == 5);
        check("tasks keep input order", w.getTasks().get(3).getName().equals("Rice"));
        check("Stir-fry depends on Chop", w.getTask("Stir-fry").getPrerequisites().contains("Chop"));
        check("indexOf Rice is 3", w.indexOf("Rice") == 3);
        check("indexOf missing task is -1", w.indexOf("Nope") == -1);

        int total = 0;
        for (Task t : w.getTasks()) {
            total += t.getDuration();
        }
        check("total duration is 90", total == 90);

        Task dumplings = new PerUnitTask("Dumplings", 4, 5);
        check("per-unit duration is 4 x 5 = 20", dumplings.getDuration() == 20);

        expectError("duplicate name is rejected", () -> w.addTask(new FixedTask("Wash", 3)));
        expectError("self dependency is rejected", () -> w.addDependency("Wash", "Wash"));
        expectError("unknown prerequisite is rejected", () -> w.addDependency("Wash", "Nope"));
        expectError("name with | is rejected", () -> new FixedTask("a|b", 3));
        expectError("zero minutes is rejected", () -> new FixedTask("Tea", 0));

        // edge cases of the 7-day limit (Task.MAX_MINUTES = 10080)
        check("exactly 7 days is allowed", new FixedTask("Week", Task.MAX_MINUTES).getDuration() == 10080);
        expectError("more than 7 days is rejected", () -> new FixedTask("Long", Task.MAX_MINUTES + 1));
        // 100000 * 100000 overflows int, the check must still catch it
        expectError("per-unit overflow is rejected", () -> new PerUnitTask("Big", 100000, 100000));

        Workflow c = w.copy();
        c.removeTask("Chop");
        check("copy is independent of the original", w.size() == 5 && c.size() == 4);
        check("removing a task clears it from prerequisites", c.getTask("Stir-fry").getPrerequisites().isEmpty());
        check("original still has its dependency", w.getTask("Stir-fry").getPrerequisites().contains("Chop"));

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
    }

    private static void check(String label, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("PASS  " + label);
        } else {
            failed++;
            System.out.println("FAIL  " + label);
        }
    }

    // runs the action and passes only if it throws IllegalArgumentException
    private static void expectError(String label, Runnable action) {
        try {
            action.run();
            check(label, false);
        } catch (IllegalArgumentException e) {
            check(label, true);
        }
    }
}
