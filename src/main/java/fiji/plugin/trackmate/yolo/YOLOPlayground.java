package fiji.plugin.trackmate.yolo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.util.cli.CLIUtils;
import fiji.plugin.trackmate.util.cli.CommandBuilder;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;

public class YOLOPlayground
{

	final static String BASE_ERROR_MESSAGE = "[YOLO] ";

	private static final String OUTPUT_FOLDER_NAME = "output";

	final static Logger logger = Logger.DEFAULT_LOGGER;

	private String errorMessage;

	public static void main( final String[] args )
	{
		final String path = "/Users/tinevez/Desktop/BacterialDynamicsDataset/SHichamDataset/YOLO-training-set/t:1:463 - Video_2.czi #1.tif";
		final YOLOPlayground yolo = new YOLOPlayground();
		if ( !yolo.run( path ) )
			System.err.println( yolo.errorMessage );
		System.out.println( "Finished!" );
	}

	private final boolean run( final String path )
	{
		final YOLOCLI cli = new YOLOCLI();
		final ImagePlus imp = IJ.openImage( path );

		Path imgTmpFolder;
		final Path outputTmpFolder;
		try
		{
			// Tmp image folder.
			imgTmpFolder = Files.createTempDirectory( "TrackMate-YOLO-imgs_" );
			CLIUtils.recursiveDeleteOnShutdownHook( imgTmpFolder );
			logger.setStatus( "Saving source image" );
			logger.log( "Saving source image to " + imgTmpFolder + "\n" );
			final boolean ok = writeStackList( imp, -1, imgTmpFolder.toString(), "-t", logger );
			if ( !ok )
			{
				errorMessage = BASE_ERROR_MESSAGE + "Problem saving image frames.\n";
				return false;
			}

			// Tmp output folder.
			outputTmpFolder = imgTmpFolder.resolve( OUTPUT_FOLDER_NAME );
			CLIUtils.recursiveDeleteOnShutdownHook( outputTmpFolder );
		}
		catch ( final IOException e )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Could not create temp folder to save input image:\n" + e.getMessage();
			return false;
		}

		cli.imageFolder().set( imgTmpFolder.toString() );
		cli.outputFolder().set( outputTmpFolder.toString() );

		// Check validity of the CLI.
		final String error = cli.check();
		final boolean ok = error == null;
		if ( !ok )
		{
			errorMessage = BASE_ERROR_MESSAGE + error;
			return false;
		}

		final String executableName = cli.getCommand();
		final List< String > cmd = CommandBuilder.build( cli );
		logger.setStatus( "Running " + executableName );
		logger.log( "Running " + executableName + " with args:\n" );
		cmd.forEach( t -> {
			if ( t.contains( File.separator ) )
				logger.log( t + ' ' );
			else
				logger.log( t + ' ', Logger.GREEN_COLOR.darker() );
		} );
		logger.log( "\n" );

		// Execute it.
		// TODO

		return true;
	}

	public static boolean writeStackList( final ImagePlus imp, final int c, final String folder, final String suffix, final Logger logger )
	{
		final int nT = imp.getNFrames();
		final int nZ = imp.getNSlices();
		final int firstC = c < 0 ? 1 : c;
		final int lastC = c < 0 ? imp.getNChannels() : c;

		for ( int t = 1; t <= nT; t++ )
		{
			final String name = String.format( imp.getShortTitle() + suffix + "%04d", t );
			final ImagePlus dup = new Duplicator().run( imp, firstC, lastC, 1, nZ, t, t );
			dup.setTitle( name );
			final String path = folder + File.separator + name + ".tif";
			final boolean ok = IJ.saveAsTiff( dup, path );
			if ( !ok )
			{
				logger.error( "Problem saving to " + path + '\n' );
				return false;
			}
		}
		return true;
	}

}
