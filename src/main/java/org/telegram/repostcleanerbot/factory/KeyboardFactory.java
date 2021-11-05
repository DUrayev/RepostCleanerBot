package org.telegram.repostcleanerbot.factory;

import org.telegram.telegrambots.meta.api.objects.LoginUrl;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import static org.telegram.repostcleanerbot.Constants.*;

import java.util.ArrayList;
import java.util.List;

public class KeyboardFactory {

    public static InlineKeyboardMarkup withOneLineButtons(String... buttons) {
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

    public static ReplyKeyboard loginButton(String text, String loginUrl) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        rowInline.add((InlineKeyboardButton.builder().text(text).loginUrl(LoginUrl.builder().url(loginUrl).build()).build()));
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

    public static ReplyKeyboard replyKeyboardWithCancelButtons(List<String> buttons, String inputFieldPlaceholder) {
        buttons.add(0, INLINE_BUTTONS.CANCEL_KEYBOARD_BUTTON);
        return replyKeyboardButtons(buttons, inputFieldPlaceholder);
    }

}
