package org.telegram.repostcleanerbot.utils;

import lombok.SneakyThrows;
import org.codejargon.feather.Feather;
import org.telegram.repostcleanerbot.RepostCleanerModule;

public class InstantiationUtils {
    public static final Feather feather = Feather.with(new RepostCleanerModule());

    public static <T> T getInstance(Class<T> clazz) {
        return getInstance(clazz, false);
    }

    /**
     * @param clazz the class to instantiate
     * @param rootInstance Use it for root objects like services to avoid endless method call loop.
     * @param <T> the class type
     * @return the created instance
     */
    @SneakyThrows
    public static <T> T getInstance(Class<T> clazz, boolean rootInstance) {
        T result;
        if (rootInstance) {
            result = clazz.getDeclaredConstructor().newInstance();
        } else {
            result = feather.instance(clazz);
        }

        feather.injectFields(result);
        return result;
    }
}
