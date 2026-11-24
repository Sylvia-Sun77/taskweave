package taskweave.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import taskweave.model.Task;
import taskweave.model.Workflow;

public class DependencyChecker {

    private enum State { VISITING, DONE }

    public static ValidationResult check(Workflow w) {
        // 1. every prerequisite has to be a real task
        for (Task t : w.getTasks()) {
            for (String p : t.getPrerequisites()) {
                if (!w.contains(p)) {
                    return ValidationResult.invalid(t.getName() + " depends on " + p + ", which doesn't exist");
                }
            }
        }

        // 2. look for a cycle with depth-first search
        Map<String, State> states = new HashMap<>();
        List<String> path = new ArrayList<>();
        for (Task t : w.getTasks()) {
            if (!states.containsKey(t.getName())) {
                List<String> cycle = findCycle(w, t.getName(), states, path);
                if (cycle != null) {
                    return ValidationResult.cycle(toExecutionOrder(w, cycle));
                }
            }
        }

        // 3. no cycle, so build the execution order
        List<String> order = topologicalOrder(w);
        if (order.size() < w.size()) {
            return ValidationResult.invalid("Could not order all tasks");
        }
        return ValidationResult.valid(order);
    }

    // returns the tasks on a cycle, or null if there is no cycle below this task
    private static List<String> findCycle(Workflow w, String name, Map<String, State> states, List<String> path) {
        states.put(name, State.VISITING);
        path.add(name);
        for (String p : w.getTask(name).getPrerequisites()) {
            State s = states.get(p);
            if (s == State.VISITING) {
                return new ArrayList<>(path.subList(path.indexOf(p), path.size()));
            }
            if (s == null) {
                List<String> cycle = findCycle(w, p, states, path);
                if (cycle != null) {
                    return cycle;
                }
            }
        }
        path.remove(path.size() - 1);
        states.put(name, State.DONE);
        return null;
    }

    // the search walks from a task to its prerequisites, so flip the list,
    // then start from the task that was added first
    private static List<String> toExecutionOrder(Workflow w, List<String> cycle) {
        List<String> flipped = new ArrayList<>(cycle);
        Collections.reverse(flipped);
        int start = 0;
        for (int i = 1; i < flipped.size(); i++) {
            if (w.indexOf(flipped.get(i)) < w.indexOf(flipped.get(start))) {
                start = i;
            }
        }
        List<String> result = new ArrayList<>();
        for (int i = 0; i < flipped.size(); i++) {
            result.add(flipped.get((start + i) % flipped.size()));
        }
        return result;
    }

    // kahn's algorithm: keep taking tasks that have no unfinished prerequisites
    private static List<String> topologicalOrder(Workflow w) {
        DependencyGraph g = new DependencyGraph(w);
        Map<String, Integer> remaining = new HashMap<>();
        Queue<String> queue = new ArrayDeque<>();
        for (Task t : w.getTasks()) {
            int d = g.getInDegree(t.getName());
            remaining.put(t.getName(), d);
            if (d == 0) {
                queue.add(t.getName());
            }
        }

        List<String> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            String name = queue.poll();
            order.add(name);
            for (String next : g.getDependents(name)) {
                int left = remaining.get(next) - 1;
                remaining.put(next, left);
                if (left == 0) {
                    queue.add(next);
                }
            }
        }
        return order;
    }
}
