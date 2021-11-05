package org.telegram.repostcleanerbot.flow;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.abilitybots.api.objects.ReplyFlow;
import org.telegram.repostcleanerbot.Constants;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.bot.Flow;
import org.telegram.repostcleanerbot.factory.KeyboardFactory;
import org.telegram.repostcleanerbot.tdlib.ClientManager;
import org.telegram.repostcleanerbot.tdlib.EventManager;
import org.telegram.repostcleanerbot.tdlib.client.BotEmbadedTelegramClient;
import org.telegram.repostcleanerbot.tdlib.entity.Chat;
import org.telegram.repostcleanerbot.tdlib.entity.Repost;
import org.telegram.repostcleanerbot.tdlib.entity.RepostsStat;
import org.telegram.repostcleanerbot.tdlib.request.GetChatsRequest;
import org.telegram.repostcleanerbot.tdlib.request.GetRepostsFromChatRequest;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;
import static org.telegram.abilitybots.api.util.AbilityUtils.getUser;
import static org.telegram.repostcleanerbot.Constants.CHATS_LIMIT;
import static org.telegram.repostcleanerbot.Constants.DB;

public class SpecificChatProcessing implements Flow {
    private final BotContext botContext;

    private static Gson gson = new Gson();

    public SpecificChatProcessing(BotContext botContext) {
        this.botContext = botContext;
    }

    @Override
    public ReplyFlow getFlow() {
        ReplyFlow chatToCleanSelectionFlow = ReplyFlow.builder(botContext.bot().db())
                .onlyIf(chatFromReplyKeyboardSelected())
                .action((bot, upd) -> {
                    String selectedTitleChatToClean = upd.getMessage().getText();

                    Map<String, String> userChatsDb = botContext.bot().db().getMap(DB.USER_CHATS);
                    String userChatsJson = userChatsDb.get(getUser(upd).getId().toString());
                    Type listType = new TypeToken<ArrayList<Chat>>(){}.getType();
                    List<Chat> userChats = gson.fromJson(userChatsJson, listType);
                    userChatsDb.put(getUser(upd).getId().toString(), "[]");

                    Chat selectedChatToClean = userChats.stream().filter(chat -> chat.getTitle().equals(selectedTitleChatToClean)).findFirst().get();

                    BotEmbadedTelegramClient client = ClientManager.getInstance().getTelegramClientForUser(getUser(upd).getId(), botContext.getTdLibSettings());
                    EventManager getRepostsFromChatEventManager = new EventManager();
                    GetRepostsFromChatRequest getRepostsFromChatRequest = new GetRepostsFromChatRequest(client, getRepostsFromChatEventManager);

                    getRepostsFromChatEventManager.addEventHandler(GetRepostsFromChatRequest.EVENTS.FINISH, allRepostsInSelectedChannelResult -> {
                        List<Repost> allRepostsInSelectedChannel = (List<Repost>)allRepostsInSelectedChannelResult;
                        int totalRepostsCount = allRepostsInSelectedChannel.size();
                        if(totalRepostsCount > 0) {
                            List<RepostsStat> repostsStatList = RepostsStat.generate(allRepostsInSelectedChannel, getUser(upd).getId());

                            List<RepostsStat> repostsStatSortedList = repostsStatList.stream().sorted(Comparator.comparing(r -> r.getRepostedByMeCount() + r.getRepostedNotByMeCount())).collect(Collectors.toList());
                            Collections.reverse(repostsStatSortedList);
                            repostsStatSortedList.forEach(repostsStat -> {
                                String newTitleWithStatInfo = repostsStat.getRepostedFrom().getTitle() + " [You: " + repostsStat.getRepostedByMeCount() + ", Not you: " + repostsStat.getRepostedNotByMeCount() + "]";
                                repostsStat.getRepostedFrom().setTitle(newTitleWithStatInfo);
                            });

                            Map<String, String> userRepostsStatInSpecificChannelDb = botContext.bot().db().getMap(DB.USER_REPOSTS_STAT_IN_SPECIFIC_CHANNEL);
                            userRepostsStatInSpecificChannelDb.put(getUser(upd).getId().toString(), gson.toJson(repostsStatSortedList));

                            botContext.enterState(upd, Constants.STATE_DB.CLEAN_REPOSTS_FROM_SPECIFIC_CHAT_STATE_DB);
                            botContext.execute(
                                    SendMessage.builder()
                                            .text("Analyzing is finished\nYou have " + totalRepostsCount + " reposts in chat " + selectedTitleChatToClean + "\nWhich reposts do you want to clean?")
                                            .chatId(getChatId(upd).toString())
                                            .replyMarkup(KeyboardFactory.replyKeyboardWithCancelButtons(repostsStatSortedList.stream().map(r -> r.getRepostedFrom().getTitle()).collect(Collectors.toList()), "Select channel name"))
                                            .allowSendingWithoutReply(false)
                                            .build());
                        } else {
                            botContext.bot().silent().send("Analyzing is finished, you don't have any reposts in chat " + selectedTitleChatToClean + "\nYou can /start process again", getChatId(upd));
                        }

                    });
                    botContext.bot().silent().execute(
                            SendMessage.builder()
                                    .text("List of reposts in chat " + selectedTitleChatToClean + " is requested\nPlease, wait...")
                                    .chatId(getChatId(upd).toString())
                                    .replyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build())
                                    .allowSendingWithoutReply(false)
                                    .build());

                    String analyzedMessagesCountNotification = "%s messages were analyzed. Please, wait...";
                    AtomicInteger analyzedMessagesCount = new AtomicInteger(0);
                    Optional<Message> sentMessageOptional = botContext.bot().silent().execute(
                            SendMessage.builder()
                                    .text(String.format(analyzedMessagesCountNotification, analyzedMessagesCount.get()))
                                    .chatId(getChatId(upd).toString())
                                    .replyMarkup(KeyboardFactory.withOneLineButtons())
                                    .allowSendingWithoutReply(false)
                                    .build());
                    Integer messageWithProcessedCountInfoId = -1;
                    if(sentMessageOptional.isPresent()) {
                        messageWithProcessedCountInfoId = sentMessageOptional.get().getMessageId();
                    }

                    Integer finalMessageWithProcessedCountInfoId = messageWithProcessedCountInfoId;
                    getRepostsFromChatEventManager.addEventHandler(GetRepostsFromChatRequest.EVENTS.BATCH_OF_MESSAGES_RECEIVED, messagesBatchSize -> {
                        if((Integer)messagesBatchSize > 0) {
                            botContext.execute(EditMessageText.builder()
                                    .chatId(getChatId(upd).toString())
                                    .messageId(finalMessageWithProcessedCountInfoId)
                                    .text(String.format(analyzedMessagesCountNotification, analyzedMessagesCount.addAndGet((Integer) messagesBatchSize)))
                                    .build());
                        }
                    });
                    getRepostsFromChatRequest.execute(selectedChatToClean);
                })
                .build();

