package taskweave.graph;

import java.util.List;
import java.util.Map;

import taskweave.model.FixedTask;
import taskweave.model.SampleWorkflows;
import taskweave.model.Workflow;

public class GraphDemo {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        // cooking example: should be valid
        Workflow w = SampleWorkflows.cooking();
        ValidationResult r = DependencyChecker.check(w);
        System.out.println(r);
        check("cooking example is valid", r.isValid());
        check("order is Wash, Rice, Soup, Chop, Stir-fry",
                r.getOrder().equals(List.of("Wash", "Rice", "Soup", "Chop", "Stir-fry")));

        DependencyGraph g = new DependencyGraph(w);
        check("Chop is waiting for Wash", g.getDependents("Wash").equals(List.of("Chop")));
        check("Stir-fry has 1 prerequisite", g.getInDegree("Stir-fry") == 1);

        Map<String, Integer> levels = LayoutCalculator.computeLevels(w, r.getOrder());
        System.out.println("levels: " + levels);
        check("Wash, Rice, Soup are in column 0",
                levels.get("Wash") == 0 && levels.get("Rice") == 0 && levels.get("Soup") == 0);
        check("Chop is in column 1", levels.get("Chop") == 1);
        check("Stir-fry is in column 2", levels.get("Stir-fry") == 2);

        // add a wrong dependency: Wash now waits for Stir-fry
        w.addDependency("Wash", "Stir-fry");
        r = DependencyChecker.check(w);
        System.out.println(r);
        check("wrong dependency makes it invalid", !r.isValid() && r.hasCycle());
        check("cycle is Wash, Chop, Stir-fry", r.getCycle().equals(List.of("Wash", "Chop", "Stir-fry")));
        check("invalid result has no order", r.getOrder().isEmpty());

        // a cycle that doesn't include the first task
        Workflow w2 = new Workflow();
        w2.addTask(new FixedTask("X", 1));
        w2.addTask(new FixedTask("Y", 1));
        w2.addTask(new FixedTask("Z", 1));
        w2.addDependency("Y", "Z");
        w2.addDependency("Z", "Y");
        r = DependencyChecker.check(w2);
        System.out.println(r);
        check("cycle Y, Z is found", r.getCycle().equals(List.of("Y", "Z")));

        // a prerequisite name that doesn't exist (typo)
        Workflow w3 = new Workflow();
        FixedTask chop = new FixedTask("Chop", 10);
        chop.addPrerequisite("Wahs");
        w3.addTask(chop);
        r = DependencyChecker.check(w3);
        System.out.println(r);
        check("unknown prerequisite is invalid", !r.isValid() && !r.hasCycle());
        check("message names the missing task", r.getMessage().contains("Wahs"));

        // diamond: A first, then B and C, then D
        Workflow w4 = new Workflow();
        w4.addTask(new FixedTask("A", 1));
        w4.addTask(new FixedTask("B", 1));
        w4.addTask(new FixedTask("C", 1));
        w4.addTask(new FixedTask("D", 1));
        w4.addDependency("B", "A");
        w4.addDependency("C", "A");
        w4.addDependency("D", "B");
        w4.addDependency("D", "C");
        r = DependencyChecker.check(w4);
        levels = LayoutCalculator.computeLevels(w4, r.getOrder());
        check("diamond order is A, B, C, D", r.getOrder().equals(List.of("A", "B", "C", "D")));
        check("D is in column 2", levels.get("D") == 2);

        // empty workflow
        r = DependencyChecker.check(new Workflow());
        check("empty workflow is valid", r.isValid() && r.getOrder().isEmpty());

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
}
