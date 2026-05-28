package code.Util;

/**
 * Rappresenta una singola casella della scacchiera e il pezzo eventualmente contenuto.
 */
public class Casella {

    private Pezzo pezzoContenuto;

    public Casella(Pezzo pezzoContenuto) {
        this.pezzoContenuto = pezzoContenuto;
    }

    public Casella() {
        pezzoContenuto = null;
    }

    public Pezzo getPezzoContenuto() {
        return pezzoContenuto;
    }

    /**
     * Inserisce o sostituisce il pezzo contenuto nella casella.
     * @param pezzoContenuto pezzo da inserire
     */
    public void inserisciPezzo(Pezzo pezzoContenuto) {
        this.pezzoContenuto = pezzoContenuto;
    }

    /**
     * Svuota la casella rimuovendo il pezzo contenuto.
     */
    public void rimuoviPezzo() {
        pezzoContenuto = null;
    }

    /**
     * Indica se la casella non contiene nessun pezzo.
     * @return true se la casella è vuota
     */
    public boolean isVuota() {
        return pezzoContenuto == null;
    }
}
