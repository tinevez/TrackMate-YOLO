/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2021 - 2023 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.yolo;

import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readStringAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeTargetChannel;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;
import static fiji.plugin.trackmate.util.cli.CondaCLIConfigurator.KEY_CONDA_ENV;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.jdom2.Element;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.detection.SpotGlobalDetector;
import fiji.plugin.trackmate.detection.SpotGlobalDetectorFactory;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.util.cli.TrackMateSettingsBuilder;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotDetectorFactory.class, priority = Priority.LOW )
public class YOLODetectorFactory< T extends RealType< T > & NativeType< T > > implements SpotGlobalDetectorFactory< T >
{

	/*
	 * CONSTANTS
	 */

	/**
	 * The key to the parameter that stores the path to the custom model file to
	 * use with YOLO. It must be an absolute file path.
	 */
	public static final String KEY_YOLO_MODEL_FILEPATH = "YOLO_MODEL_FILEPATH";

	public static final String DEFAULT_YOLO_MODEL_FILEPATH = "";

	/**
	 * Key for the parameters that sets the minimum confidence threshold for
	 * detections.
	 */
	public static final String KEY_YOLO_CONF = "YOLO_CONF_THRESHOLD";

	public static final double DEFAULT_YOLO_CONF = 0.25;

	/**
	 * Key for the parameters that sets the IoU threshold for Non-Maximum
	 * Suppression.
	 */
	public static final String KEY_YOLO_IOU = "YOLO_IOU_THRESHOLD";

	public static final double DEFAULT_YOLO_IOU = 0.7;

	/**
	 * The key to the parameter that stores the logger instance, to which
	 * Cellpose messages wil be sent. Values must be implementing
	 * {@link Logger}. This parameter won't be serialized.
	 */
	public static final String KEY_LOGGER = "LOGGER";

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "YOLO_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "YOLO detector";

	public static final ImageIcon ICON;
	static
	{
		final URL resource = GuiUtils.getResource( "images/YOLO-logo.png", YOLODetectorFactory.class );
		ICON = new ImageIcon( resource );
	}

	public static final String DOC_YOLO_URL = "https://imagej.net/plugins/trackmate/detectors/trackmate-yolo";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This detector relies on YOLO to detect objects."
			+ "<p>"
			+ "The detector simply calls an external YOLO installation. So for this "
			+ "to work, you must have a YOLO installation running on your computer. "
			+ "<p>"
			+ "If you use this detector for your work, please be so kind as to "
			+ "also cite the YOLO github repo: <a href=\"https://github.com/ultralytics/ultralytics\">"
			+ "Jocher, G., Qiu, J., & Chaurasia, A. (2023). "
			+ "Ultralytics YOLO. https://github.com/ultralytics/ultralytics</a>"
			+ "<p>"
			+ "Documentation for this module "
			+ "<a href=\"" + DOC_YOLO_URL + "\">on the ImageJ Wiki</a>."
			+ "</html>";

	/*
	 * FIELDS
	 */

	/** The image to operate on. Multiple frames. */
	protected ImgPlus< T > img;

	protected Map< String, Object > settings;

	protected String errorMessage;

	/*
	 * METHODS
	 */

	@Override
	public SpotGlobalDetector< T > getDetector( final Interval interval )
	{
		// Logger.
		final Logger logger = ( Logger ) settings.get( KEY_LOGGER );
		// CLI.
		final YOLOCLI cli = new YOLOCLI();
		TrackMateSettingsBuilder.fromTrackMateSettings( settings, cli );

		final YOLODetector< T > detector = new YOLODetector<>(
				img,
				interval,
				cli,
				logger );
		return detector;
	}

	@Override
	public boolean forbidMultithreading()
	{
		return true;
	}

	@Override
	public boolean setTarget( final ImgPlus< T > img, final Map< String, Object > settings )
	{
		this.img = img;
		this.settings = settings;
		return checkSettings( settings );
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public boolean marshall( final Map< String, Object > settings, final Element element )
	{
		final StringBuilder errorHolder = new StringBuilder();
		boolean ok = writeTargetChannel( settings, element, errorHolder );
		ok = ok & writeAttribute( settings, element, KEY_CONDA_ENV, String.class, errorHolder );
		ok = ok & writeAttribute( settings, element, KEY_YOLO_MODEL_FILEPATH, String.class, errorHolder );
		ok = ok & writeAttribute( settings, element, KEY_YOLO_CONF, Double.class, errorHolder );
		ok = ok & writeAttribute( settings, element, KEY_YOLO_IOU, Double.class, errorHolder );

		if ( !ok )
			errorMessage = errorHolder.toString();

		return ok;
	}

	@Override
	public boolean unmarshall( final Element element, final Map< String, Object > settings )
	{
		settings.clear();
		final StringBuilder errorHolder = new StringBuilder();
		boolean ok = true;
		ok = ok & readStringAttribute( element, settings, KEY_CONDA_ENV, errorHolder );
		ok = ok & readStringAttribute( element, settings, KEY_YOLO_MODEL_FILEPATH, errorHolder );
		ok = ok & readDoubleAttribute( element, settings, KEY_YOLO_CONF, errorHolder );
		ok = ok & readDoubleAttribute( element, settings, KEY_YOLO_IOU, errorHolder );

		return checkSettings( settings );
	}

	@Override
	public ConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		return new YOLODetectorConfigurationPanel( model, settings );
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > settings = new HashMap<>();
		settings.put( KEY_CONDA_ENV, "" );
		settings.put( KEY_YOLO_MODEL_FILEPATH, DEFAULT_YOLO_MODEL_FILEPATH );
		settings.put( KEY_YOLO_CONF, DEFAULT_YOLO_CONF );
		settings.put( KEY_YOLO_IOU, DEFAULT_YOLO_IOU );
		settings.put( KEY_LOGGER, Logger.DEFAULT_LOGGER );
		return settings;
	}

	@Override
	public boolean checkSettings( final Map< String, Object > settings )
	{
		boolean ok = true;
		final StringBuilder errorHolder = new StringBuilder();
		ok = ok & checkParameter( settings, KEY_CONDA_ENV, String.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_YOLO_MODEL_FILEPATH, String.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_YOLO_CONF, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_YOLO_IOU, Double.class, errorHolder );

		// If we have a logger, test it is of the right class.
		final Object loggerObj = settings.get( KEY_LOGGER );
		if ( loggerObj != null && !Logger.class.isInstance( loggerObj ) )
		{
			errorHolder.append( "Value for parameter " + KEY_LOGGER + " is not of the right class. "
					+ "Expected " + Logger.class.getName() + ", got " + loggerObj.getClass().getName() + ".\n" );
			ok = false;
		}

		final List< String > mandatoryKeys = Arrays.asList( KEY_CONDA_ENV, KEY_YOLO_MODEL_FILEPATH, KEY_YOLO_CONF, KEY_YOLO_IOU );
		final List< String > optionalKeys = Arrays.asList( KEY_LOGGER );

		ok = ok & checkMapKeys( settings, mandatoryKeys, optionalKeys, errorHolder );
		if ( !ok )
			errorMessage = errorHolder.toString();

		return ok;
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public String getKey()
	{
		return DETECTOR_KEY;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public boolean has2Dsegmentation()
	{
		return false;
	}

	@Override
	public SpotDetectorFactoryBase< T > copy()
	{
		return new YOLODetectorFactory<>();
	}
}
