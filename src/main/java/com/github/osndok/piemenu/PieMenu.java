package com.github.osndok.piemenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by robert on 2015-11-04 13:51.
 */
public
class PieMenu<T> extends JList<T> implements MouseMotionListener, MouseListener
{
	private static final
	Logger log = LoggerFactory.getLogger(PieMenu.class);

	private
	boolean centeredLabels = false;

	private static final
	int LABEL_PADDING = 5;

	private static final
	boolean DEBUG = true;

	private
	Point centerPoint;

	private final
	DefaultListModel<T> listModel;

	private final
	ShadowList<PieMenuEntry> pieMenuEntries;

	public
	PieMenu(List<T> entries)
	{
		super(new DefaultListModel<T>());
		this.listModel = (DefaultListModel<T>) getModel();

		int i = 0;

		for (T entry : entries)
		{
			listModel.add(i, entry);
			i++;
		}

		this.pieMenuEntries = new ShadowList<PieMenuEntry>(listModel);

		//NB: the RepaintManager will not paint something of zero size, so we will never be bootstrapped in... kludgy... yuck.
		setSize(1, 1);
	}

	@Override
	public
	void paint(Graphics g)
	{
		if (DEBUG)
		{
			g.setClip(null);
		}

		final
		Point globalCenter;
		{
			//TODO: BUG: the fact that this thrashes indicates to me that setting the bounds in paint() is inherently unsafe
			//globalCenter=SwingUtilities.convertPoint(getParent(), this.centerPoint, this); <-- thrashes
			globalCenter=this.centerPoint; // <-- off by menu bar height
			log.debug("globalCenter = {}", globalCenter);
		}

		final
		PieMenuEntry[] entries=getSemiStablePieMenuEntries();

		final
		int numEntries=entries.length;

		final
		FontMetrics fontMetrics = g.getFontMetrics();

		final
		int paddedLabelHeight=2*LABEL_PADDING+fontMetrics.getHeight();

		final
		int innerRadius;
		{
			final
			double pedanticRadiansPerWedge=2*Math.PI/numEntries;

			final
			int grabbableTargetBound = 40;

			final
			int labelBound = (int) (paddedLabelHeight / Math.tan(pedanticRadiansPerWedge));

			innerRadius=Math.max(grabbableTargetBound, labelBound);
		}

		final
		int allocatedLabelWidth;
		{
			final
			BasicTableWidthOptimizer optimizer=new BasicTableWidthOptimizer(fontMetrics);

			allocatedLabelWidth=optimizer.getPixelsWide(getLabels(entries));
		}

		log.debug("labels @ {} x {}", allocatedLabelWidth, paddedLabelHeight);

		final
		int outerRadius=innerRadius+2*LABEL_PADDING+allocatedLabelWidth;

		log.debug("innerRadius = {} & outerRadius = {}", innerRadius, outerRadius);

		final
		Rectangle desiredBounds=new Rectangle(globalCenter.x - outerRadius, globalCenter.y - outerRadius, 2 * outerRadius +1, 2 * outerRadius+1);
		{
			//TODO: is there a better way? Seems odd to set bounds *INSIDE* the paint function... but we need the fontMetrics?!?!
			if (getBounds().equals(desiredBounds))
			{
				log.debug("bounds: {}", desiredBounds);
			}
			else
			{
				log.debug("started with wrong bounds... repaint: {} != {}", getBounds(), desiredBounds);
				setBounds(desiredBounds);
				repaint();
				return;
			}
		}

		final
		int localCenterX=((int)desiredBounds.getWidth()/2);

		final
		int localCenterY=((int)desiredBounds.getHeight()/2);

		final
		Graphics2D g2=(Graphics2D)g;
		{
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}

		g2.setColor(Color.BLACK);

		Shape shape=new Ellipse2D.Double(localCenterX-innerRadius, localCenterY-innerRadius, 2*innerRadius, 2*innerRadius);
		{
			g2.draw(shape);
		}

		shape=new Ellipse2D.Double(localCenterX-outerRadius, localCenterY-outerRadius, 2*outerRadius, 2*outerRadius);
		{
			g2.draw(shape);
		}

		final
		double radiansPerWedge;
		{
			radiansPerWedge=Math.min(45.0, 2*Math.PI/numEntries);

			final
			int estimatedWedgeH=(int)(outerRadius*Math.sin(radiansPerWedge));

			log.debug("estimatedWedgeHeight = {}px @ {} rad", estimatedWedgeH, radiansPerWedge);

			if (estimatedWedgeH<paddedLabelHeight)
			{
				throw new UnsupportedOperationException("unimplemented; too many items for a single ring, need to implement outer ring logic.");
			}
		}

		final
		AffineTransform toCenter=new AffineTransform();
		{
			toCenter.translate(-innerRadius, -innerRadius);
		}

		for (int i=0; i<numEntries; i++)
		{
			final
			PieMenuEntry pieMenuEntry=entries[i];

			/*
			The offsetAngle is the principal "angle" of the wedge, as defined by its *greater* boundary leg.
			Therefore, the angle might not seem intuitive.
			 */
			final
			double offsetAngle=(radiansPerWedge*(i+1)-HALF_PI)%TWO_PI;
			{
				log.debug("offsetAngle={}", offsetAngle);
			}

			final
			PieMenuQuadrant quadrant;
			{
				quadrant = computeQuadrant(radiansPerWedge, i, offsetAngle);
				pieMenuEntry.quadrant = quadrant;
			}

			final
			int wedgeW =(int)(outerRadius*Math.cos(offsetAngle));

			final
			int wedgeH=(int)(outerRadius*Math.sin(offsetAngle));

			//g2.drawLine(localCenterX, localCenterY, localCenterX+wedgeW, localCenterY+wedgeH);

			final
			AffineTransform originalTransformation=g2.getTransform();

			final
			AffineTransform affineTransform=new AffineTransform();
			{
				//affineTransform.setToRotation(offsetAngle);
				//affineTransform.translate(-innerRadius, -innerRadius);
				//affineTransform.setToRotation(offsetAngle);
				affineTransform.concatenate(originalTransformation);
				//affineTransform.setToRotation(offsetAngle);
				//affineTransform.concatenate(toCenter);
				//affineTransform.setToRotation(offsetAngle);
				affineTransform.translate(localCenterX, localCenterY);
				//affineTransform.setToRotation(offsetAngle);
				//affineTransform.translate(innerRadius, 0);
				//affineTransform.setToRotation(offsetAngle);
			}

			g2.setTransform(affineTransform);
			{
				if (centeredLabels)
				{
					g2.transform(AffineTransform.getRotateInstance(offsetAngle + radiansPerWedge / 2));
				}
				else
				{
					g2.transform(AffineTransform.getRotateInstance(offsetAngle));
				}

				final
				String label=pieMenuEntry.label;

				final
				int x=innerRadius+LABEL_PADDING;

				final
				int y=(centeredLabels ?-LABEL_PADDING/2:-LABEL_PADDING);

				final
				int w=fontMetrics.stringWidth(label);

				final
				int h=fontMetrics.getHeight();

				final
				Point2D transformed=affineTransform.transform(new Point2D.Double(x, y), null);

				g2.setColor(pieMenuEntry.backgroundColor);

				//Main colored 'wedge'.
				//TODO: It might be better if we could color the pie without having to overwrite the center "hub"... e.g. for transparency.
				shape=new Arc2D.Double(-outerRadius,-outerRadius,2*outerRadius,2*outerRadius,0,Math.toDegrees(radiansPerWedge), Arc2D.PIE);
				{
					g2.fill(shape);
				}
				//g2.fillArc(-outerRadius, -outerRadius, 2*outerRadius, 2*outerRadius, 0, (int)Math.toDegrees(radiansPerWedge));

				g2.setColor(Color.BLACK);
				g2.drawLine(innerRadius, 0, outerRadius, 0);

				final
				String debugLabel;
				{
					if (DEBUG)
					{
						debugLabel=label;
						//debugLabel=pieMenuEntry.quadrant+": "+label;
						//debugLabel=String.format("%1f %s: %s", offsetAngle, pieMenuEntry.quadrant, label);
					}
					else
					{
						debugLabel=label;
					}
				}

				if (quadrant==PieMenuQuadrant.SOUTH_EAST)
				{
					g2.transform(AffineTransform.getRotateInstance(-radiansPerWedge));
					g2.transform(AffineTransform.getTranslateInstance(0, paddedLabelHeight));
				}
				/*
				if (quadrant==PieMenuQuadrant.NORTH_WEST)
				{
					g2.transform(AffineTransform.getRotateInstance(Math.PI));
				}
				*/

				log.debug("drawString('{}', {}+{}, {}+{}) -> {}", label, x, w, y, h, transformed);
				g2.setColor(pieMenuEntry.foregroundColor);
				g2.drawString(debugLabel, x, y);
				//g2.drawRect(x, y, w, -h);
			}
			g2.setTransform(originalTransformation);

			//break;
		}

		//Center hub
		{
			g2.setColor(Color.WHITE);
			g2.fillArc(localCenterX - innerRadius, localCenterY - innerRadius, 2 * innerRadius, 2 * innerRadius, 0, 360);
			g.setColor(Color.BLACK);
			g2.drawString("X", localCenterX-(fontMetrics.charWidth('X')/2), localCenterY+(fontMetrics.getHeight()/2));
		}

		paintBorder(g);
		paintChildren(g);
	}

