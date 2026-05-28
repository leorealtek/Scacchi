package code.Util;

import java.util.ArrayList;
import java.util.List;

import code.Partite.PartitaAstratta;
import code.Pezzi.*;

/**
 * Bot degli scacchi basato su minimax con potatura alfa-beta. Genera le mosse legali, valuta le posizioni e sceglie la posizione migliore.
 */
public class Bot {
    private int profondita;
    private PartitaAstratta partita;
    private Pedone pedoneEnPassantMigliore;

    /**
     * Crea un bot con la profondità di ricerca indicata.
     * @param profondita numero di mezze mosse da analizzare con minimax
     */
    public Bot(int profondita) {
        this.profondita = profondita;
    }

    /**
     * Valuta materialmente e posizionalmente una scacchiera: valori positivi favoriscono il bianco, negativi il nero.
     * @param scacchiera posizione da valutare
     * @return punteggio della posizione
     */
    private double valutaPosizione(Casella[][] scacchiera) {
        double valore = 0;

        for (int i = 0; i < scacchiera.length; i++) {
            for (int j = 0; j < scacchiera[i].length; j++) {
                Pezzo p = scacchiera[i][j].getPezzoContenuto();

                if (p == null) {
                    continue;
                }

                double valorePezzo = p.getValore();

                if ((i == 3 || i == 4) && (j == 3 || j == 4)) {
                    valorePezzo += 0.3;
                } else if ((i >= 2 && i <= 5) && (j >= 2 && j <= 5)) {
                    valorePezzo += 0.1;
                }

                if (p instanceof Pedone) {
                    if (p.isBianco()) {
                        valorePezzo += (6 - i) * 0.05;
                    } else {
                        valorePezzo += (i - 1) * 0.05;
                    }
                }

                if (p.isBianco()) {
                    valore += valorePezzo;
                } else {
                    valore -= valorePezzo;
                }
            }
        }

        return valore;
    }

    /**
     * Valuta una posizione terminale o di profondità massima considerando anche scacco matto e stallo.
     * @param scacchiera posizione da valutare
     * @param pedoneEnPassant pedone eventualmente catturabile en passant
     * @param toccaBianco colore che dovrebbe muovere
     * @return punteggio finale della posizione
     */
    private double valutaPosizioneFinale(Casella[][] scacchiera, Pedone pedoneEnPassant, boolean toccaBianco) {
        Casella[][] mappaPrecedente = partita.getMappa();
        Pedone pedoneEnPassantPrecedente = partita.getPedoneEnPassant();

        partita.setMappa(scacchiera);
        partita.setPedoneEnPassant(pedoneEnPassant);

        try {
            ArrayList<Pedone> enPassantMosse = new ArrayList<>();
            boolean sottoScacco = partita.isSottoScacco(toccaBianco);
            boolean haMosse = !creaTutteMosseDisponibili(
                    scacchiera,
                    pedoneEnPassant,
                    toccaBianco,
                    enPassantMosse
            ).isEmpty();

            if (sottoScacco && !haMosse) {
                return toccaBianco ? -100000 : 100000;
            }

            if (!sottoScacco && !haMosse) {
                return 0;
            }

            return valutaPosizione(scacchiera);
        } finally {
            partita.setMappa(mappaPrecedente);
            partita.setPedoneEnPassant(pedoneEnPassantPrecedente);
        }
    }

