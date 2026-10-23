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
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.type.GeyserMappingItem;

/**
 * Handles the Bedrock 1.21.110 → 1.21.100 downgrade. Everything that was added in 1.21.110
 * (copper chains, copper bars, copper lanterns, shelves, copper golem statues and the powered
 * lightning rod state) has to be mapped back onto blocks that exist on 1.21.100.
 */
public class Legacy121Converter extends ConversionHelper {

    public static NbtMap convertBlock(NbtMap tag) {
        final String name = tag.getString("name");
        if (name.equals("minecraft:iron_chain") || name.endsWith("copper_chain")) {
            return withName(tag, "chain");
        } else if (name.endsWith("lightning_rod")) {
            NbtMapBuilder statesWithoutPoweredBit = tag.getCompound("states").toBuilder();
            statesWithoutPoweredBit.remove("powered_bit");
            return tag.toBuilder()
                .putString("name", "minecraft:lightning_rod")
                .putCompound("states", statesWithoutPoweredBit.build())
                .build();
        } else if (name.endsWith("_shelf")) {
            // Shelves do not exist on 1.21.100 at all; a plain bookshelf has no block states on Bedrock,
            // so it is always a valid target.
            return withoutStates("minecraft:bookshelf");
        } else if (name.endsWith("copper_golem_statue")) {
            // Copper golem statues are a 1.21.110 block; fall back to a plain copper block.
            return withoutStates("minecraft:copper_block");
        } else if (name.equals("minecraft:copper_torch")) {
            return withName(tag, "torch");
        } else if (name.endsWith("copper_bars")) {
            return withName(tag, "iron_bars");
        } else if (name.endsWith("copper_lantern")) {
            return withName(tag, "lantern");
        }

        return tag;
    }

    /**
     * 1.21.100 does have a {@code minecraft:chain} item, so chains can be remapped directly
     * instead of being marked as Java-only.
     */
    public static GeyserMappingItem remapItem(Item item, GeyserMappingItem mapping) {
        if (item == Items.IRON_CHAIN || item.javaIdentifier().path().endsWith("copper_chain")) {
            return mapping.withBedrockIdentifier("minecraft:chain");
        }
        return mapping;
    }
}
