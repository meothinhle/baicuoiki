package com.warehouse.client;

import com.warehouse.ui.LoginFrame;
import com.warehouse.util.UITheme;

import javax.swing.*;

public class MainClient {
    public static void main(String[] args) {
        UITheme.applyGlobalTheme();
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
