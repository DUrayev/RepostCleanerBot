package org.telegram.repostcleanerbot.factory;

import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.abilitybots.api.objects.ReplyFlow;
import org.telegram.repostcleanerbot.flow.AllChatsProcessing;
import org.telegram.repostcleanerbot.flow.Login;
import org.telegram.repostcleanerbot.flow.SpecificChatProcessing;
import org.telegram.repostcleanerbot.flow.state.*;
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

    public ReplyFlow getTwoStepVerificationPasswordEnteringState() {
        return InstantiationUtils.getInstance(TwoStepVerificationPasswordEnteringState.class).getFlow();
    }

    public ReplyFlow getPhoneNumberEnteringState() {
        return InstantiationUtils.getInstance(PhoneNumberEnteringState.class).getFlow();
    }

    public ReplyFlow getVerificationCodeEnteringState() {
        return InstantiationUtils.getInstance(VerificationCodeEnteringState.class).getFlow();
    }
}
