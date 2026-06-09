package chihalu.endrrta.server;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record PracticeDestination(String scenarioId, String label, ResourceKey<Level> dimension, BlockPos pos) {
}
