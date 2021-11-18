/**
 * @author LeeWyatt
 * QQ 9670453
 */
module simple.musicplayer {
    requires javafx.controls;
    requires javafx.media;
    requires javafx.fxml;
    requires rxcontrols;

    exports com.leewyatt.player;
    opens com.leewyatt.player to javafx.fxml;
}