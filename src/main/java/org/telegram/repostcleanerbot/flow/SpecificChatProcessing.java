package org.telegram.repostcleanerbot.flow;

import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.abilitybots.api.objects.ReplyFlow;
import org.telegram.repostcleanerbot.Constants;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.bot.Flow;
import org.telegram.repostcleanerbot.factory.KeyboardFactory;
import org.telegram.repostcleanerbot.repository.UserChatsRepository;
import org.telegram.repostcleanerbot.repository.UserChatsRepostedFromRepository;
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

import javax.inject.Inject;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;
import static org.telegram.abilitybots.api.util.AbilityUtils.getUser;
import static org.telegram.repostcleanerbot.Constants.CHATS_LIMIT;

public class SpecificChatProcessing implements Flow {

    @Inject
    private BotContext botContext;

    @Inject
    private UserChatsRepostedFromRepository repostedFromRepository;

    @Inject
    private UserChatsRepository userChatsRepository;

    @Inject
    private ClientManager clientManager;

    @Override
    public ReplyFlow getFlow() {
        ReplyFlow chatToCleanSelectionFlow = ReplyFlow.builder(botContext.bot().db())
                .onlyIf(chatFromReplyKeyboardSelected())
                .action((bot, upd) -> {
                    String selectedTitleChatToClean = upd.getMessage().getText();
                    List<Chat> userChats = userChatsRepository.get(getUser(upd).getId());
                    userChatsRepository.save(getUser(upd).getId(), Collections.emptyList());
                    Chat selectedChatToClean = userChats.stream().filter(chat -> chat.getTitle().equals(selectedTitleChatToClean)).findFirst().get();
                    BotEmbadedTelegramClient client = clientManager.getTelegramClientForUser(getUser(upd).getId());

                    EventManager getRepostsFromChatEventManager = new EventManager();
                    GetRepostsFromChatRequest getRepostsFromChatRequest = new GetRepostsFromChatRequest(client, getRepostsFromChatEventManager);

                    getRepostsFromChatEventManager.addEventHandler(GetRepostsFromChatRequest.EVENTS.FINISH, onRepostsForChatReceived(upd, selectedTitleChatToClean));
                    addRepostsReceivingProgressNotifier(upd, getRepostsFromChatEventManager);

                    botContext.bot().silent().execute(
                            SendMessage.builder()
                                    .text("List of reposts in chat " + selectedTitleChatToClean + " is requested\nPlease, wait...")
                                    .chatId(getChatId(upd).toString())
                                    .replyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build())
                                    .allowSendingWithoutReply(false)
                                    .build());

                    getRepostsFromChatRequest.execute(selectedChatToClean);
                })
                .build();

