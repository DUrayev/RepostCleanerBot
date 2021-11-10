package org.telegram.repostcleanerbot.factory;

import org.telegram.repostcleanerbot.utils.I18nService;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import javax.inject.Inject;

import static org.telegram.repostcleanerbot.Constants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KeyboardFactory {

    @Inject
    private I18nService i18n;

    public InlineKeyboardMarkup withOneLineButtons(InlineKeyboardButton... buttons) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>(Arrays.asList(buttons));
        rowsInline.add(rowInline);
        inlineKeyboard.setKeyboard(rowsInline);
        return inlineKeyboard;
    }

    public ReplyKeyboard replyKeyboardButtons(List<String> buttons, String inputFieldPlaceholder) {
        List<KeyboardRow> rowButtonsList = new ArrayList<>();
        for (String button : buttons) {
            KeyboardRow row = new KeyboardRow();
            row.add(button);
            rowButtonsList.add(row);
        }
        return ReplyKeyboardMarkup.builder().inputFieldPlaceholder(inputFieldPlaceholder).keyboard(rowButtonsList).oneTimeKeyboard(false).resizeKeyboard(true).build();
    }

    public ReplyKeyboard replyKeyboardWithCancelButtons(List<String> buttons, String inputFieldPlaceholder, String languageCode) {
        buttons.add(0, i18n.forLanguage(languageCode).getMsg("cancel_keyboard_btn"));
        return replyKeyboardButtons(buttons, inputFieldPlaceholder);
    }

}
