package org.telegram.repostcleanerbot;

import org.telegram.repostcleanerbot.Constants;

import java.util.ArrayList;
import java.util.List;

public class CommonMap {
    public static List<String> repostsFromAllChatsList = new ArrayList<>();

    public static List<String> myChats = new ArrayList<>();
    public static List<String> repostsFromSpecificChatList = new ArrayList<>();


    static {
        repostsFromAllChatsList.add(0, Constants.INLINE_BUTTONS.FINISH_CLEANING);
        repostsFromAllChatsList.add("Nexta Live");
        repostsFromAllChatsList.add("Алкаш_TV");
        repostsFromAllChatsList.add("Пул первого");
        repostsFromAllChatsList.add("Болкунец");
        repostsFromAllChatsList.add("Чалый");
        repostsFromAllChatsList.add("Zerkalo");
        repostsFromAllChatsList.add("Чай з малиновым вареньем");

        repostsFromSpecificChatList.add(0, Constants.INLINE_BUTTONS.FINISH_CLEANING);
        repostsFromSpecificChatList.add("Nexta Live");
        repostsFromSpecificChatList.add("Алкаш_TV");
        repostsFromSpecificChatList.add("Пул первого");
        repostsFromSpecificChatList.add("Болкунец");
        repostsFromSpecificChatList.add("Чалый");
        repostsFromSpecificChatList.add("Zerkalo");
        repostsFromSpecificChatList.add("Чай з малиновым вареньем");

        myChats.add(0, Constants.INLINE_BUTTONS.FINISH_CLEANING);
        myChats.add("Mashenka");
        myChats.add("Коля");
        myChats.add("Oster");
        myChats.add("Витя");
        myChats.add("Алкаш_ТВ");
        myChats.add("Женя");
        myChats.add("Шампур");
        myChats.add("Машин Папа");
        myChats.add("Макс");
        myChats.add("Варя");
        myChats.add("EasyRPA");
        myChats.add("Max F");
        myChats.add("Oleg Holod");
    }
}
