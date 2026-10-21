package facade;

/** A bullet as the client sees it in a STATE message. */
public record BulletView(long id, int x, int y) {
}
