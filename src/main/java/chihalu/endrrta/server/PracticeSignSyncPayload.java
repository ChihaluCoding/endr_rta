package chihalu.endrrta.server;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import chihalu.endrrta.EndrRTA;

public record PracticeSignSyncPayload(boolean pending) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PracticeSignSyncPayload> TYPE = new CustomPacketPayload.Type<>(
			Identifier.parse(EndrRTA.MOD_ID + ":practice_sign_pending")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, PracticeSignSyncPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL,
			PracticeSignSyncPayload::pending,
			PracticeSignSyncPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
