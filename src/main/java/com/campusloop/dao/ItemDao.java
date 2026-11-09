package com.campusloop.dao;

import com.campusloop.domain.Item;
import com.campusloop.domain.ItemStatus;
import com.campusloop.domain.SearchCriteria;

import java.util.List;
import java.util.Optional;

/**
 * 物品数据访问接口。
 */
public interface ItemDao {

    long insert(Item item);

    int update(Item item);

    Optional<Item> findById(long id);

    /** 条件检索（不做排序，排序交给服务层策略）。 */
    List<Item> search(SearchCriteria criteria);

    /** 仅更新状态字段。 */
    int updateStatus(long itemId, ItemStatus status);

    /** 浏览量 +1。 */
    void incrementViewCount(long itemId);

    int deleteById(long itemId);

    int countByOwner(long ownerId);
}
