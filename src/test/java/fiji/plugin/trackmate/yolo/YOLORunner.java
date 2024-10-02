package fiji.plugin.trackmate.yolo;

import net.imagej.patcher.LegacyInjector;

public class YOLORunner
{

	public static void main( final String[] args )
	{
		LegacyInjector.preinit();
		YOLOTrackMateTestDrive.main( args );
	}
}
