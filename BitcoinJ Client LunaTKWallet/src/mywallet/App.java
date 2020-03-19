package mywallet;

/*
 * Herkunft/Origin: http://javacrypto.bplaced.net/
 * Programmierer/Programmer: TaeGeun Moon
 * Copyright/Copyright: TaeGeun Moon
 * Lizenttext/Licence: verschiedene Lizenzen / several licenses
 * see LICENCE
 * getestet mit/tested with: Java Runtime Environment 11.0.5 x64
 * verwendete IDE/used IDE: intelliJ IDEA 2019.3.1
 * Datum/Date (dd.mm.jjjj): 19.03.2020
 * Programm/Program: BitcoinJ Client LunaTKWallet
 * Funktion: zeigt die Erstellung eines funtionierenden Bitcoin Clients
 * Function: show the creating of a working bitcoin client
 *
 * Sicherheitshinweis/Security notice
 * Die Programmroutinen dienen nur der Darstellung und haben keinen Anspruch auf eine korrekte Funktion,
 * insbesondere mit Blick auf die Sicherheit !
 * Prüfen Sie die Sicherheit bevor das Programm in der echten Welt eingesetzt wird.
 * The program routines just show the function but please be aware of the security part -
 * check yourself before using in the real world !
 *
 * Sie benötigen diverse Bibliotheken (alle im Github-Archiv im Unterordner "libs")
 * You need a lot of libraries (see my Github-repository in subfolder "libs")
 * verwendete BitcoinJ-Bibliothek / used BitcoinJ Library: bitcoinj-core-0.15.6.jar
 * my Github-Repository: https://github.com/java-crypto/BitcoinJ
 * libs in my Github-Repo: https://github.com/java-crypto/BitcoinJ_Libraries
 *
 * Source/Basis: https://github.com/LunaTK/Simple-Bitcoinj-GUI-Wallet
 */

import javafx.application.Platform;
import org.bitcoinj.utils.Threading;

public class App {
    public static void main(String args[]) {
        Threading.USER_THREAD = (Runnable runnable) -> {
            Platform.runLater(runnable);
        };
        MyBitcoinWallet.main(args);
    }
}