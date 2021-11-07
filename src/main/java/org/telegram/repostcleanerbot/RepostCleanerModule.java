package org.telegram.repostcleanerbot;

import it.tdlight.client.APIToken;
import org.codejargon.feather.Provides;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.repository.UserAllRepostsGroupedByRepostedFromRepository;
import org.telegram.repostcleanerbot.repository.UserChatsRepository;
import org.telegram.repostcleanerbot.repository.UserChatsRepostedFromRepository;
import org.telegram.repostcleanerbot.tdlib.ClientManager;
import org.telegram.repostcleanerbot.utils.InstantiationUtils;

import javax.inject.Named;
import javax.inject.Singleton;

public class RepostCleanerModule {

    @Provides
    @Named(Constants.BOT_TOKEN)
    @Singleton
    public String getBotToken() {
        return System.getenv().get(Constants.BOT_TOKEN);
    }

    @Provides
    @Named(Constants.BOT_NAME)
    @Singleton
    public String getBotName() {
        return System.getenv().get(Constants.BOT_NAME);
    }

    @Provides
    @Named(Constants.BOT_ADMIN_ID)
    @Singleton
    public String getBotAdminId() {
        return System.getenv().get(Constants.BOT_ADMIN_ID);
    }

    @Provides
    @Named(Constants.BOT_API_ID)
    @Singleton
    public String getBotApiId() {
        return System.getenv().get(Constants.BOT_API_ID);
    }

    @Provides
    @Named(Constants.BOT_API_HASH)
    @Singleton
    public String getBotApiHash() {
        return System.getenv().get(Constants.BOT_API_HASH);
    }


    @Provides
    @Singleton
    public APIToken getClientApiToken(@Named(Constants.BOT_API_ID) String apiId, @Named(Constants.BOT_API_HASH) String apiHash) {
        return new APIToken(Integer.parseInt(apiId), apiHash);
    }

    @Provides
    @Singleton
    public AbilityBot getAbilityBot() {
        return InstantiationUtils.feather.instance(RepostCleanerAbilityBot.class);
    }

    @Provides
    @Singleton
    public BotContext getBotContext() {
        return InstantiationUtils.getInstance(BotContext.class, true);
    }

    @Provides
    @Singleton
    public UserAllRepostsGroupedByRepostedFromRepository getUserAllRepostsGroupedByRepostedFromRepository() {
        return InstantiationUtils.getInstance(UserAllRepostsGroupedByRepostedFromRepository.class, true);
    }

    @Provides
    @Singleton
    public UserChatsRepostedFromRepository getUserChatsRepostedFromRepository() {
        return InstantiationUtils.getInstance(UserChatsRepostedFromRepository.class, true);
    }

    @Provides
    @Singleton
    public UserChatsRepository getUserChatsRepository() {
        return InstantiationUtils.getInstance(UserChatsRepository.class, true);
    }

    @Provides
    @Singleton
    public ClientManager getClientManager() {
        return InstantiationUtils.getInstance(ClientManager.class, true);
    }

}
