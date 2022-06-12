package deveone.graphics;

import deveone.logic.Logic;
import deveone.graphics.additional.ManipulateModes;
import deveone.graphics.additional.ObjectModes;
import deveone.logic.graph.AdjMatrixWeightedGraph;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private JPanel mainPanel;
    private JCheckBox vertexModeCheckBox;
    private JCheckBox edgeModeCheckBox;
    private JCheckBox addModeCheckBox;
    private JTextField weightField;
    private JButton getAnswerButton;
    private JCheckBox delModeCheckBox;
    private JLabel weightLabel;
    private JPanel graphPanel;
    private JLabel answerLabel;
    private JTextField answerField;
    private JTextField circuitStartField;
    private JTextField circuitEndField;
    private JLabel circuitStartLabel;
    private JLabel circuitEndLabel;
    private JButton clearButton;

    private Canvas canvas = new Canvas();


    public MainFrame() {
        super();

        configureFrame();
        graphPanel.add(canvas, BorderLayout.CENTER);
        createButtonListeners();

        pack();
        setVisible(true);
    }

    private void configureFrame() {
        setContentPane(mainPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Resistance calculator");
        setMinimumSize(new Dimension(1500, 800));
        setResizable(false);
    }

    private void createButtonListeners() {
        edgeModeCheckBox.addActionListener(o -> {
            vertexModeCheckBox.setSelected(false);
            canvas.changeObjectMode(ObjectModes.EDGE);
        });

        vertexModeCheckBox.addActionListener(o -> {
            edgeModeCheckBox.setSelected(false);
            canvas.changeObjectMode(ObjectModes.VERTEX);
        });

        weightField.addActionListener(o -> {
            try {
                int weight = Integer.parseInt(o.getActionCommand());
                canvas.setWeight(weight);
            } catch (Exception ignore) {}
        });

        addModeCheckBox.addActionListener(o ->{
            delModeCheckBox.setSelected(false);
            canvas.changeManipulateMode(ManipulateModes.ADD);
        });

        delModeCheckBox.addActionListener(o ->{
            addModeCheckBox.setSelected(false);
            canvas.changeManipulateMode(ManipulateModes.DELETE);
        });

        getAnswerButton.addActionListener(o -> {
            int startNode = Integer.parseInt(circuitStartField.getText());
            int endNode = Integer.parseInt(circuitEndField.getText());

            AdjMatrixWeightedGraph graph = canvas.getGraph();

            double answer = Logic.calcCircuitResistance(graph, startNode, endNode);
            if (answer == -1) {
                JOptionPane.showMessageDialog(this, "Начало цепи не связано с концом цепи",
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            answerField.setText(String.valueOf(answer));
        });

        clearButton.addActionListener(o -> canvas.clearAll());
    }
}