    /**
     * Applica minimax con potatura alfa-beta per stimare il valore migliore raggiungibile dalla posizione.
     * @param scacchiera posizione corrente
     * @param pedoneEnPassant stato en passant corrente
     * @param profondita profondità ancora da esplorare
     * @param alpha miglior valore già garantito al massimizzatore
     * @param beta miglior valore già garantito al minimizzatore
     * @param toccaBianco true se tocca al bianco
     * @return valore minimax della posizione
     */
    private double minimax(
            Casella[][] scacchiera,
            Pedone pedoneEnPassant,
            int profondita,
            double alpha,
            double beta,
            boolean toccaBianco
    ) {
        ArrayList<Pedone> enPassantMosse = new ArrayList<>();
        List<Casella[][]> mosse = creaTutteMosseDisponibili(
                scacchiera,
                pedoneEnPassant,
                toccaBianco,
                enPassantMosse
        );

        if (profondita == 0 || mosse.isEmpty()) {
            return valutaPosizioneFinale(scacchiera, pedoneEnPassant, toccaBianco);
        }

        if (toccaBianco) {
            double maxPoint = Double.NEGATIVE_INFINITY;

            for (int i = 0; i < mosse.size(); i++) {
                Casella[][] scacchieraAttuale = mosse.get(i);
                Pedone enPassantAttuale = enPassantMosse.get(i);

                double val = minimax(
                        scacchieraAttuale,
                        enPassantAttuale,
                        profondita - 1,
                        alpha,
                        beta,
                        false
                );

                maxPoint = Math.max(maxPoint, val);
                alpha = Math.max(alpha, val);

                if (beta <= alpha) {
                    break;
                }
            }

            return maxPoint;
        }

        double minPoint = Double.POSITIVE_INFINITY;

        for (int i = 0; i < mosse.size(); i++) {
            Casella[][] scacchieraAttuale = mosse.get(i);
            Pedone enPassantAttuale = enPassantMosse.get(i);

            double val = minimax(
                    scacchieraAttuale,
                    enPassantAttuale,
                    profondita - 1,
                    alpha,
                    beta,
                    true
            );

            minPoint = Math.min(minPoint, val);
            beta = Math.min(beta, val);

            if (beta <= alpha) {
                break;
            }
        }

        return minPoint;
    }

    /**
     * Controlla se nella posizione simulata il colore indicato è in scacco matto.
     * @param scacchiera posizione simulata
     * @param pedoneEnPassant stato en passant simulato
     * @param coloreDaControllare colore di cui controllare il matto
     * @return true se è scacco matto
     */
    private boolean isScaccoMatto(Casella[][] scacchiera, Pedone pedoneEnPassant, boolean coloreDaControllare) {
        Casella[][] mappaPrecedente = partita.getMappa();
        Pedone pedoneEnPassantPrecedente = partita.getPedoneEnPassant();

        partita.setMappa(scacchiera);
        partita.setPedoneEnPassant(pedoneEnPassant);

        try {
            ArrayList<Pedone> enPassantMosse = new ArrayList<>();
            boolean sottoScacco = partita.isSottoScacco(coloreDaControllare);
            boolean haMosse = !creaTutteMosseDisponibili(
                    scacchiera,
                    pedoneEnPassant,
                    coloreDaControllare,
                    enPassantMosse
            ).isEmpty();

            return sottoScacco && !haMosse;
        } finally {
            partita.setMappa(mappaPrecedente);
            partita.setPedoneEnPassant(pedoneEnPassantPrecedente);
        }
    }

    /**
     * Cerca e restituisce la posizione migliore per il colore che deve muovere, dando priorità al matto immediato.
     * @param toccaBianco true se deve muovere il bianco, false se deve muovere il nero
     * @return mappa risultante dalla mossa scelta dal bot
     */
    public Casella[][] trovaPosizioneMigliore(boolean toccaBianco) {
        if (partita == null) {
            throw new IllegalStateException("Devi prima chiamare setPartita(partita)");
        }

        pedoneEnPassantMigliore = null;

        ArrayList<Pedone> enPassantMosse = new ArrayList<>();
        List<Casella[][]> mosse = creaTutteMosseDisponibili(
                partita.getMappa(),
                partita.getPedoneEnPassant(),
                toccaBianco,
                enPassantMosse
        );

        if (mosse.isEmpty()) {
            return partita.getMappa();
        }

        for (int i = 0; i < mosse.size(); i++) {
            Casella[][] scacchiera = mosse.get(i);
            Pedone enPassant = enPassantMosse.get(i);

            if (isScaccoMatto(scacchiera, enPassant, !toccaBianco)) {
                pedoneEnPassantMigliore = enPassant;
                return scacchiera;
            }
        }

        Casella[][] migliore = null;

        if (toccaBianco) {
            double valoreMigliore = Double.NEGATIVE_INFINITY;

            for (int i = 0; i < mosse.size(); i++) {
                Casella[][] scacchiera = mosse.get(i);
                Pedone enPassant = enPassantMosse.get(i);

                double valore = minimax(
                        scacchiera,
                        enPassant,
                        profondita - 1,
                        Double.NEGATIVE_INFINITY,
                        Double.POSITIVE_INFINITY,
                        false
                );

                if (valore > valoreMigliore) {
                    valoreMigliore = valore;
                    migliore = scacchiera;
                    pedoneEnPassantMigliore = enPassant;
                }
            }
        } else {
            double valoreMigliore = Double.POSITIVE_INFINITY;

            for (int i = 0; i < mosse.size(); i++) {
                Casella[][] scacchiera = mosse.get(i);
                Pedone enPassant = enPassantMosse.get(i);

                double valore = minimax(
                        scacchiera,
                        enPassant,
                        profondita - 1,
                        Double.NEGATIVE_INFINITY,
                        Double.POSITIVE_INFINITY,
                        true
                );

                if (valore < valoreMigliore) {
                    valoreMigliore = valore;
                    migliore = scacchiera;
                    pedoneEnPassantMigliore = enPassant;
                }
            }
        }

        return migliore;
    }

