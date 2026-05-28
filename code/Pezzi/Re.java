package code.Pezzi;

import code.Util.*;

/**
 * Rappresenta il re e conserva lo stato necessario per stabilire se può arroccare.
 */
public class Re extends Pezzo {

    private boolean haMosso;

    public Re(char nome, int riga, int colonna, Casella[][] mappa) {
        super(nome, riga, colonna, mappa);
        this.haMosso = !((isBianco && riga == 7 && colonna == 4)
                      || (!isBianco && riga == 0 && colonna == 4));
    }

    /**
     * Restituisce il valore del pezzo usato nella valutazione del bot.
     * @return valore molto alto del re per il bot
     */
    @Override
    public double getValore() {
        return 1000.0d;
    }

    /**
     * Sposta il re e segna che ha già mosso, impedendo futuri arrocchi.
     * @param x riga di destinazione
     * @param y colonna di destinazione
     */
    @Override
    public void muovi(int x, int y) {
        super.muovi(x, y);
        haMosso = true;
    }

    /**
     * Calcola le caselle adiacenti raggiungibili dal re, senza includere l'arrocco.
     * @return matrice con le caselle raggiungibili dal pezzo
     */
    @Override
    public Casella[][] mossePossibili() {
        Casella[][] casellePossibili = new Casella[8][8];

        int[][] direzioni = {
            {0, 1}, {0, -1}, {1, 0}, {-1, 0},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };

        for (int[] dir : direzioni) {
            int x = riga + dir[0];
            int y = colonna + dir[1];

            if (!coordinateValide(x, y)) continue;

            Casella casella = mappa[x][y];

            if (casella.isVuota() || casella.getPezzoContenuto().isBianco() != this.isBianco) {
                casellePossibili[x][y] = casella;
            }
        }

        return casellePossibili;
    }

    public boolean haMosso() {
        return haMosso;
    }

    public void setHaMosso(boolean haMosso) {
        this.haMosso = haMosso;
    }
}
