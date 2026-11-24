package taskweave.scheduler;

import java.util.List;

public class ScheduleResult {

    private final String strategyName;
    private final int workerCount;
    private final List<ScheduledSegment> segments;
    private final int totalTime;
    private final int idleTime;
    private final double averageWait;

    public ScheduleResult(String strategyName, int workerCount, List<ScheduledSegment> segments,
            int totalTime, int idleTime, double averageWait) {
        this.strategyName = strategyName;
        this.workerCount = workerCount;
        this.segments = List.copyOf(segments);
        this.totalTime = totalTime;
        this.idleTime = idleTime;
        this.averageWait = averageWait;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public int getWorkerCount() {
        return workerCount;
    }

    public List<ScheduledSegment> getSegments() {
        return segments;
    }

    // minutes from the start until the last task ends
    public int getTotalTime() {
        return totalTime;
    }

    // minutes workers spent doing nothing
    public int getIdleTime() {
        return idleTime;
    }

    // average minutes a task waited after it was able to start
    public double getAverageWait() {
        return averageWait;
    }

    @Override
    public String toString() {
        return strategyName + " on " + workerCount + " workers: total " + totalTime
                + " min, idle " + idleTime + " min, average wait " + String.format("%.1f", averageWait) + " min";
    }
}
