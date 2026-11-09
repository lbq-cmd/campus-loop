package com.campusloop.dao;

import java.util.List;

/**
 * 收藏数据访问接口。
 */
public interface FavoriteDao {

    /** 收藏，已收藏时返回 false（幂等）。 */
    boolean add(long userId, long itemId);

    /** 取消收藏，未收藏时返回 false（幂等）。 */
    boolean remove(long userId, long itemId);

    boolean exists(long userId, long itemId);

    List<Long> findItemIdsByUser(long userId);

    int countByItem(long itemId);
}
