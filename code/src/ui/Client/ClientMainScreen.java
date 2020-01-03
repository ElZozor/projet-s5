package ui.Client;

import backend.data.Groupe;

import javax.swing.*;
import java.awt.*;
import java.util.TreeSet;

public class ClientMainScreen extends JFrame {

    JSplitPane mainPanel = new JSplitPane();
    GroupePanel groupePanel;
    MessageEditor messageEditor;

    TreeSet<Groupe> groupes;

    public ClientMainScreen(TreeSet<Groupe> groups) {
        super();

        if (groups == null) {
            groupes = new TreeSet<>();
        } else {
            groupes = groups;
        }

        initPanel();

        setSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setVisible(true);
    }

    private void initPanel() {
        setContentPane(mainPanel);
        mainPanel.setOrientation(JSplitPane.HORIZONTAL_SPLIT);

        initLeftSidePanel();
        initRightSidePanel();

        mainPanel.setLeftComponent(groupePanel);
        mainPanel.setRightComponent(messageEditor);
    }

    private void initLeftSidePanel() {
        groupePanel = new GroupePanel(groupes);
    }

    private void initRightSidePanel() {
        messageEditor = new MessageEditor();
    }

}
