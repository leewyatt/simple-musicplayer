package com.leewyatt.player;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

import java.io.File;

/**
 * @author LeeWyatt
 * QQ 9670453
 */
public class MusicListCell extends ListCell<File> {

    private final Label label;
    private final BorderPane pane;

    public MusicListCell() {
        pane = new BorderPane();
        label = new Label();
        BorderPane.setAlignment(label, Pos.CENTER_LEFT);
        Button button = new Button();
        button.getStyleClass().add("remove-btn");
        button.setGraphic(new Region());
        button.setOnAction(event -> {
            if (getItem() == getListView().getSelectionModel().getSelectedItem()) {
                getListView().getSelectionModel().clearSelection();
            }
            getListView().getItems().remove(getItem());
        });
        pane.setCenter(label);
        pane.setRight(button);
    }

    @Override
    protected void updateItem(File item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
            setGraphic(null);
            setText("");
        } else {
            String name = item.getName();
            label.setText(name.substring(0, name.length() - 4) );
            setGraphic(pane);
        }

    }
}
