# LegendarySpawnDiscord (for Arclight + NeoForge 1.21.1 + Pixelmon 9.3.x)

전설 포켓몬이 스폰될 때, 디스코드 채널로 알림을 보내는 **독립 플러그인**입니다.
- Pixelmon 코드에 직접 의존하지 않고 **리플렉션**으로 이벤트를 처리하므로, GitHub Actions에서도 빌드가 됩니다.
- NeoForge `EVENT_BUS` 에 **리플렉션**으로 등록하고, `@SubscribeEvent`(bus 라이브러리)만 컴파일 타임에 사용합니다.
- 디스코드 전송은 **JDA 5**를 **리로케이션(shade)** 해서 다른 플러그인과 충돌을 피합니다.

## 요구사항
- **Arclight NeoForge 1.21.1**
- **Java 21**
- **Pixelmon Reforged 9.3.x** (서버에 설치되어 있어야 함)

## 설치
1. `config.yml`의 `discord.token`, `discord.channel_id` 채우기
2. `mvn -q -U -e -B -DskipTests package`
3. `target/LegendarySpawnDiscord-1.0.0.jar` 를 서버 `plugins/`에 넣고 서버 실행

## 설정 (plugins/LegendarySpawnDiscord/config.yml)
```yaml
discord:
  token: "봇 토큰"
  channel_id: "채널 ID"
  mention_everyone: false
  mention_role_id: ""

message:
  template: "✨ 전설 포켓몬 스폰! **{name}** (#{dex}) — 월드 {dimension}, 바이옴 {biome}, 좌표 {x} {y} {z}{near}"
  include_biome: true
  include_coords: true
  include_dimension: true
  include_near_player: true
  dedupe_seconds: 10
```

템플릿 키: `{name}`, `{dex}`, `{x}`, `{y}`, `{z}`, `{dimension}`, `{biome}`, `{near}`

## 참고한 API 문서
- `LegendarySpawnEvent.DoSpawn` (스폰 시점, `getLegendary()` 제공) — Reforged 1.21.1 docs
- `SpawnAction` / `SpawnLocation` / `MutableLocation` (좌표, 바이옴, 월드) — Reforged 1.21.1 docs
