package code.Pezzi;

import code.Util.*;

/**
 * Rappresenta il cavallo e calcola le sue mosse a L.
 */
public class Cavallo extends Pezzo {

    public Cavallo(char nome, int riga, int colonna, Casella[][] mappa) {
        super(nome, riga, colonna, mappa);
    }

    /**
     * Restituisce il valore del pezzo usato nella valutazione del bot.
     * @return valore del cavallo per il bot
     */
    @Override
    public double getValore() {
        return 3.0d;
    }

    /**
     * Calcola tutte le mosse a L possibili del cavallo.
     * @return matrice con le caselle raggiungibili dal pezzo
     */
    @Override
    public Casella[][] mossePossibili() {
        Casella[][] casellePossibili = new Casella[8][8];

        int[][] mosse = {
            {-2, -1}, {-2, +1},
            {-1, -2}, {-1, +2},
            {+1, -2}, {+1, +2},
            {+2, -1}, {+2, +1}
        };

        for (int[] mossa : mosse) {
            int x = riga + mossa[0];
            int y = colonna + mossa[1];

            if (x < 0 || x >= 8 || y < 0 || y >= 8) continue;

            Casella casella = mappa[x][y];

            if (casella.isVuota() || casella.getPezzoContenuto().isBianco() != this.isBianco) {
                casellePossibili[x][y] = casella;
            }
        }

        return casellePossibili;
    }
    
}
