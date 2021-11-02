package org.telegram.repostcleanerbot;

public interface Constants {
    interface INLINE_BUTTONS {
        String LOGIN = "Login";

        String ALL_CHATS = "All chats";
        String SPECIFIC_CHAT = "Specific chat";

        String START_CLEANING = "Start cleaning";
        String CANCEL = "Cancel";

        String FINISH_CLEANING = "✖️ Finish cleaning";
    }

    interface STATE_DB {
        String CLEAN_REPOSTS_FROM_ALL_CHATS_STATE_DB = "CLEAN_REPOSTS_FROM_ALL_CHATS_STATE_DB";
        String CLEAN_REPOSTS_FROM_SPECIFIC_CHAT_STATE_DB = "CLEAN_REPOSTS_FROM_SPECIFIC_CHAT_STATE_DB";
    }
}
