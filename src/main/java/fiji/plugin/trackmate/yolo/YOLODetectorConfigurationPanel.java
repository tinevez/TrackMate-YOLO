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
import static fiji.plugin.trackmate.yolo.YOLODetectorFactory.KEY_LOGGER;
import static fiji.plugin.trackmate.yolo.YOLODetectorFactory.KEY_YOLO_CONF;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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

		final GridBagLayout gridBagLayout = new GridBagLayout();
		setLayout( gridBagLayout );
		gridBagLayout.columnWidths = new int[] { 180 };
		gridBagLayout.columnWeights = new double[] { 1. };

		/*
		 * HEADER
		 */

		final JPanel header = new JPanel();
		header.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		header.setLayout( new BoxLayout( header, BoxLayout.Y_AXIS ) );

		final JLabel lblDetector = new JLabel( TITLE, ICON, JLabel.RIGHT );
		lblDetector.setFont( BIG_FONT );
		lblDetector.setHorizontalAlignment( SwingConstants.CENTER );
		lblDetector.setAlignmentX( JLabel.CENTER_ALIGNMENT );
		header.add( lblDetector );
		header.add( Box.createVerticalStrut( 5 ) );
		header.add( GuiUtils.infoDisplay( "<html>" + YOLODetectorFactory.INFO_TEXT + "</html>", false ) );

		final GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = 0;
		gc.gridy = 0;
		gc.fill = GridBagConstraints.BOTH;
		add( header, gc );

		/*
		 * CONFIG
		 */

		this.mainPanel = YOLOCLI.build( cli );
		final JScrollPane scrollPane = new JScrollPane( mainPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
//		scrollPane.setBorder( null );
		scrollPane.getVerticalScrollBar().setUnitIncrement( 16 );

		gc.gridy++;
		gc.weighty = 1.;
		gc.weightx = 0.;
		add( scrollPane, gc );

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

		gc.gridy++;
		gc.weighty = 0.;
		add( p, gc );
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
