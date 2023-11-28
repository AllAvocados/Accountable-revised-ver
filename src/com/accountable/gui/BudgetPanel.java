package com.accountable.gui;// BudgetPanel.java
import com.accountable.CategoryUpdateListener;
import com.accountable.core.Category;
import com.accountable.core.DataManager;
import com.accountable.core.Expense;
import com.accountable.core.Transaction;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.util.stream.Collectors;


public class BudgetPanel extends JPanel implements CategoryUpdateListener {
    private String currentUsername;
    private DefaultTableModel categoryModel;
    // Class variable to keep track of the total allocated amount.
    private static double totalAllocated = 0.0;
    private static double predictedIncome = 0.0;
    // You need a JLabel to show the allocation message, let's call it 'allocationMessageLabel'
    private static JLabel allocationMessageLabel;
    private JTextField categoryNameField;
    private JTextField categoryAmountField;
    private JButton addCategoryButton;
    private JButton deleteCategoryButton;
    private JTable categoryTable;

    private JComboBox<String> expenseCategoryComboBox;

    private List<CategoryUpdateListener> categoryUpdateListeners = new ArrayList<>();

    public BudgetPanel(String currentUsername) {
        this.currentUsername = currentUsername;
        setLayout(new BorderLayout());

        JLabel headerLabel = new JLabel("Budget", JLabel.CENTER);
        add(headerLabel, BorderLayout.NORTH);

        JPanel budgetManagementPanel = new JPanel();
        budgetManagementPanel.setLayout(new GridLayout(0, 2));

        budgetManagementPanel.add(new JLabel("Category Name:"));
        categoryNameField = new JTextField(20);
        budgetManagementPanel.add(categoryNameField);

        budgetManagementPanel.add(new JLabel("Amount Allocated:"));
        categoryAmountField = new JTextField(20);
        budgetManagementPanel.add(categoryAmountField);

        addCategoryButton = new JButton("Add Category");
        addCategoryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addBudget();
            }
        });
        budgetManagementPanel.add(addCategoryButton);

        String[] columnNames = {"Category Name", "Amount Allocated"};
        categoryModel = new DefaultTableModel(columnNames, 0);
        categoryTable = new JTable(categoryModel);
        add(new JScrollPane(categoryTable), BorderLayout.CENTER);

        deleteCategoryButton = new JButton("Delete Category");
        deleteCategoryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteCategory();
            }
        });
        budgetManagementPanel.add(deleteCategoryButton);

        add(budgetManagementPanel, BorderLayout.SOUTH);

        initializeAllocationMessage(); // Call this method to initialize the label
        loadCategories(); // Load categories when the panel is initialized
    }

    // Use this method to get the current user's username when saving/loading categories
    private String getCurrentUsername() {
        return currentUsername;
    }

    // This method gets the budget for a specific category.
    public double getBudgetForCategory(String categoryName) {
        for (int i = 0; i < categoryModel.getRowCount(); i++) {
            String category = (String) categoryModel.getValueAt(i, 0);
            if (category.equals(categoryName)) {
                String budgetStr = (String) categoryModel.getValueAt(i, 1);
                try {
                    return Double.parseDouble(budgetStr.replace("$", "").replace(",", ""));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    return 0.0; // or handle the error as appropriate
                }
            }
        }
        return 0.0; // if the category was not found, you might want to handle this case differently
    }

    // Method to update combo boxes or lists in other panels when a new category is added
    private void updateCategoryComponents() {
        // Get the list of category names from the category model
        String[] categories = getCategoryNames();

        // Notify other panels that need to update their category list
        for (CategoryUpdateListener listener : categoryUpdateListeners) {
            listener.updateCategories(Arrays.asList(categories));
        }
    }

    private void addBudget() {
        String categoryName = categoryNameField.getText();
        String categoryAmount = categoryAmountField.getText();

        // Assuming you have validation in place for categoryAmount and spendingCategory
        Transaction newExpense = new Transaction(categoryName, Double.parseDouble(categoryAmount), new Date(), true);

        categoryModel.addRow(new Object[]{
                newExpense.getDescription(),
                String.format("$%.2f", newExpense.getAmount()),
        });

        notifyListeners();
        notifyBudgetListeners();

        categoryNameField.setText("");
        categoryAmountField.setText("");

        JOptionPane.showMessageDialog(BudgetPanel.this,
                "Category Added: " + categoryName);
        recalculateAndDisplayAllocation(); // Add this line after the category is successfully added
        updateCategoryComponents();
    }


    // Method to save categories into a JSON file
    public void saveCategories() {
        List<Category> categories = getCategoriesFromTable();
        DataManager.saveCategories(currentUsername, categories);
    }

    // Method to load categories from a JSON file
    public void loadCategories() {
        List<Category> categories = DataManager.loadCategories(currentUsername);
        for (Category category : categories) {
            categoryModel.addRow(new Object[]{category.getName(), category.getAmountAllocated()});
        }

        recalculateAndDisplayAllocation(); // Update the budget panel with loaded data
    }


    private void deleteCategory() {
        int selectedRow = categoryTable.getSelectedRow();
        if (selectedRow != -1) {
            categoryModel.removeRow(selectedRow);
            notifyListeners();
            notifyBudgetListeners();
            recalculateAndDisplayAllocation(); // Add this line after the category is successfully deleted
            JOptionPane.showMessageDialog(BudgetPanel.this,
                    "Category Deleted",
                    "Category Deleted", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(BudgetPanel.this,
                    "Please select a category to delete",
                    "No Category Selected", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void addCategoryUpdateListener(CategoryUpdateListener listener) {
        categoryUpdateListeners.add(listener);
    }

    private void notifyListeners() {
        for (CategoryUpdateListener listener : categoryUpdateListeners) {
            listener.updateCategories(List.of(getCategoryNames()));
        }
    }

        // This method collects the budgets from the table model and returns them as a List of Doubles.
        public List<Double> getCategoryBudgets() {
            List<Double> budgets = new ArrayList<>();
            for (int i = 0; i < categoryModel.getRowCount(); i++) {
                // Assuming your model stores the budget as a String with a dollar sign, like "$100.00"
                String budgetStr = (String) categoryModel.getValueAt(i, 1);
                // Remove the dollar sign and commas if present, and parse the String to a Double
                try {
                    double budget = Double.parseDouble(budgetStr.replace("$", "").replace(",", ""));
                    budgets.add(budget);
                } catch (NumberFormatException e) {
                    // Handle the case where the string is not a valid number
                    e.printStackTrace();
                    // Perhaps log this error and/or add a default value
                    budgets.add(0.0);
                }
            }
            return budgets;
        }

    private List<Category> getCategoriesFromTable() {
        List<Category> categories = new ArrayList<>();
        for (int i = 0; i < categoryModel.getRowCount(); i++) {
            String name = (String) categoryModel.getValueAt(i, 0);
            // Parse the String to a Double after removing the dollar sign
            Double amount = Double.parseDouble(((String) categoryModel.getValueAt(i, 1)).replace("$", "").replace(",", ""));
            categories.add(new Category(name, amount));
        }
        return categories;
    }


    // Method to notify all listeners about budget updates
    private void notifyBudgetListeners() {
        List<String> categoryNames = List.of(getCategoryNames());
        List<Double> budgets = getCategoryBudgets();
        for (CategoryUpdateListener listener : categoryUpdateListeners) {
            listener.updateBudgetCategories(categoryNames, budgets);
        }
    }

    private String[] getCategoryNames() {
        int rowCount = categoryModel.getRowCount();
        String[] categoryNames = new String[rowCount];
        for (int i = 0; i < rowCount; i++) {
            categoryNames[i] = (String) categoryModel.getValueAt(i, 0);
        }
        return categoryNames;
    }

    @Override
    public void updateCategories(List<String> updatedCategories) {
        // Implement this method if needed
    }

    @Override
    public void updateBudgetCategories(List<String> updatedCategories, List<Double> updatedBudgets) {
        // Implement this method to handle budget updates
    }


    // Remove the static keyword to make this an instance method
    public void initializeAllocationMessage() {
        if (allocationMessageLabel == null) {
            allocationMessageLabel = new JLabel("$0.00 / $0.00 of this month's income allocated");
            add(allocationMessageLabel, BorderLayout.NORTH);
        }
    }

    private void updateAllocationMessage() {
        double amountLeft = predictedIncome - totalAllocated;
        allocationMessageLabel.setText(String.format("$%.2f / $%.2f of this month's income allocated, $%.2f left",
                totalAllocated, predictedIncome, amountLeft));
    }

    // Change this to an instance method as well
    public void setPredictedIncome(double income) {
        this.predictedIncome = income; // removed static modifier
        recalculateAndDisplayAllocation();
    }

    // Call this method whenever a new category is added or the income is updated.
    public void recalculateAndDisplayAllocation() {
        totalAllocated = calculateTotalAllocated();
        updateAllocationMessage();
    }

    private double calculateTotalAllocated() {
        double total = 0.0;
        for (int i = 0; i < categoryModel.getRowCount(); i++) {
            Object amountObj = categoryModel.getValueAt(i, 1);
            if (amountObj instanceof Double) {
                total += (Double) amountObj;
            } else {
                // If for some reason the value is not a Double, handle it here
                String amountStr = amountObj.toString();
                total += Double.parseDouble(amountStr.replace("$", "").replace(",", ""));
            }
        }
        return total;
    }


    // Method to be called when the user allocates money to a category
    public void onCategoryAllocation(double allocation) {
        // Add the allocation to the totalAllocated
        totalAllocated += allocation;
        // Update the allocation message
        updateAllocationMessage();
    }



}