    /**
     * Crea una copia indipendente di un pezzo mantenendo tipo, colore, posizione e stati speciali.
     * @param p pezzo da copiare
     * @param nuovaMappa mappa a cui il pezzo copiato farà riferimento
     * @return copia del pezzo, oppure null
     */
    private Pezzo copiaPezzo(Pezzo p, Casella[][] nuovaMappa) {
        char nome = p.getNome();
        int riga = p.getRiga();
        int colonna = p.getColonna();

        if (p instanceof Pedone vecchioPedone) {
            Pedone nuovoPedone = new Pedone(nome, riga, colonna, nuovaMappa);
            nuovoPedone.setPrimaMossa(vecchioPedone.isPrimaMossa());
            nuovoPedone.setSecondaMossa(vecchioPedone.isSecondaMossa());
            return nuovoPedone;
        }

        if (p instanceof Torre vecchiaTorre) {
            Torre nuovaTorre = new Torre(nome, riga, colonna, nuovaMappa);
            nuovaTorre.setHaMosso(vecchiaTorre.haMosso());
            return nuovaTorre;
        }

        if (p instanceof Re vecchioRe) {
            Re nuovoRe = new Re(nome, riga, colonna, nuovaMappa);
            nuovoRe.setHaMosso(vecchioRe.haMosso());
            return nuovoRe;
        }

        if (p instanceof Cavallo) {
            return new Cavallo(nome, riga, colonna, nuovaMappa);
        }

        if (p instanceof Alfiere) {
            return new Alfiere(nome, riga, colonna, nuovaMappa);
        }

        if (p instanceof Regina) {
            return new Regina(nome, riga, colonna, nuovaMappa);
        }

        throw new IllegalArgumentException("Pezzo non valido: " + nome);
    }

