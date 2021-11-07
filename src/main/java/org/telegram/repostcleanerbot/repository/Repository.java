package org.telegram.repostcleanerbot.repository;

public interface Repository<T> {
    T get(Long userId);
    void save(Long userId, T entity);
}
