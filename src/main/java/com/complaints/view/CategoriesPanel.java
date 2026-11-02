package com.complaints.view;

import com.complaints.controller.ApplicationController;
import com.complaints.model.Category;
import com.complaints.model.User;
import com.complaints.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

final class CategoriesPanel extends JPanel {
    private final ApplicationController controller;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Name", "Description", "Status"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = UITheme.table(new JTable(model));
    private List<Category> rows = List.of();

    CategoriesPanel(PortalFrame frame, ApplicationController controller, User user) {
        this.controller = controller;
        setLayout(new BorderLayout(0, 18)); setBackground(UITheme.BG); setBorder(new EmptyBorder(26, 28, 26, 28));
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        JPanel copy = new JPanel(); copy.setOpaque(false); copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(UITheme.label("Complaint categories", 24, true, UITheme.TEXT));
        copy.add(Box.createVerticalStrut(5)); copy.add(UITheme.label("Configure the active categories shown on complaint forms.", 13, false, UITheme.MUTED));
        header.add(copy, BorderLayout.WEST);
        JButton add = UITheme.button("+  Add category", true); add.addActionListener(e -> edit(null));
        header.add(add, BorderLayout.EAST); add(header, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0)); actions.setOpaque(false);
        JButton edit = UITheme.button("Edit selected", false); edit.addActionListener(e -> edit(selected()));
        JButton deactivate = UITheme.button("Deactivate selected", false); deactivate.addActionListener(e -> deactivate());
        JButton refresh = UITheme.button("Refresh", false); refresh.addActionListener(e -> load());
        actions.add(edit); actions.add(deactivate); actions.add(refresh); add(actions, BorderLayout.SOUTH);
        load();
    }

    private void load() {
        UITheme.load(this, controller::managedCategories, result -> {
            @SuppressWarnings("unchecked") List<Category> categories = (List<Category>) result;
            rows = categories; model.setRowCount(0);
            for (Category category : rows) model.addRow(new Object[]{category.name(), category.description(),
                    category.active() ? "Active" : "Inactive"});
        });
    }

    private Category selected() {
        int index = table.getSelectedRow();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Select a category first.");
            return null;
        }
        return rows.get(table.convertRowIndexToModel(index));
    }

    private void edit(Category category) {
        JTextField name = new JTextField(category == null ? "" : category.name());
        JTextField description = new JTextField(category == null ? "" : category.description());
        JCheckBox active = new JCheckBox("Active", category == null || category.active());
        JPanel fields = new JPanel(new GridLayout(0, 1, 5, 5));
        fields.add(new JLabel("Category name")); fields.add(name);
        fields.add(new JLabel("Description")); fields.add(description); fields.add(active);
        if (JOptionPane.showConfirmDialog(this, fields, category == null ? "Add category" : "Edit category",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        Category updated = new Category(category == null ? 0 : category.id(), name.getText(), description.getText(), active.isSelected());
        UITheme.run(this, () -> controller.saveCategory(updated), this::load);
    }

    private void deactivate() {
        Category category = selected();
        if (category == null) return;
        if (JOptionPane.showConfirmDialog(this, "Deactivate " + category.name() + "? Existing complaints remain unchanged.",
                "Confirm deactivation", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        UITheme.run(this, () -> controller.deactivateCategory(category.id()), this::load);
    }
}