	private static final
	double TRIVIAL_RADIAN_ANGLE=2*Math.PI/360;

	private static final
	double TWO_PI=2*Math.PI;

	private static final
	double HALF_PI=Math.PI/2;

	private static final
	double PI_AND_A_HALF=Math.PI*1.5;

	private
	PieMenuQuadrant computeQuadrant(double radiansPerWedge, int i, double offsetAngle)
	{
		if (offsetAngle>TWO_PI)
		{
			throw new IllegalArgumentException("bad offsetAngle: "+offsetAngle);
		}

		if (offsetAngle<0)
		{
			offsetAngle+=TWO_PI;
			assert(offsetAngle>0);
		}

		/*
		ATM, the wedge is defined from offsetAngle to offsetAngle-radiansPerWedge (kinda... backwards).
		Since a wedge can technically span more than one quadrant, we assign it to the quadrant that it's "median angle" is in.
		 */
		final
		double median=offsetAngle-radiansPerWedge/2;

		if (median <= TRIVIAL_RADIAN_ANGLE)
		{
			return PieMenuQuadrant.NORTH_EAST;
		}

		if (median<HALF_PI)
		{
			return PieMenuQuadrant.SOUTH_EAST;
		}

		if (median<Math.PI)
		{
			return PieMenuQuadrant.SOUTH_WEST;
		}

		if (median<PI_AND_A_HALF)
		{
			return PieMenuQuadrant.NORTH_WEST;
		}

		return PieMenuQuadrant.NORTH_EAST;
	}

