package org.telegram.repostcleanerbot.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.telegram.repostcleanerbot.Constants;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.tdlib.entity.Repost;

import javax.inject.Inject;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserAllRepostsGroupedByRepostedFromRepository implements Repository<List<List<Repost>>> {

    @Inject
    private BotContext botContext;

    private final Gson gson = new Gson();

    @Override
    public List<List<Repost>> get(Long userId) {
        Map<String, String> userAllRepostsGroupedByRepostedFromDb = botContext.bot().db().getMap(Constants.DB.USER_ALL_REPOSTS_GROUPED_BY_REPOSTED_FROM);
        String userAllRepostsGroupedByRepostedFromJson = userAllRepostsGroupedByRepostedFromDb.get(userId.toString());
        Type listType = new TypeToken<ArrayList<ArrayList<Repost>>>(){}.getType();
        return gson.fromJson(userAllRepostsGroupedByRepostedFromJson, listType);
    }

    @Override
    public void save(Long userId, List<List<Repost>> repostsListGroupedByRepostedFrom) {
        Map<String, String> userAllRepostsGroupedByRepostedFromDb = botContext.bot().db().getMap(Constants.DB.USER_ALL_REPOSTS_GROUPED_BY_REPOSTED_FROM);
        userAllRepostsGroupedByRepostedFromDb.put(userId.toString(), gson.toJson(repostsListGroupedByRepostedFrom));
    }
}
