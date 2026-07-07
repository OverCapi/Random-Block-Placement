package capi.rnd_block_placer.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {

	@Invoker("ensureHasSentCarriedItem")
	void callEnsureHasSentCarriedItem();
}
