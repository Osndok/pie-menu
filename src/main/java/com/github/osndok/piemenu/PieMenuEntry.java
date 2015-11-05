package com.github.osndok.piemenu;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * Created by robert on 2015-11-04 16:00.
 */
class PieMenuEntry<T>
{
	String          label;
	T               value;
	boolean         enabled;
	AffineTransform affineTransform;
	Rectangle2D     bounds;
	ListModel<?>    subMenuModel;
	PieMenuQuadrant quadrant;
	Image           icon;
	Color           foregroundColor;
	Color           backgroundColor;
}
