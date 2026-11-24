package taskweave.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import taskweave.model.Task;
import taskweave.model.Workflow;

public class Simulator {

    // the workflow must already pass DependencyChecker, otherwise some tasks never become ready
    public static ScheduleResult run(Workflow w, SchedulingStrategy strategy, int workerCount) {
        if (workerCount < 1) {
            throw new IllegalArgumentException("Need at least 1 worker");
        }
        strategy.reset(w);

        Map<String, Integer> startTime = new HashMap<>();
        Map<String, Integer> endTime = new HashMap<>();
        Set<String> queued = new HashSet<>();
        Set<String> finished = new HashSet<>();
        int[] busyUntil = new int[workerCount];
        List<ScheduledSegment> segments = new ArrayList<>();

        int totalWork = 0;
        for (Task t : w.getTasks()) {
            totalWork += t.getDuration();
        }

        int clock = 0;
        while (finished.size() < w.size()) {
            if (clock > totalWork + 1) {
                throw new IllegalStateException("Simulation did not finish, check the dependencies first");
            }

            // 1. tasks that end at this minute are finished
            for (Map.Entry<String, Integer> e : endTime.entrySet()) {
                if (e.getValue() == clock) {
                    finished.add(e.getKey());
                }
            }

            // 2. tasks whose prerequisites are all finished go into the ready queue
            for (Task t : w.getTasks()) {
                if (!queued.contains(t.getName()) && finished.containsAll(t.getPrerequisites())) {
                    strategy.addReady(t);
                    queued.add(t.getName());
                }
            }

            // 3. each free worker takes the next task, lower worker number first
            for (int i = 0; i < workerCount; i++) {
                if (busyUntil[i] <= clock && strategy.hasReady()) {
                    Task t = strategy.pollNext();
                    int end = clock + t.getDuration();
                    startTime.put(t.getName(), clock);
                    endTime.put(t.getName(), end);
                    busyUntil[i] = end;
                    segments.add(new ScheduledSegment(t.getName(), i, clock, end));
                }
            }

            // 4. one minute passes
            clock++;
        }

        int totalTime = 0;
        for (int end : endTime.values()) {
            totalTime = Math.max(totalTime, end);
        }
        int idleTime = workerCount * totalTime - totalWork;

        double waitSum = 0;
        for (Task t : w.getTasks()) {
            int readyAt = 0;
            for (String p : t.getPrerequisites()) {
                readyAt = Math.max(readyAt, endTime.get(p));
            }
            waitSum += startTime.get(t.getName()) - readyAt;
        }
        double averageWait = w.isEmpty() ? 0 : waitSum / w.size();

        return new ScheduleResult(strategy.getName(), workerCount, segments, totalTime, idleTime, averageWait);
    }

    // runs every strategy on the same workflow, used for the comparison line
    public static List<ScheduleResult> runAll(Workflow w, List<SchedulingStrategy> strategies, int workerCount) {
        List<ScheduleResult> results = new ArrayList<>();
        for (SchedulingStrategy s : strategies) {
            results.add(run(w, s, workerCount));
        }
        return results;
    }
}
