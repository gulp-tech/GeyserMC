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

package org.geysermc.geyser.network.bedrock;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v1001.Bedrock_v1001;
import org.cloudburstmc.protocol.bedrock.codec.v2168.Bedrock_v2168;
import org.cloudburstmc.protocol.bedrock.codec.v2168.Bedrock_v2168_hotfix4;
import org.cloudburstmc.protocol.bedrock.codec.v2169.Bedrock_v2169;
import org.cloudburstmc.protocol.bedrock.codec.v2193.Bedrock_v2193;
import org.cloudburstmc.protocol.bedrock.codec.v827.Bedrock_v827;
import org.cloudburstmc.protocol.bedrock.codec.v844.Bedrock_v844;
import org.cloudburstmc.protocol.bedrock.codec.v859.Bedrock_v859;
import org.cloudburstmc.protocol.bedrock.codec.v860.Bedrock_v860;
import org.cloudburstmc.protocol.bedrock.codec.v898.Bedrock_v898;
import org.cloudburstmc.protocol.bedrock.codec.v924.Bedrock_v924;
import org.cloudburstmc.protocol.bedrock.codec.v944.Bedrock_v944;
import org.cloudburstmc.protocol.bedrock.codec.v975.Bedrock_v975;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec;
import org.geysermc.geyser.api.util.MinecraftVersion;
import org.geysermc.geyser.impl.MinecraftVersionImpl;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;
import org.geysermc.mcprotocollib.protocol.codec.PacketCodec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Contains information about the supported protocols in Geyser.
 * <p>
 * This fork supports every Bedrock protocol released from 1.21.100 (protocol 827) up to and
 * including 26.52 (protocol 2193). Older codecs share the same packet codepath wherever
 * possible; the few features that only exist on newer protocol versions are gated behind the
 * {@code isXorHigher} convenience methods below.
 */
public final class GameProtocol {

    /**
     * All Bedrock protocol codecs that Geyser uses, in ascending protocol order.
     */
    static final List<BedrockCodec> SUPPORTED_BEDROCK_CODECS = new ArrayList<>();

    /**
     * All bedrock protocol versions that Geyser supports
     */
    public static final IntList SUPPORTED_BEDROCK_PROTOCOLS = new IntArrayList();

    /**
     * All bedrock minecraft versions that Geyser supports.
     * There may be multiple MinecraftVersions with the same protocol version.
     */
    public static final List<MinecraftVersion> SUPPORTED_BEDROCK_VERSIONS = new ArrayList<>();

    /**
     * O(1) lookup for incoming connections, instead of scanning {@link #SUPPORTED_BEDROCK_CODECS}.
     */
    private static final Int2ObjectMap<BedrockCodec> CODECS_BY_PROTOCOL = new Int2ObjectOpenHashMap<>();

    /**
     * Minecraft version names per protocol version, in registration order.
     */
    private static final Int2ObjectMap<List<String>> VERSIONS_BY_PROTOCOL = new Int2ObjectOpenHashMap<>();

    /**
     * The latest Bedrock protocol version that Geyser supports.
     */
    public static final int DEFAULT_BEDROCK_PROTOCOL;

    /**
     * The oldest Bedrock protocol version that Geyser supports (1.21.100).
     */
    public static final int MINIMUM_BEDROCK_PROTOCOL;

    /**
     * The latest Bedrock Minecraft version that Geyser supports.
     */
    public static final String DEFAULT_BEDROCK_VERSION;

    /**
     * The oldest Bedrock Minecraft version that Geyser supports.
     */
    public static final String MINIMUM_BEDROCK_VERSION;

    /**
     * Java codec that is supported. We only ever support one version for
     * Java Edition.
     */
    private static final PacketCodec DEFAULT_JAVA_CODEC = MinecraftCodec.CODEC;

