package ui.Server;

import backend.data.Groupe;
import backend.database.DatabaseManager;

import javax.swing.*;
import java.awt.*;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EditGroupPanel extends JPanel {

    private JTextField labelField = new JTextField();
    private JButton enregistrerButton = new JButton();
    private JButton annulerButton = new JButton();

    private ServerUI parent;


    public EditGroupPanel(ServerUI parent) {
        this.parent = parent;
        initialize();
        setActionListeners();
    }

    private void initialize() {
        this.setLayout(new GridBagLayout());

        createLabelField();
        createActionsSection();

        final JPanel spacer = new JPanel();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.VERTICAL;
        this.add(spacer, constraints);


    }

    private void createLabelField() {
        final JLabel label = new JLabel();
        label.setText("Label");
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(8, 8, 8, 8);
        this.add(label, constraints);

        labelField = new JTextField();
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        this.add(labelField, constraints);
    }

    private void createActionsSection() {
        final JLabel actions_label = new JLabel();
        actions_label.setHorizontalAlignment(0);
        actions_label.setText("Actions");

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(8, 8, 8, 8);
        this.add(actions_label, constraints);

        enregistrerButton = new JButton();
        enregistrerButton.setText("Enregistrer");
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        this.add(enregistrerButton, constraints);

        annulerButton = new JButton();
        annulerButton.setText("Annuler");
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        this.add(annulerButton, constraints);
    }

    private void setActionListeners() {
        enregistrerButton.addActionListener(actionEvent -> {
            final String label = labelField.getText();

            boolean success = false;
            ResultSet result;
            try {
                result = DatabaseManager.getInstance().createNewGroup(label);
                success = (result != null) && result.next();

                if (success) {
                    ServerUIPanel.groupTableModel.addRow(new Groupe(result.getLong(1), label));
                }
            } catch (SQLException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            if (success) {
                JOptionPane.showMessageDialog(this, "Groupe ajouté avec succès !");
                if (parent != null) {
                    parent.setMainPanel();
                }

            } else {
                JOptionPane.showMessageDialog(this, "Impossible d'ajouter le groupe !");
            }
        });

        annulerButton.addActionListener(actionEvent -> {
            if (parent != null) {
                parent.setMainPanel();
            }
        });
    }

}