	private
	PieMenuEntry[] getSemiStablePieMenuEntries()
	{
		final
		int l=listModel.getSize();

		final
		List<PieMenuEntry> retval=new ArrayList<PieMenuEntry>(l);
		{
			for (int i=0; i<l; i++)
			{
				final
				Object o=listModel.get(i);

				PieMenuEntry pieMenuEntry=pieMenuEntries.get(i);

				if (pieMenuEntry==null)
				{
					pieMenuEntry=createPieMenuEntry(o);
					pieMenuEntries.set(i, pieMenuEntry);
				}
				else
				{
					assert(pieMenuEntry.value==o);
					pieMenuEntry.label=o.toString(); //In case it changes?!?
				}

				retval.add(pieMenuEntry);
			}
		}

		return retval.toArray(new PieMenuEntry[l]);
	}

	private
	float lastWedgeHue = 0.0f;

	/**
	 * It is good for this to be prime-ish (or not a divisor of 10/100/1000), so that if it wraps we will
	 * get 'new' and 'different' numbers. Also, it should be around 0.5 to get the maximum color difference
	 * for neighboring wedges, or near-zero to get a gradient effect.
	 */
	private static final
	float WEDGE_HUE_SEPARATION = 0.371f; //0.123f;

	private static final
	float WEDGE_BACKGROUND_SATURATION = 0.3f;

	private static final
	float WEDGE_BACKGROUND_BRIGHTNESS = 0.94f;

	private static final
	float WEDGE_TEXT_SATURATION = 1.0f;

	private static final
	float WEDGE_TEXT_BRIGHTNESS = 0.4f;

	private
	PieMenuEntry createPieMenuEntry(Object o)
	{
		final
		float hue;
		{
			//Skipping around the color wheel...
			hue=(lastWedgeHue+WEDGE_HUE_SEPARATION)%1.0f;
			lastWedgeHue=hue;
		}

		final
		PieMenuEntry retval = new PieMenuEntry();
		{
			retval.enabled = true;
			retval.label = o.toString();
			//Rectangle2D bounds;
			//ListModel<?>    subMenuModel;
			//later: PieMenuQuadrant quadrant;
			//Image           icon;
			retval.hue=hue;
			retval.foregroundColor=deriveTextColor(hue);
			retval.backgroundColor=deriveBackgroundColor(hue);
		}

		return retval;
	}

	private
	Color deriveTextColor(float hue)
	{
		return new Color(Color.HSBtoRGB(hue, WEDGE_TEXT_SATURATION, WEDGE_TEXT_BRIGHTNESS));
	}

	private
	Color deriveBackgroundColor(float hue)
	{
		return new Color(Color.HSBtoRGB(hue, WEDGE_BACKGROUND_SATURATION, WEDGE_BACKGROUND_BRIGHTNESS));
	}

	private static
	List<String> getLabels(PieMenuEntry[] entries)
	{
		final
		List<String> retval = new ArrayList<String>(entries.length);
		{
			for (PieMenuEntry entry : entries)
			{
				if (entry.label==null)
				{
					retval.add("<null>");
				}
				else
				{
					retval.add(entry.label);
				}
			}
		}

		return retval;
	}

	public
	void mouseClicked(MouseEvent mouseEvent)
	{

	}

	public
	void mousePressed(MouseEvent mouseEvent)
	{

	}

	public
	void mouseReleased(MouseEvent mouseEvent)
	{

	}

	public
	void mouseEntered(MouseEvent mouseEvent)
	{

	}

	public
	void mouseExited(MouseEvent mouseEvent)
	{

	}

	public
	void mouseDragged(MouseEvent mouseEvent)
	{

	}

	public
	void mouseMoved(MouseEvent mouseEvent)
	{

	}

	public
	void setCenter(Point point)
	{
		this.centerPoint = point;
		repaint();
	}

	public
	void setCenteredLabels(boolean centeredLabels)
	{
		this.centeredLabels = centeredLabels;
	}
}
