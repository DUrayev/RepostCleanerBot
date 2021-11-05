package org.telegram.repostcleanerbot;

import java.util.ArrayList;
import java.util.List;

public class CommonMap {
    public static List<String> repostsFromAllChatsList = new ArrayList<>();

    static {
        repostsFromAllChatsList.add(0, Constants.INLINE_BUTTONS.CANCEL_KEYBOARD_BUTTON);
        repostsFromAllChatsList.add("Nexta Live");
        repostsFromAllChatsList.add("Алкаш_TV");
        repostsFromAllChatsList.add("Пул первого");
        repostsFromAllChatsList.add("Болкунец");
        repostsFromAllChatsList.add("Чалый");
        repostsFromAllChatsList.add("Zerkalo");
        repostsFromAllChatsList.add("Чай з малиновым вареньем");
    }
}
