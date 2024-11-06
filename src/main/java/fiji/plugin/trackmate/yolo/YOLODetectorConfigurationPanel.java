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

import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.yolo.YOLODetectorFactory.DOC_YOLO_URL;
import static fiji.plugin.trackmate.yolo.YOLODetectorFactory.KEY_LOGGER;
import static fiji.plugin.trackmate.yolo.YOLODetectorFactory.KEY_YOLO_CONF;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.util.DetectionPreview;
import fiji.plugin.trackmate.util.DetectionPreviewPanel;
import fiji.plugin.trackmate.util.cli.CliGuiBuilder.CliConfigPanel;
import fiji.plugin.trackmate.util.cli.TrackMateSettingsBuilder;

public class YOLODetectorConfigurationPanel extends ConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	private static final String TITLE = YOLODetectorFactory.NAME;

	protected static final ImageIcon ICON = GuiUtils.scaleImage( YOLODetectorFactory.ICON, 64, 64 );

	private final YOLOCLI cli;

	private final CliConfigPanel mainPanel;

	private final Logger logger;

	public YOLODetectorConfigurationPanel( final Model model, final Settings settings )
	{
		this.cli = new YOLOCLI();
		this.logger = model.getLogger();

		setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		final BoxLayout layout = new BoxLayout( this, BoxLayout.PAGE_AXIS );
		setLayout( layout );

		/*
		 * HEADER
		 */

		final JLabel lblDetector = new JLabel( TITLE, ICON, JLabel.RIGHT );
		lblDetector.setFont( BIG_FONT );
		lblDetector.setHorizontalAlignment( SwingConstants.CENTER );
		lblDetector.setAlignmentX( JLabel.CENTER_ALIGNMENT );
		add( lblDetector );
		add( Box.createVerticalStrut( 5 ) );
		final JEditorPane infoDisplay = GuiUtils.infoDisplay( "<html>" + "Documentation for this module "
				+ "<a href=\"" + DOC_YOLO_URL + "\">on the ImageJ Wiki</a>."
				+ "</html>", false );
		infoDisplay.setMaximumSize( new Dimension( 100_000, 40 ) );
		add( infoDisplay );

		/*
		 * CONFIG
		 */

		this.mainPanel = YOLOCLI.build( cli );
		add( Box.createVerticalStrut( 20 ) );
		add( mainPanel );

		/*
		 * PREVIEW
		 */

		final DetectionPreview detectionPreview = DetectionPreview.create()
				.model( model )
				.settings( settings )
				.detectorFactory( getDetectorFactory() )
				.detectionSettingsSupplier( () -> getSettings() )
				.thresholdKey( KEY_YOLO_CONF )
				.thresholdUpdater( t -> {
					cli.confidenceThreshold().set( t );
					mainPanel.refresh();
				} )
				.axisLabel( "confidence" )
				.get();
		final DetectionPreviewPanel p = detectionPreview.getPanel();

		add( Box.createVerticalStrut( 10 ) );
		add( p );
	}

	protected SpotDetectorFactoryBase< ? > getDetectorFactory()
	{
		return new YOLODetectorFactory<>();
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		TrackMateSettingsBuilder.fromTrackMateSettings( settings, cli );
		mainPanel.refresh();
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final Map< String, Object > map = new HashMap<>();
		TrackMateSettingsBuilder.toTrackMateSettings( map, cli );
		map.put( KEY_LOGGER, logger );
		return map;
	}

	@Override
	public void clean()
	{}
}
