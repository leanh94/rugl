
package com.ryanm.droid.rugl.res;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.res.Resources;
import android.util.Log;

/**
 * An asynchronous resource-loading service. Maintains two threads:
 * one to do the loading IO, one to do post-loading processing
 * 
 * @author ryanm
 */
public class ResourceLoader
{
	/***/
	public static Resources resources;

	private static List<Loader> complete = Collections
			.synchronizedList( new LinkedList<Loader>() );

	private static ExecutorService loaderService = Executors.newSingleThreadExecutor();

	private static AtomicInteger queueSize = new AtomicInteger( 0 );

	private static ExecutorService postLoaderService = Executors.newSingleThreadExecutor();

	/***/
	public static final String LOG_TAG = "ResourceLoader";

	/**
	 * Starts the loader thread
	 * 
	 * @param resources
	 */
	public static void start( Resources resources )
	{
		ResourceLoader.resources = resources;
	}

	/**
	 * Asynchronously load a resource
	 * 
	 * @param l
	 */
	public static void load( Loader l )
	{
		queueSize.incrementAndGet();
		loaderService.submit( new LoaderRunnable( l ) );
	}

	/**
	 * Synchronously load a resource
	 * 
	 * @param l
	 */
	public static void loadNow( Loader l )
	{
		l.load();
		l.postLoad();
		l.complete();
	}

	/**
	 * Call this in the main thread, it'll cause completed loaders to
	 * call {@link Loader#complete()}
	 */
	public static void checkCompletion()
	{
		while( !complete.isEmpty() )
		{
			Loader l = complete.remove( 0 );
			queueSize.decrementAndGet();
			Log.i( LOG_TAG, "Loaded resource " + l );

			l.complete();
		}
	}

	/**
	 * Gets the size of the loader queue
	 * 
	 * @return the number of loaders waiting to be executed
	 */
	public static int queueSize()
	{
		return queueSize.get();
	}

	/**
	 * Override this class to load resources
	 * 
	 * @author ryanm
	 * @param <T>
	 */
	public static abstract class Loader<T>
	{
		/**
		 * The loaded resource
		 */
		protected T resource;

		/**
		 * Overload this to do the loading and set {@link #resource}.
		 * This is called on a common loading thread
		 */
		public abstract void load();

		/**
		 * This method is called on its own thread. Use it to do any
		 * processing
		 */
		public void postLoad()
		{
		};

		/**
		 * This is called on the main thread when loading is complete
		 */
		public abstract void complete();
	}

	private static class LoaderRunnable implements Runnable
	{
		private final Loader loader;

		private boolean loaded = false;

		private LoaderRunnable( Loader loader )
		{
			this.loader = loader;
		}

		@Override
		public void run()
		{
			if( !loaded )
			{
				loader.load();
				loaded = true;
				postLoaderService.submit( this );
			}
			else
			{
				loader.postLoad();
				complete.add( loader );
			}
		}
	}
}
