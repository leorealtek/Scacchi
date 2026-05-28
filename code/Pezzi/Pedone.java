package code.Pezzi;

import code.Util.*;

/**
 * Rappresenta il pedone, includendo prima mossa, avanzamento doppio e stato utile per l'en passant.
 */
public class Pedone extends Pezzo {

    private boolean primaMossa;
    private boolean secondaMossa;

    public Pedone(char nome, int riga, int colonna, Casella[][] mappa) {
        super(nome, riga, colonna, mappa);
        this.primaMossa = (isBianco && riga == 6) || (!isBianco && riga == 1);
        this.secondaMossa = (isBianco && riga == 5 || riga == 4) || (!isBianco && riga == 2 || riga == 3);
    }

    /**
     * Restituisce il valore del pezzo usato nella valutazione del bot.
     * @return valore del pedone per il bot
     */
    @Override
    public double getValore() {
        return 1.0d;
    }

    /**
     * Sposta il pedone aggiornando gli stati primaMossa e secondaMossa usati per doppio passo ed en passant.
     * @param x riga di destinazione
     * @param y colonna di destinazione
     */
    @Override
    public void muovi(int x, int y) {
        int rigaPrecedente = riga;
        super.muovi(x, y);

        secondaMossa = Math.abs(x - rigaPrecedente) == 2;
        primaMossa = false;
    }

    /**
     * Calcola le mosse teoriche del pedone: avanzamento, doppio passo iniziale e catture diagonali.
     * @return matrice con le caselle raggiungibili dal pezzo
     */
    @Override
    public Casella[][] mossePossibili() {
        Casella[][] casellePossibili = new Casella[8][8];

        int direzione = isBianco ? -1 : +1;
        int x1 = riga + direzione;
        if (coordinateValide(x1, colonna) && mappa[x1][colonna].isVuota()) {
            casellePossibili[x1][colonna] = mappa[x1][colonna];

            int x2 = riga + direzione * 2;
            if (primaMossa && coordinateValide(x2, colonna) && mappa[x2][colonna].isVuota()) {
                casellePossibili[x2][colonna] = mappa[x2][colonna];
            }
        }

        int[] colonneDiagonali = {colonna - 1, colonna + 1};
        for (int y : colonneDiagonali) {
            if (coordinateValide(x1, y)) {
                Casella casella = mappa[x1][y];
                if (!casella.isVuota() && casella.getPezzoContenuto().isBianco() != this.isBianco) {
                    casellePossibili[x1][y] = casella;
                }
            }
        }

        return casellePossibili;
    }

    public boolean isPrimaMossa() {
        return primaMossa;
    }

    public boolean isSecondaMossa() {
        return secondaMossa;
    }

    public void setPrimaMossa(boolean primaMossa) {
        this.primaMossa = primaMossa;
    }

    public void setSecondaMossa(boolean secondaMossa) {
        this.secondaMossa = secondaMossa;
    }
}
