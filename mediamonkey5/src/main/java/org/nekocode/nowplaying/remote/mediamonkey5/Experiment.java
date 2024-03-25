/*
 * Copyright (c) 2024. Dan Clark
 */

package org.nekocode.nowplaying.remote.mediamonkey5;

import com.github.kklisura.cdt.protocol.commands.Runtime;
import com.github.kklisura.cdt.protocol.events.runtime.ExceptionThrown;
import com.github.kklisura.cdt.protocol.types.runtime.AwaitPromise;
import com.github.kklisura.cdt.protocol.types.runtime.Evaluate;
import com.github.kklisura.cdt.protocol.types.runtime.ExceptionDetails;
import com.github.kklisura.cdt.protocol.types.runtime.RemoteObject;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import com.github.kklisura.cdt.services.ChromeService;
import com.github.kklisura.cdt.services.impl.ChromeServiceImpl;

import java.util.Objects;
import java.util.Optional;

public class Experiment {
    public static void main(String[] args) {
        final ChromeService chromeService = new ChromeServiceImpl("localhost", 9222);

        chromeService.getTabs().forEach(tab -> System.out.println(tab.toString()));
        Optional<ChromeDevToolsService> devToolsServiceOptional = chromeService.getTabs()
                .stream()
                .filter(tab -> Objects.equals("file:///mainwindow.html", tab.getUrl()))
                .findFirst()
                .map(chromeService::createDevToolsService);
        if (devToolsServiceOptional.isPresent())
        {
            ChromeDevToolsService devToolsService = devToolsServiceOptional.get();
            // get "Player" class
            // call playPauseAsync (returns promise that completes when done)
            evaluateAsyncAndWait(devToolsService, "player.playPauseAsync()");
            Object o = evaluate(devToolsService, "player.getCurrentTrack()");
            System.out.println(o);
        }
    }

    public static void evaluateOnly(ChromeDevToolsService devToolsService, String script)
    {
        Runtime runtime = devToolsService.getRuntime();
        Evaluate evaluate = runtime.evaluate(script);
        checkException(evaluate.getExceptionDetails());
    }

    public static <T> T evaluatePromise(ChromeDevToolsService devToolsService, String promiseScript)
    {
        Runtime runtime = devToolsService.getRuntime();
        Evaluate evaluate = runtime.evaluate(
                promiseScript,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                null);
        checkException(evaluate.getExceptionDetails());
        RemoteObject evaluateResult = evaluate.getResult();

//        AwaitPromise promise = runtime.awaitPromise(evaluateResult.getObjectId(), true, false);
//        checkException(promise.getExceptionDetails());

//        RemoteObject pResult = promise.getResult();
//        @SuppressWarnings("unchecked")
        T result = (T) evaluateResult.getValue();
        return result;
    }

    public static <T> T evaluate(ChromeDevToolsService devToolsService, String promiseScript)
    {
        Runtime runtime = devToolsService.getRuntime();
        Evaluate evaluate = runtime.evaluate(
                promiseScript,
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        checkException(evaluate.getExceptionDetails());
        RemoteObject evaluateResult = evaluate.getResult();

//        AwaitPromise promise = runtime.awaitPromise(evaluateResult.getObjectId(), true, false);
//        checkException(promise.getExceptionDetails());

//        RemoteObject pResult = promise.getResult();
//        @SuppressWarnings("unchecked")
        T result = (T) evaluateResult.getValue();
        return result;
    }

    public static void evaluateAsyncAndWait(ChromeDevToolsService devToolsService, String promiseScript)
    {
        Runtime runtime = devToolsService.getRuntime();
        Evaluate evaluate = runtime.evaluate(
                promiseScript,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                null);
        checkException(evaluate.getExceptionDetails());
    }

    public static <T> T evaluatePromise_orig(ChromeDevToolsService devToolsService, String promiseScript)
    {
        Runtime runtime = devToolsService.getRuntime();
        Evaluate evaluate = runtime.evaluate(promiseScript);
        checkException(evaluate.getExceptionDetails());
        RemoteObject evaluateResult = evaluate.getResult();

        AwaitPromise promise = runtime.awaitPromise(evaluateResult.getObjectId(), true, false);
        checkException(promise.getExceptionDetails());

        RemoteObject pResult = promise.getResult();
        @SuppressWarnings("unchecked")
        T result = (T) pResult.getValue();
        return result;
    }

    protected static void checkException(ExceptionDetails exceptionDetails)
    {
        if (exceptionDetails != null)
        {
            String exceptionString = null;
            RemoteObject exception = exceptionDetails.getException();
            if (exception != null)
            {
                if (exception.getDescription() != null)
                {
                    exceptionString = exception.getDescription();
                }
                else if (exception.getValue() != null)
                {
                    exceptionString = exception.getValue().toString();
                }
            }
//            log.error("Script error: " + exceptionDetails.getText()
//                    + ", exception: " + exceptionString);

            throw new RuntimeException("Script failed: " + exceptionDetails.getText());
        }
    }

    private static void pageExceptionEvent(ExceptionThrown event)
    {

    }
}
