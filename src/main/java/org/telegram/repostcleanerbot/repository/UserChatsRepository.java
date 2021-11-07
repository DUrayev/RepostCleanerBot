package org.telegram.repostcleanerbot.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.telegram.repostcleanerbot.Constants;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.tdlib.entity.Chat;

import javax.inject.Inject;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserChatsRepository implements Repository<List<Chat>> {

    @Inject
    private BotContext botContext;

    private final Gson gson = new Gson();

    @Override
    public List<Chat> get(Long userId) {
        Map<String, String> userChatsDb = botContext.bot().db().getMap(Constants.DB.USER_CHATS);
        String userChatsJson = userChatsDb.get(userId.toString());
        Type listType = new TypeToken<ArrayList<Chat>>(){}.getType();
        return gson.fromJson(userChatsJson, listType);
    }

    @Override
    public void save(Long userId, List<Chat> userChats) {
        Map<String, String> userChatsDb = botContext.bot().db().getMap(Constants.DB.USER_CHATS);
        userChatsDb.put(userId.toString(), gson.toJson(userChats));
    }
}
