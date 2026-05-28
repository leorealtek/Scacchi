package Pezzi;

import Scuola.Progettini.Scacchi.Util.*;

/**
 * Rappresenta l'alfiere e calcola le sue mosse in diagonale.
 */
public class Alfiere extends Pezzo {

    public Alfiere(char nome, int riga, int colonna, Casella[][] mappa) {
        super(nome, riga, colonna, mappa);
    }

    /**
     * Restituisce il valore del pezzo usato nella valutazione del bot.
     * @return valore dell'alfiere per il bot
     */
    @Override
    public double getValore() {
        return 3.0d;
    }

    /**
     * Calcola tutte le mosse diagonali possibili dell'alfiere fino a ostacolo o cattura.
     * @return matrice con le caselle raggiungibili dal pezzo
     */
    @Override
    public Casella[][] mossePossibili() {
        Casella[][] casellePossibili = new Casella[8][8];

        int[][] direzioni = {
            {+1, +1}, {+1, -1}, {-1, +1}, {-1, -1}
        };

        for (int[] dir : direzioni) {
            int x = riga + dir[0];
            int y = colonna + dir[1];

            while (coordinateValide(x, y)) {
                Casella casella = mappa[x][y];

                if (casella.isVuota()) {
                    casellePossibili[x][y] = casella;
                } else {
                    if (casella.getPezzoContenuto().isBianco() != this.isBianco) {
                        casellePossibili[x][y] = casella;
                    }
                    break;
                }

                x += dir[0];
                y += dir[1];
            }
        }

        return casellePossibili;
    }
    
}
