package taskweave.scheduler;

public class ScheduledSegment {

    private final String taskName;
    private final int workerIndex;
    private final int start;
    private final int end;

    public ScheduledSegment(String taskName, int workerIndex, int start, int end) {
        this.taskName = taskName;
        this.workerIndex = workerIndex;
        this.start = start;
        this.end = end;
    }

    public String getTaskName() {
        return taskName;
    }

    // starts from 0
    public int getWorkerIndex() {
        return workerIndex;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return "worker " + (workerIndex + 1) + ": " + taskName + " " + start + "-" + end;
    }
}
