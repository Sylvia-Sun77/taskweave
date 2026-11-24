package taskweave.scheduler;

import java.util.List;

import taskweave.model.FixedTask;
import taskweave.model.PerUnitTask;
import taskweave.model.SampleWorkflows;
import taskweave.model.Task;
import taskweave.model.Workflow;

public class SchedulerDemo {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        Workflow w = SampleWorkflows.cooking();
        SchedulingStrategy inputOrder = new InputOrderStrategy();
        SchedulingStrategy longest = new LongestFirstStrategy();

        ScheduleResult a = Simulator.run(w, inputOrder, 2);
        ScheduleResult b = Simulator.run(w, longest, 2);
        print(a);
        print(b);

        check("input order, 2 workers: total 65", a.getTotalTime() == 65);
        check("input order, 2 workers: idle 40", a.getIdleTime() == 40);
        check("input order, 2 workers: average wait 5.0", a.getAverageWait() == 5.0);
        check("longest first, 2 workers: total 50", b.getTotalTime() == 50);
        check("longest first, 2 workers: idle 10", b.getIdleTime() == 10);
        check("every task appears once", a.getSegments().size() == 5 && b.getSegments().size() == 5);
        check("schedule follows dependencies", followsDependencies(w, a) && followsDependencies(w, b));
        check("no worker does two tasks at once", noOverlap(a) && noOverlap(b));

        check("3 workers: both rules give 40",
                Simulator.run(w, inputOrder, 3).getTotalTime() == 40
                && Simulator.run(w, longest, 3).getTotalTime() == 40);
        check("1 worker: total equals all durations (90)", Simulator.run(w, inputOrder, 1).getTotalTime() == 90);

        List<ScheduleResult> all = Simulator.runAll(w, List.of(inputOrder, longest), 2);
        check("runAll gives one result per rule", all.size() == 2 && all.get(1).getTotalTime() == 50);
        check("running again gives the same result", Simulator.run(w, longest, 2).getTotalTime() == 50);

        Workflow w2 = new Workflow();
        w2.addTask(new PerUnitTask("Dumplings", 4, 5));
        w2.addTask(new FixedTask("Boil", 10));
        w2.addDependency("Boil", "Dumplings");
        check("per-unit task: 20 + 10 = 30", Simulator.run(w2, inputOrder, 2).getTotalTime() == 30);

        ScheduleResult empty = Simulator.run(new Workflow(), inputOrder, 2);
        check("empty workflow: total 0", empty.getTotalTime() == 0 && empty.getSegments().isEmpty());

        try {
            Simulator.run(w, inputOrder, 0);
            check("0 workers is rejected", false);
        } catch (IllegalArgumentException e) {
            check("0 workers is rejected", true);
        }

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
    }

    private static void print(ScheduleResult r) {
        System.out.println(r);
        for (ScheduledSegment s : r.getSegments()) {
            System.out.println("  " + s);
        }
    }

    private static boolean followsDependencies(Workflow w, ScheduleResult r) {
        for (ScheduledSegment s : r.getSegments()) {
            Task t = w.getTask(s.getTaskName());
            for (String p : t.getPrerequisites()) {
                if (s.getStart() < segmentOf(r, p).getEnd()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static ScheduledSegment segmentOf(ScheduleResult r, String name) {
        for (ScheduledSegment s : r.getSegments()) {
            if (s.getTaskName().equals(name)) {
                return s;
            }
        }
        return null;
    }

    private static boolean noOverlap(ScheduleResult r) {
        List<ScheduledSegment> list = r.getSegments();
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                ScheduledSegment x = list.get(i);
                ScheduledSegment y = list.get(j);
                if (x.getWorkerIndex() == y.getWorkerIndex()
                        && x.getStart() < y.getEnd() && y.getStart() < x.getEnd()) {
                    return false;
                }
            }
        }
        return true;
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
