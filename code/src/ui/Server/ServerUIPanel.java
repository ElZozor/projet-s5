package ui.Server;

import backend.data.*;
import backend.modele.*;
import backend.server.communication.classic.ClassicMessage;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeSet;

import static backend.database.Keys.*;

public class ServerUIPanel extends JPanel {

    private static final String[] tables = new String[]{"Utilisateur", "Groupe", "Ticket", "Message"};
    private static final String HINT_SEARCH_BAR = "Rechercher";

    public static UserModel userTableModel;
    public static GroupModel groupTableModel;
    public static TicketModel ticketTableModel;
    public static MessageModel messageTableModel;
    public static SearchableModel<? extends ProjectTable> currentModel;
    public static SearchableModel<? extends ProjectTable> searchModel;


    private JComboBox<String> table_selector;
    private JTextField search_bar;
    private JButton add_button;
    private JButton edit_button;
    private JButton del_button;
    private JPanel upside_options;
    private JScrollPane table_container;
    private JTable table;
    private ServerUI parent;

    public ServerUIPanel(ServerUI parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        initialize();
    }

    public void updateModels(UserModel userModel, GroupModel groupModel, TicketModel ticketModel, MessageModel messageModel) {
        userTableModel = userModel;
        groupTableModel = groupModel;
        ticketTableModel = ticketModel;
        messageTableModel = messageModel;

        updateModelTable();
    }

    private void initialize() {
        this.setLayout(new BorderLayout(0, 0));


        initTopPanel();
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        this.add(panel1, BorderLayout.CENTER);
        table_container = new JScrollPane();
        panel1.add(table_container, BorderLayout.CENTER);

        setUserModel();


        add_button.addActionListener((e) -> {
            final String selectedItem = (String) table_selector.getSelectedItem();
            if (selectedItem != null) {
                parent.edit(selectedItem, null);
            }
        });

        table_selector.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                searchModel = null;
                updateModelTable();
            }
        });

        search_bar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                warn();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                warn();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                warn();
            }

            private void warn() {
                if (currentModel != null) {
                    if (search_bar.hasFocus() || !(search_bar.getText().equals(HINT_SEARCH_BAR))) {
                        searchModel = currentModel.retrieveSearchModel(search_bar.getText());
                        table.setModel(searchModel);
                    }
                }
            }
        });

        search_bar.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
                if (search_bar.getText().equals(HINT_SEARCH_BAR)) {
                    search_bar.setText("");
                }
            }

            @Override
            public void focusLost(FocusEvent focusEvent) {
                if (search_bar.getText().isEmpty()) {
                    search_bar.setText(HINT_SEARCH_BAR);
                    searchModel = null;
                }
            }
        });

        edit_button.addActionListener(action -> {

        });

        del_button.addActionListener(action -> {
            int selectRow = table.getSelectedRow();
            if (selectRow == -1) {
                return;
            }

            int input = JOptionPane.showConfirmDialog(
                    null,
                    "Voulez-vous vraiment supprimer cette entrée ?",
                    "Suppression",
                    JOptionPane.YES_NO_OPTION
            );

            if (input == 1) {
                return;
            }


            Long id = (Long) table.getValueAt(selectRow, 0);

            final String selected_table = (String) table_selector.getSelectedItem();

            if (selected_table != null) {
                switch (selected_table) {
                    case "Utilisateur":
                        delUser(id);
                        break;

                    case "Groupe":
                        delGroup(id);
                        break;

                    case "Ticket":
                        delTicket(id);
                        break;

                    case "Message":
                        delMessage(id);
                        break;
                }
            }
        });

        edit_button.addActionListener(action -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                return;
            }

            String selected_table = (String) table_selector.getSelectedItem();
            if (selected_table == null) {
                return;
            }


            switch (selected_table) {
                case "Utilisateur":

                case "Groupe":
                    if (searchModel == null) {
                        parent.edit(selected_table, currentModel.getReferenceTo(selectedRow));
                    } else {
                        parent.edit(selected_table, searchModel.getReferenceTo(selectedRow));
                    }

                    break;


                default:
                    JOptionPane.showMessageDialog(
                            this,
                            "Impossible de modifier une entrée de cette table !"
                    );
            }
        });

    }

    private void updateModelTable() {
        search_bar.setText("");
        String selection = (String) table_selector.getSelectedItem();
        if (selection == null) {
            return;
        }

        switch (selection) {
            case "Utilisateur":
                setUserModel();
                break;

            case "Groupe":
                setGroupModel();
                break;

            case "Ticket":
                setTicketModel();
                break;

            case "Message":
                setMessageModel();
                break;
        }
    }

    private void initTopPanel() {
        upside_options = new JPanel(new GridBagLayout());
        this.add(upside_options, BorderLayout.NORTH);

        table_selector = new JComboBox<>(tables);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.5;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        upside_options.add(table_selector, constraints);

        search_bar = new JTextField();
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.weightx = 0.5;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 8, 0, 8);
        upside_options.add(search_bar, constraints);

        add_button = new JButton("Ajouter");
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        upside_options.add(add_button, constraints);

        edit_button = new JButton("Éditer");
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.weightx = 0.5;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        upside_options.add(edit_button, constraints);

        del_button = new JButton("Supprimer");
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 1;
        constraints.weightx = 0.5;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        upside_options.add(del_button, constraints);

    }


    private void setUserModel() {
        setModel(userTableModel);
    }

    private void setGroupModel() {
        setModel(groupTableModel);
    }


    private void setTicketModel() {
        setModel(ticketTableModel);
    }

    private void setMessageModel() {
        setModel(messageTableModel);
    }


    private void setModel(SearchableModel<? extends ProjectTable> model) {
        if (table == null) {
            table = new JTable();
            table_container.setViewportView(table);
        }

        if (model != null) {
            table.setModel(model);
            currentModel = model;
        }
    }


    private void delUser(Long id) {
        parent.client.sendData(
                ClassicMessage.createDeleteMessage(
                        TABLE_NAME_UTILISATEUR,
                        new Utilisateur(id, "", "", "", "")
                )
        );
    }

    private void delGroup(Long id) {
        parent.client.sendData(
                ClassicMessage.createDeleteMessage(
                        TABLE_NAME_GROUPE,
                        new Groupe(id, "")
                )
        );
    }

    private void delTicket(Long id) {
        parent.client.sendData(
                ClassicMessage.createDeleteMessage(
                        TABLE_NAME_TICKET,
                        new Ticket(id, "", new TreeSet<>())
                )
        );
    }

    private void delMessage(Long id) {
        parent.client.sendData(
                ClassicMessage.createDeleteMessage(
                        TABLE_NAME_MESSAGE,
                        new Message(id, 0L, 0L, new Date(0), "", new ArrayList<>(), new ArrayList<>())
                )
        );
    }


    public void update() {
        updateModelTable();
        table.updateUI();
    }
}
