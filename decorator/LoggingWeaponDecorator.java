package decorator;

import bridge.Player;
import prototype.Bullet;
import singleton.Logger;
import strategy.WeaponStrategy;

import java.util.List;
import java.util.function.LongSupplier;

public final class LoggingWeaponDecorator extends WeaponDecorator {
    public LoggingWeaponDecorator(WeaponStrategy wrappee) { super(wrappee); }

    @Override public List<Bullet> fire(Player player, double angle, LongSupplier bulletIds) {
        Logger.getInstance().log("Player " + player.id + " fires " + wrappee.getClass().getSimpleName());
        List<Bullet> bullets = super.fire(player, angle, bulletIds);
        Logger.getInstance().log("Player " + player.id + " fired " + bullets.size() + " bullet(s)");
        return bullets;
    }
}
