package com.campusloop.dao;

import com.campusloop.domain.Category;

import java.util.List;
import java.util.Optional;

/**
 * 分类数据访问接口。
 */
public interface CategoryDao {

    long insert(Category category);

    Optional<Category> findById(long id);

    Optional<Category> findByName(String name);

    List<Category> findAll();

    boolean existsByName(String name);
}
