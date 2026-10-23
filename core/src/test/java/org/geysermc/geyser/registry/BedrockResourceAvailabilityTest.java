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

package org.geysermc.geyser.registry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Makes sure that every Bedrock version Geyser claims to support actually ships the data files
 * that the registry populators expect. Without this, Geyser would only fail at runtime - after
 * the server was already started.
 */
public class BedrockResourceAvailabilityTest {

    /**
     * protocol version -> the data files that version needs
     */
    private static final Map<Integer, String[]> RESOURCES = new LinkedHashMap<>();

    static {
        RESOURCES.put(827, new String[]{"block_palette.1_21_100", "runtime_item_states.1_21_100", "item_components.1_21_100", "creative_items.1_21_100", "item_tags.1_21_100"});
        RESOURCES.put(844, new String[]{"block_palette.1_21_110", "runtime_item_states.1_21_110", "item_components.1_21_110", "creative_items.1_21_110", "item_tags.1_21_110"});
        RESOURCES.put(859, new String[]{"block_palette.1_21_110", "runtime_item_states.1_21_120", "item_components.1_21_120", "creative_items.1_21_120", "item_tags.1_21_120"});
        RESOURCES.put(860, new String[]{"block_palette.1_21_110", "runtime_item_states.1_21_120", "item_components.1_21_120", "creative_items.1_21_120", "item_tags.1_21_120"});
        RESOURCES.put(898, new String[]{"block_palette.1_21_110", "runtime_item_states.1_21_130", "item_components.1_21_130", "creative_items.1_21_130", "item_tags.1_21_130"});
        RESOURCES.put(924, new String[]{"block_palette.26_0", "runtime_item_states.26_0", "item_components.26_0", "creative_items.26_0", "item_tags.26_0"});
        RESOURCES.put(944, new String[]{"block_palette.26_10", "runtime_item_states.26_10", "item_components.26_10", "creative_items.26_10", "item_tags.26_10"});
        RESOURCES.put(975, new String[]{"block_palette.26_20", "runtime_item_states.26_20", "item_components.26_20", "creative_items.26_10", "item_tags.26_20"});
        RESOURCES.put(1001, new String[]{"block_palette.26_30", "runtime_item_states.26_30", "item_components.26_30", "creative_items.26_30", "item_tags.26_30"});
        RESOURCES.put(2168, new String[]{"block_palette.26_40", "runtime_item_states.26_40", "item_components.26_40", "creative_items.26_40", "item_tags.26_30"});
        // 26.45 (2169) has no palette of its own; registries fall back to the closest older version (26.40)
        RESOURCES.put(2169, new String[]{"block_palette.26_40", "runtime_item_states.26_40", "item_components.26_40", "creative_items.26_40", "item_tags.26_30"});
        RESOURCES.put(2193, new String[]{"block_palette.26_50", "runtime_item_states.26_50", "item_components.26_50", "creative_items.26_50", "item_tags.26_30"});
    }

    @Test
    public void testResourcesExistForEverySupportedProtocol() {
        it.unimi.dsi.fastutil.ints.IntList supported = org.geysermc.geyser.network.bedrock.GameProtocol.SUPPORTED_BEDROCK_PROTOCOLS;
        for (int i = 0; i < supported.size(); i++) {
            int protocol = supported.getInt(i);
            String[] resources = RESOURCES.get(protocol);
            Assertions.assertNotNull(resources, "No data file entry for supported protocol " + protocol);

            for (String resource : resources) {
                String path = "/bedrock/" + resource + "." + (resource.startsWith("block_palette") || resource.startsWith("item_components") || resource.startsWith("data_driven_blocks") ? "nbt" : "json");
                try (InputStream stream = BedrockResourceAvailabilityTest.class.getResourceAsStream(path)) {
                    Assertions.assertNotNull(stream, "Missing Bedrock data file " + path);
                } catch (Exception e) {
                    Assertions.fail("Unable to read Bedrock data file " + path, e);
                }
            }
        }
    }
}
