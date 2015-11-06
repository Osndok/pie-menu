package com.github.osndok.piemenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.module.util.SystemPropertyOrEnvironment;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
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
		double degreesPerWedge;

		final
		double radiansPerWedge;
		{
			radiansPerWedge=Math.min(45.0, 2*Math.PI/numEntries);
			degreesPerWedge=Math.toDegrees(radiansPerWedge);

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
			The offsetAngle is the principal "angle" of the wedge, as defined by its "lower" boundary leg.
			Zero radians & zero degrees should point "right".
			 */
			final
			double highAngle=(HALF_PI-radiansPerWedge*(i))%TWO_PI;

			final
			double lowAngle=(HALF_PI-radiansPerWedge*(i+1))%TWO_PI;
			{
				log.debug("{} < angle < {}", lowAngle, highAngle);
			}

			final
			PieMenuQuadrant quadrant;
			{
				quadrant = computeQuadrant(lowAngle, highAngle);
				pieMenuEntry.quadrant = quadrant;
				log.debug("{} quadrant = {}; {} < {}", pieMenuEntry.label, quadrant, lowAngle, highAngle);
			}

			final
			int wedgeW =(int)(outerRadius*Math.cos(lowAngle));

			final
			int wedgeH=(int)(outerRadius*Math.sin(lowAngle));

			//Primary/large colored "wedge"
			//if (i==1)
			{
				g2.setColor(pieMenuEntry.backgroundColor);

				//TODO: Why are these angles so far off from what I would expect?
				double startDegrees=Math.toDegrees(lowAngle);

				log.debug("drawing '{}' arc, {} rad -> {} degrees", pieMenuEntry.label, lowAngle, startDegrees);
				shape = new Arc2D.Double(0, 0, getWidth(), getHeight(),
												startDegrees, degreesPerWedge,
												Arc2D.PIE);

				g2.fill(shape);
				pieMenuEntry.shape=shape;
			}

			// ------------- TRANSFORM BOUNDARY -----------------

			final
			AffineTransform originalTransformation=g2.getTransform();

			final
			AffineTransform locallyCentered=new AffineTransform();
			{
				locallyCentered.concatenate(originalTransformation);
				locallyCentered.translate(localCenterX, localCenterY);
			}

			g2.setTransform(locallyCentered);
			{
				final
				String label=pieMenuEntry.label;

				final
				int w=fontMetrics.stringWidth(label);

				final
				int h=fontMetrics.getHeight();

				//USED TO draw black lines between the wedges, now does not work (affineTransformation moved).
				//g2.setColor(Color.BLACK);
				//g2.drawLine(innerRadius, 0, outerRadius, 0);

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

				final
				Shape originalClipRegion = g2.getClip();

				g2.setColor(pieMenuEntry.foregroundColor);

				if (quadrant == PieMenuQuadrant.EAST)
				{
					//Left-centered label, no rotation, against the inner radius.
					g2.setClip(new Rectangle2D.Double(innerRadius+LABEL_PADDING, LABEL_PADDING-paddedLabelHeight+LABEL_PADDING, allocatedLabelWidth, paddedLabelHeight));
					g2.drawString(debugLabel, innerRadius+LABEL_PADDING, LABEL_PADDING);
				}

				if (quadrant == PieMenuQuadrant.WEST)
				{
					//Right-centered label, no rotation, against the outer/left radius.
					//g2.setClip(new Rectangle2D.Double(LABEL_PADDING-outerRadius, LABEL_PADDING-outerRadius-paddedLabelHeight+LABEL_PADDING, allocatedLabelWidth, paddedLabelHeight));
					g2.setClip(new Rectangle2D.Double(LABEL_PADDING-outerRadius, LABEL_PADDING-paddedLabelHeight+LABEL_PADDING, allocatedLabelWidth, paddedLabelHeight));
					g2.drawString(debugLabel, LABEL_PADDING-outerRadius, LABEL_PADDING);
				}

				if (quadrant==PieMenuQuadrant.NORTH_EAST)
				{
					//The first & native transform... left-aligned text, anchored at the lower wedge border.
					g2.transform(AffineTransform.getRotateInstance(-lowAngle));
					g2.setClip(new Rectangle2D.Double(innerRadius + LABEL_PADDING,
														 -LABEL_PADDING - paddedLabelHeight + LABEL_PADDING,
														 allocatedLabelWidth, paddedLabelHeight));
					g2.drawString(debugLabel, innerRadius+LABEL_PADDING, -LABEL_PADDING);
				}

				if (quadrant==PieMenuQuadrant.SOUTH_EAST)
				{
					//The second transform is quite similar... left-aligned text, anchored at the upper wedge border.
					//Offset the rotation & translation a bit, to make the words appear closer to the readable position
					g2.transform(AffineTransform.getRotateInstance(-highAngle));
					g2.setClip(new Rectangle2D.Double(innerRadius+LABEL_PADDING, h+LABEL_PADDING-paddedLabelHeight+LABEL_PADDING, allocatedLabelWidth, paddedLabelHeight));
					g2.drawString(debugLabel, innerRadius+LABEL_PADDING, h+LABEL_PADDING);
				}

				if (quadrant==PieMenuQuadrant.SOUTH_WEST)
				{
					g2.transform(AffineTransform.getRotateInstance(Math.PI-lowAngle));
					g2.setClip(new Rectangle2D.Double(LABEL_PADDING-outerRadius,
														 paddedLabelHeight - LABEL_PADDING - paddedLabelHeight + LABEL_PADDING,
														 allocatedLabelWidth, paddedLabelHeight));
					g2.drawString(debugLabel, LABEL_PADDING - outerRadius, paddedLabelHeight-LABEL_PADDING);
				}

				if (quadrant==PieMenuQuadrant.NORTH_WEST)
				{
					g2.transform(AffineTransform.getRotateInstance(Math.PI-highAngle));
					g2.setClip(new Rectangle2D.Double(LABEL_PADDING-outerRadius,
														 -LABEL_PADDING - paddedLabelHeight + LABEL_PADDING,
														 allocatedLabelWidth, paddedLabelHeight));
					g2.drawString(debugLabel, LABEL_PADDING-outerRadius, -LABEL_PADDING);
				}

				//log.debug("drawString('{}', {}+{}, {}+{})", label, x, w, y, h);
				//g2.drawString(debugLabel, innerRadius+LABEL_PADDING, -LABEL_PADDING);
				//g2.drawRect(x, y, w, -h);
				g2.setClip(originalClipRegion);
			}
			g2.setTransform(originalTransformation);

			//break;
		}

		//Center hub
		{
			g2.setColor(getBackground());
			g2.fillArc(localCenterX - innerRadius,
						  localCenterY - innerRadius, 2 * innerRadius, 2 * innerRadius, 0, 360);
			g2.setColor(Color.BLACK);
			g2.drawArc(localCenterX - innerRadius, localCenterY - innerRadius, 2 * innerRadius, 2 * innerRadius, 0, 360);
			g.setColor(Color.BLACK);
			g2.drawString("X", localCenterX-(fontMetrics.charWidth('X')/2), localCenterY +(fontMetrics.getHeight()/2));
		}

		paintBorder(g);
		paintChildren(g);
	}

	private static final
	double TWO_PI=2*Math.PI;

	private static final
	double HALF_PI=Math.PI/2;

	private static final
	double PI_AND_A_HALF=Math.PI*1.5;

	private
	PieMenuQuadrant computeQuadrant(double lowAngle, double highAngle)
	{
		if (lowAngle<0) lowAngle+=TWO_PI;
		if (lowAngle>TWO_PI) lowAngle-=TWO_PI;
		if (highAngle<0) highAngle+=TWO_PI;
		if (highAngle>TWO_PI) highAngle-=TWO_PI;

		assert(lowAngle>=0);
		assert(highAngle>=0);
		assert(lowAngle<=TWO_PI);
		assert(highAngle<=TWO_PI);

		//TODO: if '0' or 'PI' is within the given range, use a special 'quadrant' (WEST or EAST).

		if (lowAngle>highAngle)
		{
			return PieMenuQuadrant.EAST;
		}

		final
		double median=(lowAngle+highAngle)/2;

		if (median < HALF_PI)
		{
			return PieMenuQuadrant.NORTH_EAST;
		}

		if (lowAngle < Math.PI && Math.PI < highAngle)
		{
			return PieMenuQuadrant.WEST;
		}

		if (median<=Math.PI)
		{
			return PieMenuQuadrant.NORTH_WEST;
		}

		if (median<PI_AND_A_HALF)
		{
			return PieMenuQuadrant.SOUTH_WEST;
		}

		return PieMenuQuadrant.SOUTH_EAST;
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
	 * It is good for this to be prime-ish (or 1/P ?), so that if it wraps we will get 'new' and 'different' numbers.
	 * Also, it should be around 0.5 to get the maximum color difference for neighboring wedges, or near-zero to get
	 * a gradient effect.
	 *
	 * The formula seems to be related to "0.5-n/k", don't know what n & k are...
	 *
	 * Benchmarks:
	 * 0.500 means "every other color is the same" (like a roulette wheel)
	 * 0.433 [avg(0.500,0.366)] - wheel of 8 have one 'similar color pairs touching
	 * 0.409 (0.5-1/11) - 6 & 11 look similar
	 * 0.371 was the first mostly-successful value found, somewhat arbitrarily
	 * --
	 * 0.366 (i.e. 0.500-0.133) - visually seems to be the best of the benchmarks... a wheel of 9 (or 12?) will have one 'similar' color pair touching.
	 * --
	 * 0.357 (0.5-1/7) - 15-color wheel will have two similar touching
	 * 0.355 - 15-element wheel has similar values touching
	 * 0.349 [avg(0.366,0.333)] has a good drift... but every 3rd color is... 'related'...
	 * 0.333 means "repeating pattern of three" (not much better)
	 * 0.267 (which is 0.500-0.333) yields good results, with "about four colors" that drift apart
	 * 0.250 means "repeating pattern of four" (does not give the impression of a gradient)
	 * ????? "looks like a gradient" boundary
	 * 0.133 (half of 0.267) related pattern of "about-8" that drift, gradient-ish
	 * 0.125 would be a pattern of 8 (gradient-ish)
	 * 0.063 would be a pattern of 16 (very color-wheel-ish)
	 */
	private static final
	float WEDGE_HUE_SEPARATION = 0.366f;

	private static final
	float WEDGE_BACKGROUND_SATURATION = 0.3f;

	private static final
	float WEDGE_BACKGROUND_BRIGHTNESS = 0.94f;

	private static final
	float WEDGE_TEXT_SATURATION = 1.0f;

	private static final
	float WEDGE_TEXT_BRIGHTNESS = 0.3f;

	//TODO: if object isa PieMenuEntry, then use it directly, filling in any missing details, and updating any state (like lastWedgeHue).
	private
	PieMenuEntry createPieMenuEntry(Object o)
	{
		final
		String label;
		{
			label=o.toString();
		}

		final
		float hue;
		{
			switch (colorizer)
			{
				case DISTINCT_HUES:
				{
					//Skipping around the color wheel...
					hue=(lastWedgeHue+WEDGE_HUE_SEPARATION)%1.0f;
					break;
				}

				case GRADIENT:
				{
					//Walking around the color wheel...
					final
					float partial=(1.0f/listModel.getSize());

					hue=(lastWedgeHue+partial)%1.0f;
					break;
				}

				case LABEL_HASH:
				{
					//Stateless... does not depend on any other wedge color...
					final
					double hash=label.toLowerCase().hashCode();

					final
					double exponent=Math.ceil(Math.log10(hash));

					final
					double magnitude=Math.pow(10, exponent);

					hue=(float)Math.abs(hash/magnitude);

					log.debug("hash-color: '{}' -> {} -> {} -> {} -> {}", label, hash, exponent, magnitude, hue);

					break;
				}

				default:
				{
					throw new AssertionError();
				}
			}

			lastWedgeHue=hue;
		}

		final
		PieMenuEntry retval = new PieMenuEntry();
		{
			retval.enabled = true;
			retval.value=o;
			retval.label = label;
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

	private
	Colorizer colorizer=getDefaultColorizer();

	private static
	Colorizer getDefaultColorizer()
	{
		final
		String preference=SystemPropertyOrEnvironment.get("PIE_MENU_COLORIZER");

		if (preference!=null)
		{
			try
			{
				return Colorizer.valueOf(preference);
			}
			catch (Exception e)
			{
				log.error("invalid PIE_MENU_COLORIZER: '{}'", preference, e);
			}
		}

		return Colorizer.DISTINCT_HUES;
	}

	public
	Colorizer getColorizer()
	{
		return colorizer;
	}

	public
	void setColorizer(Colorizer colorizer)
	{
		if (colorizer==null)
		{
			throw new NullPointerException();
		}

		this.colorizer = colorizer;
	}
}
