package code.Grafica;

import code.Partite.Esercizio;
import java.awt.Color;
import java.io.IOException;

/**
 * Finestra grafica della modalità esercizio, con controllo del matto entro il numero di mosse richiesto.
 */
public class FrameEsercizio extends FrameScacchiAstratto {

    private final Esercizio esercizio;
    private final boolean coloreCheDeveDareMatto;

    /**
     * Crea la finestra della modalità esercizio leggendo i dati dal file indicato.
     * @param percorsoFile file dell'esercizio
     * @param conBot true se deve essere attivo il bot
     * @throws IOException se il file non può essere letto
     */
    public FrameEsercizio(String percorsoFile, boolean conBot) throws IOException {
        this(new Esercizio(percorsoFile), conBot);
    }

    private FrameEsercizio(Esercizio esercizio, boolean conBot) {
        super("Esercizio", esercizio, conBot);
        this.esercizio = esercizio;
        this.coloreCheDeveDareMatto = esercizio.getColoreCheDeveDareMatto();
    }

    /**
     * Mostra nella barra superiore le mosse rimanenti per completare l'esercizio.
     */
    @Override
    protected void aggiornaInfoExtra() {
        if (info == null) return;

        info.setForeground(Color.DARK_GRAY);
        info.setText(
                "Parte: " + nomeColore(coloreCheDeveDareMatto)
                + "  |  Mosse disponibili per dare matto: " + esercizio.getMosseRimanenti()
        );
    }

    /**
     * Controlla se l'esercizio è stato risolto, fallito o se la partita è finita.
     * @param coloreCheHaMosso colore che ha appena mosso
     */
    @Override
    protected void controllaFine(boolean coloreCheHaMosso) {
        String risultato = esercizio.checkWin();

        if (!"Nessuno".equals(risultato) && !risultato.startsWith("Patta")) {
            boolean vincitoreBianco = "Bianco".equals(risultato);
            boolean esercizioRiuscito = vincitoreBianco == coloreCheDeveDareMatto && esercizio.getMosseRimanenti() >= 0;

            finisciConMessaggio(
                    esercizioRiuscito
                    ? "Esercizio completato! Scacco matto: vince il " + risultato
                    : "Esercizio fallito: ha vinto il " + risultato
            );
            return;
        }

        if (risultato.startsWith("Patta")) {
            finisciConMessaggio("Esercizio fallito: la partita è patta");
            return;
        }

        if (coloreCheHaMosso == coloreCheDeveDareMatto && esercizio.getMosseRimanenti() == 0) {
            finisciConMessaggio("Esercizio fallito: mosse finite, non hai dato scacco matto");
            return;
        }

        if (esercizio.isSottoScacco(esercizio.isAttaccaBianco())) {
            stato.setForeground(Color.RED);
            stato.setText("Scacco al " + nomeColore(esercizio.isAttaccaBianco()) + "!");
        }
    }

}
