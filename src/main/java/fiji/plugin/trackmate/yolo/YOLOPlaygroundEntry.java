package fiji.plugin.trackmate.yolo;

import net.imagej.patcher.LegacyInjector;

public class YOLOPlaygroundEntry
{

	public static void main( final String... args )
	{
		LegacyInjector.preinit();
		YOLOPlayground.main( args );
	}
}
