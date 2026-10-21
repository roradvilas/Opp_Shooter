package prototype;

/** Nested mutable state of a bullet; copying it separately is what distinguishes a deep copy. */
public class BulletModifiers implements Cloneable {
    private int damageBonus;

    public int getDamageBonus() { return damageBonus; }
    public void setDamageBonus(int damageBonus) { this.damageBonus = damageBonus; }

    public BulletModifiers copy() {
        try {
            return (BulletModifiers) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
