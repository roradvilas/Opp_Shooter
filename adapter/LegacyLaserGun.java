package adapter;

/** Adaptee: an existing laser API that does not match {@link WeaponStrategy}. */
public class LegacyLaserGun {
    private boolean charged;

    public void chargeCapacitor() { charged = true; }
    public boolean isCharged() { return charged; }
    public double rechargeTimeMs() { return 400; }
    public double beamSpeed() { return 18; }
    public int beamDamage() { return 35; }
    public void dischargeCapacitor() { charged = false; }
}
