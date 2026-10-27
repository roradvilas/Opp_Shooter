# Singleton UML class diagram

`Logger` exposes one shared instance through `getInstance()`. Its private constructor prevents
other classes from creating additional logger instances.

```mermaid
classDiagram
    direction LR

    class Logger {
        <<singleton>>
        -INSTANCE Logger
        -Logger()
        +getInstance() Logger
        +log(text) void
    }
    class GameServer
    class GameFacade
    class LoggingWeaponDecorator

    GameServer ..> Logger : gets shared instance
    GameFacade ..> Logger : gets shared instance
    LoggingWeaponDecorator ..> Logger : logs weapon fire
```

`INSTANCE` is initialized by the JVM during class initialization, which runs once. `log()` is
synchronized so concurrent calls do not interleave output lines.
