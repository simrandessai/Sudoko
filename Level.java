/** Abstract difficulty level. Each subclass decides how many cells are removed. */
public abstract class Level {
    private final String name;

    protected Level(String name) {
        this.name = name;
    }

    public abstract int getHoles();

    /** Overridden so the level shows its name in the combo box. */
    @Override
    public String toString() {
        return name;
    }
}

class EasyLevel extends Level {
    EasyLevel() {
        super("Easy");
    }

    @Override
    public int getHoles() {
        return 35;
    }
}

class MediumLevel extends Level {
    MediumLevel() {
        super("Medium");
    }

    @Override
    public int getHoles() {
        return 45;
    }
}

class HardLevel extends Level {
    HardLevel() {
        super("Hard");
    }

    @Override
    public int getHoles() {
        return 55;
    }
}
