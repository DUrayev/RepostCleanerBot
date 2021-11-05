package org.telegram.repostcleanerbot.state;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.repostcleanerbot.Constants;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.bot.State;
import org.telegram.repostcleanerbot.factory.KeyboardFactory;
import org.telegram.repostcleanerbot.tdlib.ClientManager;
import org.telegram.repostcleanerbot.tdlib.EventManager;
import org.telegram.repostcleanerbot.tdlib.client.BotEmbadedTelegramClient;
import org.telegram.repostcleanerbot.tdlib.entity.RepostsStat;
import org.telegram.repostcleanerbot.tdlib.request.DeleteMessagesRequest;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;
import static org.telegram.abilitybots.api.util.AbilityUtils.getUser;
import static org.telegram.repostcleanerbot.Constants.INLINE_BUTTONS;
import static org.telegram.repostcleanerbot.Constants.STATE_DB;

public class CleanRepostsFromSpecificChatState implements State {
    private final BotContext botContext;
    private static Gson gson = new Gson();

    public CleanRepostsFromSpecificChatState(BotContext botContext) {
        this.botContext = botContext;
    }

    @Override
    public Reply getReply() {
        return Reply.of((bot, upd) -> {
            String selectedChatToClean = upd.getMessage().getText();
            botContext.hidePreviousReplyMarkup(upd);

            if(selectedChatToClean.equals(INLINE_BUTTONS.CANCEL_KEYBOARD_BUTTON)) {
                finishCleaningAndNotify(upd, false);
            } else {
                Map<String, String> userChatsRepostedFromDb = botContext.bot().db().getMap(Constants.DB.USER_REPOSTS_STAT_IN_SPECIFIC_CHANNEL);
                String userChatsRepostedFromJson = userChatsRepostedFromDb.get(getUser(upd).getId().toString());
                Type listType = new TypeToken<ArrayList<RepostsStat>>(){}.getType();
                List<RepostsStat> userRepostsStatList = gson.fromJson(userChatsRepostedFromJson, listType);
                RepostsStat selectedChatRepostedFromToClean = userRepostsStatList.stream().filter(repostsStat -> repostsStat.getRepostedFrom().getTitle().equals(upd.getMessage().getText())).findFirst().get();
                List<Long> repostMessageIdList = selectedChatRepostedFromToClean.getRepostMessageIdList();

                BotEmbadedTelegramClient client = ClientManager.getInstance().getTelegramClientForUser(getUser(upd).getId(), botContext.getTdLibSettings());
                EventManager deleteMessagesEventManager = new EventManager();
                DeleteMessagesRequest deleteMessagesRequest = new DeleteMessagesRequest(client, deleteMessagesEventManager);

                deleteMessagesEventManager.addEventHandler(DeleteMessagesRequest.EVENTS.FINISH, deleteResult -> {
                    if((Boolean) deleteResult) {
                        userRepostsStatList.remove(selectedChatRepostedFromToClean);
                        if(userRepostsStatList.size() > 0) {
                            userChatsRepostedFromDb.put(getUser(upd).getId().toString(), gson.toJson(userRepostsStatList));
                            botContext.execute(
                                    SendMessage.builder()
                                            .text("Reposts from chat '" + selectedChatToClean + "' are cleaned.\nWould you like to clean more reposts?")
                                            .chatId(getChatId(upd).toString())
                                            .replyMarkup(KeyboardFactory.replyKeyboardWithCancelButtons(userRepostsStatList.stream().map(r -> r.getRepostedFrom().getTitle()).collect(Collectors.toList()), "Select channel name"))
                                            .allowSendingWithoutReply(false)
                                            .build());
                        } else {
                            finishCleaningAndNotify(upd, true);
                        }
                    } else {
                        botContext.execute(
                                SendMessage.builder()
                                        .text("Reposts weren't cleaned in chat '" + selectedChatToClean + "' because of some unexpected error.\nYou can try one more time or cancel")
                                        .chatId(getChatId(upd).toString())
                                        .replyMarkup(KeyboardFactory.replyKeyboardWithCancelButtons(userRepostsStatList.stream().map(r -> r.getRepostedFrom().getTitle()).collect(Collectors.toList()), "Select channel name"))
                                        .allowSendingWithoutReply(false)
                                        .build());
                    }
                });
                deleteMessagesRequest.execute(selectedChatRepostedFromToClean.getRepostedIn().getId(), repostMessageIdList, true);
            }
        },
            botContext.isChatInState(STATE_DB.CLEAN_REPOSTS_FROM_SPECIFIC_CHAT_STATE_DB),
            chatWithRepostsFromReplyKeyboardSelectedOrCancelBtn()
        );
    }

    private void finishCleaningAndNotify(Update upd, boolean allRepostsAreCleaned) {
        botContext.exitState(upd, STATE_DB.CLEAN_REPOSTS_FROM_SPECIFIC_CHAT_STATE_DB);
        Map<String, String> userChatsRepostedFromDb = botContext.bot().db().getMap(Constants.DB.USER_REPOSTS_STAT_IN_SPECIFIC_CHANNEL);
        userChatsRepostedFromDb.put(getUser(upd).getId().toString(), "[]");

        String responseText = "Ok, cleaning finished. You can /start again";
        if(allRepostsAreCleaned) {
            responseText = "What a spy! All reposts are cleaned. You can /start again";
        }
        botContext.execute(
                SendMessage.builder()
                        .text(responseText)
                        .chatId(getChatId(upd).toString())
                        .replyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build())
                        .allowSendingWithoutReply(false)
                        .build());
    }

    private Predicate<Update> chatWithRepostsFromReplyKeyboardSelectedOrCancelBtn() {
        return upd -> {
            if(botContext.cancelKeyboardButtonSelected().test(upd)) {
                return true;
            }
            if(upd.hasMessage()) {
                Map<String, String> userChatsRepostedFromDb = botContext.bot().db().getMap(Constants.DB.USER_REPOSTS_STAT_IN_SPECIFIC_CHANNEL);
                String userChatsRepostedFromJson = userChatsRepostedFromDb.get(getUser(upd).getId().toString());
                Type listType = new TypeToken<ArrayList<RepostsStat>>(){}.getType();
                List<RepostsStat> userRepostsStatList = gson.fromJson(userChatsRepostedFromJson, listType);
                return userRepostsStatList.stream().anyMatch(repostsStat -> repostsStat.getRepostedFrom().getTitle().equals(upd.getMessage().getText()));
            } else {
                return false;
            }
        };
    }
}
