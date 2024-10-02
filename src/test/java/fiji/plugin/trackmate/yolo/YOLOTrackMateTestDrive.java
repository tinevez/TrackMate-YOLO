package fiji.plugin.trackmate.yolo;

import fiji.plugin.trackmate.TrackMatePlugIn;
import ij.ImageJ;

public class YOLOTrackMateTestDrive
{
	public static void main( final String[] args )
	{
		// GuiUtils.setSystemLookAndFeel();
		ImageJ.main( args );
		new TrackMatePlugIn().run( "samples/SHicham_Video1_crop.tif" );
	}
}
