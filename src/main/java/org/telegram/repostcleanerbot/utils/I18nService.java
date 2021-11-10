package org.telegram.repostcleanerbot.utils;

import org.telegram.repostcleanerbot.Constants;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class I18nService {

    public LanguageResourceBundle forLanguage(String languageTag) {
        return new LanguageResourceBundle(Locale.forLanguageTag(languageTag));
    }

    public static class LanguageResourceBundle {

        private final ResourceBundle resourceBundle;

        private LanguageResourceBundle(Locale locale) {
            this.resourceBundle = ResourceBundle.getBundle(Constants.BOT_MESSAGES_PROPERTY_FILE, locale, new UTF8Control());
        }

        public String getMsg(String msgPropertyName, Object... msgArguments) {
            String messagePattern = resourceBundle.getString(msgPropertyName);
            return MessageFormat.format(messagePattern, msgArguments);
        }
    }
}