    static {
        // Strict ordering
        register(Bedrock_v827.CODEC, "1.21.100", "1.21.101");
        register(Bedrock_v844.CODEC, "1.21.111", "1.21.112", "1.21.113", "1.21.114");
        register(Bedrock_v859.CODEC, "1.21.120", "1.21.121", "1.21.122", "1.21.123");
        register(Bedrock_v860.CODEC, "1.21.124");
        register(Bedrock_v898.CODEC, "1.21.130", "1.21.131", "1.21.132");
        register(Bedrock_v924.CODEC, "26.0", "26.1", "26.2", "26.3");
        register(Bedrock_v944.CODEC, "26.10", "26.11", "26.12", "26.13");
        register(Bedrock_v975.CODEC, "26.20", "26.21", "26.22", "26.23");
        register(Bedrock_v1001.CODEC, "26.30", "26.31", "26.32", "26.33", "26.34");
        register(Bedrock_v2168_hotfix4.CODEC, "26.40", "26.41", "26.42", "26.43", "26.44");
        register(Bedrock_v2169.CODEC, "26.45");
        register(Bedrock_v2193.CODEC, "26.50", "26.51", "26.52");

        MinecraftVersion latestBedrock = SUPPORTED_BEDROCK_VERSIONS.getLast();
        DEFAULT_BEDROCK_VERSION = latestBedrock.versionString();
        DEFAULT_BEDROCK_PROTOCOL = latestBedrock.protocolVersion();

        MinecraftVersion oldestBedrock = SUPPORTED_BEDROCK_VERSIONS.getFirst();
        MINIMUM_BEDROCK_VERSION = oldestBedrock.versionString();
        MINIMUM_BEDROCK_PROTOCOL = oldestBedrock.protocolVersion();
    }

    /**
     * Registers a bedrock codec, along with its protocol version and minecraft version(s).
     * This method must be called in ascending order in terms of protocol version.
     *
     * @param codec the codec to register
     * @param minecraftVersions all versions the codec supports, in ascending order
     */
    private static void register(BedrockCodec codec, String... minecraftVersions) {
        // modify packet serializers to better fit our use
        codec = CodecProcessor.processCodec(codec);

        int protocolVersion = codec.getProtocolVersion();
        if (CODECS_BY_PROTOCOL.putIfAbsent(protocolVersion, codec) != null) {
            throw new IllegalStateException("Duplicate Bedrock protocol version registered: " + protocolVersion);
        }
        if (!SUPPORTED_BEDROCK_PROTOCOLS.isEmpty()
            && SUPPORTED_BEDROCK_PROTOCOLS.getInt(SUPPORTED_BEDROCK_PROTOCOLS.size() - 1) >= protocolVersion) {
            throw new IllegalStateException("Bedrock codecs must be registered in ascending protocol order: " + protocolVersion);
        }

        SUPPORTED_BEDROCK_CODECS.add(codec);
        SUPPORTED_BEDROCK_PROTOCOLS.add(protocolVersion);

        List<String> versions = new ArrayList<>(minecraftVersions.length);
        for (String version : minecraftVersions) {
            versions.add(version);
            SUPPORTED_BEDROCK_VERSIONS.add(new MinecraftVersionImpl(version, protocolVersion));
        }
        VERSIONS_BY_PROTOCOL.put(protocolVersion, Collections.unmodifiableList(versions));
    }

    /**
     * Registers a bedrock codec, its protocol version, and a single minecraft version which is taken from the codec.
     * This method must be called in ascending order in terms of protocol version.
     *
     * @param codec the codec to register
     */
    private static void register(BedrockCodec codec) {
        register(codec, codec.getMinecraftVersion());
    }

    /**
     * Gets the {@link BedrockPacketCodec} of the given protocol version.
     * @param protocolVersion The protocol version to attempt to find
     * @return The packet codec, or null if the client's protocol is unsupported
     */
    public static @Nullable BedrockCodec getBedrockCodec(int protocolVersion) {
        return CODECS_BY_PROTOCOL.get(protocolVersion);
    }

    /**
     * @param protocolVersion the protocol version to look up
     * @return whether Geyser accepts clients on this protocol version
     */
    public static boolean isSupported(int protocolVersion) {
        return protocolVersion >= MINIMUM_BEDROCK_PROTOCOL && protocolVersion <= DEFAULT_BEDROCK_PROTOCOL
            && CODECS_BY_PROTOCOL.containsKey(protocolVersion);
    }

