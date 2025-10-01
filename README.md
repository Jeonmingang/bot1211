# ArcLinkDiscordBridge 1.3.1

- Discord ↔ MC chat bridge
- Server start/stop + join/quit alerts
- `/discord stats`

## Build
```
mvn -U -B clean package
# target/ArcLinkDiscordBridge-1.3.1.jar
```

## Why 1.3.1?
- Shade minimize **disabled**
- **Relocated** JDA/OkHttp/Okio/NV-WebSocket classes to avoid classpath conflicts with Arclight or other plugins.
