package taskweave.scheduler;

import taskweave.model.Task;
import taskweave.model.Workflow;

// a scheduling rule is a ready queue: tasks that can start go in,
// free workers take the next one out. rules differ only in which one comes out first
public interface SchedulingStrategy {

    String getName();

    // empty the queue before a new run
    void reset(Workflow w);

    void addReady(Task t);

    // next task to start, or null if nothing is ready
    Task pollNext();

    boolean hasReady();
}
