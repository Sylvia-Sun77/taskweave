package taskweave.scheduler;

import java.util.PriorityQueue;

import taskweave.model.Task;
import taskweave.model.Workflow;

public class InputOrderStrategy implements SchedulingStrategy {

    private PriorityQueue<Task> queue;

    @Override
    public String getName() {
        return "Input order";
    }

    @Override
    public void reset(Workflow w) {
        // smaller input index comes out first
        queue = new PriorityQueue<>((a, b) -> w.indexOf(a.getName()) - w.indexOf(b.getName()));
    }

    @Override
    public void addReady(Task t) {
        if (queue == null) {
            throw new IllegalStateException("Call reset() before using the strategy");
        }
        queue.add(t);
    }

    @Override
    public Task pollNext() {
        return queue == null ? null : queue.poll();
    }

    @Override
    public boolean hasReady() {
        return queue != null && !queue.isEmpty();
    }
}