    /**
     * Finds the closest supported Bedrock version, used to build a helpful kick message for
     * clients that are either too old or too new.
     *
     * @param protocolVersion the unsupported protocol version the client is using
     * @return the nearest protocol version that Geyser does support
     */
    public static int getClosestSupportedProtocol(int protocolVersion) {
        if (protocolVersion <= MINIMUM_BEDROCK_PROTOCOL) {
            return MINIMUM_BEDROCK_PROTOCOL;
        }
        if (protocolVersion >= DEFAULT_BEDROCK_PROTOCOL) {
            return DEFAULT_BEDROCK_PROTOCOL;
        }

        int closest = MINIMUM_BEDROCK_PROTOCOL;
        for (int supported : SUPPORTED_BEDROCK_PROTOCOLS) {
            if (supported <= protocolVersion) {
                closest = supported;
            } else {
                // Registration is ascending, so the first entry above the client's protocol is the
                // nearest one going up. Pick whichever side is closer.
                if (supported - protocolVersion < protocolVersion - closest) {
                    closest = supported;
                }
                break;
            }
        }
        return closest;
    }

    /**
     * @param protocolVersion the protocol version to look up
     * @return the Minecraft version names that map to this protocol version, or an empty list
     */
    public static List<String> getMinecraftVersions(int protocolVersion) {
        return VERSIONS_BY_PROTOCOL.getOrDefault(protocolVersion, List.of());
    }

    /* Bedrock convenience methods to gatekeep features and easily remove the check on version removal */

    /** 1.21.110 introduced item stack size components and copper equipment. */
    public static boolean is1_21_110orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v844.CODEC.getProtocolVersion();
    }

    /** 1.21.120 introduced the graphics parameter override packet. */
    public static boolean is1_21_120orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v859.CODEC.getProtocolVersion();
    }

    /** 1.21.130 introduced the data store packets. */
    public static boolean is1_21_130orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v898.CODEC.getProtocolVersion();
    }

    /** The 26.0 range introduced voxel shapes, camera splines and data driven UI. */
    public static boolean is26_0orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v924.CODEC.getProtocolVersion();
    }

    /** 26.10 introduced the locator bar (waypoint), party and world clock packets. */
    public static boolean is26_10orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v944.CODEC.getProtocolVersion();
    }

    /** 26.20 changed item slots and the entity motion/slot serializers. */
    public static boolean is26_20orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v975.CODEC.getProtocolVersion();
    }

    /** 26.30 introduced friction/bounciness/air drag attributes. */
    public static boolean is26_30orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v1001.CODEC.getProtocolVersion();
    }

    public static boolean is26_40orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v2168.CODEC.getProtocolVersion();
    }

    public static boolean is26_50orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v2193.CODEC.getProtocolVersion();
    }

    /**
     * Gets the supported Minecraft: Java Edition version names.
     *
     * @return the supported Minecraft: Java Edition version names
     */
    public static List<String> getJavaVersions() {
        return List.of(DEFAULT_JAVA_CODEC.getMinecraftVersion());
    }

    /**
     * Gets the supported Minecraft: Java Edition protocol version.
     *
     * @return the supported Minecraft: Java Edition protocol version
     */
    public static int getJavaProtocolVersion() {
        return DEFAULT_JAVA_CODEC.getProtocolVersion();
    }

    /**
     * Gets the supported Minecraft: Java Edition version.
     *
     * @return the supported Minecraft: Java Edition version
     */
    public static String getJavaMinecraftVersion() {
        return DEFAULT_JAVA_CODEC.getMinecraftVersion();
    }

    /**
     * @return a string showing all supported Bedrock versions for this Geyser instance
     */
    public static String getAllSupportedBedrockVersions() {
        return SUPPORTED_BEDROCK_VERSIONS.stream()
            .map(MinecraftVersion::versionString)
            .collect(Collectors.joining(", "));
    }

    /**
     * @return a compact human-readable description of the supported Bedrock version range,
     * e.g. {@code "1.21.100 - 26.52"}.
     */
    public static String getSupportedBedrockVersionRange() {
        return MINIMUM_BEDROCK_VERSION + " - " + DEFAULT_BEDROCK_VERSION;
    }

    /**
     * @return a map of protocol version to the Minecraft version names using it, in ascending order
     */
    public static Map<Integer, List<String>> getSupportedVersionsByProtocol() {
        Map<Integer, List<String>> versions = new LinkedHashMap<>();
        for (int protocol : SUPPORTED_BEDROCK_PROTOCOLS) {
            versions.put(protocol, getMinecraftVersions(protocol));
        }
        return Collections.unmodifiableMap(versions);
    }

    /**
     * @return a string showing all supported Java versions for this Geyser instance
     */
    public static String getAllSupportedJavaVersions() {
        return String.join(", ", getJavaVersions());
    }

    private GameProtocol() {
    }
}
