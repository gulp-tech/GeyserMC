# GeyserMC — extended Bedrock version range fork

This repository is a fork of [GeyserMC/Geyser](https://github.com/GeyserMC/Geyser), the proxy that
lets Minecraft: Bedrock Edition players join Minecraft: Java Edition servers.

The upstream project only supports a short window of recent Bedrock builds. **This fork keeps old
Bedrock clients working**: players on **1.21.100** can join a Java server alongside players on
**26.52** — 12 protocol families in total, from protocol `827` up to `2193`.

## Supported versions

| Bedrock version(s)      | Protocol | Notes                                   |
|-------------------------|----------|-----------------------------------------|
| 1.21.100 – 1.21.101     | 827      | oldest supported                        |
| 1.21.111 – 1.21.114     | 844      |                                         |
| 1.21.120 – 1.21.123     | 859      |                                         |
| 1.21.124                | 860      |                                         |
| 1.21.130 – 1.21.132     | 898      |                                         |
| 26.0 – 26.3             | 924      |                                         |
| 26.10 – 26.13           | 944      |                                         |
| 26.20 – 26.23           | 975      |                                         |
| 26.30 – 26.34           | 1001     |                                         |
| 26.40 – 26.44           | 2168     |                                         |
| 26.45                   | 2169     |                                         |
| 26.50 – 26.52           | 2193     | latest supported, default protocol      |

Java Edition side: whatever Minecraft version this snapshot targets (26.3 / protocol 775).

## What this fork changes

### Version support

* **All Bedrock codecs between 1.21.100 and 26.52 are registered** — `GameProtocol` now exposes the
  whole range instead of only 26.30+, together with `MINIMUM_BEDROCK_PROTOCOL` /
  `DEFAULT_BEDROCK_PROTOCOL` and their Minecraft version names.
* **Per-version block and item palettes** (`block_palette.*`, `runtime_item_states.*`,
  `item_components.*`, `creative_items.*`, `item_tags.*`) are bundled for every supported
  protocol, and `BlockRegistryPopulator`, `ItemRegistryPopulator` and `TagRegistryPopulator`
  register them per protocol, exactly like upstream does for 26.30+.
* **Downgrade conversion chain** for blocks and items that only exist on newer Bedrock builds:
  `WildernessBoundConverter` (26.50) → `ChaosCubedConverter` (26.10) →
  `GoldenDandelionConverter` (26.0) → `Legacy121Converter` (1.21.100), wired together in
  `BedrockVersionConversions`. New Java blocks that do not exist on an older Bedrock palette are
  mapped onto the closest older block (e.g. concrete stairs → quartz stairs), new items onto older
  items (`LegacyItemFallbacks`, e.g. spears → swords, nautilus armour → horse armour).
* **Version-gated features** so that older clients are never sent packets they cannot decode:
  * 26.50 record/`ClientboundUpdateSoundData` handling,
  * 26.40 recipe packet layout (`CraftingDataPacket` split data),
  * 26.30 friction/bounciness/air-drag attributes, inventory action ordering, item stack size
    components,
  * 26.20 furnace recipes and `VoxelShapesPacket`,
  * 26.10 waypoints/locator bar, `inClientPredictedVehicle` and party packets,
  * 1.21.130 collision-box component format and attack-range/kinetic-weapon components.

### Quality of life

* **O(1) protocol lookup** — `GameProtocol.getBedrockCodec` no longer scans a list on every login,
  and duplicate/out-of-order codec registration now fails fast with a clear exception.
* **`GameProtocol.isSupported`, `getClosestSupportedProtocol`, `getMinecraftVersions`,
  `getSupportedBedrockVersionRange`** — small helpers so version checks (and kick messages) do not
  have to be reinvented every time.
* **Tests** — `GameProtocolTest` validates ordering, boundaries, lookups and version gates;
  `BedrockResourceAvailabilityTest` asserts that every protocol Geyser advertises ships all the
  data files it needs, so a missing palette fails in CI instead of at runtime.

### Stability — fewer disconnects

A player being kicked with "An internal error occurred!" or "Invalid packet received!" was the most
common complaint, caused by a single odd packet or a network hiccup ending the whole session:

* **`InvalidPacketHandler` now classifies exceptions instead of kicking on everything.**
  * Only true `IllegalPacketException`s (packets a client must never send) still disconnect the player.
  * I/O errors, timeouts, closed channels and rejected tasks are logged at debug level and the channel
    is closed quietly — no scary message for a packet that arrived after the disconnect.
  * A packet that simply cannot be decoded (newer client field layout, unexpected trailing data) is
    skipped instead of ending the session, because Bedrock batches are length prefixed. After 25 such
    packets the player is still disconnected; `-DGeyser.MaxUndecodablePackets=0` restores the old
    behaviour and any other value sets a new limit.
  * Exceptions on an already closed session are ignored, and every disconnect path is exception safe,
    so a failing listener can no longer leave a "ghost" session behind.
* **Packet violation warnings no longer kick** unless the client reports a `FATAL` severity; a warning
  means the client clamped an out-of-range value and kept playing.
* **Unknown block break actions are ignored** with a warning instead of disconnecting the player.
* **`GeyserSession#disconnect` always completes cleanup** — the session is removed from the session
  manager and its tick thread is cancelled even if a `SessionDisconnectEvent` listener throws, which
  previously caused "you are already logged in" errors on reconnect.
* **`GeyserServerInitializer` can no longer throw while reporting an initialisation failure.**
* **Serverbound movement packets no longer kick.** Clients occasionally send `MovePlayerPacket` /
  `MoveEntityAbsolutePacket` (vehicles, teleports, knockback) even though movement is server
  authoritative; Geyser treated those as illegal packets and disconnected the player - a common cause
  of "kicked when hitting/being hit". They are now dropped (logged once in debug mode).
* **Bedrock's client-side chunk caching packets** (`SubChunkRequestPacket`, `ClientCacheBlobStatusPacket`)
  are ignored instead of terminating the session when a client sends them anyway.
* **Clients on an in-between protocol version are no longer rejected.** Preview/beta builds (e.g. the
  1.21.110 betas report protocols 835/840/843) and skipped patch versions now fall back to the closest
  lower codec instead of being kicked with "your client is outdated";
  `-DGeyser.StrictProtocolVersions=true` restores the strict behaviour.
* **Kick logs now include the Bedrock protocol version**, so a report from a player can be traced back
  to the exact client build.

### Performance

* **`VersionedRegistry#forVersion` is a binary search over a cached, sorted, immutable snapshot**
  instead of a linear scan of every registered version. It is queried on hot paths such as inventory
  translation (`getTotemOfUndying`, `getUnusableSpaceBlockDefinition`, `getUpgradeTemplate`), so this
  removes a scan (and an iterator allocation) per inventory update; the snapshot is invalidated
  automatically on `register`/`set`.
* **The session tick no longer allocates an empty packet array** every tick when nothing was queued.
* `VersionedRegistryTest` covers exact matches, closest-lower lookups, the "no version" case and cache
  invalidation.

## Download

Prebuilt jars of this fork are published automatically by the `Fork Build` workflow:

**https://github.com/gulp-tech/GeyserMC/releases/tag/fork-latest**

| File | Where it goes |
|------|---------------|
| `Geyser-Spigot.jar` | Paper / Spigot / Purpur — `plugins/` |
| `Geyser-Velocity.jar` | Velocity — `plugins/` |
| `Geyser-BungeeCord.jar` | BungeeCord / Waterfall — `plugins/` |
| `Geyser-Fabric.jar` | Fabric — `mods/` |
| `Geyser-NeoForge.jar` | NeoForge — `mods/` |
| `Geyser-ViaProxy.jar` | ViaProxy — `plugins/` |
| `Geyser-Standalone.jar` | standalone — `java -jar Geyser-Standalone.jar` |

## Building

The build is unchanged from upstream:

```bash
./gradlew build            # everything
./gradlew :standalone:shadowJar
```

The output jar is `bootstrap/standalone/build/libs/Geyser-Standalone.jar`.

The `core/src/main/resources/mappings` and `core/src/main/resources/languages` directories are git
submodules:

```bash
git submodule update --init --recursive
```

## Notes and limitations

* The 1.21.x support is a **best-effort downgrade**, not a byte-for-byte reimplementation of those
  client versions. Contents that do not exist on an old Bedrock version are substituted with the
  closest available block or item, so worlds and inventories may look slightly different for
  players on old clients.
* The bundled `mappings` submodule is generated for the current Java version (26.3). Custom
  blocks/items that are defined by plugins keep working, but on very old clients they are rendered
  with their fallback blocks.
* Only the Java version this snapshot targets can join; Bedrock ↔ Bedrock is not supported.
* `./gradlew build` (including Geyser's own game tests, which populate the block/item registries for
  every supported palette) passes on CI, which is what publishes the jars above.

## License

Same as upstream — MIT, see [LICENSE](LICENSE).
