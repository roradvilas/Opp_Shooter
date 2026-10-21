package game;

import java.util.Objects;

public record Loot(int x, int y, LootType type) {
    public Loot { Objects.requireNonNull(type); }
}
