package strategy;

import factory.WeaponFactory;

public enum WeaponType {
    ASSAULT_RIFLE("Assault Rifle"), PISTOL("Pistol"), SHOTGUN("Shotgun"), LASER("Laser");

    private final String label;
    WeaponType(String label) { this.label = label; }
    public WeaponStrategy createStrategy() {
        return new WeaponFactory().createWeapon(this);
    }
    @Override public String toString() { return label; }
}
