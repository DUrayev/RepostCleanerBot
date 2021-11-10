package org.telegram.repostcleanerbot.tdlib;

import it.tdlight.client.APIToken;
import it.tdlight.client.TDLibSettings;
import org.telegram.repostcleanerbot.tdlib.client.BotEmbadedTelegramClient;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ClientManager {

    @Inject
    private APIToken apiToken;

    private Map<Long, BotEmbadedTelegramClient> clientPerUser = new HashMap<>();

    synchronized public BotEmbadedTelegramClient getTelegramClientForUser(long userId) {
        TDLibSettings settings = TDLibSettings.create(apiToken);
        BotEmbadedTelegramClient client = clientPerUser.get(userId);
        if(client == null) {
            // Configure the session directory
            Path sessionPath = Paths.get("tdlight-session\\" + userId);
            settings.setDatabaseDirectoryPath(sessionPath.resolve("data"));
            settings.setDownloadedFilesDirectoryPath(sessionPath.resolve("downloads"));

            client = new BotEmbadedTelegramClient(settings);
            clientPerUser.put(userId, client);
        }
        return client;
    }

    public void removeClientForUser(Long userId) {
        clientPerUser.remove(userId);
    }
}
