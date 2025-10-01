# ArcLinkDiscordBridge 1.3.2 (Arclight-NeoForge 1.21.1 / Java 21)

- 채팅 브리지 (MC ↔ Discord)
- 서버 시작/종료 알림 + 접속/퇴장 알림
- `/discord stats`

## Build
```bash
mvn -U -B clean package
# target/ArcLinkDiscordBridge-1.3.2.jar
```

## Notes
- Shade **minimizeJar=false** (종료시 의존성 누락 방지)
- **Relocations** 적용(충돌 회피). *소스 import는 원 패키지(net.dv8tion… 등) 유지*
- JDA5 ReadyEvent 패키지(`events.session.ReadyEvent`), `BukkitRunnable` 스케줄 적용
