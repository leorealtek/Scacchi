package code.Util;

/**
 * Classe astratta comune a tutti i pezzi della scacchiera. Salva posizione, colore e riferimento alla mappa.
 */
public abstract class Pezzo {
    protected char nome;
    protected int riga;
    protected int colonna;
    protected boolean isBianco;
    protected Casella[][] mappa;

    /**
     * Crea un pezzo nella posizione indicata e ne determina il colore dal carattere del nome.
     * @param nome carattere del pezzo, maiuscolo per bianco e minuscolo per nero
     * @param riga riga iniziale
     * @param colonna colonna iniziale
     * @param mappa scacchiera di appartenenza
     */
    public Pezzo(char nome, int riga, int colonna, Casella[][] mappa) {
        this.nome = nome;
        this.riga = riga;
        this.colonna = colonna;
        this.isBianco = Character.isUpperCase(nome);
        this.mappa = mappa;
    }

    /**
     * Sposta il pezzo nella nuova casella aggiornando la scacchiera e la posizione interna.
     * @param x riga di destinazione
     * @param y colonna di destinazione
     */
    public void muovi(int x, int y) {
        mappa[x][y].inserisciPezzo(this);
        mappa[riga][colonna].rimuoviPezzo();
        aggiornaPosizione(x, y);
    }

    /**
     * Controlla se una posizione è dentro la scacchiera.
     * @param x riga da controllare
     * @param y colonna da controllare
     * @return true se le coordinate sono valide
     */
    protected boolean coordinateValide(int x, int y) {
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }

    /**
     * Aggiorna solo le coordinate interne del pezzo, senza modificare la scacchiera.
     * @param x nuova riga
     * @param y nuova colonna
     */
    public void aggiornaPosizione(int x, int y) {
        if (!coordinateValide(x, y)) {
            throw new IllegalArgumentException("Coordinate non valide: (" + x + ", " + y + ")");
        }
        this.riga = x;
        this.colonna = y;
    }

    /**
     * Calcola le mosse teoriche del pezzo secondo il suo movimento, senza controllare lo scacco al proprio re.
     * @return matrice con le caselle raggiungibili
     */
    public abstract Casella[][] mossePossibili();

    public char getNome() {
        return nome;
    }

    /**
     * Restituisce il valore usato dal bot per valutare il pezzo.
     * @return valore numerico del pezzo
     */
    public abstract double getValore();

    public int getRiga() {
        return riga;
    }

    public int getColonna() {
        return colonna;
    }

    public boolean isBianco() {
        return isBianco;
    }
}
