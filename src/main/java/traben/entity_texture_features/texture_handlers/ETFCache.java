package traben.entity_texture_features.texture_handlers;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import org.jetbrains.annotations.Nullable;

public class ETFCache<X, Y> {

    //cache with lru
    final Object2ObjectLinkedOpenHashMap<X, Y> cache;
    final int capacity;

    public ETFCache(int capacity) {
        this.cache = new Object2ObjectLinkedOpenHashMap<>(capacity);
        this.capacity = capacity - 1;
    }

    public boolean containsKey(X key) {
        return this.cache.containsKey(key);
//        if (cache.containsKey(key)) {
//            cache.putAndMoveToFirst(key, cache.get(key));
//            return true;
//        } else {
//            return false;
//        }
    }

    @Nullable
    public Y get(X key) {
        return cache.getAndMoveToFirst(key);

    }

    public void put(X key, Y value) {
        if (cache.size() >= capacity) {
            cache.removeLast();
        }
        cache.putAndMoveToFirst(key, value);
    }

    public void clear() {
        cache.clear();
    }


    public int size() {
        return cache.size();
    }

}
