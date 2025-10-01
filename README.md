# ArcLinkDiscordBridge 1.3.0 (Arclight-NeoForge 1.21.1 / Java 21)

- 채팅 브리지 (MC ↔ Discord)
- 서버 시작/종료 알림
- 접속/퇴장 알림 및 카운터
- `/discord stats` (Joins/Quits/Uptime 표시)

## Build
```bash
mvn -U -B clean package
# target/ArcLinkDiscordBridge-1.3.0.jar
```

## Fixes vs 1.2.x
- Shade **minimizeJar=false** (JDA/OkHttp/Kotlin 누락으로 인한 종료시 `NoClassDefFoundError` 방지)
- `plugin.yml` 버전 치환을 위해 **resources filtering** 추가
- JDA 5 ReadyEvent import (`events.session.ReadyEvent`) 적용
- `scheduleStartupAnnounce()`는 `BukkitRunnable` 사용 (task id 캡처 문제 해결)
