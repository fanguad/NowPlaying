package org.nekocode.nowplaying.remote.mediamonkey5;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kklisura.cdt.protocol.commands.Runtime;
import com.github.kklisura.cdt.protocol.events.runtime.ExceptionThrown;
import com.github.kklisura.cdt.protocol.types.runtime.Evaluate;
import com.github.kklisura.cdt.protocol.types.runtime.ExceptionDetails;
import com.github.kklisura.cdt.protocol.types.runtime.RemoteObject;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import com.github.kklisura.cdt.services.ChromeService;
import com.github.kklisura.cdt.services.impl.ChromeServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nekocode.nowplaying.NowPlayingProperties;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;

public class MM5Connection {
    private static final Logger LOG = LogManager.getLogger(MM5Connection.class);
    private final Runtime runtime;
    private final ChromeDevToolsService devToolsService;
    private final ObjectMapper objectMapper;

    public MM5Connection() {
        Properties properties = NowPlayingProperties.loadProperties();
        String host = properties.getProperty(NowPlayingProperties.REMOTE_MACHINE.name());
        int port = Integer.parseInt(properties.getProperty(NowPlayingProperties.REMOTE_PORT.name()));

        objectMapper = new ObjectMapper();

        ChromeService chromeService = new ChromeServiceImpl(host, port);
        Optional<ChromeDevToolsService> devToolsServiceOptional = chromeService.getTabs()
                .stream()
                .filter(tab -> Objects.equals("file:///mainwindow.html", tab.getUrl()))
                .findFirst()
                .map(chromeService::createDevToolsService);
        if (devToolsServiceOptional.isPresent()) {
            // Get DevTools service to this tab
            devToolsService = devToolsServiceOptional.get();
            runtime = devToolsService.getRuntime();
            runtime.onExceptionThrown(this::handleRuntimeException);
            runtime.enable();
        } else {
            throw new RuntimeException("Could not connect to Chrome Dev Tools");
        }

    }


    private void handleRuntimeException(ExceptionThrown exceptionThrown) {
        try {
            String exceptionDetails = objectMapper.writeValueAsString(exceptionThrown);
            LOG.error("Exception from Chrome Dev Tools: {}", exceptionDetails);
        } catch (JsonProcessingException e) {
            LOG.error("Exception serializing Chrome Dev Tools exception");
        }
    }

    private void handleException(ExceptionDetails exceptionDetails) throws ScriptException {
        Consumer<String> logMessage = it -> LOG.error("Script Error: {}.  Details: {}", it, exceptionDetails.getText());
        Optional.ofNullable(exceptionDetails)
                .map(ExceptionDetails::getException)
                .map(RemoteObject::getDescription)
                .ifPresent(logMessage);
        Optional.ofNullable(exceptionDetails)
                .map(ExceptionDetails::getException)
                .map(RemoteObject::getValue)
                .map(Object::toString)
                .ifPresent(logMessage);

        if (exceptionDetails != null) {
            throw new ScriptException("Script failed: " + exceptionDetails.getText());
        }
    }

    public <T> T evaluate(String script) throws ScriptException {
        Evaluate evaluate = runtime.evaluate(
                script,
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                null,
                null,
                null);
        handleException(evaluate.getExceptionDetails());
        RemoteObject evaluateResult = evaluate.getResult();

        T result = (T) evaluateResult.getValue();
        return result;
    }


    public void evaluateAsyncAndWait(String promiseScript) throws ScriptException {
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
                null);
        handleException(evaluate.getExceptionDetails());
    }

    public void close() {
        devToolsService.close();
    }
}
