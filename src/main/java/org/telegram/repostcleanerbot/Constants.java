package org.telegram.repostcleanerbot;

public interface Constants {
    String BOT_TOKEN = "REPOST_CLEANER_BOT_TOKEN";
    String BOT_NAME = "REPOST_CLEANER_BOT_NAME";
    String BOT_ADMIN_ID = "REPOST_CLEANER_BOT_ADMIN_ID";

    String BOT_API_ID = "REPOST_CLEANER_BOT_API_ID";
    String BOT_API_HASH = "REPOST_CLEANER_BOT_API_HASH";

    String AUTHENTICATION_QR_CODE_IMAGE_NAME = "qr_code";
    int AUTHENTICATION_QR_CODE_IMAGE_SIZE = 250;
    int ANALYZE_ALL_CHATS_LIMIT = 50;
    int CHATS_LIMIT = 300;

    interface INLINE_BUTTONS {
        String LOGIN = "Login";

        String PHONE_NUMBER_LOGIN = "Phone number";
        String QR_CODE_LOGIN = "QR Code";

        String ALL_CHATS = "All chats";
        String SPECIFIC_CHAT = "Specific chat";

        String START_ANALYZING_ALL_CHATS = "Start analyzing";
        String CANCEL_ANALYZING_ALL_CHATS = "Cancel";

        String CANCEL_KEYBOARD_BUTTON = "✖️ Cancel";
    }

    interface STATE_DB {
        String CLEAN_REPOSTS_FROM_ALL_CHATS_STATE_DB = "CLEAN_REPOSTS_FROM_ALL_CHATS_STATE_DB";
        String CLEAN_REPOSTS_FROM_SPECIFIC_CHAT_STATE_DB = "CLEAN_REPOSTS_FROM_SPECIFIC_CHAT_STATE_DB";

        String LOGIN_STATE_DB = "LOGIN_STATE_DB";
        String PASSWORD_ENTERING_STATE_DB = "PASSWORD_ENTERING_STATE_DB";
    }

    interface DB {
        String USER_CHATS = "USER_CHATS";
        String USER_REPOSTS_STAT_IN_SPECIFIC_CHANNEL = "USER_CHATS_REPOSTED_FROM";

        String USER_ALL_REPOSTS_GROUPED_BY_REPOSTED_FROM = "USER_ALL_REPOSTS_GROUPED_BY_REPOSTED_FROM";
    }
}
