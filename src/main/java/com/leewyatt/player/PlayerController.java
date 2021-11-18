package com.leewyatt.player;

import com.leewyatt.rxcontrols.controls.RXAudioSpectrum;
import com.leewyatt.rxcontrols.controls.RXLrcView;
import com.leewyatt.rxcontrols.controls.RXMediaProgressBar;
import com.leewyatt.rxcontrols.controls.RXToggleButton;
import com.leewyatt.rxcontrols.pojo.LrcDoc;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @author LeeWyatt
 */
public class PlayerController {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private AnchorPane drawerPane;

    @FXML
    private BorderPane sliderPane;
    @FXML
    private StackPane soundBtn;
    @FXML
    private StackPane skinBtn;

    @FXML
    private ListView<File> listView;

    @FXML
    private RXAudioSpectrum audioSpectrum;

    @FXML
    private RXMediaProgressBar progressBar;

    @FXML
    private RXLrcView lrcView;

    @FXML
    private Label timeLabel;

    @FXML
    private ToggleButton playBtn;

    private Timeline showAnim;
    private Timeline hideAnim;
    private ContextMenu soundPopup;
    private ContextMenu skinPopup;

    private MediaPlayer player;
    private Slider soundSlider;

    private SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");

    /**
     * 频谱数据发生改变的时候,修改频谱可视化组件的数据
     */
    private AudioSpectrumListener audioSpectrumListener = new AudioSpectrumListener() {
        @Override
        public void spectrumDataUpdate(double timestamp, double duration, float[] magnitudes, float[] phases) {
            audioSpectrum.setMagnitudes(magnitudes);
        }
    };

    /**
     * 播放进度发生改变的时候..修改进度条的播放进度, 修改时间的显示
     */
    private ChangeListener<Duration> durationChangeListener = (ob1, ov1, nv1) -> {
        progressBar.setCurrentTime(nv1);
        changeTimeLabel(nv1);
    };

    /**
     * 当播放时间改变的时候. 修改时间的显示
     */
    private void changeTimeLabel(Duration nv1) {
        String currentTime = sdf.format(nv1.toMillis());
        String bufferedTimer = sdf.format(player.getBufferProgressTime().toMillis());
        timeLabel.setText(currentTime+ " / "+bufferedTimer);
    }


    private float[] emptyAry = new float[128];

    @FXML
    void initialize() {
        initAnim();
        initSoundPopup();
        initSkinPopup();
        initListView();


        Arrays.fill(emptyAry, -60.0f);

        initProgressBar();
    }

    private void initProgressBar() {
        //进度条的拖动 或者 点击 进行处理
        EventHandler<MouseEvent> progressBarHandler = event -> {
            if (player != null) {
                player.seek(progressBar.getCurrentTime());
                changeTimeLabel(progressBar.getCurrentTime());
            }
        };
        progressBar.setOnMouseClicked(progressBarHandler);
        progressBar.setOnMouseDragged(progressBarHandler);
    }

