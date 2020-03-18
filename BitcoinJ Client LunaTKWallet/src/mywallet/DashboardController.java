package mywallet;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import com.jfoenix.controls.JFXButton;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletChangeEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import mywallet.helper.DialogBuilder;
import mywallet.helper.QRRenderer;

public class DashboardController implements Initializable, WalletCoinsReceivedEventListener,
        WalletCoinsSentEventListener, WalletChangeEventListener {

    @FXML
    ListView<Transaction> lvHistory;
    @FXML
    ImageView qrImage;
    @FXML
    Label labelAddress;
    @FXML
    StackPane stackPane;
    @FXML
    Label labelBalance;
    @FXML
    Label labelEncryptionStatus;
    @FXML
    ImageView imgEncryptionStatus;
    @FXML
    JFXButton btnEncryptWallet;

    ObservableList<Transaction> transactionHistories = FXCollections.observableArrayList();

    private static WalletAppKit kit;
    private QRRenderer qrRenderer;
    private long lastUpdateTimestamp = System.currentTimeMillis();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        new Thread(() -> {
            initWallet();
        }).start();
    }

    private void initWallet() {
        BriefLogFormatter.initWithSilentBitcoinJ();
        NetworkParameters params = TestNet3Params.get();
        String filePrefix = "WalletTemplate-test";

        kit = new WalletAppKit(params, new File("."), filePrefix) {
            @Override
            protected void onSetupCompleted() {
                if (wallet().getKeyChainGroupSize() < 1) {
                    wallet().importKey(new ECKey());
                }
                // kit.wallet().allowSpendingUnconfirmedTransactions();
                kit.wallet().addCoinsReceivedEventListener(DashboardController.this);
                kit.wallet().addCoinsSentEventListener(DashboardController.this);
                kit.wallet().addChangeEventListener(DashboardController.this);
                initHistoryListView();
                updateDisplayedWalletInfo();
                logTransactions();
            }
        };
        kit.startAsync();
        kit.awaitRunning();
    }

    private void initHistoryListView() {
        lvHistory.setCellFactory(new Callback<ListView<Transaction>, ListCell<Transaction>>() {
            @Override
            public ListCell<Transaction> call(ListView<Transaction> param) {
                return new HistoryCell();
            }
        });
        lvHistory.setItems(transactionHistories);
    }

    @FXML
    private void onSendBitcoin(ActionEvent event) {
        SendBitcoinController.show(getClass());
    }

    @FXML
    private void onRequestBitcoin(ActionEvent event) {
        RequestBitcoinController.show(getClass());
    }

    @FXML
    private void onDownloadQRImage(ActionEvent event) {
        Stage stage = (Stage) ((Node) ((EventObject) event).getSource()).getScene().getWindow();
        FileChooser fileChooser = new FileChooser();

        // Set extension filter for text files
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("PNG files (*.png)", "*.png");
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setInitialFileName("my-bitcoin-address");

        // Show save file dialog
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                qrRenderer.saveAsFile(file);
            } catch (IOException e) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Failed to save QR Image");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        }
    }

    @FXML
    private void exitApp(ActionEvent event) {
        new DialogBuilder("Do you want to quit?").buildYesNo(stackPane, (e) -> {
            Platform.exit();
            System.exit(0);
        }, null).show();
    }

    @FXML
    private void onToggleWalletEncryption(ActionEvent event) {
        System.out.println("Toggle Wallet Encryption");
        if (kit.wallet().isEncrypted()) {
            Optional<String> password = DialogBuilder.buildPasswordInputDialog("Enter current password").showAndWait();
            if (!password.isEmpty()) {
                try {
                    kit.wallet().decrypt(password.get());
                } catch (KeyCrypterException e) {
                    Alert alert = new Alert(AlertType.INFORMATION);
                    alert.setTitle("Error");
                    alert.setHeaderText("Invalid password");
                    alert.setContentText("Password is wrong");
                    alert.showAndWait();
                }
            }
        } else {
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText("!!! IMPORTANT !!!");
            alert.setContentText(
                    "There is no way to recover password. \nIf you forget the password, \nyou lost your bitcoin FOREVER.");
            alert.showAndWait();
            Optional<String> password = DialogBuilder.buildPasswordInputDialog("Enter new password").showAndWait();
            if (!password.isEmpty()) {
                kit.wallet().encrypt(password.get());
            }
        }
        updateDisplayedWalletEncryptionStatus();
    }

    @FXML
    private void onCopyAddress(ActionEvent event) {
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(kit.wallet().currentReceiveAddress().toString()), null);

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Alert");
        alert.setHeaderText("Address Copied!");
        alert.setContentText(kit.wallet().currentReceiveAddress().toString());
        alert.showAndWait();
    }

    private void updateDisplayedWalletInfo() {
        Platform.runLater(() -> {
            qrRenderer = new QRRenderer(kit.wallet().currentReceiveAddress().toString());
            qrRenderer.displayIn(qrImage);
            labelAddress.setText(kit.wallet().currentReceiveAddress().toString());
            labelBalance.setText(kit.wallet().getBalance().toFriendlyString());
        });
        updateDisplayedWalletEncryptionStatus();
        updateDisplayedTransactionHistories();
    }

    private void updateDisplayedTransactionHistories() {
        Platform.runLater(() -> {
            ArrayList<Transaction> txList = new ArrayList(kit.wallet().getTransactions(true));
            txList.sort((tx1, tx2) -> {
                return tx2.getUpdateTime().compareTo(tx1.getUpdateTime());
            });
            transactionHistories.setAll(txList);
        });
    }

    private void updateDisplayedWalletEncryptionStatus() {
        Platform.runLater(() -> {
            if (kit.wallet().isEncrypted()) {
                imgEncryptionStatus
                        .setImage(new Image(getClass().getResource("resources/shield-green.png").toString(), true));
                labelEncryptionStatus.setText("Wallet is encrypted");
                labelEncryptionStatus.setTextFill(Paint.valueOf("#18b663"));
                btnEncryptWallet.setText("Decrypt Wallet");
            } else {
                imgEncryptionStatus
                        .setImage(new Image(getClass().getResource("resources/shield-red.png").toString(), true));
                labelEncryptionStatus.setText("Wallet is not encrypted");
                labelEncryptionStatus.setTextFill(Paint.valueOf("#c32121"));
                btnEncryptWallet.setText("Encrypt Wallet");
            }
        });
    }

    private void logTransactions() {
        Set<Transaction> transactions = kit.wallet().getTransactions(true);
        for (Transaction tx : transactions) {
            System.out.println("TXID : " + tx.getTxId());
            System.out.println("Confidence : " + tx.getConfidence());
            System.out.println("Value Sent to Me : " + tx.getValueSentToMe(kit.wallet()));
            System.out.println("Value Sent from Me : " + tx.getValueSentFromMe(kit.wallet()));
            System.out.println("Memo : " + tx.getMemo());
            System.out.println("Purpose : " + tx.getPurpose());
            System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
        }
    }

    public static WalletAppKit getKit() {
        return kit;
    }

    @Override
    public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Notice");
        alert.setHeaderText("Coin Sent");
        alert.setContentText("Previous balance : " + prevBalance.toFriendlyString() + "\n" + "New balance : "
                + newBalance.toFriendlyString());
        alert.showAndWait();
    }

    @Override
    public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
        Coin value = tx.getValueSentToMe(wallet);
        System.out.println("Received tx for " + value.toFriendlyString() + ": " + tx);
        System.out.println("Transaction will be forwarded after it confirms.");

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Notice");
        alert.setHeaderText("Coin Received");
        alert.setContentText("Received tx for " + tx.getTxId() + "\n" + "Received Value : " + value.toFriendlyString());
        alert.showAndWait();
    }

    @Override
    public void onWalletChanged(Wallet wallet) {
        long timeElapsed = System.currentTimeMillis() - lastUpdateTimestamp;
        if (timeElapsed > 1000) {
            updateDisplayedWalletInfo();
            lastUpdateTimestamp = System.currentTimeMillis();
        }
    }
}
