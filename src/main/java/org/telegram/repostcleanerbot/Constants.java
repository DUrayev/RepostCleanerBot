package org.telegram.repostcleanerbot;

public interface Constants {
    String AUTHENTICATION_QR_CODE_IMAGE_NAME = "qr_code";
    int AUTHENTICATION_QR_CODE_IMAGE_SIZE = 250;
    int CHATS_LIMIT = 300;

    interface INLINE_BUTTONS {
        String LOGIN = "Login";

        String PHONE_NUMBER_LOGIN = "Phone number";
        String QR_CODE_LOGIN = "QR Code";

        String ALL_CHATS = "All chats";
        String SPECIFIC_CHAT = "Specific chat";

        String START_CLEANING = "Start cleaning";
        String CANCEL = "Cancel";

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
    }
}
