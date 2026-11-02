package com.complaints.service;

import com.complaints.dao.CategoryDAO;
import com.complaints.model.Category;
import com.complaints.model.User;
import com.complaints.util.Validation;

import java.sql.SQLException;
import java.util.List;

public final class CategoryService {
    private final CategoryDAO categories;
    public CategoryService(CategoryDAO categories) { this.categories = categories; }

    public List<Category> list() throws SQLException { return categories.findAll(false); }

    public List<Category> manage(User actor) throws SQLException {
        UserService.requireAdmin(actor);
        return categories.findAll(true);
    }

    public void save(User actor, Category category) throws SQLException {
        UserService.requireAdmin(actor);
        categories.save(new Category(category.id(), Validation.required(category.name(), "Category name", 80),
                category.description() == null ? "" : category.description().trim(),
                category.active()));
    }

    public void deactivate(User actor, long id) throws SQLException {
        UserService.requireAdmin(actor);
        categories.deactivate(id);
    }
}
