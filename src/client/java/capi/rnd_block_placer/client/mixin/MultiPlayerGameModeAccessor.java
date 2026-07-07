package capi.rnd_block_placer.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;

// Mixin accessor to expose the private ensureHasSentCarriedItem method
@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {

	// Calls ensureHasSentCarriedItem() on the target class to sync the held item with the server
	@Invoker("ensureHasSentCarriedItem")
	void callEnsureHasSentCarriedItem();
}
