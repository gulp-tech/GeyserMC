/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.registry.loader.RegistryLoader;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A versioned, mapped registry. Like {@link SimpleMappedRegistry}, the {@link Map} interface is
 * not able to be specified here, but unlike it, it does not have support for specialized
 * instances, and ONLY supports {@link Int2ObjectMap} for optimal performance to prevent boxing
 * of integers.
 *
 * @param <V> the value
 */
public class VersionedRegistry<V> extends AbstractMappedRegistry<Integer, V, Int2ObjectMap<V>> {
    /**
     * Sorted snapshot of the registered versions, rebuilt lazily after a mutation.
     * Immutable, so {@link #forVersion(int)} can be called from any thread without locking.
     */
    private volatile Snapshot<V> snapshot;

    protected <I> VersionedRegistry(I input, RegistryLoader<I, Int2ObjectMap<V>> registryLoader) {
        super(input, registryLoader);
    }

    /**
     * Gets the value registered for the newest version that is not newer than the given one.
     * Useful in the event that you want to get a resource which is guaranteed to exist for
     * older versions, but not on newer ones.
     *
     * <p>The lookup uses a lazily built, immutable sorted snapshot of the registered versions,
     * so repeated lookups (which happen on hot paths such as inventory translation) are a
     * binary search instead of a linear scan. The snapshot is rebuilt after any registration.</p>
     *
     * @param version the version
     * @return the closest value for the specified version
     * @throws IllegalArgumentException if no registered version is at or below the given version
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public V forVersion(int version) {
        Snapshot<V> snapshot = this.snapshot;
        if (snapshot == null) {
            snapshot = rebuildSnapshot();
        }

        int[] versions = snapshot.versions;
        if (versions.length == 0) {
            throw new IllegalArgumentException("No appropriate value for version: " + version);
        }

        // Binary search for the newest registered version that is not newer than the requested one
        int index = Arrays.binarySearch(versions, version);
        if (index < 0) {
            index = -index - 2;
        }
        if (index < 0) {
            throw new IllegalArgumentException("No appropriate value for version: " + version);
        }
        return (V) snapshot.values[index];
    }

    private Snapshot<V> rebuildSnapshot() {
        synchronized (this) {
            Snapshot<V> snapshot = this.snapshot;
            if (snapshot == null) {
                snapshot = createSnapshot();
                this.snapshot = snapshot;
            }
            return snapshot;
        }
    }

    private Snapshot<V> createSnapshot() {
        if (this.mappings == null || this.mappings.isEmpty()) {
            return new Snapshot<>(new int[0], new Object[0]);
        }

        int[] versions = new int[this.mappings.size()];
        int index = 0;
        for (int version : this.mappings.keySet()) {
            versions[index++] = version;
        }
        IntArrays.quickSort(versions);

        Object[] values = new Object[versions.length];
        for (int i = 0; i < versions.length; i++) {
            values[i] = this.mappings.get(versions[i]);
        }
        return new Snapshot<>(versions, values);
    }

    /**
     * Invalidates the cached snapshot - must be called whenever the underlying mappings change.
     */
    private void invalidate() {
        this.snapshot = null;
    }

    @Override
    public V register(Integer key, V value) {
        V previous = super.register(key, value);
        invalidate();
        return previous;
    }

    @Override
    public void register(Consumer<Int2ObjectMap<V>> consumer) {
        super.register(consumer);
        invalidate();
    }

    @Override
    public void set(Int2ObjectMap<V> mappings) {
        super.set(mappings);
        invalidate();
    }

    private static final class Snapshot<V> {
        private final int[] versions;
        private final Object[] values;

        private Snapshot(int[] versions, Object[] values) {
            this.versions = versions;
            this.values = values;
        }
    }

    /**
     * Creates a new versioned registry with the given {@link RegistryLoader}. The
     * input type is not specified here, meaning the loader return type is either
     * predefined, or the registry is populated at a later point.
     *
     * @param registryLoader the registry loader
     * @param <I> the input
     * @param <V> the map value
     * @return a new registry with the given RegistryLoader
     */
    public static <I, V> VersionedRegistry<V> create(RegistryLoader<I, Int2ObjectMap<V>> registryLoader) {
        return new VersionedRegistry<>(null, registryLoader);
    }

    /**
     * Creates a new versioned registry with the given {@link RegistryLoader} and input.
     *
     * @param registryLoader the registry loader
     * @param <I> the input
     * @param <V> the map value
     * @return a new registry with the given RegistryLoader
     */
    public static <I, V> VersionedRegistry<V> create(I input, RegistryLoader<I, Int2ObjectMap<V>> registryLoader) {
        return new VersionedRegistry<>(input, registryLoader);
    }

    /**
     * Creates a new versioned registry with the given {@link RegistryLoader} supplier.
     * The input type is not specified here, meaning the loader return type is either
     * predefined, or the registry is populated at a later point.
     *
     * @param registryLoader the registry loader
     * @param <I> the input
     * @param <V> the map value
     * @return a new registry with the given RegistryLoader supplier
     */
    public static <I, V> VersionedRegistry< V> create(Supplier<RegistryLoader<I, Int2ObjectMap<V>>> registryLoader) {
        return new VersionedRegistry<>(null, registryLoader.get());
    }

    /**
     * Creates a new versioned registry with the given {@link RegistryLoader} supplier and input.
     *
     * @param registryLoader the registry loader
     * @param <I> the input
     * @param <V> the map value
     * @return a new registry with the given RegistryLoader supplier
     */
    public static <I, V> VersionedRegistry< V> create(I input, Supplier<RegistryLoader<I, Int2ObjectMap<V>>> registryLoader) {
        return new VersionedRegistry<>(input, registryLoader.get());
    }
}