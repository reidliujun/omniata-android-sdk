package com.omniata.android.sdk;

import java.lang.reflect.Constructor;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

/* package */ class PersistentBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {
	//private SQLiteDatabase db;
	private static final String TAG = "PersistentBlockingQueue";
	private SQLiteOpenHelper helper;
	private String name;
	private Class<E> type;
	private int size;
	
	private Context context;
	
	public PersistentBlockingQueue(Context context, String name, Class<E> type) {
		super();
		
		this.name    = name;
		this.type    = type;
		this.context = context;
		this.size    = -1;
		
		this.helper = new OmniataDBHelper(this.context, name);
		this.size = OmniataDBHelper.size(getDB(), name);
	}
	
	/**
	 * Helper method that returns the database bound to this persistent queue
	 * @return The SQLiteDatabase that holds this queue
	 */
	
	protected SQLiteDatabase getDB() {
		try {
			return helper.getWritableDatabase();
		} catch (SQLiteException e) {
			return null;
		}
	}

	/**
	 * Returns the head element of this queue without removing
	 * 
	 * @return The head of the queue or null if empty
	 */
	@Override
	public E peek() {
		E e = null;
		
		synchronized(this) {
			Cursor c = OmniataDBHelper.first(getDB(), name);
			
			if (c != null) {
				if (c.moveToFirst()) {
					e = instantiateE(c);
				}
				c.close();
			}
		}
		
		return e;
	}
	
	/**
	 * Returns the head element of this queue without removing
	 * 
	 * @return The head of the queue or null if empty
	 */
	public E blockingPeek() throws InterruptedException{
		E e = null;
		
		synchronized(this) {
			e = peek();
			while(e == null) {
				OmniataLog.d(TAG, "Queue Empty");
				wait();
				e = peek();
			}
		}
		
		return e;
	}
	
	private E instantiateE(Cursor c) {
		String data = c.getString(1);
		return instantiateE(data);
	}
	
	private E instantiateE(String data) {
		E element = null;
		
		try {
			Constructor<E> ctor = type.getConstructor(String.class);
			element = ctor.newInstance(data);
		} catch (Exception e) {
		}
		
		return element;
	}


	/**
	 * Removes and returns the head of the queue
	 * 
	 * @return The head of the queue
	 */
	@Override
	public E poll() {
		E e = null;

		// TODO: Determine if this is faster wrapped in a transaction
		synchronized (this) {
			Cursor c = OmniataDBHelper.first(getDB(), name);
			
			if (c != null) {
				if (c.moveToFirst() /*&& c.getCount() > 0 */) {
					int id = c.getInt(0);
					String data   = c.getString(1);
					c.close();
				
					OmniataDBHelper.delete(getDB(), name, id);
				
					if (--size == 0) {
						OmniataDBHelper.resetAutoIncrement(getDB(), name);
					}
				
					e = instantiateE(data);
				} else {
					c.close();
				}
			}
		}
		
		return e;
	}
	
	/**
	 * Clears the queue of all elements
	 */
	@Override
	public void clear() {
		synchronized(this) {
			OmniataDBHelper.deleteAll(getDB(), name);
		}
	}

	/**
	 * Removes and adds all elements from this queue into a collection
	 * 
	 * @param
	 */
	@Override
	public int drainTo(Collection<? super E> collection) {		
		synchronized(this) {
			Cursor c = OmniataDBHelper.all(getDB(), name);
			c.moveToFirst();
			for(int i = 0; i < c.getCount(); i++, c.moveToNext()) {
				E e = instantiateE(c);
				collection.add(e);
			}
			c.close();
		}
		
		return 0;
	}

	@Override
	public int drainTo(Collection<? super E> collection, int n) {
		// TODO: Implement
		return 0;
	}

	@Override
	public boolean offer(E e) {
		long rowID;
		
		synchronized(this) {
			String data = e.toString();
			rowID = OmniataDBHelper.insert(getDB(), name, data);
			
			if (rowID != -1) {
				size++;
				notifyAll();
			}
		}
		
		return rowID != -1;
	}

	@Override
	public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
		return offer(e);
	}

	@Override
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		return poll();
	}

	@Override
	public void put(E e) throws InterruptedException {
		offer(e);
	}

	@Override
	public int remainingCapacity() {
		return Integer.MAX_VALUE;
	}

	@Override
	public E take() throws InterruptedException {
		E e;
		
		synchronized (this) {
			e = poll();
			while(e == null) {
				OmniataLog.d(TAG, "Queue Empty");
				wait();
				e = poll();
			}
		}
		
		return e;
	}

	@Override
	public Iterator<E> iterator() {
		// TODO: Implement this
		return null;
	}

	@Override
	public int size() {
		synchronized(this) {
			return size;
		}
	}
}
