package com.github.osndok.piemenu;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Maintains a parallel list of items related to an original/primary list. Useful for when there are
 * "duplicates" in the list, as it only cares about the integer address. 'null' placeholders are used
 * (rather than a callback) so that you can backfill the missing data as-needed... even with random
 * data, this should let you be able to keep the separate-but-related data intact.
 *
 * Created by robert on 2015-11-05 12:35.
 */
class ShadowList<T> implements ListDataListener, Iterable<T>
{
	private final
	List<T> list = new ArrayList<T>();

	private final
	ListModel listModel;

	ShadowList(ListModel model)
	{
		this.listModel=model;
		model.addListDataListener(this);

		for (int i=listModel.getSize(); i>0; i--)
		{
			list.add(null);
		}

		assert(list.size()==listModel.getSize());
	}

	public
	void intervalAdded(ListDataEvent listDataEvent)
	{
		final
		int i=singleEntriesOnly(listDataEvent);

		list.add(i, null);
		assert(list.size()==listModel.getSize());
	}

	private
	int singleEntriesOnly(ListDataEvent listDataEvent)
	{
		final
		int a=listDataEvent.getIndex0();

		final
		int b=listDataEvent.getIndex1();

		if (a!=b)
		{
			throw new UnsupportedOperationException("only operates on one element changes");
		}

		return a;
	}

	public
	void intervalRemoved(ListDataEvent listDataEvent)
	{
		final
		int i=singleEntriesOnly(listDataEvent);

		list.remove(i);
		assert(list.size()==listModel.getSize());
	}

	public
	void contentsChanged(ListDataEvent listDataEvent)
	{
		final
		int i=singleEntriesOnly(listDataEvent);

		list.set(i, null);
		assert(list.size()==listModel.getSize());
	}

	public
	int size()
	{
		return list.size();
	}

	public
	T get(int i)
	{
		return list.get(i);
	}

	public
	T set(int i, T t)
	{
		return list.set(i, t);
	}

	public
	Iterator<T> iterator()
	{
		return list.iterator();
	}
}
