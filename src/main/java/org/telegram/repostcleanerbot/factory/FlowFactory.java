package org.telegram.repostcleanerbot.factory;

import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.abilitybots.api.objects.ReplyFlow;
import org.telegram.repostcleanerbot.flow.AllChatsProcessing;
import org.telegram.repostcleanerbot.flow.Login;
import org.telegram.repostcleanerbot.flow.SpecificChatProcessing;
import org.telegram.repostcleanerbot.state.CleanRepostsFromAllChatsState;
import org.telegram.repostcleanerbot.state.CleanRepostsFromSpecificChatState;
import org.telegram.repostcleanerbot.state.PasswordEnteringState;
import org.telegram.repostcleanerbot.utils.InstantiationUtils;

public class FlowFactory {

    public ReplyFlow getLoginFlow() {
        return InstantiationUtils.getInstance(Login.class).getFlow();
    }

    public ReplyFlow getAllChatsProcessingFlow() {
        return InstantiationUtils.getInstance(AllChatsProcessing.class).getFlow();
    }

    public ReplyFlow getSpecificChatProcessingFlow() {
        return InstantiationUtils.getInstance(SpecificChatProcessing.class).getFlow();
    }

    public Reply getCleanRepostsFromAllChatsState() {
        return InstantiationUtils.getInstance(CleanRepostsFromAllChatsState.class).getReply();
    }

    public Reply getCleanRepostsFromSpecificChatState() {
        return InstantiationUtils.getInstance(CleanRepostsFromSpecificChatState.class).getReply();
    }

    public ReplyFlow getPasswordEnteringState() {
        return InstantiationUtils.getInstance(PasswordEnteringState.class).getFlow();
    }
}
