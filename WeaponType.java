public enum WeaponType {
    ASSAULT_RIFLE("Assault Rifle"), PISTOL("Pistol"), SHOTGUN("Shotgun");

    private final String label;
    WeaponType(String label) { this.label = label; }
    public WeaponStrategy createStrategy() {
        return switch (this) {
            case ASSAULT_RIFLE -> new AssaultRifle();
            case PISTOL -> new Pistol();
            case SHOTGUN -> new Shotgun();
        };
    }
    @Override public String toString() { return label; }
}
