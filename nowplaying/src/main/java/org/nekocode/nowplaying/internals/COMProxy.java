/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.internals;

import lombok.extern.log4j.Log4j2;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Base class of an object that provides a proxy to a COM object.  Specifically,
 * by extending this class, the proxy object has the ability to execute all of
 * its actions on a single thread - a requirement for proper access to COM objects.
 *
 * @author dan.clark@nekocode.org
 */
@Log4j2
public class COMProxy {
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
