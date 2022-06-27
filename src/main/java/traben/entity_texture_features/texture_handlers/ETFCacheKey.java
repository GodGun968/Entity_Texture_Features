package traben.entity_texture_features.texture_handlers;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

record ETFCacheKey(UUID uuid, Identifier identifier) {

    //don't need to create new object every time
    private static final Identifier NULL_IDENTIFIER = new Identifier("null:null");

    ETFCacheKey(UUID uuid, @Nullable Identifier identifier) {
        this.uuid = uuid;
        this.identifier = identifier == null ? NULL_IDENTIFIER : identifier;
    }
    public UUID getMobUUID(){
        return this.uuid;
    }

    //required for hashmap comparison functionality
//    public boolean equals(Object obj) {
//        if(obj instanceof ETFCacheKey key) {
//            return (this.identifier.equals(key.identifier) && this.uuid.equals(key.uuid));
//        }else{
//            return false;
//        }
//    }
    //required for hashmap comparison functionality
//    public int hashCode(){
//        return uuid.hashCode() + identifier.hashCode();
//    }
//
//    @Override
//    public String toString() {
//        return "ETFCacheKey{"+uuid.toString()+","+identifier.toString()+"}";
//    }
}
