package code;

import java.io.*;
import java.util.ArrayList;
import javax.swing.*;

import code.Grafica.*;
import code.Util.Bot;

/**
 * Punto di avvio del programma. Mostra i menu iniziali e apre la modalità scelta dall'utente.
 */
public class Main {
    /**
     * Mostra un dialog grafico riutilizzabile per menu e scelte dell'utente.
     * @param titolo titolo del dialog
     * @param messaggio messaggio o componente da mostrare
     * @param percorsoImmagine percorso dell'icona
     * @param opzioni opzioni disponibili
     * @param valoreIniziale opzione selezionata inizialmente
     * @return indice della scelta effettuata
     */
    private static int mostraDialogPerScelta(String titolo, Object messaggio, String percorsoImmagine, String[] opzioni, String valoreIniziale) {
        int scelta = JOptionPane.showOptionDialog(
            null,
            messaggio,
            titolo,
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            new ImageIcon(percorsoImmagine),
            opzioni,
            valoreIniziale
        );
        return scelta;
    }

    /**
     * Chiede all'utente se vuole giocare in due oppure contro il bot.
     * @return true se viene scelta la modalità contro bot
     */
    private static boolean scegliConBot() {
        int sceltaBot = mostraDialogPerScelta(
            "Modalità partita",
            "Vuoi giocare contro il bot?",
            "code/Immagini/Pedone.png",
            new String[]{"2 giocatori", "Contro bot"},
            "2 giocatori"
        );

        if (sceltaBot < 0) {
            System.exit(0);
        }

        return sceltaBot == 1;
    }

    /**
     * Chiede all'utente la difficoltà del bot e la converte nella profondità di ricerca.
     * @return profondità del bot
     */
    private static int scegliDifficoltaBot() {
        int sceltaBot = mostraDialogPerScelta(
            "Difficoltà bot",
            "Imposta difficoltà bot",
            "code/Immagini/Pedone.png",
            new String[]{"Facile", "Media", "Difficile"},
            "Media"
        );

        if (sceltaBot == 0) sceltaBot += 1;
        if (sceltaBot == 1 || sceltaBot == 2) sceltaBot += 2;

        if (sceltaBot < 0) {
            System.exit(0);
        }

        return sceltaBot;
    }

    /**
     * Cerca i file .txt disponibili in una cartella e mostra un errore se non ne trova.
     * @param percorsoCartella cartella in cui cercare
     * @return lista dei file trovati
     */
    private static ArrayList<File> trovaFile(String percorsoCartella) {
        ArrayList<File> files = new ArrayList<>();
        File cartella = new File(percorsoCartella);

        if (cartella.exists() && cartella.isDirectory()) {
            File[] elencoFile = cartella.listFiles();
            if (elencoFile != null) {
                for (File file : elencoFile) {
                    if (file.isFile() && file.getName().endsWith(".txt")) {
                        files.add(file);
                    }
                }
            }
        }
        else {
            JOptionPane.showMessageDialog(
                null,
                "La cartella delle partite non esiste",
                "Errore",
                JOptionPane.ERROR_MESSAGE
            );

            System.exit(0);
        }

        if (files.isEmpty()) {
            JOptionPane.showMessageDialog(
                null,
                "Non ci sono partite salvate",
                "Errore",
                JOptionPane.ERROR_MESSAGE
            );

            System.exit(0);
        }
        return files;
    }

    /**
     * Avvia il programma mostrando il menu principale e aprendo partita o esercizio in base alla scelta.
     * @param args argomenti da riga di comando non usati
     * @throws IOException se il caricamento dei file fallisce
     */
    public static void main(String[] args) throws IOException {
        int sceltaIniziale = mostraDialogPerScelta(
                                "Menù", 
                                "Scegli che modalita vuoi giocare",
                                "code/Immagini/Pedone.png",
                                new String[]{"Partita", "Esercizio"},
                                "Partita"
                            );

        if (sceltaIniziale == 0) {
            int sceltaPartita = mostraDialogPerScelta(
                                    "Partita", 
                                    "                    Crea partita",
                                    "code/Immagini/Pedone.png",
                                    new String[]{"Crea nuova partita", "Carica partita da file"},
                                    "Crea nuova partita"
                                );

            if (sceltaPartita == 0) {
                boolean conBot = scegliConBot();
                FramePartita fp = new FramePartita(conBot);
                if (conBot) {
                    int difficoltaBot = scegliDifficoltaBot();
                    Bot bot = new Bot(difficoltaBot);
                    fp.getPartita().setBot(bot);
                }
                fp.avviaFrame();
            }

            else if (sceltaPartita == 1) {
                ArrayList<File> files = trovaFile("code/FilePartite");

                String[] nomiFile = new String[files.size()];

                for (int i = 0; i < nomiFile.length; i++) {
                    nomiFile[i] = files.get(i).getName().replace(".txt", "");
                }

                JComboBox<String> tendinaPartite = new JComboBox<String>(nomiFile);

                int sceltaFile = mostraDialogPerScelta(
                                    "Scegli partita", 
                                    tendinaPartite, 
                                    "code/Immagini/File.png",
                                    null,
                                    null
                                );

                if (sceltaFile != JOptionPane.OK_OPTION) {
                    System.exit(0);
                }

                int indiceFile = tendinaPartite.getSelectedIndex();

                FramePartita fp = new FramePartita(files.get(indiceFile).getAbsolutePath());
                fp.avviaFrame();
                
            }
            else {
                System.exit(0);
            }
        }

        if (sceltaIniziale == 1) {
            ArrayList<File> files = trovaFile("code/FileEsercizi");

            String[] scelteFile = new String[files.size()];

            for (int i = 0; i < scelteFile.length; i++) {
                scelteFile[i] = files.get(i).getName().replace(".txt", "");
            }

            JComboBox<String> tendinaEsercizi = new JComboBox<>(scelteFile);

            int sceltaFile = mostraDialogPerScelta(
                                "Scegli partita", 
                                tendinaEsercizi, 
                                "code/Immagini/File.png",
                                null,
                                null
                            );

            if (sceltaFile != JOptionPane.OK_OPTION) {
                System.exit(0);
            }

            int indiceFile = tendinaEsercizi.getSelectedIndex();

            FrameEsercizio fe = new FrameEsercizio(files.get(indiceFile).getAbsolutePath(), true);
            int difficoltaBot = scegliDifficoltaBot();
            Bot bot = new Bot(difficoltaBot);
            fe.getPartita().setBot(bot);
            fe.avviaFrame();
            
        }

        if (sceltaIniziale < 0) {
            System.exit(0);
        }
    }
}
