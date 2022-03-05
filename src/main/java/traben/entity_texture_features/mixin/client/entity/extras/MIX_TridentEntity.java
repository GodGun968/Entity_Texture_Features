package traben.entity_texture_features.mixin.client.entity.extras;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static traben.entity_texture_features.client.ETF_CLIENT.UUID_TridentName;

@Mixin(TridentEntity.class)
public abstract class MIX_TridentEntity {

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V", at = @At("TAIL"))
    public void injected(World world, LivingEntity owner, ItemStack stack, CallbackInfo ci) {
        if (stack.hasCustomName()) {
            UUID_TridentName.put(((TridentEntity)(Object)this).getUuid(), stack.getName().getString());
        } else {
            UUID_TridentName.put(((TridentEntity)(Object)this).getUuid(), null);
        }
    }
}
