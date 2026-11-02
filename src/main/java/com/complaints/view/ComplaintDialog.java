package com.complaints.view;

import com.complaints.controller.ApplicationController;
import com.complaints.model.*;
import com.complaints.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class ComplaintDialog extends JDialog {
    private final PortalFrame frame;
    private final ApplicationController controller;
    private final User user;
    private final Complaint existing;
    private final boolean readOnly;
    private final JComboBox<Category> category = new JComboBox<>();
    private final JTextField title = new JTextField();
    private final JTextArea description = new JTextArea(5, 28);
    private final JComboBox<String> priority = new JComboBox<>(new String[]{"LOW", "MEDIUM", "HIGH", "URGENT"});
    private final JComboBox<String> status = new JComboBox<>(new String[]{"PENDING", "IN_PROGRESS", "RESOLVED", "REJECTED"});
    private final JComboBox<AdminChoice> assignee = new JComboBox<>();
    private final JTextArea remarks = new JTextArea(3, 28);
    private final JLabel info = UITheme.label("Loading categories…", 12, false, UITheme.MUTED);
    private JButton save;
    private boolean saved;

    ComplaintDialog(PortalFrame frame, ApplicationController controller, User user, Complaint existing, boolean readOnly) {
        super(frame, existing == null ? "New complaint" : readOnly ? "Complaint details" : "Update complaint",
                ModalityType.APPLICATION_MODAL);
        this.frame = frame; this.controller = controller; this.user = user;
        this.existing = existing; this.readOnly = readOnly;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(560, 660));
        setSize(620, 740); setLocationRelativeTo(frame);
        setContentPane(build());
        loadFormOptions();
    }

    boolean isSaved() { return saved; }

    private JPanel build() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBorder(new EmptyBorder(20, 24, 20, 24));
        root.add(info, BorderLayout.NORTH);
        JPanel form = new JPanel(new GridBagLayout());
        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 3, 6, 3); c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.32; c.gridx = 0;
        int row = 0;
        if (existing != null) {
            addField(form, c, row++, "Complaint ID", new JLabel(existing.code()));
            addField(form, c, row++, "Submitted by", new JLabel(existing.submittedBy()));
            addField(form, c, row++, "Submitted", new JLabel(existing.createdAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"))));
        }
        addField(form, c, row++, "Category", category);
        addField(form, c, row++, "Title", title);
        description.setLineWrap(true); description.setWrapStyleWord(true);
        addField(form, c, row++, "Description", new JScrollPane(description));
        addField(form, c, row++, "Priority", priority);
        if (user.isAdmin() && existing != null) {
            addField(form, c, row++, "Status", status);
            addField(form, c, row++, "Assigned administrator", assignee);
            remarks.setLineWrap(true); remarks.setWrapStyleWord(true);
            addField(form, c, row++, "Administrative remarks", new JScrollPane(remarks));
        } else if (existing != null) {
            addField(form, c, row++, "Status", new JLabel(existing.status().replace('_', ' ')));
            addField(form, c, row++, "Assigned administrator", new JLabel(
                    existing.assignedAdmin() == null ? "Not assigned" : existing.assignedAdmin()));
            addField(form, c, row++, "Administrative remarks", new JLabel(
                    existing.remarks() == null || existing.remarks().isBlank() ? "No remarks yet" : existing.remarks()));
        }
        c.gridx = 0; c.gridy = row; c.gridwidth = 2; c.weightx = 1;
        JButton history = UITheme.button("View status history", false);
        history.addActionListener(e -> showHistory());
        if (existing != null) form.add(history, c);
        root.add(scroll, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT)); buttons.setOpaque(false);
        JButton cancel = UITheme.button("Close", false); cancel.addActionListener(e -> dispose());
        buttons.add(cancel);
        if (!readOnly) {
            save = UITheme.button(existing == null ? "Submit complaint" : "Save changes", true);
            save.addActionListener(e -> save()); buttons.add(save);
        }
        root.add(buttons, BorderLayout.SOUTH);
        return root;
    }

    private void addField(JPanel panel, GridBagConstraints base, int row, String label, Component component) {
        GridBagConstraints c = (GridBagConstraints) base.clone();
        c.gridy = row; c.gridx = 0; c.gridwidth = 1; c.weightx = 0.32;
        JLabel title = UITheme.label(label, 12, true, UITheme.TEXT);
        panel.add(title, c);
        c.gridx = 1; c.weightx = 0.68;
        if (component instanceof JComponent item) item.setPreferredSize(new Dimension(320, Math.max(item.getPreferredSize().height, 36)));
        panel.add(component, c);
    }

    private void loadFormOptions() {
        UITheme.load(this, () -> {
            List<Category> categories = controller.categories();
            List<User> admins = user.isAdmin() && existing != null
                    ? controller.users().stream().filter(admin -> admin.isAdmin() && admin.active()).toList()
                    : List.of();
            return new FormOptions(categories, admins);
        }, result -> {
            FormOptions options = (FormOptions) result;
            category.removeAllItems();
            for (Category item : options.categories()) category.addItem(item);
            if (existing != null) {
                category.setSelectedItem(options.categories().stream().filter(item -> item.id() == existing.categoryId()).findFirst().orElse(null));
                title.setText(existing.title()); description.setText(existing.description());
                priority.setSelectedItem(existing.priority());
                status.setSelectedItem(existing.status()); remarks.setText(existing.remarks() == null ? "" : existing.remarks());
            }
            if (user.isAdmin() && existing != null) {
                assignee.addItem(new AdminChoice(null, "Unassigned"));
                for (User admin : options.admins()) assignee.addItem(new AdminChoice(admin.id(), admin.fullName()));
                if (existing != null && existing.assignedAdminId() != null) {
                    for (int i = 0; i < assignee.getItemCount(); i++)
                        if (existing.assignedAdminId().equals(assignee.getItemAt(i).id())) assignee.setSelectedIndex(i);
                }
            }
            if (readOnly || (existing != null && !user.isAdmin())) {
                category.setEnabled(false); title.setEditable(false); description.setEditable(false); priority.setEnabled(false);
                status.setEnabled(false); assignee.setEnabled(false); remarks.setEditable(false);
            }
            info.setText(existing == null ? "A unique complaint reference will be generated on submission."
                    : "Last updated " + existing.updatedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")));
        });
    }

    private void save() {
        Category selected = (Category) category.getSelectedItem();
        if (selected == null) { UITheme.error(this, new IllegalArgumentException("Select a category.")); return; }
        String selectedStatus = user.isAdmin() ? (String) status.getSelectedItem()
                : existing == null ? "PENDING" : existing.status();
        AdminChoice selectedAdmin = user.isAdmin() ? (AdminChoice) assignee.getSelectedItem() : null;
        save.setEnabled(false);
        UITheme.run(this, () -> {
            if (existing == null) {
                controller.createComplaint(selected.id(), title.getText(), description.getText(), (String) priority.getSelectedItem());
            } else {
                controller.updateComplaint(existing, selected.id(), title.getText(), description.getText(),
                        (String) priority.getSelectedItem(), selectedStatus,
                        selectedAdmin == null ? existing.assignedAdminId() : selectedAdmin.id(), remarks.getText());
            }
        }, () -> { saved = true; dispose(); }, error -> {
            UITheme.error(this, error);
            if (save != null) save.setEnabled(true);
        });
    }

    private void showHistory() {
        UITheme.load(this, () -> controller.history(existing.id()), result -> {
            @SuppressWarnings("unchecked") List<ComplaintEvent> events = (List<ComplaintEvent>) result;
            StringBuilder text = new StringBuilder();
            DateTimeFormatter format = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");
            for (ComplaintEvent event : events) {
                text.append(event.createdAt().format(format)).append("  ·  ").append(event.actor()).append('\n');
                if (event.oldStatus() != null) text.append(event.oldStatus().replace('_', ' ')).append(" → ");
                text.append(event.newStatus() == null ? "" : event.newStatus().replace('_', ' ')).append('\n');
                if (event.remarks() != null && !event.remarks().isBlank()) text.append(event.remarks()).append('\n');
                text.append('\n');
            }
            JTextArea content = new JTextArea(text.isEmpty() ? "No history available." : text.toString(), 16, 48);
            content.setEditable(false); content.setLineWrap(true); content.setWrapStyleWord(true);
            JOptionPane.showMessageDialog(this, new JScrollPane(content), "Complaint status history", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private record AdminChoice(Long id, String label) {
        @Override public String toString() { return label; }
    }
    private record FormOptions(List<Category> categories, List<User> admins) {}
}
