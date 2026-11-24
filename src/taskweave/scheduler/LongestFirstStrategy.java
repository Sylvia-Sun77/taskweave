package taskweave.scheduler;

import java.util.PriorityQueue;

import taskweave.model.Task;
import taskweave.model.Workflow;

public class LongestFirstStrategy implements SchedulingStrategy {

    private PriorityQueue<Task> queue;

    @Override
    public String getName() {
        return "Longest task first";
    }

    @Override
    public void reset(Workflow w) {
        // longer task first, on a tie the one added earlier.
        // PriorityQueue does not keep ties in order by itself, so the tie rule has to be here
        queue = new PriorityQueue<>((a, b) -> {
            if (a.getDuration() != b.getDuration()) {
                return b.getDuration() - a.getDuration();
            }
            return w.indexOf(a.getName()) - w.indexOf(b.getName());
        });
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
