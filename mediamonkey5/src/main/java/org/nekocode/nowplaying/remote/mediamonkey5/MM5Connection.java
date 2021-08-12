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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;

public class MM5Connection {
    private static final Logger LOG = LogManager.getLogger(MM5Connection.class);
    private final Runtime runtime;
    private final ChromeDevToolsService devToolsService;
    private final ObjectMapper objectMapper;

    private final PropertyChangeSupport playbackStateListeners;


    public MM5Connection() {
        Properties properties = NowPlayingProperties.loadProperties();
        String host = properties.getProperty(NowPlayingProperties.REMOTE_MACHINE.name());
        int port = Integer.parseInt(properties.getProperty(NowPlayingProperties.REMOTE_PORT.name()));

        objectMapper = new ObjectMapper();
        playbackStateListeners = new PropertyChangeSupport(this);

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
            registerCallbacks();
        } else {
            throw new RuntimeException("Could not connect to Chrome Dev Tools");
        }
    }

    /**
     * valid property names: seekChange, playbackState, playbackEnd
     */
    void addPropertyChangeListener(PropertyChangeListener listener) {
        playbackStateListeners.addPropertyChangeListener(listener);
    }

    /**
     * Use the console to receive events
     */
    private void registerCallbacks() {
        try {
            String callbacks = """
                    var seekChange = e => console.debug('seekChange:' + e);
                    var playbackState = e => console.debug('playbackState:' + e);
                    var playbackEnd = e => console.debug('playbackEnd:' + e);
                                        
                    app.listen(app.player, 'seekChange', seekChange);
                    app.listen(app.player, 'playbackState', playbackState);
                    app.listen(app.player, 'playbackEnd', playbackEnd);
                    """;

            evaluateAsync(callbacks);
            runtime.onConsoleAPICalled(e -> {
                e.getArgs().forEach(a -> {
                    String value = a.getValue().toString();
                    if (isUniqueNotification(value))
                        setLastNotification(value);
                    else
                        return;

                    String[] chunks = value.split(":");
                    switch (chunks[0]) {
                        case "seekChange" -> {
                            LOG.info("event [{}] with value [{}]", chunks[0], chunks[1]);
                            playbackStateListeners.firePropertyChange(chunks[0], null, chunks[1]);
                        }
                        case "playbackState" -> {
                            LOG.info("event [{}] with value [{}]", chunks[0], chunks[1]);
                            playbackStateListeners.firePropertyChange(chunks[0], null, chunks[1]);
                        }
                        case "thumbnail" -> {
                            String url = String.join(":", List.of(chunks).subList(2, chunks.length));
                            playbackStateListeners.firePropertyChange(chunks[0], chunks[1], url);
                        }
                    }
                });
            });

        } catch (ScriptException e) {
            LOG.error("Error registering listeners:", e);
        }
    }

    private final Object notificationLock = new Object();
    /**
     * minimum time that must pass between identical notifications before they
     * are not considered a duplicate of the same event
     */
    private final static long DUPLICATE_NOTIFICATION_WINDOW_MS = 100;
    private long lastNotificationTime;
    private String lastNotificationValue;

    /**
     * A notification is unique if enough time has passed, or the strings are different.
     */
    private boolean isUniqueNotification(String notificationValue) {
        synchronized (notificationLock) {
            return System.currentTimeMillis() > (lastNotificationTime + DUPLICATE_NOTIFICATION_WINDOW_MS)
                    || !Objects.equals(lastNotificationValue, notificationValue);
        }
    }

    private void setLastNotification(String notificationValue) {
        synchronized (notificationLock) {
            lastNotificationTime = System.currentTimeMillis();
            lastNotificationValue = notificationValue;
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

    public void evaluateAsync(String script) throws ScriptException {
        Evaluate evaluate = runtime.evaluate(
                script,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                false,
                null,
                null);
        handleException(evaluate.getExceptionDetails());
    }

    public void close() {
        devToolsService.close();
    }
}
