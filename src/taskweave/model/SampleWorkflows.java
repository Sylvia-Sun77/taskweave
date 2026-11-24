package taskweave.model;

public class SampleWorkflows {

    // the cooking example used in the design doc and acceptance tests
    public static Workflow cooking() {
        Workflow w = new Workflow();
        w.addTask(new FixedTask("Wash", 5));
        w.addTask(new FixedTask("Chop", 10));
        w.addTask(new FixedTask("Stir-fry", 10));
        w.addTask(new FixedTask("Rice", 25));
        w.addTask(new FixedTask("Soup", 40));
        w.addDependency("Chop", "Wash");
        w.addDependency("Stir-fry", "Chop");
        return w;
    }
}