        Reply cancel = Reply.of((bot, upd) -> {
                    Map<String, String> userChatsDb = botContext.bot().db().getMap(DB.USER_CHATS);
                    userChatsDb.put(getUser(upd).getId().toString(), "[]");
                    botContext.execute(
                            SendMessage.builder()
                                    .text("Ok, process is canceled.\nYou can /start again")
                                    .chatId(getChatId(upd).toString())
                                    .replyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build())
                                    .build());
                    },
                botContext.cancelKeyboardButtonSelected());

        return ReplyFlow.builder(botContext.bot().db())
                .onlyIf(botContext.hasCallbackQuery(Constants.INLINE_BUTTONS.SPECIFIC_CHAT))
                .onlyIf(botContext.isChatNotInState(Constants.STATE_DB.LOGIN_STATE_DB))
                .action((bot, upd) -> {
                    botContext.hidePreviousReplyMarkup(upd);

                    BotEmbadedTelegramClient client = ClientManager.getInstance().getTelegramClientForUser(getUser(upd).getId(), botContext.getTdLibSettings());
                    EventManager getChatsRequestEventManager = new EventManager();
                    GetChatsRequest getChatsRequest = new GetChatsRequest(client, getChatsRequestEventManager);

                    getChatsRequestEventManager.addEventHandler(GetChatsRequest.EVENTS.CHATS_COUNT_RECEIVED, chatsCount -> {
                        int chatsCountInt = Integer.parseInt(chatsCount.toString());
                        botContext.bot().silent().send("You have " + chatsCountInt + " chats (limit is " + CHATS_LIMIT + " )\nReceiving chat details...", getChatId(upd));
                    });

                    getChatsRequestEventManager.addEventHandler(GetChatsRequest.EVENTS.FINISH, chatList -> {
                        List<Chat> chatsWhereYouCanSendMessage = ((List<Chat>)chatList).stream().filter(Chat::isCanSendMessage).collect(Collectors.toList());
                        for (int i = 0; i < chatsWhereYouCanSendMessage.size(); i++) {
                            Chat chat = chatsWhereYouCanSendMessage.get(i);
                            int index = i + 1;
                            chat.setTitle(index + ". " + chat.getTitle());
                        }

                        Map<String, String> userChatsDb = botContext.bot().db().getMap(DB.USER_CHATS);
                        userChatsDb.put(getUser(upd).getId().toString(), gson.toJson(chatsWhereYouCanSendMessage));

                        botContext.execute(
                                SendMessage.builder()
                                        .text("Select the chat where you can send messages.\nWhich one do you want to analyze and clean?")
                                        .chatId(getChatId(upd).toString())
                                        .replyMarkup(KeyboardFactory.replyKeyboardWithCancelButtons(chatsWhereYouCanSendMessage.stream().map(Chat::getTitle).collect(Collectors.toList()), "Select channel name"))
                                        .allowSendingWithoutReply(false)
                                        .build());
                    });

                    botContext.bot().silent().send("List of chats is requested", getChatId(upd));
                    getChatsRequest.execute(CHATS_LIMIT);
                })
                .next(cancel)
                .next(chatToCleanSelectionFlow)
                .build();
    }

    private Predicate<Update> chatFromReplyKeyboardSelected() {
        return upd -> {
            if(upd.hasMessage() && !botContext.cancelKeyboardButtonSelected().test(upd)) {
                Map<String, String> userChatsDb = botContext.bot().db().getMap(DB.USER_CHATS);
                String userChatsJson = userChatsDb.get(getUser(upd).getId().toString());
                Type listType = new TypeToken<ArrayList<Chat>>(){}.getType();
                List<Chat> userChats = gson.fromJson(userChatsJson, listType);
                return userChats.stream().anyMatch(chat -> chat.getTitle().equals(upd.getMessage().getText()));
            } else {
                return false;
            }
        };
    }
}
