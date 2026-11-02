package com.complaints.view;

import com.complaints.controller.ApplicationController;
import com.complaints.model.Category;
import com.complaints.model.Complaint;
import com.complaints.model.User;
import com.complaints.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

final class ComplaintPanel extends JPanel {
    private final PortalFrame frame;
    private final ApplicationController controller;
    private final User user;
    private final JTextField search = new JTextField(18);
    private final JComboBox<String> status = new JComboBox<>(new String[]{"All statuses", "PENDING", "IN_PROGRESS", "RESOLVED", "REJECTED"});
    private final JComboBox<String> priority = new JComboBox<>(new String[]{"All priorities", "LOW", "MEDIUM", "HIGH", "URGENT"});
    private final JComboBox<Category> category = new JComboBox<>();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Complaint ID", "Title", "Submitted by", "Category", "Priority", "Status", "Updated"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = UITheme.table(new JTable(tableModel));
    private List<Complaint> rows = new ArrayList<>();

    ComplaintPanel(PortalFrame frame, ApplicationController controller, User user) {
        this.frame = frame; this.controller = controller; this.user = user;
        setLayout(new BorderLayout(0, 16)); setBackground(UITheme.BG);
        setBorder(new EmptyBorder(24, 28, 26, 28));
        add(header(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(actions(), BorderLayout.SOUTH);
        search.addActionListener(e -> reload());
        status.addActionListener(e -> reload());
        priority.addActionListener(e -> reload());
        category.addActionListener(e -> reload());
        loadCategories();
        reload();
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout(10, 14)); header.setOpaque(false);
        JPanel labels = new JPanel(); labels.setOpaque(false); labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        labels.add(UITheme.label(user.isAdmin() ? "Complaint management" : "My complaints", 24, true, UITheme.TEXT));
        labels.add(Box.createVerticalStrut(5));
        labels.add(UITheme.label("Search, review, and follow every service request.", 13, false, UITheme.MUTED));
        header.add(labels, BorderLayout.NORTH);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); filters.setOpaque(false);
        search.putClientProperty("JTextField.placeholderText", "Search complaints");
        filters.add(search); filters.add(category); filters.add(priority); filters.add(status);
        JButton apply = UITheme.button("Search", false); apply.addActionListener(e -> reload());
        filters.add(apply);
        JButton create = UITheme.button("+  New complaint", true); create.addActionListener(e -> createNew());
        filters.add(create);
        header.add(filters, BorderLayout.CENTER);
        return header;
    }

    private JPanel actions() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0)); actions.setOpaque(false);
        JButton view = UITheme.button("View details", false); view.addActionListener(e -> viewSelected());
        JButton edit = UITheme.button("Edit", false); edit.addActionListener(e -> editSelected());
        JButton delete = UITheme.button("Delete", false); delete.addActionListener(e -> deleteSelected());
        actions.add(view); actions.add(edit); actions.add(delete);
        JLabel count = UITheme.label("Select a row to see available actions.", 12, false, UITheme.MUTED);
        actions.add(count);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                count.setText(row < 0 ? "Select a row to see available actions." : rows.get(table.convertRowIndexToModel(row)).code());
            }
        });
        return actions;
    }

    private void loadCategories() {
        UITheme.load(this, controller::categories, result -> {
            category.removeAllItems(); category.addItem(null);
            for (Category item : castCategories(result)) category.addItem(item);
            category.setRenderer((list, value, index, selected, focus) -> {
                JLabel label = new JLabel(value == null ? "All categories" : value.name());
                label.setOpaque(true); label.setBackground(selected ? list.getSelectionBackground() : list.getBackground());
                label.setForeground(selected ? list.getSelectionForeground() : UITheme.TEXT);
                label.setBorder(new EmptyBorder(4, 8, 4, 8));
                return label;
            });
        });
    }

    @SuppressWarnings("unchecked")
    private static List<Category> castCategories(Object value) { return (List<Category>) value; }

    private void reload() {
        String selectedStatus = status.getSelectedIndex() <= 0 ? "" : (String) status.getSelectedItem();
        String selectedPriority = priority.getSelectedIndex() <= 0 ? "" : (String) priority.getSelectedItem();
        Category selectedCategory = (Category) category.getSelectedItem();
        table.setEnabled(false);
        UITheme.load(this, () -> controller.search(search.getText(), selectedStatus, selectedPriority,
                selectedCategory == null ? null : selectedCategory.id()), result -> {
            rows = castComplaints(result); tableModel.setRowCount(0);
            DateTimeFormatter format = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");
            for (Complaint item : rows) tableModel.addRow(new Object[]{item.code(), item.title(), item.submittedBy(),
                    item.category(), item.priority(), item.status().replace('_', ' '), item.updatedAt().format(format)});
            table.setEnabled(true);
        });
    }

    @SuppressWarnings("unchecked")
    private static List<Complaint> castComplaints(Object value) { return (List<Complaint>) value; }

    private Complaint selected() {
        int index = table.getSelectedRow();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Select a complaint first.", "No complaint selected", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return rows.get(table.convertRowIndexToModel(index));
    }

    void createNew() {
        ComplaintDialog dialog = new ComplaintDialog(frame, controller, user, null, false);
        dialog.setVisible(true);
        if (dialog.isSaved()) reload();
    }

    private void viewSelected() {
        Complaint complaint = selected();
        if (complaint == null) return;
        new ComplaintDialog(frame, controller, user, complaint, true).setVisible(true);
    }

    private void editSelected() {
        Complaint complaint = selected();
        if (complaint == null) return;
        if (!user.isAdmin() && !"PENDING".equals(complaint.status())) {
            JOptionPane.showMessageDialog(this, "Only pending complaints can be edited."); return;
        }
        ComplaintDialog dialog = new ComplaintDialog(frame, controller, user, complaint, false);
        dialog.setVisible(true);
        if (dialog.isSaved()) reload();
    }

    private void deleteSelected() {
        Complaint complaint = selected();
        if (complaint == null) return;
        if (!user.isAdmin() && !"PENDING".equals(complaint.status())) {
            JOptionPane.showMessageDialog(this, "Only pending complaints can be deleted."); return;
        }
        if (JOptionPane.showConfirmDialog(this, "Delete complaint " + complaint.code() + "? This cannot be undone.",
                "Confirm deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        UITheme.run(this, () -> controller.deleteComplaint(complaint.id()), this::reload);
    }
}
