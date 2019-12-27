package ui.Client;

import backend.data.Groupe;

import javax.swing.*;
import java.util.TreeSet;

public class ClientMainScreen extends JFrame {

    JSplitPane mainPanel = new JSplitPane();
    GroupePanel groupePanel;
    MessageEditor messageEditor;

    TreeSet<Groupe> groupes = new TreeSet<>();

    public ClientMainScreen() {
        super();

        initPanel();
    }

    private void initPanel() {
        setContentPane(mainPanel);
        mainPanel.setOrientation(JSplitPane.VERTICAL_SPLIT);

        mainPanel.setLeftComponent(groupePanel);
        mainPanel.setRightComponent(messageEditor);
    }

    private void initLeftSidePanel() {
        groupePanel = new GroupePanel(new TreeSet<>());
    }

    private void initRightSidePanel() {
        messageEditor = new MessageEditor();
    }

}
