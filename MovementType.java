public enum MovementType {
    SNEAK, WALK, SPRINT, DASH;

    public MovementStrategy createStrategy() {
        return switch (this) {
            case SNEAK -> new Sneak();
            case WALK -> new Walk();
            case SPRINT -> new Sprint();
            case DASH -> new Dash();
        };
    }
}
