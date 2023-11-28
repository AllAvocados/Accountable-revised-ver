
package com.accountable.gui;
import com.accountable.CategoryUpdateListener;
import com.accountable.core.DataManager;
import com.accountable.core.Expense;
import com.accountable.core.Transaction;
import com.accountable.util.NonEditableTableModel;
import com.accountable.core.Progress;
import javax.swing.table.DefaultTableModel;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ExpensePanel extends JPanel implements CategoryUpdateListener {
    private String currentUsername;
    private DefaultTableModel expenseModel;
    private JTextField expenseNameField;
    private JTextField expenseAmountField;
    private JButton addExpenseButton;
    private JButton deleteExpenseButton;
    private JTable expenseTable;

    private JComboBox<String> expenseCategoryComboBox;

    private BudgetPanel budgetPanel;

    public ExpensePanel(String currentUsername, BudgetPanel budgetPanel) {
        this.currentUsername = currentUsername;
        this.budgetPanel = budgetPanel; // Set the reference here
        // rest of your constructor code


        setLayout(new BorderLayout());

        JLabel headerLabel = new JLabel("Expense Tracking", JLabel.CENTER);
        add(headerLabel, BorderLayout.NORTH);

        JPanel expenseManagementPanel = new JPanel();
        expenseManagementPanel.setLayout(new GridLayout(0, 2));

        expenseManagementPanel.add(new JLabel("Expense Name:"));
        expenseNameField = new JTextField(20);
        expenseManagementPanel.add(expenseNameField);

        expenseManagementPanel.add(new JLabel("Expense Amount:"));
        expenseAmountField = new JTextField(20);
        expenseManagementPanel.add(expenseAmountField);

        expenseManagementPanel.add(new JLabel("Spending Category:"));
        expenseCategoryComboBox = new JComboBox<>(new DefaultComboBoxModel<>());
        expenseManagementPanel.add(expenseCategoryComboBox);

        addExpenseButton = new JButton("Add Expense");
        addExpenseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addExpense();
            }
        });
        expenseManagementPanel.add(addExpenseButton);

        String[] columnNames = {"Expense Name", "Amount", "Spending Category", "Date", "Budget", "Remaining"};
        //String[] columnNames = {"Expense Name", "Amount", "Spending Category", "Date"};

        expenseModel = new NonEditableTableModel(columnNames, 0);
        expenseTable = new JTable(expenseModel);
        add(new JScrollPane(expenseTable), BorderLayout.CENTER);

        deleteExpenseButton = new JButton("Delete Expense");
        deleteExpenseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteExpense();
            }
        });
        expenseManagementPanel.add(deleteExpenseButton);
        add(expenseManagementPanel, BorderLayout.SOUTH);

        loadExpenses(); // Load expenses when the panel is initialized

    }

    // Use this method to get the current user's username when saving/loading expenses
    private String getCurrentUsername() {
        return currentUsername;
    }

    public void updateCategoryComboBox(List<String> categories) {
        expenseCategoryComboBox.setModel(new DefaultComboBoxModel<>(categories.toArray(new String[0])));
    }

    private void addExpense() {
        // Get the expense name and category from the input fields
        String expenseName = expenseNameField.getText();
        String spendingCategory = (String) expenseCategoryComboBox.getSelectedItem();

        // Parse the expense amount and validate the input
        String formattedAmount = expenseAmountField.getText();
        double amount = parseAmount(formattedAmount);
        if (amount == 0.0) {
            // Handle invalid amount input
            JOptionPane.showMessageDialog(this, "Invalid amount format.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Fetch the budget for the selected category from the budget data
        double categoryBudget = budgetPanel.getBudgetForCategory(spendingCategory);
        double remainingBudget = categoryBudget - sumOfExpensesInCategory(spendingCategory) - amount;

        // Validate the spending category
        if (spendingCategory == null || spendingCategory.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a spending category or create one in the Budget section first.", "Invalid Spending Category", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Create a new expense object
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Expense newExpense = new Expense(expenseName, amount, spendingCategory, new Date());

        // Add the new expense to the table model
        Object[] rowData = {
                newExpense.getName(),
                String.format("$%.2f", newExpense.getAmount()),
                newExpense.getCategory(),
                dateFormat.format(newExpense.getDate()),
                String.format("$%.2f", categoryBudget),
                String.format("$%.2f", remainingBudget)
        };
        expenseModel.addRow(rowData);

        // Clear the input fields after adding the expense
        expenseNameField.setText("");
        expenseAmountField.setText("");

        // Notify the user of the successful addition
        JOptionPane.showMessageDialog(this, "Expense Added: " + expenseName, "Expense Added", JOptionPane.INFORMATION_MESSAGE);
    }

    // Example method that converts a formatted string to a double
    private double parseAmount(String formattedAmount) {
        formattedAmount = formattedAmount.replaceAll("[^\\d.]+", ""); // Remove non-numeric characters
        try {
            return Double.parseDouble(formattedAmount);
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse amount: " + formattedAmount);
            return 0.0; // Return 0 or handle the error appropriately
        }
    }


    // Method to calculate sum of expenses for a given category
    private double sumOfExpensesInCategory(String category) {
        double sum = 0.0;
        // Assuming "Spending Category" is the third column (index 2)
        int spendingCategoryColumnIndex = 2;
        // Assuming "Amount" is the second column (index 1)
        int amountColumnIndex = 1;
        for (int i = 0; i < expenseModel.getRowCount(); i++) {
            String expenseCategory = (String) expenseModel.getValueAt(i, spendingCategoryColumnIndex);
            if (category.equals(expenseCategory)) {
                String amountString = ((String) expenseModel.getValueAt(i, amountColumnIndex)).replace("$", "").replace(",", "");
                double amount = Double.parseDouble(amountString);
                sum += amount;
            }
        }
        return sum;
    }



    private void deleteExpense() {
        int selectedRow = expenseTable.getSelectedRow();
        if (selectedRow != -1) {
            expenseModel.removeRow(selectedRow);
            JOptionPane.showMessageDialog(ExpensePanel.this,
                    "Expense Deleted",
                    "Expense Deleted", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(ExpensePanel.this,
                    "Please select an expense to delete",
                    "No Expense Selected", JOptionPane.WARNING_MESSAGE);
        }
    }

    // Helper method to get expenses from the table model
    private List<Expense> getExpensesFromTable() {
        List<Expense> expenses = new ArrayList<>();
        for (int i = 0; i < expenseModel.getRowCount(); i++) {
            String name = (String) expenseModel.getValueAt(i, 0);
            Object amountObject = expenseModel.getValueAt(i, 1);
            String amountString = amountObject instanceof Double ? String.format("%.2f", (Double) amountObject) : amountObject.toString();
            String category = (String) expenseModel.getValueAt(i, 2);
            Date date = (Date) expenseModel.getValueAt(i, 3); // Ensure this is the correct way you are storing dates
            expenses.add(new Expense(name, Double.parseDouble(amountString), category, date));
        }
        return expenses;
    }

    // Method to save expenses into a JSON file
    public void saveExpenses() {
        List<Expense> expenses = getExpensesFromTable();
        DataManager.saveExpenses(currentUsername, expenses);
    }


    public void loadExpenses() {
        List<Expense> expenses = DataManager.loadExpenses(currentUsername);
        for (Expense expense : expenses) {
            double budget = 100.00; // Set budget to 100.00 as an example
            double remaining = calculateRemainingBudget(expense, budget);

            // Add existing expense to the table model including the budget and remaining values
            Object[] rowData = new Object[]{
                    expense.getName(),
                    String.format("$%.2f", expense.getAmount()),
                    expense.getCategory(),
                    expense.getDate(),
                    String.format("$%.2f", budget),
                    String.format("$%.2f", remaining)
            };
            expenseModel.addRow(rowData);
        }
    }

    // Override method to update the category combo box when categories are updated
    @Override
    public void updateCategories(List<String> categories) {
        // Update the combo box model with the new list of categories
        expenseCategoryComboBox.setModel(new DefaultComboBoxModel<>(categories.toArray(new String[0])));
    }


    @Override
    public void updateBudgetCategories(List<String> updatedCategories, List<Double> updatedBudgets) {
        updateCategoryComboBox(updatedCategories);

        // TODO: Use the updated budget information in the ExpensePanel
        // For example, access the budget for the first category as follows:
        Double budgetForFirstCategory = updatedBudgets.get(0);
        System.out.println("Budget for the first category: " + budgetForFirstCategory);
    }
    private double calculateRemainingBudget(Expense expense, double budget) {
        return budget - expense.getAmount();
    }



}

