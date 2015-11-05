package com.github.osndok.piemenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Collection;
import java.util.Iterator;

/**
 * Given some font metrics and a list of string-values (that will appear in a particular column), compute
 * the 'optimal' column with such that most of the data is displayed, but very-long-lines will be truncated.
 *
 * The value is computed without respect to margins or padding, so (depending on your application) you might
 * have to pad the value a bit to actually make the next look nice.
 *
 * Created by robert on 2015-11-04 16:07.
 */
class BasicTableWidthOptimizer
{
	/**
	 * Roughly translates to: "how much empty space is tolerated?"
	 */
	private static final
	int PERCENT_ALLOWABLE_INFLATION = 10;

	private static final
	Logger log = LoggerFactory.getLogger(BasicTableWidthOptimizer.class);

	private final
	FontMetrics fontMetrics;

	public
	BasicTableWidthOptimizer(FontMetrics fontMetrics)
	{
		this.fontMetrics = fontMetrics;
	}

	public
	int getPixelsWide(Collection<String> labels)
	{
		final
		Iterator<String> i = labels.iterator();

		if (!i.hasNext())
		{
			throw new UnsupportedOperationException("cannot get optimal width for an empty collection of labels");
		}

		String label = i.next();
		int pixelsWide = fontMetrics.stringWidth(label);

		int count = 1;
		int min = pixelsWide;
		int max = pixelsWide;
		int accum = pixelsWide;

		log.debug("first width is {}", pixelsWide);

		while (i.hasNext())
		{
			label = i.next();
			pixelsWide = fontMetrics.stringWidth(label);
			count++;

			if (pixelsWide < min)
			{
				min=pixelsWide;
			}

			if (pixelsWide>max)
			{
				max=pixelsWide;
			}

			accum+=pixelsWide;

			log.debug("accumulated #{}: {} <= {} <= {} ... (total = {})", count, min, pixelsWide, max, accum);
		}

		final
		double average;
		{
			final
			double fullAverage = ((double) accum) / count;

			//TODO: who says the outlier must be LARGE?
			final
			double partialAverage = ((double) accum-max) / (count-1);

			if (fullAverage > partialAverage*(100+PERCENT_ALLOWABLE_INFLATION)/100)
			{
				average=partialAverage;
				log.debug("partialAverage={} (removed one outlier)", average);
			}
			else
			{
				average=fullAverage;
				log.debug("fullAverage={}", average);
			}
		}

		final
		double inflated=average*(100+PERCENT_ALLOWABLE_INFLATION)/100;

		log.debug("final: avg={}, inflated={}, max={}", average, inflated, max);

		if (inflated>max)
		{
			return max;
		}
		else
		{
			return (int)inflated;
		}
	}
}
