package org.telegram.repostcleanerbot.factory;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class KeyboardFactory {

    public static ReplyKeyboard withOneLineButtons(String... buttons) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        for (String button : buttons) {
            rowInline.add((InlineKeyboardButton.builder().text(button).callbackData(button).build()));
        }
        rowsInline.add(rowInline);
        inlineKeyboard.setKeyboard(rowsInline);
        return inlineKeyboard;

    }

    public static ReplyKeyboard replyKeyboardButtons(List<String> buttons, String inputFieldPlaceholder) {
        List<KeyboardRow> rowButtonsList = new ArrayList<>();
        for (String button : buttons) {
            KeyboardRow row = new KeyboardRow();
            row.add(button);
            rowButtonsList.add(row);
        }
        return ReplyKeyboardMarkup.builder().inputFieldPlaceholder(inputFieldPlaceholder).keyboard(rowButtonsList).oneTimeKeyboard(false).resizeKeyboard(true).build();
    }

}
