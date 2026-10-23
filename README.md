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

## License

Same as upstream — MIT, see [LICENSE](LICENSE).
