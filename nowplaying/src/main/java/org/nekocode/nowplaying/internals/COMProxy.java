/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.internals;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

/**
 * Base class of an object that provides a proxy to a COM object.  Specifically,
 * by extending this class, the proxy object has the ability to execute all of
 * its actions on a single thread - a requirement for proper access to COM objects.
 *
 * @author fanguad@nekocode.org
 */
public class COMProxy {
	private static final Logger log = Logger.getLogger(COMProxy.class);

	protected ExecutorService executor;

	public COMProxy() {
		executor = Executors.newSingleThreadExecutor();
	}

	public COMProxy(ExecutorService executor) {
		this.executor = executor;
	}

	public void submit(Runnable r) {
		try {
			executor.submit(r).get();
		} catch (InterruptedException e) {
			// ignore
		} catch (java.util.concurrent.ExecutionException e) {
			// NOTE: this exception will be generated if the info window is open
			log.warn(e);
		}
	}

	public <T> T submit(Callable<T> r) {
		try {
			return executor.submit(r).get();
		} catch (InterruptedException e) {
			// ignore
			return null;
		} catch (java.util.concurrent.ExecutionException e) {
			// NOTE: this exception will be generated if the info window is open
			log.warn(e);
			return null;
		}
	}

	public void shutdown() {
		executor.shutdown();
	}
}
