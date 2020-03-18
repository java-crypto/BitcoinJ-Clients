package mywallet;

import javafx.application.Platform;
import org.bitcoinj.utils.Threading;

// source: https://github.com/LunaTK/Simple-Bitcoinj-GUI-Wallet
// HistoryCellController changed method as it is deprecated


public class App {
    public static void main(String args[]) {
        Threading.USER_THREAD = (Runnable runnable) -> {
            Platform.runLater(runnable);
        };
        MyBitcoinWallet.main(args);
    }
}