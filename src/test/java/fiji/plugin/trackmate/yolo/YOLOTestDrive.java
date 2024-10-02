package fiji.plugin.trackmate.yolo;

import net.imagej.patcher.LegacyInjector;

public class YOLOTestDrive
{
	public static void main( final String[] args )
	{
		LegacyInjector.preinit();
		YOLOPlaygroundEntry.run();
	}
}
