package com.github.osndok.piemenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
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

	private static final
	boolean CENTERED_LABELS=false;

	private static final
	int LABEL_PADDING = 5;

	private
	Point centerPoint;

	private final
	DefaultListModel<T> listModel;

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

		//NB: the RepaintManager will not paint something of zero size, so we will never be bootstrapped in... kludgy... yuck.
		setSize(1,1);
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
		List<String> labels = getLabels();

		final
		int numLabels=labels.size();

		final
		FontMetrics fontMetrics = g.getFontMetrics();

		final
		int paddedLabelHeight=2*LABEL_PADDING+fontMetrics.getHeight();

		final
		int innerRadius;
		{
			final
			double pedanticRadiansPerWedge=2*Math.PI/labels.size();

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

			allocatedLabelWidth=optimizer.getPixelsWide(labels);
		}

		log.debug("labels @ {} x {}", allocatedLabelWidth, paddedLabelHeight);

		final
		int outerRadius=innerRadius+2*LABEL_PADDING+allocatedLabelWidth;

		log.debug("innerRadius = {} & outerRadius = {}", innerRadius, outerRadius);

		final
		Rectangle desiredBounds=new Rectangle(globalCenter.x - outerRadius, globalCenter.y - outerRadius, 2 * outerRadius, 2 * outerRadius);
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
			radiansPerWedge=Math.min(45.0, 2*Math.PI/numLabels);

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

		for (int i=0; i<numLabels; i++)
		{
			final
			double offsetAngle=radiansPerWedge*i;
			{
				log.debug("offsetAngle={}", offsetAngle);
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
				if (CENTERED_LABELS)
				{
					g2.transform(AffineTransform.getRotateInstance(offsetAngle + radiansPerWedge / 2));
				}
				else
				{
					g2.transform(AffineTransform.getRotateInstance(offsetAngle));
				}

				final
				String label=labels.get(i);

				final
				int x=innerRadius+LABEL_PADDING;

				final
				int y=(CENTERED_LABELS?-LABEL_PADDING/2:-LABEL_PADDING);

				final
				int w=fontMetrics.stringWidth(label);

				final
				int h=fontMetrics.getHeight();

				final
				Point2D transformed=affineTransform.transform(new Point2D.Double(x, y), null);

				g2.drawLine(innerRadius, 0, outerRadius, 0);
				log.debug("drawString('{}', {}+{}, {}+{}) -> {}", label, x, w, y, h, transformed);
				g2.drawString(label, x, y);
				//g2.drawRect(x, y, w, -h);
			}
			g2.setTransform(originalTransformation);

			//break;
		}

		paintBorder(g);
		paintChildren(g);
	}

	private
	List<String> getLabels()
	{
		final
		int l=listModel.getSize();

		final
		List<String> retval=new ArrayList<String>(l);
		{
			for (int i=0; i<l; i++)
			{
				retval.add(listModel.get(i).toString());
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
		this.centerPoint=point;
		repaint();
	}
}
