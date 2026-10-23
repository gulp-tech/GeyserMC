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

package org.geysermc.geyser.network.bedrock;

import it.unimi.dsi.fastutil.ints.IntList;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Verifies that the (extended) list of supported Bedrock versions is sane: every registered codec
 * can be looked up, versions are registered in ascending order and nothing outside of the range
 * 1.21.100 - 26.52 is accepted.
 */
public class GameProtocolTest {

    @Test
    public void testSupportedProtocolsAreAscending() {
        IntList protocols = GameProtocol.SUPPORTED_BEDROCK_PROTOCOLS;
        Assertions.assertFalse(protocols.isEmpty(), "No Bedrock protocols are registered!");
        for (int i = 1; i < protocols.size(); i++) {
            Assertions.assertTrue(protocols.getInt(i - 1) < protocols.getInt(i),
                "Bedrock protocols are not registered in ascending order: %s".formatted(protocols));
        }
    }

    @Test
    public void testEveryProtocolHasACodec() {
        IntList supported = GameProtocol.SUPPORTED_BEDROCK_PROTOCOLS;
        for (int i = 0; i < supported.size(); i++) {
            int protocol = supported.getInt(i);
            BedrockCodec codec = GameProtocol.getBedrockCodec(protocol);
            Assertions.assertNotNull(codec, "No codec registered for protocol " + protocol);
            Assertions.assertEquals(protocol, codec.getProtocolVersion());

            Assertions.assertTrue(GameProtocol.isSupported(protocol));
            Assertions.assertFalse(GameProtocol.getMinecraftVersions(protocol).isEmpty(),
                "No Minecraft version name registered for protocol " + protocol);
        }
    }

    @Test
    public void testRangeBoundaries() {
        // 1.21.100 and 1.21.101
        Assertions.assertEquals(827, GameProtocol.MINIMUM_BEDROCK_PROTOCOL);
        Assertions.assertEquals("1.21.100", GameProtocol.MINIMUM_BEDROCK_VERSION);
        // 26.50 - 26.52
        Assertions.assertEquals(2193, GameProtocol.DEFAULT_BEDROCK_PROTOCOL);
        Assertions.assertEquals("26.52", GameProtocol.DEFAULT_BEDROCK_VERSION);

        Assertions.assertFalse(GameProtocol.isSupported(826));
        Assertions.assertFalse(GameProtocol.isSupported(828));
        Assertions.assertFalse(GameProtocol.isSupported(2194));
        Assertions.assertTrue(GameProtocol.isSupported(2193));

        Assertions.assertNull(GameProtocol.getBedrockCodec(826));
        Assertions.assertNull(GameProtocol.getBedrockCodec(828));
        Assertions.assertNull(GameProtocol.getBedrockCodec(2194));
    }

    @Test
    public void testClosestSupportedProtocol() {
        Assertions.assertEquals(827, GameProtocol.getClosestSupportedProtocol(800));
        Assertions.assertEquals(2193, GameProtocol.getClosestSupportedProtocol(3000));
        // 828 is not a supported protocol (1.21.102 - 1.21.110 gap); 827 is the nearest supported one
        Assertions.assertEquals(827, GameProtocol.getClosestSupportedProtocol(828));
        // Exact matches always return the same protocol
        IntList supported = GameProtocol.SUPPORTED_BEDROCK_PROTOCOLS;
        for (int i = 0; i < supported.size(); i++) {
            int protocol = supported.getInt(i);
            Assertions.assertEquals(protocol, GameProtocol.getClosestSupportedProtocol(protocol));
        }
    }

    @Test
    public void testVersionNames() {
        Assertions.assertEquals(List.of("1.21.100", "1.21.101"), GameProtocol.getMinecraftVersions(827));
        Assertions.assertEquals(List.of("26.50", "26.51", "26.52"), GameProtocol.getMinecraftVersions(2193));
        Assertions.assertEquals("1.21.100 - 26.52", GameProtocol.getSupportedBedrockVersionRange());
    }

    @Test
    public void testVersionGates() {
        // Oldest supported version must not claim support for features it does not have
        Assertions.assertFalse(GameProtocol.is1_21_110orHigher(GameProtocol.MINIMUM_BEDROCK_PROTOCOL));
        Assertions.assertFalse(GameProtocol.is26_0orHigher(GameProtocol.MINIMUM_BEDROCK_PROTOCOL));
        Assertions.assertFalse(GameProtocol.is26_10orHigher(GameProtocol.MINIMUM_BEDROCK_PROTOCOL));
        Assertions.assertFalse(GameProtocol.is26_20orHigher(GameProtocol.MINIMUM_BEDROCK_PROTOCOL));
        Assertions.assertFalse(GameProtocol.is26_30orHigher(GameProtocol.MINIMUM_BEDROCK_PROTOCOL));
        Assertions.assertFalse(GameProtocol.is26_40orHigher(GameProtocol.MINIMUM_BEDROCK_PROTOCOL));
        Assertions.assertFalse(GameProtocol.is26_50orHigher(GameProtocol.MINIMUM_BEDROCK_PROTOCOL));

        // The newest supported version must support everything
        Assertions.assertTrue(GameProtocol.is1_21_110orHigher(GameProtocol.DEFAULT_BEDROCK_PROTOCOL));
        Assertions.assertTrue(GameProtocol.is26_0orHigher(GameProtocol.DEFAULT_BEDROCK_PROTOCOL));
        Assertions.assertTrue(GameProtocol.is26_10orHigher(GameProtocol.DEFAULT_BEDROCK_PROTOCOL));
        Assertions.assertTrue(GameProtocol.is26_20orHigher(GameProtocol.DEFAULT_BEDROCK_PROTOCOL));
        Assertions.assertTrue(GameProtocol.is26_30orHigher(GameProtocol.DEFAULT_BEDROCK_PROTOCOL));
        Assertions.assertTrue(GameProtocol.is26_40orHigher(GameProtocol.DEFAULT_BEDROCK_PROTOCOL));
        Assertions.assertTrue(GameProtocol.is26_50orHigher(GameProtocol.DEFAULT_BEDROCK_PROTOCOL));
    }
}
