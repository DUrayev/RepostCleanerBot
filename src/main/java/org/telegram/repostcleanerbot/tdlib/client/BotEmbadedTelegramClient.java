package org.telegram.repostcleanerbot.tdlib.client;

import it.tdlight.client.*;
import it.tdlight.common.ConstructorDetector;
import it.tdlight.common.ExceptionHandler;
import it.tdlight.common.Init;
import it.tdlight.common.ResultHandler;
import it.tdlight.common.TelegramClient;
import it.tdlight.common.internal.CommonClientManager;
import it.tdlight.common.internal.InternalClient;
import it.tdlight.common.utils.CantLoadLibrary;
import it.tdlight.common.utils.LibraryVersion;
import it.tdlight.jni.TdApi;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BotEmbadedTelegramClient implements Authenticable {

    public static final Logger LOG = LoggerFactory.getLogger(BotEmbadedTelegramClient.class);

    static {
        try {
            Init.start();
        } catch (CantLoadLibrary e) {
            throw new RuntimeException("Can't load native libraries", e);
        }
    }

    private final TelegramClient client;
    private final TDLibSettings settings;
    private AuthenticationData authenticationData;

    private final Map<String, Set<CommandHandler>> commandHandlers = new ConcurrentHashMap<>();
    private final Set<ResultHandler<TdApi.Update>> updateHandlers = new ConcurrentHashMap<ResultHandler<TdApi.Update>, Object>().keySet(
            new Object());
    private final Set<ExceptionHandler> updateExceptionHandlers = new ConcurrentHashMap<ExceptionHandler, Object>().keySet(
            new Object());
    private final Set<ExceptionHandler> defaultExceptionHandlers = new ConcurrentHashMap<ExceptionHandler, Object>().keySet(
            new Object());

    private final CountDownLatch closed = new CountDownLatch(1);

    public BotEmbadedTelegramClient(TDLibSettings settings) {
        this.client = CommonClientManager.create(LibraryVersion.IMPLEMENTATION_NAME);
        this.settings = settings;
        this.addUpdateHandler(TdApi.UpdateAuthorizationState.class, new AuthorizationStateWaitForExit(this.closed));
    }

    public void handleUpdate(TdApi.Object update) {
        boolean handled = false;
        for (ResultHandler<TdApi.Update> updateHandler : updateHandlers) {
            updateHandler.onResult(update);
            handled = true;
        }
        if (!handled) {
            LOG.warn("An update was not handled, please use addUpdateHandler(handler) before starting the client!");
        }
    }

    private void handleUpdateException(Throwable ex) {
        boolean handled = false;
        for (ExceptionHandler updateExceptionHandler : updateExceptionHandlers) {
            updateExceptionHandler.onException(ex);
            handled = true;
        }
        if (!handled) {
            LOG.warn("Error received from Telegram!", ex);
        }
    }

    private void handleDefaultException(Throwable ex) {
        boolean handled = false;
        for (ExceptionHandler exceptionHandler : defaultExceptionHandlers) {
            exceptionHandler.onException(ex);
            handled = true;
        }
        if (!handled) {
            LOG.warn("Unhandled exception!", ex);
        }
    }

    private void handleResultHandlingException(Throwable ex) {
        LOG.error("Failed to handle the request result", ex);
    }

    @Override
    public AuthenticationData getAuthenticationData() {
        return authenticationData;
    }

    public TDLibSettings getSettings() {
        return settings;
    }

    @SuppressWarnings("unchecked")
    public <T extends TdApi.Update> void addUpdateHandler(Class<T> updateType, GenericUpdateHandler<T> handler) {
        int updateConstructor = ConstructorDetector.getConstructor(updateType);
        this.updateHandlers.add(update -> {
            if (update.getConstructor() == updateConstructor) {
                handler.onUpdate((T) update);
            }
        });
    }

    public void addUpdatesHandler(GenericUpdateHandler<TdApi.Update> handler) {
        this.updateHandlers.add(update -> {
            if (update instanceof TdApi.Update) {
                handler.onUpdate((TdApi.Update) update);
            } else {
                LOG.warn("Unknown update type: {}", update);
            }
        });
    }

    /**
     * Optional handler to handle errors received from TDLib
     */
    public void addUpdateExceptionHandler(ExceptionHandler updateExceptionHandler) {
        this.updateExceptionHandlers.add(updateExceptionHandler);
    }

    /**
     * Optional handler to handle uncaught errors (when using send without an appropriate error handler)
     */
    public void addDefaultExceptionHandler(ExceptionHandler defaultExceptionHandlers) {
        this.defaultExceptionHandlers.add(defaultExceptionHandlers);
    }

    /**
     * Start the client
     */
    public void start(AuthenticationData authenticationData) {
        this.authenticationData = authenticationData;
        createDirectories();
        client.initialize(this::handleUpdate, this::handleUpdateException, this::handleDefaultException);

        // Handle unexpected shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // Send close function
                this.client.send(new TdApi.Close(), ok -> {
                }, ex -> {
                });
                // Wait until the client has been closed successfully
                this.waitForExit();
            } catch (Throwable ignored) {
                // Ignore errors since we are shutting down everything
            }
        }));
    }

    private void createDirectories() {
        try {
            if (Files.notExists(settings.getDatabaseDirectoryPath())) {
                Files.createDirectories(settings.getDatabaseDirectoryPath());
            }
            if (Files.notExists(settings.getDownloadedFilesDirectoryPath())) {
                Files.createDirectories(settings.getDownloadedFilesDirectoryPath());
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Can't create TDLight directories", ex);
        }
    }

    /**
     * Send a function and get the result
     */
    public <R extends TdApi.Object> void send(TdApi.Function<R> function, GenericResultHandler<R> resultHandler) {
        client.send(function, result -> resultHandler.onResult(Result.of(result)), this::handleResultHandlingException);
    }

    /**
     * Execute a synchronous function.
     * <strong>Please note that only some functions can be executed using this method.</strong>
     * If you want to execute a function please use {@link #send(TdApi.Function, GenericResultHandler)}!
     */
    public <R extends TdApi.Object> Result<R> execute(TdApi.Function<R> function) {
        return Result.of(client.execute(function));
    }

    /**
     * Send the close signal but don't wait
     */
    public void sendClose() {
        client.send(new TdApi.Close(), ok -> {
            if (ok.getConstructor() == TdApi.Error.CONSTRUCTOR) {
                throw new TelegramError((TdApi.Error) ok);
            }
        });
    }

    /**
     * Send the close signal and wait for exit
     */
    public void closeAndWait() throws InterruptedException {
        client.send(new TdApi.Close(), ok -> {
            if (ok.getConstructor() == TdApi.Error.CONSTRUCTOR) {
                throw new TelegramError((TdApi.Error) ok);
            }
        });
        this.waitForExit();
    }

    /**
     * Wait until TDLight is closed
     */
    public void waitForExit() throws InterruptedException {
        closed.await();
    }

    public boolean isClientInitialized() {
        try {
            ((InternalClient)client).getClientId();
            return true;
        } catch (NullPointerException e) {
            return false;
        }
    }
}
