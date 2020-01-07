package ui.Server;

import backend.data.Utilisateur;
import backend.server.communication.classic.ClassicMessage;

import javax.swing.*;
import java.awt.*;

import static backend.database.Keys.TABLE_NAME_UTILISATEUR;

public class EditUserPanel extends JPanel {

    private final static String[] types = {"admin", "staff", "other"};

    private static final Insets default_insets
            = new Insets(8, 8, 8, 8);
    private JTextField ineField = new JTextField();
    private JTextField nomField = new JTextField();
    private JTextField prenomField = new JTextField();
    private JTextField groupField = new JTextField();
    private JPasswordField mdpField = new JPasswordField();
    private JComboBox<String> typeField = new JComboBox<>(types);
    private JButton enregistrerButton = new JButton();
    private JButton annulerButton = new JButton();
    private ServerUI parent;

    private Utilisateur user;

    public EditUserPanel(ServerUI parent, Utilisateur user) {
        this.parent = parent;
        initialize();
        setActionListeners();

        if (user != null) {
            setFieldsValues(user);
            this.user = user;
        }
    }

    public void initialize() {
        setLayout(new GridBagLayout());

        createINEField();
        createNomField();
        createPrenomField();
        createMdpField();
        createTypeField();
        createGroupField();
        createActionsSection();


        // Avoid the vertically centered position
        final JPanel spacer1 = new JPanel();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 7;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.VERTICAL;
        this.add(spacer1, constraints);
    }

    private void createINEField() {
        final JLabel label1 = new JLabel();
        label1.setText("INE");

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = default_insets;
        this.add(label1, constraints);


        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = default_insets;
        this.add(ineField, constraints);
    }

    private void createNomField() {
        final JLabel label2 = new JLabel();
        label2.setText("NOM");
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = default_insets;
        this.add(label2, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = default_insets;
        this.add(nomField, constraints);
    }

    private void createPrenomField() {
        final JLabel label3 = new JLabel();
        label3.setText("PRENOM");
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = default_insets;
        this.add(label3, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = default_insets;
        this.add(prenomField, constraints);
    }

    private void createMdpField() {
        final JLabel label5 = new JLabel();
        label5.setText("MDP");
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = default_insets;
        this.add(label5, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 5;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = default_insets;
        this.add(mdpField, constraints);
    }

    private void createGroupField() {
        final JLabel label5 = new JLabel();
        label5.setText("GROUPES");
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = default_insets;
        this.add(label5, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 6;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = default_insets;
        this.add(groupField, constraints);
    }

    private void createTypeField() {
        final JLabel label4 = new JLabel();
        label4.setText("TYPE");
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = default_insets;
        this.add(label4, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = default_insets;
        this.add(typeField, constraints);
    }

    private void createActionsSection() {
        final JLabel label6 = new JLabel();
        label6.setHorizontalAlignment(0);
        label6.setText("Actions");
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = default_insets;
        this.add(label6, constraints);


        enregistrerButton.setText("Enregistrer");
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = default_insets;
        this.add(enregistrerButton, constraints);


        annulerButton.setText("Annuler");
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = default_insets;
        this.add(annulerButton, constraints);
    }

    private void setActionListeners() {
        annulerButton.addActionListener(actionEvent -> parent.setMainPanel());


        enregistrerButton.addActionListener(actionEvent -> {
            if (user == null) {
                saveNewUser();
            } else {
                editExistingUser();
            }
        });
    }


    private void setFieldsValues(Utilisateur user) {
        ineField.setText(user.getINE());
        prenomField.setText(user.getPrenom());
        nomField.setText(user.getNom());
        groupField.setText(String.join(";", user.getGroups()));
    }


    private void saveNewUser() {
        final String INE = ineField.getText();
        final String nom = nomField.getText();
        final String prenom = prenomField.getText();
        final String groups = groupField.getText();
        final String mdp = new String(mdpField.getPassword());

        Object selectedItem = typeField.getSelectedItem();
        if (selectedItem == null) {
            return;
        }
        final String type = selectedItem.toString();

        Utilisateur user = new Utilisateur(0, nom, prenom, INE, type);
        user.setGroups(groups.split(";"));
        user.setPassword(mdp);

        parent.client.sendData(
                ClassicMessage.createAddMessage(
                        TABLE_NAME_UTILISATEUR,
                        user
                )
        );

        parent.setMainPanel();
    }


    private void editExistingUser() {
        final String INE = ineField.getText();
        final String nom = nomField.getText();
        final String prenom = prenomField.getText();
        final String groups = groupField.getText();
        final String mdp = new String(mdpField.getPassword());

        Object selectedItem = typeField.getSelectedItem();
        if (selectedItem == null) {
            return;
        }
        final String type = selectedItem.toString();

        if (user != null) {
            Utilisateur edittedUser = new Utilisateur(user.getID(), nom, prenom, INE, type);
            edittedUser.setGroups(groups.split(";"));
            edittedUser.setPassword(mdp);

            parent.client.sendData(
                    ClassicMessage.createUpdateMessage(
                            TABLE_NAME_UTILISATEUR,
                            edittedUser
                    )
            );
        }

        parent.setMainPanel();
    }

}
