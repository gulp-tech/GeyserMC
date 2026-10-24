/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.geyser.registry.loader.RegistryLoaders;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the (cached, binary searched) version lookup of {@link VersionedRegistry}.
 */
public class VersionedRegistryTest {

    private static VersionedRegistry<String> createRegistry() {
        VersionedRegistry<String> registry = VersionedRegistry.create(RegistryLoaders.empty(Int2ObjectOpenHashMap::new));
        registry.register(827, "1.21.100");
        registry.register(844, "1.21.111");
        registry.register(1001, "1.26.30");
        registry.register(2193, "26.52");
        return registry;
    }

    @Test
    public void testExactMatch() {
        VersionedRegistry<String> registry = createRegistry();

        assertEquals("1.21.100", registry.forVersion(827));
        assertEquals("1.21.111", registry.forVersion(844));
        assertEquals("1.26.30", registry.forVersion(1001));
        assertEquals("26.52", registry.forVersion(2193));
    }

    @Test
    public void testClosestLowerVersion() {
        VersionedRegistry<String> registry = createRegistry();

        assertEquals("1.21.100", registry.forVersion(830));
        assertEquals("1.21.111", registry.forVersion(900));
        assertEquals("1.26.30", registry.forVersion(2000));
        // Above the newest registered version the newest one is used
        assertEquals("26.52", registry.forVersion(99999));
    }

    @Test
    public void testNoLowerVersion() {
        VersionedRegistry<String> registry = createRegistry();

        assertThrows(IllegalArgumentException.class, () -> registry.forVersion(826));
    }

    @Test
    public void testCacheInvalidationOnRegister() {
        VersionedRegistry<String> registry = createRegistry();
        // Fill the cache first
        assertEquals("1.21.100", registry.forVersion(827));

        registry.register(830, "1.21.102");
        assertEquals("1.21.102", registry.forVersion(830));
        assertEquals("1.21.102", registry.forVersion(835));
        assertEquals("1.21.111", registry.forVersion(844));
    }

    @Test
    public void testCacheInvalidationOnConsumerRegister() {
        VersionedRegistry<String> registry = createRegistry();
        assertEquals("1.21.100", registry.forVersion(827));

        registry.register(mappings -> mappings.put(100, "legacy"));
        assertEquals("legacy", registry.forVersion(100));
        assertEquals("legacy", registry.forVersion(826));
    }
}