        Reply cancel = Reply.of((bot, upd) -> {
                    userChatsRepository.save(getUser(upd).getId(), Collections.emptyList());
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

                    BotEmbadedTelegramClient client = clientManager.getTelegramClientForUser(getUser(upd).getId());
                    EventManager getChatsRequestEventManager = new EventManager();
                    GetChatsRequest getChatsRequest = new GetChatsRequest(client, getChatsRequestEventManager);

                    getChatsRequestEventManager.addEventHandler(GetChatsRequest.EVENTS.CHATS_COUNT_RECEIVED, onChatsCountReceived(upd));
                    getChatsRequestEventManager.addEventHandler(GetChatsRequest.EVENTS.FINISH, onChatListReceived(upd));

                    botContext.bot().silent().send("List of chats is requested", getChatId(upd));
                    getChatsRequest.execute(CHATS_LIMIT);
                })
                .next(cancel)
                .next(chatToCleanSelectionFlow)
                .build();
    }

    private Consumer<Object> onChatsCountReceived(Update upd) {
        return chatsCount -> {
            int chatsCountInt = Integer.parseInt(chatsCount.toString());
            botContext.bot().silent().send("You have " + chatsCountInt + " chats (limit is " + CHATS_LIMIT + " )\nReceiving chat details...", getChatId(upd));
        };
    }

    private Consumer<Object> onChatListReceived(Update upd) {
        return chatList -> {
            List<Chat> chatsWhereYouCanSendMessage = ((List<Chat>) chatList).stream().filter(Chat::isCanSendMessage).collect(Collectors.toList());
            addIndexesToChatsTitle(chatsWhereYouCanSendMessage);
            userChatsRepository.save(getUser(upd).getId(), chatsWhereYouCanSendMessage);

            botContext.execute(
                    SendMessage.builder()
                            .text("There are " + chatsWhereYouCanSendMessage.size() + " chats where you can send messages\nWhich one do you want to analyze?")
                            .chatId(getChatId(upd).toString())
                            .replyMarkup(KeyboardFactory.replyKeyboardWithCancelButtons(chatsWhereYouCanSendMessage.stream().map(Chat::getTitle).collect(Collectors.toList()), "Select channel name"))
                            .allowSendingWithoutReply(false)
                            .build());
        };
    }

    private void addRepostsReceivingProgressNotifier(Update upd, EventManager getRepostsFromChatEventManager) {
        String analyzedMessagesCountNotification = "%s messages were analyzed. Please, wait...";
        AtomicInteger analyzedMessagesCount = new AtomicInteger(0);
        Optional<Message> sentMessageOptional = botContext.bot().silent().execute(
                SendMessage.builder()
                        .text(String.format(analyzedMessagesCountNotification, analyzedMessagesCount.get()))
                        .chatId(getChatId(upd).toString())
                        .replyMarkup(KeyboardFactory.withOneLineButtons()) //without this empty reply markup you're not able to edit message
                        .allowSendingWithoutReply(false)
                        .build());
        if(sentMessageOptional.isPresent()) {
            Integer messageWithProcessedCountInfoId = sentMessageOptional.get().getMessageId();
            getRepostsFromChatEventManager.addEventHandler(GetRepostsFromChatRequest.EVENTS.BATCH_OF_MESSAGES_RECEIVED, messagesBatchSize -> {
                if((Integer)messagesBatchSize > 0) {
                    botContext.execute(EditMessageText.builder()
                            .chatId(getChatId(upd).toString())
                            .messageId(messageWithProcessedCountInfoId)
                            .text(String.format(analyzedMessagesCountNotification, analyzedMessagesCount.addAndGet((Integer) messagesBatchSize)))
                            .build());
                }
            });
        }
    }

    private Consumer<Object> onRepostsForChatReceived(Update upd, String selectedTitleChatToClean) {
        return allRepostsInSelectedChannelResult -> {
            List<Repost> allRepostsInSelectedChannel = (List<Repost>) allRepostsInSelectedChannelResult;
            int totalRepostsCount = allRepostsInSelectedChannel.size();
            if (totalRepostsCount > 0) {
                List<RepostsStat> repostsStatList = RepostsStat.generate(allRepostsInSelectedChannel, getUser(upd).getId());
                List<RepostsStat> repostsStatSortedList = sortByTotalRepostsCountDesc(repostsStatList);
                addStatisticInfoToRepostsChatsTitle(repostsStatSortedList);
                repostedFromRepository.save(getUser(upd).getId(), repostsStatSortedList);

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

        };
    }

    private Predicate<Update> chatFromReplyKeyboardSelected() {
        return upd -> {
            if(upd.hasMessage() && !botContext.cancelKeyboardButtonSelected().test(upd)) {
                List<Chat> userChats = userChatsRepository.get(getUser(upd).getId());
                return userChats.stream().anyMatch(chat -> chat.getTitle().equals(upd.getMessage().getText()));
            } else {
                return false;
            }
        };
    }

    private void addIndexesToChatsTitle(List<Chat> chats) {
        for (int i = 0; i < chats.size(); i++) {
            Chat chat = chats.get(i);
            int index = i + 1;
            chat.setTitle(index + ". " + chat.getTitle());
        }
    }

    private void addStatisticInfoToRepostsChatsTitle(List<RepostsStat> repostsStatList) {
        repostsStatList.forEach(repostsStat -> {
            String newTitleWithStatInfo = repostsStat.getRepostedFrom().getTitle() + " [You: " + repostsStat.getRepostedByMeCount() + ", Not you: " + repostsStat.getRepostedNotByMeCount() + "]";
            repostsStat.getRepostedFrom().setTitle(newTitleWithStatInfo);
        });
    }


    private List<RepostsStat> sortByTotalRepostsCountDesc(List<RepostsStat> repostsStatList) {
        List<RepostsStat> repostsStatSortedList = repostsStatList.stream().sorted(Comparator.comparing(r -> r.getRepostedByMeCount() + r.getRepostedNotByMeCount())).collect(Collectors.toList());
        Collections.reverse(repostsStatSortedList);
        return repostsStatSortedList;
    }
}