    /**
     * Crea una copia profonda della scacchiera per simulare mosse senza modificare la partita reale.
     * @param originale mappa da copiare
     * @return nuova mappa indipendente
     */
    private Casella[][] copiaMappa(Casella[][] originale) {
        Casella[][] copia = new Casella[8][8];

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Pezzo p = originale[i][j].getPezzoContenuto();

                if (p == null) {
                    copia[i][j] = new Casella();
                } else {
                    copia[i][j] = new Casella(copiaPezzo(p, copia));
                }
            }
        }

        return copia;
    }

    /**
     * Promuove automaticamente a regina un pedone arrivato in fondo durante le simulazioni del bot.
     * @param scacchiera mappa simulata
     * @param riga riga del pedone
     * @param colonna colonna del pedone
     */
    private void promuoviPedoneAutomaticamente(Casella[][] scacchiera, int riga, int colonna) {
        Pezzo pezzo = scacchiera[riga][colonna].getPezzoContenuto();

        if (!(pezzo instanceof Pedone)) {
            return;
        }

        if (riga != 0 && riga != 7) {
            return;
        }

        char nomeRegina = pezzo.isBianco() ? 'D' : 'd';
        scacchiera[riga][colonna].inserisciPezzo(new Regina(nomeRegina, riga, colonna, scacchiera));
    }

    /**
     * Verifica se un arrocco è legale dentro una posizione simulata dal bot.
     * @param scacchiera mappa simulata
     * @param bianco true per il bianco, false per il nero
     * @param latoLungo true per arrocco lungo, false per arrocco corto
     * @return true se l'arrocco simulato è legale
     */
    private boolean arroccoLegaleBot(Casella[][] scacchiera, boolean bianco, boolean latoLungo) {
        int rigaRe = bianco ? 7 : 0;

        Pezzo pezzoRe = scacchiera[rigaRe][4].getPezzoContenuto();

        if (!(pezzoRe instanceof Re re)) {
            return false;
        }

        if (pezzoRe.isBianco() != bianco) {
            return false;
        }

        if (re.haMosso()) {
            return false;
        }

        if (partita.isSottoScacco(bianco)) {
            return false;
        }

        int colonnaTorre = latoLungo ? 0 : 7;
        int primaCasellaLibera = latoLungo ? 1 : 5;
        int ultimaCasellaLibera = latoLungo ? 3 : 6;
        int colonnaReDestinazione = latoLungo ? 2 : 6;
        int passo = latoLungo ? -1 : 1;

        Pezzo pezzoTorre = scacchiera[rigaRe][colonnaTorre].getPezzoContenuto();

        if (!(pezzoTorre instanceof Torre torre)) {
            return false;
        }

        if (pezzoTorre.isBianco() != bianco) {
            return false;
        }

        if (torre.haMosso()) {
            return false;
        }

        for (int col = primaCasellaLibera; col <= ultimaCasellaLibera; col++) {
            if (!scacchiera[rigaRe][col].isVuota()) {
                return false;
            }
        }

        for (int col = 4 + passo; col != colonnaReDestinazione + passo; col += passo) {
            if (partita.casellaAttaccata(rigaRe, col, !bianco)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Aggiunge alle mosse simulate anche l'arrocco, quando è legale nella posizione analizzata.
     */
    private void aggiungiArroccoSePossibile(
            Casella[][] scacchiera,
            boolean toccaBianco,
            boolean latoLungo,
            List<Casella[][]> mosseDisponibili,
            ArrayList<Pedone> enPassantMosse
    ) {
        if (!arroccoLegaleBot(scacchiera, toccaBianco, latoLungo)) {
            return;
        }

        Casella[][] nuovaScacchiera = copiaMappa(scacchiera);

        int rigaRe = toccaBianco ? 7 : 0;
        int colonnaTorre = latoLungo ? 0 : 7;
        int colonnaReDestinazione = latoLungo ? 2 : 6;
        int colonnaTorreDestinazione = latoLungo ? 3 : 5;

        Pezzo pezzoRe = nuovaScacchiera[rigaRe][4].getPezzoContenuto();
        Pezzo pezzoTorre = nuovaScacchiera[rigaRe][colonnaTorre].getPezzoContenuto();

        if (!(pezzoRe instanceof Re re)) {
            return;
        }

        if (!(pezzoTorre instanceof Torre torre)) {
            return;
        }

        nuovaScacchiera[rigaRe][4].rimuoviPezzo();
        nuovaScacchiera[rigaRe][colonnaTorre].rimuoviPezzo();

        re.aggiornaPosizione(rigaRe, colonnaReDestinazione);
        re.setHaMosso(true);
        nuovaScacchiera[rigaRe][colonnaReDestinazione].inserisciPezzo(re);

        torre.aggiornaPosizione(rigaRe, colonnaTorreDestinazione);
        torre.setHaMosso(true);
        nuovaScacchiera[rigaRe][colonnaTorreDestinazione].inserisciPezzo(torre);

        mosseDisponibili.add(nuovaScacchiera);

        enPassantMosse.add(null);
    }

    /**
     * Genera tutte le mosse legali del colore indicato in una posizione simulata, includendo en passant e arrocco.
     * @param scacchiera posizione da cui generare le mosse
     * @param pedoneEnPassant pedone eventualmente catturabile en passant
     * @param toccaBianco colore che deve muovere
     * @param enPassantMosse lista parallela in cui salvare lo stato en passant di ogni mossa
     * @return lista delle mappe raggiungibili con mosse legali
     */
    private List<Casella[][]> creaTutteMosseDisponibili(
            Casella[][] scacchiera,
            Pedone pedoneEnPassant,
            boolean toccaBianco,
            ArrayList<Pedone> enPassantMosse
    ) {
        List<Casella[][]> mosseDisponibili = new ArrayList<>();

        Casella[][] mappaPrecedente = partita.getMappa();
        Pedone pedoneEnPassantPrecedente = partita.getPedoneEnPassant();

        partita.setMappa(scacchiera);
        partita.setPedoneEnPassant(pedoneEnPassant);

        try {
            for (int rigaPartenza = 0; rigaPartenza < 8; rigaPartenza++) {
                for (int colonnaPartenza = 0; colonnaPartenza < 8; colonnaPartenza++) {

                    Pezzo p = scacchiera[rigaPartenza][colonnaPartenza].getPezzoContenuto();

                    if (p == null || p.isBianco() != toccaBianco) {
                        continue;
                    }

                    Casella[][] mosse = p.mossePossibili();

                    for (int rigaArrivo = 0; rigaArrivo < 8; rigaArrivo++) {
                        for (int colonnaArrivo = 0; colonnaArrivo < 8; colonnaArrivo++) {

                            boolean enPassant = partita.isEnPassantValido(
                                    p,
                                    rigaPartenza,
                                    colonnaPartenza,
                                    rigaArrivo,
                                    colonnaArrivo
                            );

                            if (mosse[rigaArrivo][colonnaArrivo] == null && !enPassant) {
                                continue;
                            }

                            Pezzo pezzoDestinazione = scacchiera[rigaArrivo][colonnaArrivo].getPezzoContenuto();

                            if (pezzoDestinazione instanceof Re) {
                                continue;
                            }

                            if (partita.lasciaReSottoScacco(
                                    p,
                                    rigaPartenza,
                                    colonnaPartenza,
                                    rigaArrivo,
                                    colonnaArrivo
                            )) {
                                continue;
                            }

                            Casella[][] nuovaScacchiera = copiaMappa(scacchiera);
                            Pezzo pezzoCopiato = nuovaScacchiera[rigaPartenza][colonnaPartenza].getPezzoContenuto();

                            Pedone nuovoPedoneEnPassant = null;
                            int distanzaRighe = Math.abs(rigaArrivo - rigaPartenza);

                            if (enPassant) {
                                nuovaScacchiera[rigaPartenza][colonnaArrivo].rimuoviPezzo();
                            }

                            pezzoCopiato.muovi(rigaArrivo, colonnaArrivo);

                            if (pezzoCopiato instanceof Pedone pedone && distanzaRighe == 2) {
                                nuovoPedoneEnPassant = pedone;
                            }

                            promuoviPedoneAutomaticamente(nuovaScacchiera, rigaArrivo, colonnaArrivo);

                            mosseDisponibili.add(nuovaScacchiera);
                            enPassantMosse.add(nuovoPedoneEnPassant);
                        }
                    }
                }
            }

            aggiungiArroccoSePossibile(
                    scacchiera,
                    toccaBianco,
                    false,
                    mosseDisponibili,
                    enPassantMosse
            );

            aggiungiArroccoSePossibile(
                    scacchiera,
                    toccaBianco,
                    true,
                    mosseDisponibili,
                    enPassantMosse
            );

        } finally {
            partita.setMappa(mappaPrecedente);
            partita.setPedoneEnPassant(pedoneEnPassantPrecedente);
        }

        return mosseDisponibili;
    }

    public Pedone getPedoneEnPassantMigliore() {
        return pedoneEnPassantMigliore;
    }

    /**
     * Collega il bot alla partita su cui deve lavorare.
     * @param partita partita corrente
     */
    public void setPartita(PartitaAstratta partita) {
        this.partita = partita;
    }

    public int getProfondita() {
        return profondita;
    }
}
