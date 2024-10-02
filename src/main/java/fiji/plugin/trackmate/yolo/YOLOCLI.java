/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2024 TrackMate developers.
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

import static fiji.plugin.trackmate.yolo.YOLODetectorFactory.DEFAULT_YOLO_MODEL_FILEPATH;
import static fiji.plugin.trackmate.yolo.YOLODetectorFactory.KEY_YOLO_MODEL_FILEPATH;

import fiji.plugin.trackmate.util.cli.CliGuiBuilder;
import fiji.plugin.trackmate.util.cli.CliGuiBuilder.CliConfigPanel;
import fiji.plugin.trackmate.util.cli.CondaExecutableCLIConfigurator;

public class YOLOCLI extends CondaExecutableCLIConfigurator
{

	private final PathArgument modelPath;

	private final PathArgument imageFolder;

	private final PathArgument outputFolder;

	public YOLOCLI()
	{
		this.modelPath = addPathArgument()
				.name( "Path to a YOLO model" )
				.argument( "model=" )
				.help( "The path to a YOLO model." )
				.defaultValue( DEFAULT_YOLO_MODEL_FILEPATH )
				.key( KEY_YOLO_MODEL_FILEPATH )
				.get();

		this.imageFolder = addPathArgument()
				.name( "Input image folder path" )
				.help( "Directory with series of .tif files." )
				.argument( "source=" )
				.visible( false )
				.required( true )
				.key( null )
				.get();

		this.outputFolder = addPathArgument()
				.name( "Output folder" )
				.help( "Path to write the text files containing the detection boxes." )
				.argument( "project=" )
				.visible( false )
				.required( true )
				.key( null )
				.get();

		addFlag()
				.name( "Save detections to text files" )
				.help( "Whether the detections will be saved as a text files." )
				.argument( "save_txt=" )
				.defaultValue( true )
				.required( true )
				.visible( false )
				.key( null )
				.get();

		addFlag()
				.name( "Save confidence with detection" )
				.help( "Whether results text files will include the confidence values." )
				.argument( "save_conf=" )
				.defaultValue( true )
				.required( true )
				.visible( false )
				.key( null )
				.get();

		addFlag()
				.name( "Save detections overlay images" )
				.help( "Whether the results will be saved as an image overlaid with results." )
				.argument( "save=" )
				.defaultValue( false ) // We don't want that.
				.required( true )
				.visible( false )
				.key( null )
				.get();
	}

	@Override
	protected String getCommand()
	{
		return "yolo detect predict";
	}

	public PathArgument modelPath()
	{
		return modelPath;
	}

	public PathArgument imageFolder()
	{
		return imageFolder;
	}

	public PathArgument outputFolder()
	{
		return outputFolder;
	}

	public static CliConfigPanel build( final YOLOCLI cli )
	{
		return CliGuiBuilder.build( cli );
	}
}
