package taskweave.model;

public class PerUnitTask extends Task {

    private final int units;
    private final int minutesPerUnit;

    public PerUnitTask(String name, int units, int minutesPerUnit) {
        super(name);
        checkPositive(units, "Units");
        checkPositive(minutesPerUnit, "Minutes per unit");
        this.units = units;
        this.minutesPerUnit = minutesPerUnit;
    }

    public int getUnits() {
        return units;
    }

    public int getMinutesPerUnit() {
        return minutesPerUnit;
    }

    @Override
    public int getDuration() {
        return units * minutesPerUnit;
    }

    @Override
    public String getType() {
        return "PER_UNIT";
    }

    @Override
    public PerUnitTask copy() {
        PerUnitTask c = new PerUnitTask(getName(), units, minutesPerUnit);
        copyPrerequisitesTo(c);
        return c;
    }
}
