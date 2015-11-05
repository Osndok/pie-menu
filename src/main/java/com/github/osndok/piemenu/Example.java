package com.github.osndok.piemenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

/**
 * Given no information, runs an example program of how the PieMenu classes might be used.
 */
public
class Example extends JFrame implements Runnable, MouseListener
{
	private static final
	Logger log = LoggerFactory.getLogger(Example.class);

	public
	Example()
	{
		setSize(1024, 1024);
		//setLayout(new FlowLayout(FlowLayout.CENTER));
		setLayout(null);

		final
		JLabel instructions=new JLabel("Right-click anywhere to open a pie menu");
		{
			instructions.setBounds(5,5,400,20);
		}
		add(instructions);

		pieMenu=new PieMenu(getPieMenuEntries());
		{
			pieMenu.setVisible(false);
		}
		add(pieMenu);

		setVisible(true);
		addMouseListener(this);
	}

	public
	void run()
	{
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

	private
	PieMenu pieMenu;

	public
	void mouseClicked(final MouseEvent mouseEvent)
	{
		log.debug("mouseClicked({})", mouseEvent);

		if (SwingUtilities.isRightMouseButton(mouseEvent) || (mouseEvent.isControlDown() && SwingUtilities.isLeftMouseButton(mouseEvent)))
		{
			log.debug("...is right button");
			SwingUtilities.invokeLater(new Runnable()
			{
				public
				void run()
				{
					if (pieMenu.isVisible())
					{
						log.debug("invoked-later, pieMenu is visible");
						pieMenu.setCenter(accountForInsets(mouseEvent.getPoint()));
						repaint();
					}
					else
					{
						log.debug("invoked-later, pieMenu is IN-visible");
						pieMenu.setCenter(accountForInsets(mouseEvent.getPoint()));
						//pieMenu.setBorder(BorderFactory.createLineBorder(Color.RED));
						pieMenu.setVisible(true);
						repaint();
					}
				}
			});
		}
	}

	/**
	 * TODO: there is probably a better (or more conventional) way to handle inset offsets, maybe in SwingUtilities?
	 *
	 * @param point
	 * @return
	 */
	private
	Point accountForInsets(Point point)
	{
		final
		Insets insets=getInsets();
		{
			log.debug("insets: {}", insets);
		}

		return new Point(point.x-insets.left, point.y-insets.top);
	}

	private
	List getPieMenuEntries()
	{
		final
		List list=new ArrayList();
		{
			list.add("Alpha");
			list.add("Beta");
			list.add("Gamma");
			list.add("Delta");
			list.add(new Object());
			list.add(this);
			list.add("Zeta");
			list.add("Theta");
			list.add("Omega");
			list.add("Omicron");
			list.add("Enough?");
		}

		return list;
	}

	public
	void mousePressed(MouseEvent mouseEvent)
	{
		log.debug("mousePressed({})", mouseEvent);
	}

	public
	void mouseReleased(MouseEvent mouseEvent)
	{
		log.debug("mouseReleased({})", mouseEvent);
	}

	public
	void mouseEntered(MouseEvent mouseEvent)
	{
		log.debug("mouseEntered({})", mouseEvent);
	}

	public
	void mouseExited(MouseEvent mouseEvent)
	{
		log.debug("mouseExited({})", mouseEvent);
	}
}
