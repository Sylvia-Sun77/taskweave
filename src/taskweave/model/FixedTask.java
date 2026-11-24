package taskweave.model;

public class FixedTask extends Task {

    private final int minutes;

    public FixedTask(String name, int minutes) {
        super(name);
        checkPositive(minutes, "Minutes");
        this.minutes = minutes;
    }

    public int getMinutes() {
        return minutes;
    }

    @Override
    public int getDuration() {
        return minutes;
    }

    @Override
    public String getType() {
        return "FIXED";
    }

    @Override
    public FixedTask copy() {
        FixedTask c = new FixedTask(getName(), minutes);
        copyPrerequisitesTo(c);
        return c;
    }
}
