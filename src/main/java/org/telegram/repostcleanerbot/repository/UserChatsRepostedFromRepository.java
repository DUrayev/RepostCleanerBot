package org.telegram.repostcleanerbot.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.telegram.repostcleanerbot.Constants;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.tdlib.entity.RepostsStat;

import javax.inject.Inject;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserChatsRepostedFromRepository implements Repository<List<RepostsStat>> {

    @Inject
    private BotContext botContext;

    private final Gson gson = new Gson();

    @Override
    public List<RepostsStat> get(Long userId) {
        Map<String, String> userChatsRepostedFromDb = botContext.bot().db().getMap(Constants.DB.USER_REPOSTS_STAT_IN_SPECIFIC_CHANNEL);
        String userChatsRepostedFromJson = userChatsRepostedFromDb.get(userId.toString());
        Type listType = new TypeToken<ArrayList<RepostsStat>>(){}.getType();
        return gson.fromJson(userChatsRepostedFromJson, listType);
    }

    @Override
    public void save(Long userId, List<RepostsStat> userRepostsStatList) {
        Map<String, String> userChatsRepostedFromDb = botContext.bot().db().getMap(Constants.DB.USER_REPOSTS_STAT_IN_SPECIFIC_CHANNEL);
        userChatsRepostedFromDb.put(userId.toString(), gson.toJson(userRepostsStatList));
    }
}
