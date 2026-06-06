package chihalu.endrrta.server;

import net.minecraft.core.BlockPos;

public record RadarTarget(String label, BlockPos pos, double distance) {
}
