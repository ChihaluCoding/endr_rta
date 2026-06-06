package chihalu.endrrta.server;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

import chihalu.endrrta.EndrRTA;

public final class EndrRTATags {
	public static final TagKey<Structure> NETHER_FORTRESS = TagKey.create(
			Registries.STRUCTURE,
			Identifier.parse(EndrRTA.MOD_ID + ":nether_fortress")
	);

	private EndrRTATags() {
	}
}
