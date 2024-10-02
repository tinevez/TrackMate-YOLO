package fiji.plugin.trackmate.yolo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.input.TailerListenerAdapter;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.TMUtils;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.Interval;
import net.imglib2.img.display.imagej.CalibrationUtils;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class YOLOUtils
{

	/**
	 * Properly wraps an {@link ImgPlus} in a {@link ImagePlus}, ensuring that
	 * the dimensionality and the calibration of the output matches the input.
	 * 
	 * @param title
	 */
	public static < T extends RealType< T > & NativeType< T > > ImagePlus wrap( final ImgPlus< T > img )
	{
		final ImagePlus imp2 = ImageJFunctions.wrap( img, img.getName() );
		CalibrationUtils.copyCalibrationToImagePlus( img, imp2 );

		// Fix dimensionality
		final int zIndex = img.dimensionIndex( Axes.Z );
		final int cIndex = img.dimensionIndex( Axes.CHANNEL );
		final int tIndex = img.dimensionIndex( Axes.TIME );
		imp2.setDimensions(
				cIndex < 0 ? 1 : ( int ) img.dimension( cIndex ),
				zIndex < 0 ? 1 : ( int ) img.dimension( zIndex ),
				tIndex < 0 ? 1 : ( int ) img.dimension( tIndex ) );
		return imp2;
	}

	/**
	 * Saves the specified {@link ImagePlus} so that it can be processed by an
	 * external process.
	 * 
	 * @param imp
	 *            the image to save.
	 * @param c
	 *            the channel to extract before saving. If negative, all
	 *            channels will be saved.
	 * @param folder
	 *            the folder in which to save
	 * @param suffix
	 * @param logger
	 * @return
	 */
	public static < T extends RealType< T > & NativeType< T > > boolean resaveSingleTimePoints( final ImgPlus< T > img, final int c, final String folder, final String suffix, final Logger logger )
	{
		final ImagePlus imp = wrap( img );
		return resaveSingleTimePoints( imp, c, folder, suffix, logger );
	}

	/**
	 * Saves the specified {@link ImagePlus} so that it can be processed by an
	 * external process.
	 * 
	 * @param imp
	 *            the image to save.
	 * @param c
	 *            the channel to extract before saving. If negative, all
	 *            channels will be saved.
	 * @param folder
	 *            the folder in which to save
	 * @param suffix
	 * @param logger
	 * @return
	 */
	public static boolean resaveSingleTimePoints( final ImagePlus imp, final int c, final String folder, final String suffix, final Logger logger )
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

	public static final Function< Long, String > nameGen = ( frame ) -> String.format( "%d", frame );

	/**
	 * Splits the input image in a list of {@link ImagePlus}, one per
	 * time-point. If the input includes several channels, they are all included
	 * in the new image, and put as the last dimension.
	 * 
	 * @param <T>
	 *            the type of the pixel in the input image.
	 * @param img
	 *            the input image.
	 * @param interval
	 *            the interval to crop the output in the input image. Must not
	 *            have a dimension for channels. Can be 2D or 3D to accommodate
	 *            the input image. If the interval contains time (min T and max
	 *            T to export), it must be in the last dimension of the
	 *            interval.
	 * @param nameGen
	 *            a generator for the name of the output ImagePlus.
	 * @return a new list of ImagePlus.
	 */
	public static final < T extends RealType< T > & NativeType< T > > List< ImagePlus > splitSingleTimePoints(
			final ImgPlus< T > img,
			final Interval interval,
			final Function< Long, String > nameGen )
	{
		final int zIndex = img.dimensionIndex( Axes.Z );
		final int cIndex = img.dimensionIndex( Axes.CHANNEL );
		final Interval cropInterval;
		if ( zIndex < 0 )
		{
			// 2D
			if ( cIndex < 0 )
				cropInterval = Intervals.createMinMax(
						interval.min( 0 ), interval.min( 1 ),
						interval.max( 0 ), interval.max( 1 ) );
			else
				// Include all channels
				cropInterval = Intervals.createMinMax(
						interval.min( 0 ), interval.min( 1 ), img.min( cIndex ),
						interval.max( 0 ), interval.max( 1 ), img.max( cIndex ) );
		}
		else
		{
			if ( cIndex < 0 )
				cropInterval = Intervals.createMinMax(
						interval.min( 0 ), interval.min( 1 ), interval.min( 2 ),
						interval.max( 0 ), interval.max( 1 ), interval.max( 2 ) );
			else
				cropInterval = Intervals.createMinMax(
						interval.min( 0 ), interval.min( 1 ), interval.min( 2 ), img.min( cIndex ),
						interval.max( 0 ), interval.max( 1 ), interval.max( 2 ), img.max( cIndex ) );
		}

		final List< ImagePlus > imps = new ArrayList<>();
		final int timeIndex = img.dimensionIndex( Axes.TIME );
		if ( timeIndex < 0 )
		{
			// No time.
			final IntervalView< T > crop = Views.interval( img, cropInterval );
			final String name = nameGen.apply( 0l ) + ".tif";
			imps.add( ImageJFunctions.wrap( crop, name ) );
		}
		else
		{
			// In the interval, time is always the last.
			final long minT = interval.min( interval.numDimensions() - 1 );
			final long maxT = interval.max( interval.numDimensions() - 1 );
			for ( long t = minT; t <= maxT; t++ )
			{
				final ImgPlus< T > tpTCZ = ImgPlusViews.hyperSlice( img, timeIndex, t );

				// Put if necessary the channel axis as the last one (CellPose
				// format)
				final int chanDim = tpTCZ.dimensionIndex( Axes.CHANNEL );
				ImgPlus< T > tp = tpTCZ;
				if ( chanDim > 1 )
				{
					tp = ImgPlusViews.moveAxis( tpTCZ, chanDim, tpTCZ.numDimensions() - 1 );
				}
				// possibly 2D or 3D with or without channel.
				final IntervalView< T > crop = Views.interval( tp, cropInterval );
				final String name = nameGen.apply( t ) + ".tif";
				imps.add( ImageJFunctions.wrap( crop, name ) );
			}
		}
		return imps;
	}

	/**
	 * A tailer listener that parse YOLO log to fetch when an image has been
	 * processed, and increase the progress counter.
	 */
	public static class YOLOTailerListener extends TailerListenerAdapter
	{
		private final Logger logger;

		private final int nTodos;

		private int nDone;

		private final static Pattern IMAGE_NUMBER_PATTERN = Pattern.compile( "^image \\d+/\\d+.*" );

		public YOLOTailerListener( final Logger logger, final int nTodos )
		{
			this.logger = logger;
			this.nTodos = nTodos;
			this.nDone = 0;
		}

		@Override
		public void handle( final String line )
		{
			final Matcher matcher = IMAGE_NUMBER_PATTERN.matcher( line );

			if ( matcher.matches() )
			{
				// Simply increment the 'done' counter.
				nDone++;
				logger.setProgress( ( double ) nDone / nTodos );
			}
			else
			{
				if ( !line.trim().isEmpty() )
					logger.log( " - " + line + '\n' );
			}
		}
	}

	/**
	 * Import the text results files generated by the 'save_txt' option, and
	 * returns them as a list of spots. The radius of the spots is the mean of
	 * the width and height of the YOLO detections.
	 * <p>
	 * The YOLO results text file is made of one line per detection, and each
	 * line is formatted as follow:
	 * <p>
	 * <code>
	 * class_id center_x center_y width height confidence
	 * </code>
	 * <p>
	 * Where:
	 * <ul>
	 * <li>class_id is the class identifier of the detected object
	 * <li>center_x and center_y are the normalized coordinates of the center of
	 * the bounding box (values between 0 and 1)
	 * <li>width and height are the normalized width and height of the bounding
	 * box (values between 0 and 1).
	 * <li>(optional) confidence is the confidence score of the detection
	 * <ul>
	 *
	 * @param path
	 *            the path to the YOLO results file.
	 * @param interval
	 *            the interval in the input image that was passed to YOLO.
	 * @param calibration
	 *            the physical calibration of the input image.
	 * @param logger
	 *            a {@link Logger} to report error messages.
	 * @return a new list of spots.
	 */
	public static List< Spot > importResultFile(
			final String path,
			final Interval interval,
			final double[] calibration,
			final Logger logger )
	{
		final long width = interval.dimension( 0 );
		final long height = interval.dimension( 1 );
		final long x0 = interval.min( 0 );
		final long y0 = interval.min( 1 );

		final List< Spot > spots = new ArrayList<>();
		try (BufferedReader br = new BufferedReader( new FileReader( path ) ))
		{
			String line;
			int ln = 0;
			while ( ( line = br.readLine() ) != null )
			{
				ln++;
				final String[] values = line.split( " " );
				if ( values.length < 5 )
				{
					logger.error( "Line " + ln + " in file " + path + " as unexpected number of values. Should be at least 5, but was " + values.length + "." );
					continue;
				}
				// Center
				final double xr = Double.parseDouble( values[ 1 ].trim() );
				final double yr = Double.parseDouble( values[ 2 ].trim() );
				// Size
				final double wr = Double.parseDouble( values[ 3 ].trim() );
				final double hr = Double.parseDouble( values[ 4 ].trim() );

				// Global coords
				final double x = calibration[ 0 ] * ( x0 + xr * width );
				final double y = calibration[ 1 ] * ( y0 + yr * height );
				final double w = calibration[ 0 ] * wr * width;
				final double h = calibration[ 1 ] * hr * height;
				final double r = 0.5 * ( w + h ) / 2.;

				// Do we have confidence?
				final double quality;
				if ( values.length >= 5 )
					quality = Double.parseDouble( values[ 5 ].trim() );
				else
					quality = 1.;

				final Spot spot = new Spot( x, y, 0., r, quality );
				spots.add( spot );
			}
		}
		catch ( final IOException e )
		{
			logger.error( "Error reading the file " + path + "\n" + e.getMessage() + '\n' );
			e.printStackTrace();
			return Collections.emptyList();
		}

		return spots;
	}

	public static < T extends RealType< T > & NativeType< T > > void main( final String[] args )
	{
		final String path = "samples/SHicham_Video1_crop.tif";
		final ImagePlus imp = IJ.openImage( path );
		@SuppressWarnings( "unchecked" )
		final ImgPlus< T > img = TMUtils.rawWraps( imp );

		ImageJ.main( args );
		wrap( img ).show();
	}

}
