package mywallet;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class HistoryCellController {
    @FXML
    Label labelTitle;
    @FXML
    Label labelValue;
    @FXML
    Label labelTime;
    @FXML
    Label labelFee;
    @FXML
    Label labelConfirmation;
    @FXML
    Label labelMemo;
    @FXML
    ImageView ivType;
    @FXML
    Pane paneFee;
    @FXML
    Pane rootPane;

    private Transaction tx;

    private static Image receiveImage = null;

    public HistoryCellController() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("fxml/transaction_cell.fxml"));
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        loadImages();
    }

    private void loadImages() {
        if (receiveImage == null) {
            receiveImage = new Image(getClass().getResource("resources/received.png").toString(), true);
        }
    }

    public void updateDisplayedInfo(Transaction tx) {
        this.tx = tx;
        Coin value = tx.getValue(DashboardController.getKit().wallet());
        Date time = tx.getUpdateTime();
        labelValue.setText(value.toFriendlyString());
        labelTime.setText(formatDate(time)); // ### changed
        // ### deprecated labelTime.setText(time.toLocaleString());
        labelConfirmation.setText("" + tx.getConfidence().getDepthInBlocks());
        labelMemo.setText(tx.getMemo() == null ? "" : tx.getMemo());

        if (value.isNegative()) { // sending
            labelFee.setText(tx.getFee() != null ? tx.getFee().toFriendlyString() : "UNKNOWN");
            if (tx.getConfidence().getDepthInBlocks() == 0) { // not comfirmed
                labelTitle.setText("Sending");
            } else {
                labelTitle.setText("Sent");
            }
        } else { // receiving
            ivType.setImage(receiveImage);
            paneFee.setVisible(false);
            if (tx.getConfidence().getDepthInBlocks() == 0) { // not comfirmed
                labelTitle.setText("Receiving");
            } else {
                labelTitle.setText("Received");
            }
        }
    }

    public Pane getRootPane() {
        return rootPane;
    }

    @FXML
    private void onSeeInBlockExplorer() {
        String baseURL = "https://tbtc.bitaps.com/";
        // ### not working in march 2020: String baseURL = "https://testnet.blockexplorer.com/tx/";
        try {
            Desktop.getDesktop().browse(new URI(baseURL + tx.getTxId()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ### for deprecated labelTime.setText(time.toLocaleString());
    private static String formatDate(Date date) {
        // provides the date and time in this format dd-MM-yyyy_HH-mm-ss e.g. 16-03-2020_10-27-15
        String pattern = "dd.MM.yyyy HH:mm:ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(date);
    }

}