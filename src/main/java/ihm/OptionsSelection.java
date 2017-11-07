package ihm;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class OptionsSelection extends JDialog {
    private JPanel contentPane;
    private JButton buttonLaunch;
    private JButton buttonClose;
    private JTextField textField1;
    private JTextField textField2;
    private JTextField textField3;

    public OptionsSelection() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonLaunch);

        buttonLaunch.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
    }

    private void onOK() {
        // add your code here
        dispose();
    }

    public static void main(String[] args) {
        OptionsSelection dialog = new OptionsSelection();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
