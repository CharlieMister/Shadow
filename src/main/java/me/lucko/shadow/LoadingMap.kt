package me.lucko.shadow

import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function

/**
 * Simple lazy-loading map used internally by shadow.
 *
 * This is a direct Kotlin port of the original Java helper and only implements
 * the operations actually required by shadow.
 */
internal class LoadingMap<K, V>(
    private val map: MutableMap<K, V>,
    private val function: Function<K, V>
) {
    fun size(): Int {
        return this.map.size
    }

    fun isEmpty(): Boolean {
        return this.map.isEmpty()
    }

    fun containsKey(key: K): Boolean {
        return this.map.containsKey(key)
    }

    fun containsValue(value: V): Boolean {
        return this.map.containsValue(value)
    }

    operator fun get(key: K): V? {
        val value = this.map[key]
        if (value != null) {
            return value
        }
        return this.map.computeIfAbsent(key, this.function)
    }

    fun getIfPresent(key: Any?): V? {
        @Suppress("UNCHECKED_CAST")
        return this.map[key as K]
    }

    fun put(key: K, value: V): V? {
        return this.map.put(key, value)
    }

    fun remove(key: Any?): V? {
        @Suppress("UNCHECKED_CAST")
        return this.map.remove(key as K)
    }

    fun putAll(that: MutableMap<out K, out V>) {
        this.map.putAll(that)
    }

    fun clear() {
        this.map.clear()
    }

    fun keySet(): MutableSet<K> {
        return this.map.keys
    }

    fun values(): MutableCollection<V> {
        return this.map.values
    }

    fun entrySet(): MutableSet<MutableMap.MutableEntry<K, V>> {
        return this.map.entries
    }

    companion object {
        fun <K, V> of(map: MutableMap<K, V>, function: Function<K, V>): LoadingMap<K, V> {
            return LoadingMap(map, function)
        }

        fun <K, V> of(function: Function<K, V>): LoadingMap<K, V> {
            return of(ConcurrentHashMap(), function)
        }
    }
}
