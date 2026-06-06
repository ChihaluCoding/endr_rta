package chihalu.endrrta.client.mixin;

import net.minecraft.client.gui.components.debugchart.ProfilerPieChart;
import net.minecraft.util.profiling.ProfileResults;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ProfilerPieChart.class)
public interface ProfilerPieChartAccessor {
	@Accessor("profilerPieChartResults")
	ProfileResults endrrta$profilerPieChartResults();

	@Accessor("profilerTreePath")
	String endrrta$profilerTreePath();

	@Accessor("bottomOffset")
	int endrrta$bottomOffset();
}
