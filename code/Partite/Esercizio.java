package code.Partite;

import code.Exception.FileNonValidoException;
import code.Util.Casella;

import java.io.*;
import java.util.Scanner;

/**
 * Rappresenta un esercizio di scacchi caricato da file, con colore che deve dare matto e numero di mosse disponibili.
 */
public class Esercizio extends PartitaAstratta {

    private boolean coloreCheDeveDareMatto;
    private int mosseRimanenti;

    /**
     * Crea un esercizio leggendo posizione, turno e mosse disponibili dal file indicato.
     * @param percorsoFile percorso del file dell'esercizio
     * @throws IOException se il file non può essere letto
     */
    public Esercizio(String percorsoFile) throws IOException {
        super();
        leggiMappaDaFile(percorsoFile);
    }

    /**
     * Carica la scacchiera dell'esercizio e le sue informazioni aggiuntive dal file.
     * @param percorsoFile percorso del file dell'esercizio
     * @throws IOException se il file non può essere letto
     * @throws FileNonValidoException se il formato non è corretto
     */
    public void leggiMappaDaFile(String percorsoFile) throws IOException {
        try (Scanner s = new Scanner(new File(percorsoFile))) {
            Casella[][] nuovaMappa = leggiScacchiera(s);

            if (!s.hasNextLine()) {
                throw new FileNonValidoException("Turno mancante nel file.");
            }

            attaccaBianco = leggiTurno(s.nextLine());
            coloreCheDeveDareMatto = attaccaBianco;

            if (!s.hasNextLine()) {
                throw new FileNonValidoException("Numero di mosse rimanenti mancante nel file.");
            }

            try {
                mosseRimanenti = Integer.parseInt(s.nextLine().trim());
            } catch (NumberFormatException e) {
                throw new FileNonValidoException("Numero di mosse rimanenti non valido.");
            }

            if (mosseRimanenti < 0) {
                throw new FileNonValidoException("Il numero di mosse rimanenti non può essere negativo.");
            }

            mappa = nuovaMappa;
        }
    }

    /**
     * Aggiorna lo stato dell'esercizio dopo una mossa valida.
     * @param coloreCheHaMosso colore che ha appena mosso
     */
    @Override
    protected void dopoMossa(boolean coloreCheHaMosso) {
        scalaMosseSeServe(coloreCheHaMosso);
    }

    /**
     * Scala le mosse rimanenti solo quando muove il colore che deve dare matto.
     * @param coloreCheHaMosso colore che ha appena mosso
     */
    private void scalaMosseSeServe(boolean coloreCheHaMosso) {
        if (coloreCheHaMosso == coloreCheDeveDareMatto) {
            mosseRimanenti--;
        }
    }

    public boolean getColoreCheDeveDareMatto() {
        return coloreCheDeveDareMatto;
    }

    public int getMosseRimanenti() {
        return mosseRimanenti;
    }
}
