package org.telegram.repostcleanerbot.tdlib.entity;

import lombok.Data;

import java.util.*;

@Data
public class RepostsStat {
    private Chat repostedFrom;
    private Chat repostedIn;
    private List<Long> repostMessageIdList = new ArrayList<>();
    private int repostedByMeCount = 0;
    private int repostedNotByMeCount = 0;

    public RepostsStat(Chat repostedFrom, Chat repostedIn) {
        this.repostedFrom = repostedFrom;
        this.repostedIn = repostedIn;
    }

    public void incrementRepostedByMe() {
        this.repostedByMeCount++;
    }

    public void incrementRepostedNotByMe() {
        this.repostedNotByMeCount++;
    }

    public void addNewRepostMessagedId(long repostMessageId) {
        this.repostMessageIdList.add(repostMessageId);
    }


    public static List<RepostsStat> generate(List<Repost> repostLists, long userId) {
        List<RepostsStat> result = new ArrayList<>();
        repostLists.forEach(repost -> {
            Chat repostedFrom = repost.getRepostedFrom();
            Optional<RepostsStat> repostsStatOptional = result.stream().filter(r -> r.getRepostedFrom().getId() == repostedFrom.getId()).findFirst();
            RepostsStat repostsStat;
            if(repostsStatOptional.isPresent()) {
                repostsStat = repostsStatOptional.get();
            } else {
                repostsStat = new RepostsStat(repostedFrom, repost.getRepostedIn());
                result.add(repostsStat);
            }
            repostsStat.addNewRepostMessagedId(repost.getId());
            if(repost.getRepostedBy() == userId) {
                repostsStat.incrementRepostedByMe();
            } else {
                repostsStat.incrementRepostedNotByMe();
            }
        });
        return result;
    }
}
