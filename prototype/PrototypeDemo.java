package prototype;

/**
 * Compares shallow and deep copies of a {@link Bullet}.
 * Usage: {@code java prototype.PrototypeDemo [shallow|deep]} (the mode only affects {@link Bullet#copy}).
 * Java has no real memory addresses, so the identity hash code stands in for "the address" of an object:
 * two references with the same value point at the same object.
 */
public final class PrototypeDemo {
    public static void main(String[] args) {
        Bullet original = new Bullet(1, 7, 100, 100, 9, 0);
        original.modifiers().setDamageBonus(5);
        Bullet shallow = original.shallowCopy();
        Bullet deep = original.deepCopy();

        System.out.println("-- before mutation");
        print("original", original);
        print("shallow", shallow);
        print("deep", deep);

        shallow.modifiers().setDamageBonus(99);
        System.out.println("-- after shallow.modifiers().setDamageBonus(99)");
        print("original", original);
        print("shallow", shallow);
        print("deep", deep);
    }

    private static void print(String name, Bullet bullet) {
        System.out.printf("%-9s bullet=%s modifiers=%s effectiveDamage=%d%n", name,
                address(bullet), address(bullet.modifiers()), bullet.effectiveDamage());
    }

    private static String address(Object o) { return String.format("0x%08x", System.identityHashCode(o)); }
}
