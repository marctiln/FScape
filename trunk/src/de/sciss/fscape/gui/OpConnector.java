/*
 *  OpConnector.java
 *  FScape
 *
 *  Copyright (c) 2001-2008 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 */

package de.sciss.fscape.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import javax.swing.*;

import de.sciss.app.AbstractApplication;
import de.sciss.app.GraphicsHandler;
import de.sciss.fscape.op.*;
import de.sciss.fscape.spect.*;
import de.sciss.fscape.util.*;

/**
 *  GUI component representing the wire
 *	connection between two spectral operators.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 15-Nov-07
 */
public class OpConnector
extends JPanel // JComponent
implements Dragable
{
// -------- private Klassenvariablen --------

	private static final String ibName	= "images" + File.separator + "arrows.gif";	// IconBitmap
	private static final int ibWidth	= 19;			// Breite der Icons
	private static final int ibHeight	= 19;			// H�he der Icons	

	private static IconBitmap arrowib;

// -------- public Variablen --------

	public static final String OBJ_NAME = "OpConnector";

	public static final int ARROW_WIDTH		= ibWidth;
	public static final int ARROW_HEIGHT	= ibHeight;

	public static final int STATE_NORMAL	= 0;
	public static final int STATE_SELECTED	= 3;
	
// -------- Klassenkonstruktor --------

	static	// Icon-Bitmap laden
	{
		arrowib = new IconBitmap( ibName, ibWidth, ibHeight );
	}

// -------- private Variablen --------

	private static final int STATE_UNKNOWN	= -1;

	private int				state		= STATE_UNKNOWN;	// Status wie STATE_NORMAL, selektiert etc.

	private SpectStreamSlot	origin;
	private	OpIcon			srcIcon;
	private	OpIcon			destIcon;
	private	Point			srcLoc, destLoc;	// Icon top/left
	private int				anchor;				// -1 = src, +1 = dest; 0 = center

	private	Point			srcP, destP;	// arrow coord
	private	Point			thisP;			// Mittelpunkt dieser Componente

	private	String			labName		= null;
	private	int				width;			// of this component
	private int				height;

	private Font			fnt;
	private FontMetrics		fntMetr;

// -------- public Methoden ---------

	/**
	 *	NOTE: origin MUSS VERLINKT SEIN!
	 */
	public OpConnector( SpectStreamSlot origin )
	{
		super();
		
		setOpaque( true );

		Operator		srcOp, destOp;
		SpectStreamSlot	target;
		String			srcName;
		String			destName;
		Dimension		dim;
		Vector			slots;

		this.origin	= origin;
		target		= origin.getLinked();

		srcOp		= origin.getOwner();
		srcIcon		= (OpIcon) srcOp.getIcon();
		srcName		= origin.toString();
		if( srcName == Slots.SLOTS_DEFWRITER ) {
			srcName	= "";
		}
		
		destOp		= target.getOwner();
		destIcon	= (OpIcon) destOp.getIcon();
		destName	= target.toString();
		if( destName == Slots.SLOTS_DEFREADER ) {
			destName = "";
		}

		// dimension
		if( (srcName.length() > 0) || (destName.length() > 0) ) {	// Slotnamen zeichnen
			labName		= srcName + '>' + destName;
			newVisualProps();
		} else {
			width		= 8;
			height		= 8;
		}
//System.err.println( "width = "+width+"; height = "+height );

		// anchor
		slots	= srcOp.getSlots( Slots.SLOTS_WRITER );
		anchor	= (slots.size() > 1) ? -1 : 0;
		slots	= destOp.getSlots( Slots.SLOTS_READER );
		anchor += (slots.size() > 1) ? 1 : 0;

		srcLoc	= srcIcon.getLocation();
		dim		= srcIcon.getSize();
		srcP	= new Point( srcLoc.x + (dim.width >> 1), srcLoc.y + (dim.height >> 1) );
		destLoc	= destIcon.getLocation();
		dim		= destIcon.getSize();
		destP	= new Point( destLoc.x + (dim.width >> 1), destLoc.y + (dim.height >> 1) );
		thisP	= new Point( (destP.x + srcP.x - width) >> 1, (destP.y + srcP.y - height) >> 1 );
		
		setSize( width, height );
		setLocation( thisP.x, thisP.y );

		setSelected( STATE_NORMAL );
		setVisible( labName != null );
//		if( labName == null ) setVisible( false );

newVisualProps();
//		new DynamicAncestorAdapter( new DynamicPrefChangeManager(
//			AbstractApplication.getApplication().getUserPrefs(),
//			new String[] { MainPrefs.KEY_ICONFONT }, new LaterInvocationManager.Listener() {
//
//			public void laterInvocation( Object o )
//			{
//				newVisualProps();
//			}
//		})).addTo( this );

		// Event handling
		enableEvents( AWTEvent.FOCUS_EVENT_MASK );
		enableEvents( AWTEvent.MOUSE_EVENT_MASK );
		
		setFocusable( false );
	}

	/**
	 *	@return returns OBJ_NAME so you can identify it ('==' Operator)
	 */
	public String toString()
	{
		return OBJ_NAME;
	}

	/**
	 *	Liefert Ursprungsslot
	 */
	public SpectStreamSlot getOrigin()
	{
		return origin;
	}

	/**
	 *	Status veraendern
	 *
	 *	@return vorheriger Status
	 */
	public int setSelected( int state )
	{
		int lastState	= this.state;
		this.state		= state;

		if( lastState != state ) {
			if( state == CurvePanel.CP_STATE_NORMAL ) {
				setForeground( SystemColor.control );
				setBackground( SystemColor.control );
			} else {
				setForeground( OpIcon.selectColor );
				setBackground( OpIcon.selectColor );
			}
			repaint();
		}
		return lastState;
	}
	
	public int isSelected()
	{
		return state;
	}

	public void setLocation( int x, int y )
	{
		super.setLocation( x, y );
		
		if( isVisible() ) {
			calcArrow( srcIcon, this, srcP, thisP, 2, 0 );
			calcArrow( this, destIcon, thisP, destP, 0, 2 );
		} else {
			calcArrow( srcIcon, this, srcP, thisP, 2, 0 );
			calcArrow( srcIcon, destIcon, srcP, destP, 2, 2 );
		}
	}

//	public void setVisible( boolean visible )
//	{
//		Point loc = getLocation();
//
//		super.setVisible( visible );
//		setLocation( loc.x, loc.y );
//		repaint();
//	}

	public void adjustLocation()
	{
		Point	newThis	= getLocation();
		Point	newSrc	= srcIcon.getLocation();
		Point	newDest	= destIcon.getLocation();

		switch( anchor ) {
		case -1:
			newThis.translate( newSrc.x - srcLoc.x, newSrc.y - srcLoc.y );
			break;
		case 1:
			newThis.translate( newDest.x - destLoc.x, newDest.y - destLoc.y );
			break;
		default:
			newThis.translate( (newSrc.x - srcLoc.x + newDest.x - destLoc.x) >> 1,
							   (newSrc.y - srcLoc.y + newDest.y - destLoc.y) >> 1 );
			break;
		}
		if( newThis.x < 0 ) newThis.x = 0;
		if( newThis.y < 0 ) newThis.y = 0;
		
		srcLoc	= newSrc;
		destLoc	= newDest;
		setLocation( newThis.x, newThis.y );
	}

	public void paintComponent( Graphics g )
	{
		super.paintComponent( g );
//System.err.println( "paintComponent" );
	
		Dimension d = getSize();
	
		g.clearRect( 0, 0, d.width, d.height );
		g.draw3DRect( 1, 1, width - 3, height - 3, true );
		g.setColor( Color.black );
		g.drawRect( 0, 0, width - 1, height - 1 );
		
		if( labName != null ) {
//System.err.println( "drawing string "+labName );
			g.drawString( labName, 2, fntMetr.getAscent() );
		}
	}

	/**
	 *	Zeichnet den Pfeil zwischen den zum Connector
	 *	gehoerenden Icons; nimmt dazu getParent().getGraphics();
	 *
	 *	@param	mode	false, um statt mit schwarz zu zeichnen, den Pfeil zu loeschen
	 */	 
	public void drawArrow( boolean mode )
	{
		Container	c = getParent();
		Graphics2D	g;
		Point		tempP;

		if( c != null ) {
			g	= (Graphics2D) c.getGraphics();
			if( g != null ) {
				g.setRenderingHint( RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON );
				g.setColor( mode ? Color.black : c.getBackground() );
				if( isVisible() ) {
					g.drawLine( srcP.x, srcP.y, thisP.x, thisP.y );
					drawArrow( g, thisP.x, thisP.y, destP.x, destP.y, mode );
				} else {
					drawArrow( g, srcP.x, srcP.y, destP.x, destP.y, mode );
				}
				g.dispose();
			}

//			loc	= srcIcon.getLocation();
			g	= (Graphics2D) srcIcon.getGraphics();
			if( g != null ) {
				g.setRenderingHint( RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON );
				tempP = isVisible() ? thisP : destP;
				g.setColor( mode ? Color.black : c.getBackground() );
				g.drawLine( srcP.x - srcLoc.x, srcP.y - srcLoc.y,
							tempP.x - srcLoc.x, tempP.y - srcLoc.y );
				g.dispose();
			}

//			loc	= destIcon.getLocation();
			g	= (Graphics2D) destIcon.getGraphics();
			if( g != null ) {
				g.setRenderingHint( RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON );
				tempP = isVisible() ? thisP : srcP;
				g.setColor( mode ? Color.black : c.getBackground() );
				drawArrow( g, tempP.x - destLoc.x, tempP.y - destLoc.y,
							  destP.x - destLoc.x, destP.y - destLoc.y, mode );
				g.dispose();
				if( !mode ) destIcon.repaint();	// since we may have cleared a part of the icon
			}
		}
	}

	/**
	 *	Zeichnet einen Pfeil mit den exakt angegebenen Koordinaten
	 *
	 *	@param	mode	false, um zu loeschen (akt. Farbe muss Hintergrundfarbe sein!)
	 */
	public static void drawArrow( Graphics2D g, int srcX, int srcY, int destX, int destY, boolean mode )
	{
		g.drawLine( srcX, srcY, destX, destY );

		if( mode ) {
			int	ID = (((int) Math.rint( (Math.atan2( destX - srcX, -destY + srcY )
									   / Math.PI * 24.0 ))) + 48) % 48;

			arrowib.paint( g, ID, destX - (ARROW_WIDTH >> 1), destY - (ARROW_HEIGHT >> 1) );
		
		} else {
		
			g.fillRect( destX - (ARROW_WIDTH >> 1), destY - (ARROW_HEIGHT >> 1),
						ARROW_WIDTH, ARROW_HEIGHT );
		}
	}

	/**
	 *	Kalkuliert einen Link Pfeil und speichert seinen Anfangs- und Endpunkt
	 *	in den uebergebenen Point-Objekten
	 *
	 *	@param	src			Componente, von dem der Pfeil ausgeht
	 *	@param	dest		Zielcomponente
	 *	@param	srcLoc		dort wird der Anfangspunkt des Pfeils gespeichert
	 *	@param	destLoc		dort wird der Anfangspunkt des Pfeils gespeichert
	 *	@param	srcDist		positive Werte verschieben den Punkt ausserhalb der
	 *						Quellcomponente, so dass noch "srcDist" Pixel Abstand
	 *						dazwischen frei ist
	 *	@param	destDist	dito fuer Zielpunkt
	 */
	public static void calcArrow( Component src, Component dest, Point srcLoc, Point destLoc,
								  int srcDist, int destDist )
	{
		Rectangle	srcB	= src.getBounds();
		Rectangle	destB	= dest.getBounds();
		double		arc;
		double		dist;
		double		cos, sin;
		String		name;
		Rectangle	labB;

		srcLoc.setLocation( srcB.x + (srcB.width >> 1),			// Mittelpunkt
							srcB.y + (srcB.height >> 1) );
		destLoc.setLocation( destB.x + (destB.width >> 1),
							 destB.y + (destB.height >> 1) );

		arc = Math.atan2( destLoc.x - srcLoc.x, -destLoc.y + srcLoc.y ) - Constants.PIH;
		cos = Math.cos( arc );
		sin = Math.sin( arc );

		if( srcDist > 0 ) {
			name = src.toString();
			if( name == OpIcon.OBJ_NAME ) {		// "runde" Form
				srcLoc.translate( (int) (((srcB.width >> 1)  + srcDist) * cos),
								  (int) (((srcB.height >> 1) + srcDist) * sin) );
			} else {							// "eckig"
				if( cos == 0 ) {
					dist = srcLoc.y - srcB.y;
				} else if( sin == 0 ) {
					dist = srcLoc.x - srcB.x;
				} else {
					dist = Math.min(
								((cos<0) ? (srcB.x-srcLoc.x) : (srcB.x+srcB.width-srcLoc.x)) / cos,
								((sin>0) ? (srcLoc.y-srcB.y) : (srcLoc.y-srcB.y-srcB.height)) / sin );
				}
				srcLoc.translate( (int) ((dist + srcDist) * cos), (int) ((dist + srcDist) * sin) );
			}
		}

dest:	if( destDist > 0 ) {

			name = dest.toString();
			if( name == OpIcon.OBJ_NAME ) {		// "runde" Form
				destLoc.translate( (int) -(((destB.width >> 1)  + destDist + (ARROW_WIDTH>>1)) * cos),
								   (int) -(((destB.height >> 1) + destDist + (ARROW_HEIGHT>>1)) * sin) );

				if( (arc >= 0.0) || (arc <= -Math.PI )) break dest;

				// JLabel �berpr�fen
				labB	= ((OpIcon) dest).getLabel().getBounds();
				dist	= labB.y + (labB.height >> 1) - destLoc.y - (ARROW_HEIGHT>>2)*sin;

				if( labB.contains( destLoc.x + (int) (dist / Math.tan( arc )),
								   destLoc.y + (int) dist )) {

					dist += (labB.height >> 1) + (ARROW_HEIGHT >> 1);
					destLoc.translate( (int) (dist / Math.tan( arc )), (int) dist );
				}

			} else {							// "eckig"
				if( cos == 0 ) {
					dist = destLoc.y - destB.y;
				} else if( sin == 0 ) {
					dist = destLoc.x - destB.x;
				} else {
					dist = Math.min(
								((cos<0) ? (destB.x-destLoc.x) : (destB.x+destB.width-destLoc.x)) / cos,
								((sin>0) ? (destLoc.y-destB.y) : (destLoc.y-destB.y-destB.height)) / sin );
				}
				destLoc.translate( (int) -((dist + destDist) * cos), (int) -((dist + destDist) * sin) );
			}
		}
	}

	/**
	 *	Ermittelt die Distanz eines Punktes zu einem Link
	 *
	 *	@return	Distanz in Pixels. Negativer Wert = angegebener Punkt liegt zu weit weg
	 */
	public static int getDistance( OpConnector con, int x, int y ) 
	{
		int			x1, y1, x2, y2;
		double		projectX, projectY;
		int			dist	= -1;
		double		offset;
		double		arc;
		Rectangle	rect	= new Rectangle();
		Point		startP[];
		Point		endP[];
		
		if( con.isVisible() ) {
		
			startP		= new Point[ 2 ];
			endP		= new Point[ 2 ];
			
			startP[ 0 ]	= con.srcP;
			startP[ 1 ]	= con.thisP;
			endP[ 0 ]	= con.thisP;
			endP[ 1 ]	= con.destP;
		
		} else {	// direkt src/dest verknuepfen

			startP		= new Point[ 1 ];
			endP		= new Point[ 1 ];
			
			startP[ 0 ]	= con.srcP;
			endP[ 0 ]	= con.destP;
		}
				
		for( int i = 0; i < startP.length; i++ ) {

			x1		= Math.min( startP[ i ].x, endP[ i ].x );
			y1		= Math.min( startP[ i ].y, endP[ i ].y );
			x2		= Math.max( startP[ i ].x, endP[ i ].x );
			y2		= Math.max( startP[ i ].y, endP[ i ].y );
			// mind. 8 x 8 wg. z.B. 90 Grad Winkel!
			rect.setBounds( x1 - 4, y1 - 4, (x2 - x1) + 8, (y2 - y1) + 8 );

			if( rect.contains( x, y )) {		// erste Abschaetzung

				offset	= Math.sqrt( (x - startP[ i ].x) * (x - startP[ i ].x) +
									 (y - startP[ i ].y) * (y - startP[ i ].y) );
				arc		= Math.atan2( endP[ i ].x - startP[ i ].x,
									  -endP[ i ].y + startP[ i ].y ) - Math.PI/2;

				projectX = startP[ i ].x + offset * Math.cos( arc );
				projectY = startP[ i ].y + offset * Math.sin( arc );

				offset = Math.sqrt( (projectX - x) * (projectX - x) +
									(projectY - y) * (projectY - y) );

				if( (dist == -1) || (offset < dist) ) {
					dist = (int) offset;
				}
			}
		}

		return dist;
	}
		
// -------- Dragable Methoden --------

	/**
	 *	Zeichnet ein Schema
	 */
	public void paintScheme( Graphics g, int x, int y, boolean mode )
	{
		g.drawRect( x - (width>>1), y - (height>>1), width - 1, height - 1 );
	}

// -------- private Methoden --------

	private void newVisualProps()
	{
		if( labName != null ) {
//			fnt = AbstractApplication.getApplication().getWindowHandler().getDefaultFont(); // Main.getFont( Main.FONT_ICON );
			fnt = AbstractApplication.getApplication().getGraphicsHandler().getFont( GraphicsHandler.FONT_SYSTEM | GraphicsHandler.FONT_SMALL );
			setFont( fnt );
			fntMetr = getFontMetrics( fnt );

			width		= fntMetr.stringWidth( labName ) + 4;
			height		= fntMetr.getHeight();
			if( isVisible() ) repaint();
		}
	}

	protected void processMouseEvent( MouseEvent e )
	{
		if( e.getID() == MouseEvent.MOUSE_PRESSED ) {
			requestFocus();
		}
		super.processMouseEvent( e );
	}

	protected void processFocusEvent( FocusEvent e )
	{
		if( e.getID() == FocusEvent.FOCUS_GAINED ) {
			setSelected( STATE_SELECTED );
		} else if( e.getID() == FocusEvent.FOCUS_LOST ) {
			setSelected( STATE_NORMAL );
		}
		super.processFocusEvent( e );
	}
}
// class OpConnector