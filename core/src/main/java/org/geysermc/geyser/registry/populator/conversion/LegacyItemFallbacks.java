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

import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * Item fallbacks for the 1.21.x Bedrock versions, which do not know the items that were added in
 * later Bedrock releases yet. These are kept separate from the block converters because items can
 * not simply be renamed - they are completely replaced by an older item.
 */
public final class LegacyItemFallbacks {

    /**
     * Items added in 1.21.110 that do not exist on 1.21.100.
     */
    public static Map<Item, Item> for1_21_100() {
        Map<Item, Item> map = new HashMap<>();
        fill1_21_100(map);
        // 1.21.100 predates the 1.21.110 items as well
        fill1_21_110(map);
        return resolveChains(map);
    }

    /**
     * Items added in 1.21.120/1.21.124 that do not exist on 1.21.110.
     */
    public static Map<Item, Item> for1_21_110() {
        Map<Item, Item> map = new HashMap<>();
        fill1_21_110(map);
        return resolveChains(map);
    }

    private static void fill1_21_100(Map<Item, Item> map) {
        map.put(Items.ACACIA_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.BAMBOO_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.BIRCH_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.CHERRY_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.CRIMSON_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.DARK_OAK_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.JUNGLE_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.MANGROVE_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.OAK_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.PALE_OAK_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.SPRUCE_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.WARPED_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.COPPER_BARS, Items.IRON_BARS);
        map.put(Items.EXPOSED_COPPER_BARS, Items.IRON_BARS);
        map.put(Items.WEATHERED_COPPER_BARS, Items.IRON_BARS);
        map.put(Items.OXIDIZED_COPPER_BARS, Items.IRON_BARS);
        map.put(Items.WAXED_COPPER_BARS, Items.IRON_BARS);
        map.put(Items.WAXED_EXPOSED_COPPER_BARS, Items.IRON_BARS);
        map.put(Items.WAXED_WEATHERED_COPPER_BARS, Items.IRON_BARS);
        map.put(Items.WAXED_OXIDIZED_COPPER_BARS, Items.IRON_BARS);
        map.put(Items.COPPER_GOLEM_STATUE, Items.ARMOR_STAND);
        map.put(Items.EXPOSED_COPPER_GOLEM_STATUE, Items.ARMOR_STAND);
        map.put(Items.WEATHERED_COPPER_GOLEM_STATUE, Items.ARMOR_STAND);
        map.put(Items.OXIDIZED_COPPER_GOLEM_STATUE, Items.ARMOR_STAND);
        map.put(Items.WAXED_COPPER_GOLEM_STATUE, Items.ARMOR_STAND);
        map.put(Items.WAXED_EXPOSED_COPPER_GOLEM_STATUE, Items.ARMOR_STAND);
        map.put(Items.WAXED_WEATHERED_COPPER_GOLEM_STATUE, Items.ARMOR_STAND);
        map.put(Items.WAXED_OXIDIZED_COPPER_GOLEM_STATUE, Items.ARMOR_STAND);
        map.put(Items.COPPER_LANTERN, Items.LANTERN);
        map.put(Items.EXPOSED_COPPER_LANTERN, Items.LANTERN);
        map.put(Items.WEATHERED_COPPER_LANTERN, Items.LANTERN);
        map.put(Items.OXIDIZED_COPPER_LANTERN, Items.LANTERN);
        map.put(Items.WAXED_COPPER_LANTERN, Items.LANTERN);
        map.put(Items.WAXED_EXPOSED_COPPER_LANTERN, Items.LANTERN);
        map.put(Items.WAXED_WEATHERED_COPPER_LANTERN, Items.LANTERN);
        map.put(Items.WAXED_OXIDIZED_COPPER_LANTERN, Items.LANTERN);
        map.put(Items.EXPOSED_LIGHTNING_ROD, Items.LIGHTNING_ROD);
        map.put(Items.WEATHERED_LIGHTNING_ROD, Items.LIGHTNING_ROD);
        map.put(Items.OXIDIZED_LIGHTNING_ROD, Items.LIGHTNING_ROD);
        map.put(Items.WAXED_LIGHTNING_ROD, Items.LIGHTNING_ROD);
        map.put(Items.WAXED_EXPOSED_LIGHTNING_ROD, Items.LIGHTNING_ROD);
        map.put(Items.WAXED_WEATHERED_LIGHTNING_ROD, Items.LIGHTNING_ROD);
        map.put(Items.WAXED_OXIDIZED_LIGHTNING_ROD, Items.LIGHTNING_ROD);
        map.put(Items.COPPER_TORCH, Items.TORCH);
        map.put(Items.COPPER_HORSE_ARMOR, Items.LEATHER_HORSE_ARMOR);
    }

    private static void fill1_21_110(Map<Item, Item> map) {
        map.put(Items.WOODEN_SPEAR, Items.WOODEN_SWORD);
        map.put(Items.STONE_SPEAR, Items.STONE_SWORD);
        map.put(Items.COPPER_SPEAR, Items.COPPER_SWORD);
        map.put(Items.IRON_SPEAR, Items.IRON_SWORD);
        map.put(Items.GOLDEN_SPEAR, Items.GOLDEN_SWORD);
        map.put(Items.DIAMOND_SPEAR, Items.DIAMOND_SWORD);
        map.put(Items.NETHERITE_SPEAR, Items.NETHERITE_SWORD);
        map.put(Items.COPPER_NAUTILUS_ARMOR, Items.COPPER_HORSE_ARMOR);
        map.put(Items.IRON_NAUTILUS_ARMOR, Items.IRON_HORSE_ARMOR);
        map.put(Items.GOLDEN_NAUTILUS_ARMOR, Items.GOLDEN_HORSE_ARMOR);
        map.put(Items.DIAMOND_NAUTILUS_ARMOR, Items.DIAMOND_HORSE_ARMOR);
        map.put(Items.NETHERITE_NAUTILUS_ARMOR, Items.DIAMOND_HORSE_ARMOR);
        map.put(Items.NETHERITE_HORSE_ARMOR, Items.DIAMOND_HORSE_ARMOR);
        map.put(Items.NAUTILUS_SPAWN_EGG, Items.PUFFERFISH_SPAWN_EGG);
        map.put(Items.ZOMBIE_NAUTILUS_SPAWN_EGG, Items.PUFFERFISH_SPAWN_EGG);
        map.put(Items.CAMEL_HUSK_SPAWN_EGG, Items.CAMEL_SPAWN_EGG);
        map.put(Items.PARCHED_SPAWN_EGG, Items.SKELETON_SPAWN_EGG);
    }
    /**
     * Some fallbacks point at an item that is itself missing on the target version (e.g.
     * {@code copper_nautilus_armor → copper_horse_armor}, but 1.21.100 has neither). Follow the
     * chain until we end up at an item that actually exists on the oldest supported version.
     */
    /**
     * Follows fallback chains so that every item ends up at a fallback that exists on the target
     * version, e.g. {@code poplar_shelf -> pale_oak_shelf -> chiseled_bookshelf}.
     *
     * @param map the fallbacks to resolve, modified in place
     * @return the same map, for convenience
     */
    public static Map<Item, Item> resolveChains(Map<Item, Item> map) {
        map.replaceAll((item, fallback) -> {
            Item resolved = fallback;
            int guard = 0;
            while (resolved != item && map.containsKey(resolved) && guard++ < 8) {
                resolved = map.get(resolved);
            }
            return resolved;
        });
        return map;
    }

    private LegacyItemFallbacks() {
    }
}
