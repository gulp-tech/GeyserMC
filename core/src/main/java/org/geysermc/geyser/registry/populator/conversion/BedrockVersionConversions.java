/*
 * Copyright (c) 2025-2026 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.registry.populator.conversion;

import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.type.GeyserMappingItem;

import java.util.HashMap;
import java.util.Map;

/**
 * Chains the individual per-version downgrade converters together, so that a block or item that
 * exists in Minecraft: Java Edition 26.3 can be represented on every Bedrock version from
 * 1.21.100 onwards.
 * <p>
 * Conversions are always applied newest → oldest:
 * <ol>
 *     <li>{@link WildernessBoundConverter} — strips everything that was added in 26.50</li>
 *     <li>{@link ChaosCubedConverter} — strips sulfur/cinnabar, added in 26.10</li>
 *     <li>{@link GoldenDandelionConverter} — strips the golden dandelion, added in 26.0</li>
 *     <li>{@link Legacy121Converter} — strips the 1.21.110 additions (copper equipment etc.)</li>
 * </ol>
 */
public final class BedrockVersionConversions {

    /**
     * Bedrock 26.40 / 26.30 — only 26.50 additions have to be removed.
     */
    public static NbtMap to26_30(NbtMap tag) {
        return WildernessBoundConverter.convertBlock(tag);
    }

    /**
     * Bedrock 26.20 / 26.10 — also remove the sulfur/cinnabar blocks.
     */
    public static NbtMap to26_10(NbtMap tag) {
        return ChaosCubedConverter.convertBlock(WildernessBoundConverter.convertBlock(tag));
    }

    /**
     * Bedrock 26.0 / 1.21.130 / 1.21.120 / 1.21.110.
     */
    public static NbtMap to26_0(NbtMap tag) {
        return GoldenDandelionConverter.convertBlock(WildernessBoundConverter.convertBlock(tag));
    }

    /**
     * Bedrock 1.21.100 — additionally remove the 1.21.110 additions.
     */
    public static NbtMap to1_21_100(NbtMap tag) {
        return Legacy121Converter.convertBlock(GoldenDandelionConverter.convertBlock(WildernessBoundConverter.convertBlock(tag)));
    }

    /**
     * @param include26_10 whether the 26.10 additions have to be removed as well
     * @param include26_0 whether the 26.0 additions have to be removed as well
     * @return the item fallbacks that are Java-only on the target version
     */
    public static Map<Item, Item> itemFallbacks(boolean include26_10, boolean include26_0) {
        return itemFallbacks(include26_10, include26_0, Map.of());
    }

    /**
     * @param additional extra fallbacks that apply to the target version
     * @return the item fallbacks that are Java-only on the target version
     */
    public static Map<Item, Item> itemFallbacks(boolean include26_10, boolean include26_0, Map<Item, Item> additional) {
        Map<Item, Item> mappings = new HashMap<>(WildernessBoundConverter.itemMappings());
        if (include26_10) {
            mappings.putAll(ChaosCubedConverter.convertItem());
        }
        if (include26_0) {
            mappings.putAll(GoldenDandelionConverter.convertItem());
        }
        mappings.putAll(additional);
        return mappings;
    }

    /**
     * Remapper applied on top of {@link #itemFallbacks} for the oldest supported version.
     */
    public static GeyserMappingItem remapItem(Item item, GeyserMappingItem mapping) {
        return Legacy121Converter.remapItem(item, mapping);
    }

    private BedrockVersionConversions() {
    }
}
