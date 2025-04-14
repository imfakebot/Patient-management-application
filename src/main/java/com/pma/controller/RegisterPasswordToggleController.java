package com.pma.controller;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Controller class for managing password visibility toggling in a registration
 * form.
 * 
 * This class provides functionality to toggle the visibility of password fields
 * (show/hide passwords) for both the main password field and the re-enter
 * password field.
 */
public class RegisterPasswordToggleController {

    @FXML
    private PasswordField passwordField; // The main password field (hidden by default)
    @FXML
    private TextField textPasswordField; // The text field to show the password in plain text
    @FXML
    private ImageView toggleImagePassword; // The toggle icon for the main password field

    @FXML
    private PasswordField reEnterPasswordField; // The re-enter password field (hidden by default)
    @FXML
    private TextField textReEnterPasswordField; // The text field to show the re-entered password in plain text
    @FXML
    private ImageView toggleImageReEnterPassword; // The toggle icon for the re-enter password field

    private boolean isPasswordVisible = false; // Tracks the visibility state of the main password field
    private boolean isReEnterPasswordVisible = false; // Tracks the visibility state of the re-enter password field

    private final String SHOW_ICON = "/com/pma/img/open.png"; // Icon for showing the password
    private final String HIDE_ICON = "/com/pma/img/closed.png"; // Icon for hiding the password

    /**
     * Initializes the controller and sets up the initial state of the password
     * fields and toggle icons.
     * 
     * - Sets the initial toggle icons to the "hidden" state.
     * - Links the content of the PasswordField and TextField for both the main
     * password
     * and re-enter password fields using bidirectional binding.
     * - Hides the TextField by default.
     */
    @FXML
    private void initialize() {
        // Initialize the toggle icons to the "hidden" state
        toggleImagePassword.setImage(new Image(getClass().getResourceAsStream(HIDE_ICON)));
        toggleImageReEnterPassword.setImage(new Image(getClass().getResourceAsStream(HIDE_ICON)));

        // Bind the content of the PasswordField and TextField
        textPasswordField.setManaged(false);
        textPasswordField.setVisible(false);
        textPasswordField.textProperty().bindBidirectional(passwordField.textProperty());

        textReEnterPasswordField.setManaged(false);
        textReEnterPasswordField.setVisible(false);
        textReEnterPasswordField.textProperty().bindBidirectional(reEnterPasswordField.textProperty());
    }

    /**
     * Toggles the visibility of the main password field.
     * 
     * - If the password is currently hidden, it will be shown in plain text.
     * - If the password is currently visible, it will be hidden.
     * - Updates the toggle icon accordingly.
     */
    @FXML
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;

        if (isPasswordVisible) {
            textPasswordField.setVisible(true);
            textPasswordField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            toggleImagePassword.setImage(new Image(getClass().getResourceAsStream(SHOW_ICON)));
        } else {
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            textPasswordField.setVisible(false);
            textPasswordField.setManaged(false);
            toggleImagePassword.setImage(new Image(getClass().getResourceAsStream(HIDE_ICON)));
        }
    }

    /**
     * Toggles the visibility of the re-enter password field.
     * 
     * - If the re-entered password is currently hidden, it will be shown in plain
     * text.
     * - If the re-entered password is currently visible, it will be hidden.
     * - Updates the toggle icon accordingly.
     */
    @FXML
    private void toggleReEnterPasswordVisibility() {
        isReEnterPasswordVisible = !isReEnterPasswordVisible;

        if (isReEnterPasswordVisible) {
            textReEnterPasswordField.setVisible(true);
            textReEnterPasswordField.setManaged(true);
            reEnterPasswordField.setVisible(false);
            reEnterPasswordField.setManaged(false);
            toggleImageReEnterPassword.setImage(new Image(getClass().getResourceAsStream(SHOW_ICON)));
        } else {
            reEnterPasswordField.setVisible(true);
            reEnterPasswordField.setManaged(true);
            textReEnterPasswordField.setVisible(false);
            textReEnterPasswordField.setManaged(false);
            toggleImageReEnterPassword.setImage(new Image(getClass().getResourceAsStream(HIDE_ICON)));
        }
    }
}