    private void initListView() {
        listView.setCellFactory(fileListView -> new MusicListCell());
        listView.getSelectionModel().selectedItemProperty().addListener((ob, oldFile, newFile) -> {
            if (player != null) {
                disposeMediaPlayer();
            }
            if (newFile != null) {
                player = new MediaPlayer(new Media(newFile.toURI().toString()));
                player.setVolume(soundSlider.getValue() / 100);
                //设置歌词
                String lrcPath = newFile.getAbsolutePath().replaceAll("mp3$", "lrc");
                File lrcFile = new File(lrcPath);
                if (lrcFile.exists()) {
                    try {
                        byte[] bytes = Files.readAllBytes(lrcFile.toPath());
                        //解析歌词
                        lrcView.setLrcDoc(LrcDoc.parseLrcDoc(new String(bytes, EncodingDetect.detect(bytes))));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //设置歌词进度
                lrcView.currentTimeProperty().bind(player.currentTimeProperty());
                //设置频谱可视化
                player.setAudioSpectrumListener(audioSpectrumListener);
                //设置进度条的总时长
                progressBar.durationProperty().bind(player.getMedia().durationProperty());
                //播放器的进度修改监听器
                player.currentTimeProperty().addListener(durationChangeListener);
                //如果播放完当前歌曲, 自动播放下一首
                player.setOnEndOfMedia(this::playNextMusic);
                playBtn.setSelected(true);

                player.play();
            }
        });

    }

    private void disposeMediaPlayer() {
        player.stop();
        lrcView.setLrcDoc(null);
        lrcView.currentTimeProperty().unbind();
        lrcView.setCurrentTime(Duration.ZERO);
        player.setAudioSpectrumListener(null);
        progressBar.durationProperty().unbind();
        progressBar.setCurrentTime(Duration.ZERO);
        player.currentTimeProperty().removeListener(durationChangeListener);
        audioSpectrum.setMagnitudes(emptyAry);
        timeLabel.setText("00:00 / 00:00");
        playBtn.setSelected(false);
        player.setOnEndOfMedia(null);
        player.dispose();
        player = null;
    }

    private void initSkinPopup() {
        skinPopup = new ContextMenu(new SeparatorMenuItem());
        Parent skinRoot = null;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/skin.fxml"));
            skinRoot = fxmlLoader.load();
            ObservableMap<String, Object> namespace = fxmlLoader.getNamespace();
            ToggleGroup skinGroup = (ToggleGroup) namespace.get("skinGroup");
            skinGroup.selectedToggleProperty().addListener((ob, ov, nv) ->{
                RXToggleButton btn = (RXToggleButton) nv;
                String skinName = btn.getText();
                String skinUrl = getClass().getResource("/css/" + skinName + ".css").toExternalForm();
                drawerPane.getScene().getRoot().getStylesheets().setAll(skinUrl);
                skinPopup.getScene().getRoot().getStylesheets().setAll(skinUrl);
                soundPopup.getScene().getRoot().getStylesheets().setAll(skinUrl);
            }) ;


        } catch (IOException e) {
            e.printStackTrace();
        }
        skinPopup.getScene().setRoot(skinRoot);
    }

    private void initSoundPopup() {
        soundPopup = new ContextMenu(new SeparatorMenuItem());
        Parent soundRoot = null;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/sound.fxml"));
            soundRoot = fxmlLoader.load();
            ObservableMap<String, Object> namespace = fxmlLoader.getNamespace();
            soundSlider = (Slider) namespace.get("soundSlider");
            Label soundNumLabel = (Label) namespace.get("soundNum");
            soundNumLabel.textProperty().bind(soundSlider.valueProperty().asString("%.0f%%"));
            //声音滑块改变时,改变player的音量
            soundSlider.valueProperty().addListener((ob, ov, nv) -> {
                if (player != null) {
                    player.setVolume(nv.doubleValue() / 100);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        soundPopup.getScene().setRoot(soundRoot);
    }

    private void initAnim() {
        showAnim = new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(sliderPane.translateXProperty(), 0)));
        hideAnim = new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(sliderPane.translateXProperty(), 300)));
        hideAnim.setOnFinished(event -> drawerPane.setVisible(false));
    }

    @FXML
    void onHideSliderPaneAction(MouseEvent event) {
        showAnim.stop();
        hideAnim.play();
    }

    @FXML
    void onShowSliderPaneAction(MouseEvent event) {
        drawerPane.setVisible(true);
        hideAnim.stop();
        showAnim.play();
    }

    @FXML
    void onCloseAction(MouseEvent event) {
        //disposeMediaPlayer();
        Platform.exit();
        //System.exit(0);
    }

    @FXML
    void onFullAction(MouseEvent event) {
        Stage stage = findStage();
        stage.setFullScreen(!stage.isFullScreen());
    }

    @FXML
    void onMiniAction(MouseEvent event) {
        Stage stage = findStage();
        stage.setIconified(true);
    }

    @FXML
    void onSoundPopupAction(MouseEvent event) {
        Bounds bounds = soundBtn.localToScreen(soundBtn.getBoundsInLocal());
        soundPopup.show(findStage(), bounds.getMinX() - 20, bounds.getMinY() - 165);
    }

    @FXML
    void onSkinPopupAction(MouseEvent event) {
        Bounds bounds = skinBtn.localToScreen(skinBtn.getBoundsInLocal());
        skinPopup.show(findStage(), bounds.getMaxX() - 135, bounds.getMaxY() + 10);
    }

    private Stage findStage() {
        return (Stage) drawerPane.getScene().getWindow();
    }

    @FXML
    void onAddMusicAction(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("mp3", "*.mp3"));
        List<File> fileList = fileChooser.showOpenMultipleDialog(findStage());
        ObservableList<File> items = listView.getItems();
        if (fileList != null) {
            fileList.forEach(file -> {
                if (!items.contains(file)) {
                    items.add(file);
                }
            });
        }
    }



    @FXML
    void onPlayAction(ActionEvent event) {
        if (player != null) {
            if (playBtn.isSelected()) {
                player.play();
            } else {
                player.pause();
            }
        } else {
            if (listView.getItems().size() != 0) {
                listView.getSelectionModel().select(0);
            }
        }


    }

    /**
     * 播放下一首
     */
    @FXML
    void onPlayNextAction(MouseEvent event) {
        playNextMusic();
    }

    private void playNextMusic() {
        int size = listView.getItems().size();
        if (size < 2) {
            return;
        }
        int index = listView.getSelectionModel().getSelectedIndex();
        //如果是最后一首歌, 那么下一首歌曲就是播放第一首歌曲
        index = (index==size-1)?0:index+1;
        listView.getSelectionModel().select(index);
    }

    @FXML
    void onPlayPrevAction(MouseEvent event) {
        int size = listView.getItems().size();
        if (size < 2) {
            return;
        }
        int index = listView.getSelectionModel().getSelectedIndex();
        //如果是最后一首歌, 那么下一首歌曲就是播放第一首歌曲
        index = (index==0)?size-1:index-1;
        listView.getSelectionModel().select(index);

    }

}
