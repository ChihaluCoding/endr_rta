package chihalu.endrrta.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import chihalu.endrrta.server.PracticeSignSyncState;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@Inject(method = "handleOpenSignEditor", at = @At("HEAD"), cancellable = true)
	private void endrrta$cancelPracticeSignEditor(ClientboundOpenSignEditorPacket packet, CallbackInfo info) {
		if (PracticeSignSyncState.hasPendingSign() || endrrta$isPracticeManagedSign(packet)) {
			info.cancel();
		}
	}

	private boolean endrrta$isPracticeManagedSign(ClientboundOpenSignEditorPacket packet) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || !(minecraft.level.getBlockEntity(packet.getPos()) instanceof SignBlockEntity sign)) {
			return false;
		}
		return endrrta$isManagedText(sign.getFrontText()) || endrrta$isManagedText(sign.getBackText());
	}

	private boolean endrrta$isManagedText(SignText text) {
		for (int index = 0; index < 4; index++) {
			String value = text.getMessage(index, false).getString().trim();
			if ("ゲーム選択".equals(value) || "練習選択".equals(value) || "個別練習".equals(value) || "[EndrRTA]".equalsIgnoreCase(value)) {
				return true;
			}
		}
		return false;
	}
}
