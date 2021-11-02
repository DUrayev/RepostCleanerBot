package org.telegram.repostcleanerbot.factory;

import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.abilitybots.api.objects.ReplyFlow;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.flow.*;

public class FlowFactory {

    public static ReplyFlow getLoginFlow(BotContext botContext) {
        return new Login(botContext).getFlow();
    }

    public static ReplyFlow getAllChatsProcessingFlow(BotContext botContext) {
        return new AllChatsProcessing(botContext).getFlow();
    }

    public static ReplyFlow getSpecificChatProcessingFlow(BotContext botContext) {
        return new SpecificChatProcessing(botContext).getFlow();
    }

    public static Reply getCleanRepostsFromAllChatsState(BotContext botContext) {
        return new CleanRepostsFromAllChatsState(botContext).getReply();
    }

    public static Reply getCleanRepostsFromSpecificChatState(BotContext botContext) {
        return new CleanRepostsFromSpecificChatState(botContext).getReply();
    }
}
