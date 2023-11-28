package com.accountable.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainWindow extends JFrame {

    private IncomePanel incomePanel;
    private BudgetPanel budgetPanel;
    private ExpensePanel expensePanel; // Declare expensePanel at the class level

    private String currentUsername;

    public MainWindow(String username) {
        this.currentUsername = username;

        budgetPanel = new BudgetPanel(currentUsername);
        incomePanel = new IncomePanel();
        expensePanel = new ExpensePanel(currentUsername, budgetPanel);;

        // Set the BudgetPanel instance to the IncomePanel
        incomePanel.setBudgetPanel(budgetPanel);

        setTitle("Accountable - Main Window");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Add ExpensePanel as a listener to BudgetPanel
        budgetPanel.addCategoryUpdateListener(expensePanel);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Budget", budgetPanel);
        tabbedPane.addTab("Expenses", expensePanel);
        tabbedPane.addTab("Income", incomePanel); // Change to use the incomePanel instance
        tabbedPane.addTab("Reports", new ReportPanel());

        // Pass 'this' as the second argument to refer to the current instance of MainWindow
        SettingsPanel settingsPanel = new SettingsPanel(currentUsername, this);
        tabbedPane.addTab("Settings", settingsPanel);

        // Set the layout of the content pane to BorderLayout
        getContentPane().setLayout(new BorderLayout());

        // Add the JTabbedPane to the CENTER of the content pane
        getContentPane().add(tabbedPane, BorderLayout.CENTER);

        // Apply the theme based on the user's settings
        settingsPanel.loadAndApplyTheme();


        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                budgetPanel.saveCategories();
                expensePanel.saveExpenses();
                incomePanel.saveMonthlyIncomes();
                System.exit(0); // This line should terminate the application.
            }
        });

        // Create a save button
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveData();
            }
        });

        // Add the save button to a toolbar or directly to the frame
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(saveButton);
        getContentPane().add(toolBar, BorderLayout.PAGE_START); // Add the toolbar to the top of the application window

    }

    private void saveData() {
        try {
            budgetPanel.saveCategories();
            expensePanel.saveExpenses();
            incomePanel.saveMonthlyIncomes();
            // Update message with current allocation
            budgetPanel.recalculateAndDisplayAllocation();
            JOptionPane.showMessageDialog(this, "Data saved successfully!", "Save Data", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to save data: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginWindow loginWindow = new LoginWindow();
            loginWindow.setVisible(true);
        });
    }
}
