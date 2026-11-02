package maihatsuu.nocturne.mixin;

import maihatsuu.nocturne.config.NocturneConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Disables the vanilla insomnia phantom spawner. The doInsomnia gamerule and the
 * TIME_SINCE_REST stat are left untouched; they just have no effect.
 */
@Mixin(PhantomSpawner.class)
public class PhantomSpawnerMixin {
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void nocturne$disableOverworldPhantoms(ServerLevel level, boolean spawnEnemies, boolean spawnFriendlies, CallbackInfoReturnable<Integer> cir) {
		if (NocturneConfig.get().disableOverworldPhantoms) {
			cir.setReturnValue(0);
		}
	}
}
