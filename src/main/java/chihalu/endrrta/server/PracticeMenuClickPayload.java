package chihalu.endrrta.server;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import chihalu.endrrta.EndrRTA;

public record PracticeMenuClickPayload(int slot) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PracticeMenuClickPayload> TYPE = new CustomPacketPayload.Type<>(
			Identifier.parse(EndrRTA.MOD_ID + ":practice_menu_click")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, PracticeMenuClickPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			PracticeMenuClickPayload::slot,
			PracticeMenuClickPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
