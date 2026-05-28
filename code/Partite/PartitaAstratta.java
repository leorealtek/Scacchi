package code.Partite;

import code.Exception.*;
import code.Pezzi.*;
import code.Util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.swing.JOptionPane;

/**
 * Classe base delle modalità di gioco. Gestisce la scacchiera, il turno, le mosse speciali e i controlli comuni come scacco, scacco matto e stallo.
 */
public abstract class PartitaAstratta {

    protected Casella[][] mappa;
    protected boolean attaccaBianco;
    protected Pedone pedoneEnPassant;
    protected List<Casella[][]> tutteMossePossibili = new ArrayList<>();
    protected int mosse;
    protected Bot bot;

    /**
     * Crea una partita vuota 8x8 e imposta il turno iniziale al bianco.
     */
    public PartitaAstratta() {
        mappa = new Casella[8][8];
        inizializzaCaselleVuote();
        attaccaBianco = true;
        pedoneEnPassant = null;
    }

    /**
     * Riempie la mappa con caselle vuote, evitando valori null nella scacchiera.
     */
    protected void inizializzaCaselleVuote() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                mappa[i][j] = new Casella();
            }
        }
    }

    /**
     * Controlla che la mappa passata sia una matrice 8x8 valida.
     * @param mappa matrice da controllare
     * @throws IllegalArgumentException se la matrice non è 8x8
     */
    protected void validaMappa(Casella[][] mappa) {
        if (mappa == null || mappa.length != 8) {
            throw new IllegalArgumentException("La mappa deve essere una matrice 8x8.");
        }

        for (int i = 0; i < 8; i++) {
            if (mappa[i] == null || mappa[i].length != 8) {
                throw new IllegalArgumentException("La mappa deve essere una matrice 8x8.");
            }
        }
    }

    /**
     * Gestisce la promozione di un pedone arrivato all'ultima traversa mostrando la scelta grafica del nuovo pezzo.
     * @param p pedone da promuovere
     * @return nuovo pezzo scelto, oppure null se il pedone non è da promuovere
     */
    public Pezzo promuoviPedone(Pedone p) {
        if (p.getRiga() == 0 || p.getRiga() == 7) {
            String[] pezziDisponibili = {"Torre", "Cavallo", "Alfiere", "Donna"};
            Pezzo[] pezzi = {
                new Torre((p.isBianco()) ? 'T' : 't', p.getRiga(), p.getColonna(), mappa),
                new Cavallo((p.isBianco()) ? 'C' : 'c', p.getRiga(), p.getColonna(), mappa),
                new Alfiere((p.isBianco()) ? 'A' : 'a', p.getRiga(), p.getColonna(), mappa),
                new Regina((p.isBianco()) ? 'D' : 'd', p.getRiga(), p.getColonna(), mappa),
            };

            int scelta = JOptionPane.showOptionDialog(
                null,
                "Scegli che in che pezzo vuoi promuovere il pedone",
                "Menù di scelta",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                pezziDisponibili,
                pezziDisponibili[3]
            );

            if (scelta < 0) {
                return pezzi[3];
            }

            return pezzi[scelta];
        }
        return null;
    }

    /**
     * Esegue una mossa normale controllando turno, legalità della mossa, en passant e sicurezza del re.
     * @param rigaPartenza riga di partenza
     * @param colonnaPartenza colonna di partenza
     * @param rigaArrivo riga di arrivo
     * @param colonnaArrivo colonna di arrivo
     * @throws MossaNonValidaException se la mossa non è consentita
     */
    public void muoviPezzo(int rigaPartenza, int colonnaPartenza, int rigaArrivo, int colonnaArrivo) {
        validaCoordinate(rigaPartenza, colonnaPartenza);
        validaCoordinate(rigaArrivo, colonnaArrivo);

        Pezzo pezzo = mappa[rigaPartenza][colonnaPartenza].getPezzoContenuto();
        if (pezzo == null) {
            throw new MossaNonValidaException("Nessun pezzo selezionato.");
        }

        if (pezzo.isBianco() != attaccaBianco) {
            throw new MossaNonValidaException("Non è il turno di questo colore.");
        }

        Casella[][] possibili = pezzo.mossePossibili();
        boolean enPassant = isEnPassantValido(pezzo, rigaPartenza, colonnaPartenza, rigaArrivo, colonnaArrivo);

        if (possibili[rigaArrivo][colonnaArrivo] == null && !enPassant) {
            throw new MossaNonValidaException("Mossa non consentita per questo pezzo.");
        }

        if (lasciaReSottoScacco(pezzo, rigaPartenza, colonnaPartenza, rigaArrivo, colonnaArrivo)) {
            throw new MossaNonValidaException("Mossa non consentita: il Re resterebbe o finirebbe sotto scacco.");
        }

        boolean coloreCheHaMosso = attaccaBianco;
        int distanzaRighe = Math.abs(rigaArrivo - rigaPartenza);

        if (enPassant) {
            mappa[rigaPartenza][colonnaArrivo].rimuoviPezzo();
        }

        pezzo.muovi(rigaArrivo, colonnaArrivo);

        pedoneEnPassant = null;
        if (pezzo instanceof Pedone pedone && distanzaRighe == 2) {
            pedoneEnPassant = pedone;
        }

        dopoMossa(coloreCheHaMosso);
        attaccaBianco = !attaccaBianco;
    }

    /**
     * Fa calcolare ed eseguire al bot la mossa migliore per il colore di turno.
     * @param turnoBianco true se deve muovere il bot bianco, false se deve muovere il bot nero
     * @throws MossaNonValidaException se non è il turno del colore indicato
     */
    public void muoviPezzoConBot(boolean turnoBianco) {
        if (turnoBianco != attaccaBianco) {
            throw new MossaNonValidaException("Non è il turno del bot.");
        }

        Casella[][] migliore = bot.trovaPosizioneMigliore(turnoBianco);

        if (migliore == null || migliore == mappa) {
            return;
        }

        mappa = migliore;
        pedoneEnPassant = bot.getPedoneEnPassantMigliore();

        dopoMossa(turnoBianco);
        attaccaBianco = !attaccaBianco;
    }

    /**
     * Verifica se lo spostamento indicato corrisponde a una cattura en passant valida.
     * @param pezzo pezzo che tenta la mossa
     * @param rigaPartenza riga di partenza
     * @param colonnaPartenza colonna di partenza
     * @param rigaArrivo riga di arrivo
     * @param colonnaArrivo colonna di arrivo
     * @return true se l'en passant è valido
     */
    public boolean isEnPassantValido(Pezzo pezzo, int rigaPartenza, int colonnaPartenza, int rigaArrivo, int colonnaArrivo) {
        if (!(pezzo instanceof Pedone)) return false;
        if (pedoneEnPassant == null) return false;
        if (pedoneEnPassant.isBianco() == pezzo.isBianco()) return false;
        if (!mappa[rigaArrivo][colonnaArrivo].isVuota()) return false;

        int direzione = pezzo.isBianco() ? -1 : 1;
        return rigaArrivo == rigaPartenza + direzione
                && Math.abs(colonnaArrivo - colonnaPartenza) == 1
                && pedoneEnPassant.getRiga() == rigaPartenza
                && pedoneEnPassant.getColonna() == colonnaArrivo;
    }

    /**
     * Esegue l'arrocco del colore di turno spostando re e torre nelle rispettive caselle finali.
     * @param latoLungo true per arrocco lungo, false per arrocco corto
     * @throws MossaNonValidaException se l'arrocco non è permesso
     */
    public void arrocca(boolean latoLungo) {
        int rigaRe = attaccaBianco ? 7 : 0;
        Pezzo pezzo = mappa[rigaRe][4].getPezzoContenuto();

        if (!(pezzo instanceof Re) || pezzo.isBianco() != attaccaBianco) {
            throw new MossaNonValidaException("Non c'è un Re del colore di turno nella posizione iniziale.");
        }

        if (!puoArroccare(latoLungo)) {
            throw new MossaNonValidaException("Arrocco non valido.");
        }

        int colonnaTorre = latoLungo ? 0 : 7;
        int colonnaReDestinazione = latoLungo ? 2 : 6;
        int colonnaTorreDestinazione = latoLungo ? 3 : 5;

        Re re = (Re) pezzo;
        Torre torre = (Torre) mappa[rigaRe][colonnaTorre].getPezzoContenuto();
        boolean coloreCheHaMosso = attaccaBianco;

        mappa[rigaRe][4].rimuoviPezzo();
        mappa[rigaRe][colonnaTorre].rimuoviPezzo();

        re.aggiornaPosizione(rigaRe, colonnaReDestinazione);
        re.setHaMosso(true);
        mappa[rigaRe][colonnaReDestinazione].inserisciPezzo(re);

        torre.aggiornaPosizione(rigaRe, colonnaTorreDestinazione);
        torre.setHaMosso(true);
        mappa[rigaRe][colonnaTorreDestinazione].inserisciPezzo(torre);

        dopoMossa(coloreCheHaMosso);
        attaccaBianco = !attaccaBianco;
    }

    /**
     * Metodo chiamato dopo ogni mossa per permettere alle sottoclassi di aggiornare contatori o salvataggi.
     * @param coloreCheHaMosso colore che ha appena mosso
     */
    protected abstract void dopoMossa(boolean coloreCheHaMosso);

    /**
     * Simula temporaneamente una mossa e controlla se il re del pezzo mosso rimarrebbe sotto scacco.
     * @param pezzo pezzo da muovere nella simulazione
     * @param rigaPartenza riga di partenza
     * @param colonnaPartenza colonna di partenza
     * @param rigaArrivo riga di arrivo
     * @param colonnaArrivo colonna di arrivo
     * @return true se la mossa lascia o mette il proprio re sotto scacco
     */
    public boolean lasciaReSottoScacco(Pezzo pezzo, int rigaPartenza, int colonnaPartenza, int rigaArrivo, int colonnaArrivo) {
        Pezzo catturato = mappa[rigaArrivo][colonnaArrivo].getPezzoContenuto();
        boolean enPassant = isEnPassantValido(pezzo, rigaPartenza, colonnaPartenza, rigaArrivo, colonnaArrivo);
        Pezzo catturatoEnPassant = null;

        if (enPassant) {
            catturatoEnPassant = mappa[rigaPartenza][colonnaArrivo].getPezzoContenuto();
            mappa[rigaPartenza][colonnaArrivo].rimuoviPezzo();
        }

        mappa[rigaArrivo][colonnaArrivo].inserisciPezzo(pezzo);
        mappa[rigaPartenza][colonnaPartenza].rimuoviPezzo();
        pezzo.aggiornaPosizione(rigaArrivo, colonnaArrivo);

        boolean sottoScacco = isSottoScacco(pezzo.isBianco());

        mappa[rigaPartenza][colonnaPartenza].inserisciPezzo(pezzo);
        mappa[rigaArrivo][colonnaArrivo].inserisciPezzo(catturato);
        if (enPassant) {
            mappa[rigaPartenza][colonnaArrivo].inserisciPezzo(catturatoEnPassant);
        }
        pezzo.aggiornaPosizione(rigaPartenza, colonnaPartenza);

        return sottoScacco;
    }

    /**
     * Controlla se il re del colore indicato è attualmente sotto scacco.
     * @param reBianco true per controllare il re bianco, false per il re nero
     * @return true se il re è sotto scacco
     */
    public boolean isSottoScacco(boolean reBianco) {
        int[] posizioneRe = trovaRe(reBianco);
        if (posizioneRe == null) {
            return true;
        }
        return casellaAttaccata(posizioneRe[0], posizioneRe[1], !reBianco);
    }

    /**
     * Controlla se una casella è attaccata da almeno un pezzo del colore indicato.
     * @param rigaTarget riga della casella da controllare
     * @param colonnaTarget colonna della casella da controllare
     * @param daBianco true per cercare attacchi dei bianchi, false dei neri
     * @return true se la casella è attaccata
     */
    public boolean casellaAttaccata(int rigaTarget, int colonnaTarget, boolean daBianco) {
        validaCoordinate(rigaTarget, colonnaTarget);

        int direzionePedone = daBianco ? -1 : 1;
        int rigaPedone = rigaTarget - direzionePedone;
        int[] colonnePedone = {colonnaTarget - 1, colonnaTarget + 1};
        for (int col : colonnePedone) {
            if (coordinateValide(rigaPedone, col)) {
                Pezzo p = mappa[rigaPedone][col].getPezzoContenuto();
                if (p instanceof Pedone && p.isBianco() == daBianco) return true;
            }
        }

        int[][] mosseCavallo = {
            {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
            {1, -2}, {1, 2}, {2, -1}, {2, 1}
        };
        for (int[] mossa : mosseCavallo) {
            int x = rigaTarget + mossa[0];
            int y = colonnaTarget + mossa[1];
            if (coordinateValide(x, y)) {
                Pezzo p = mappa[x][y].getPezzoContenuto();
                if (p instanceof Cavallo && p.isBianco() == daBianco) return true;
            }
        }

        int[][] direzioniRe = {
            {0, 1}, {0, -1}, {1, 0}, {-1, 0},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };
        for (int[] dir : direzioniRe) {
            int x = rigaTarget + dir[0];
            int y = colonnaTarget + dir[1];
            if (coordinateValide(x, y)) {
                Pezzo p = mappa[x][y].getPezzoContenuto();
                if (p instanceof Re && p.isBianco() == daBianco) return true;
            }
        }

        int[][] direzioni = {
            {0, 1}, {0, -1}, {1, 0}, {-1, 0},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };
        for (int[] dir : direzioni) {
            int x = rigaTarget + dir[0];
            int y = colonnaTarget + dir[1];
            while (coordinateValide(x, y)) {
                Pezzo p = mappa[x][y].getPezzoContenuto();
                if (p != null) {
                    if (p.isBianco() == daBianco) {
                        boolean lineaDritta = dir[0] == 0 || dir[1] == 0;
                        if ((lineaDritta && (p instanceof Torre || p instanceof Regina))
                            || (!lineaDritta && (p instanceof Alfiere || p instanceof Regina))) {
                            return true;
                        }
                    }
                    break;
                }
                x += dir[0];
                y += dir[1];
            }
        }

        return false;
    }

    /**
     * Cerca nella scacchiera la posizione del re del colore indicato.
     * @param bianco true per cercare il re bianco, false per il re nero
     * @return array con riga e colonna del re, oppure null se non trovato
     */
    protected int[] trovaRe(boolean bianco) {
        char nomeRe = bianco ? 'R' : 'r';
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Pezzo pezzo = mappa[i][j].getPezzoContenuto();
                if (pezzo != null && pezzo.getNome() == nomeRe) {
                    return new int[] {i, j};
                }
            }
        }
        return null;
    }

    /**
     * Determina se la posizione corrente è scacco matto, stallo oppure partita ancora in corso.
     * @return nome del vincitore, una stringa di patta oppure "Nessuno"
     */
    public String checkWin() {
        boolean coloreDiTurno = attaccaBianco;
        boolean sottoScacco = isSottoScacco(coloreDiTurno);
        boolean haMosseLegali = esisteAlmenoUnaMossaLegale(coloreDiTurno);

        if (sottoScacco && !haMosseLegali) {
            return coloreDiTurno ? "Nero" : "Bianco";
        }

        if (!sottoScacco && !haMosseLegali) {
            return "Patta " + (coloreDiTurno ? "BIANCO" : "NERO");
        }

        return "Nessuno";
    }

    /**
     * Verifica se il colore indicato ha almeno una mossa legale disponibile, includendo en passant e arrocco.
     * @param bianco true per il bianco, false per il nero
     * @return true se esiste almeno una mossa legale
     */
    protected boolean esisteAlmenoUnaMossaLegale(boolean bianco) {
        for (int rigaPartenza = 0; rigaPartenza < 8; rigaPartenza++) {
            for (int colonnaPartenza = 0; colonnaPartenza < 8; colonnaPartenza++) {

                Pezzo pezzo = mappa[rigaPartenza][colonnaPartenza].getPezzoContenuto();

                if (pezzo == null || pezzo.isBianco() != bianco) {
                    continue;
                }

                Casella[][] mosse = pezzo.mossePossibili();

                for (int rigaArrivo = 0; rigaArrivo < 8; rigaArrivo++) {
                    for (int colonnaArrivo = 0; colonnaArrivo < 8; colonnaArrivo++) {

                        if (mosse[rigaArrivo][colonnaArrivo] == null) {
                            continue;
                        }

                        Pezzo pezzoDestinazione = mappa[rigaArrivo][colonnaArrivo].getPezzoContenuto();
                        if (pezzoDestinazione instanceof Re) {
                            continue;
                        }

                        if (!lasciaReSottoScacco(
                                pezzo,
                                rigaPartenza,
                                colonnaPartenza,
                                rigaArrivo,
                                colonnaArrivo
                        )) {
                            return true;
                        }
                    }
                }
            }
        }

        if (esisteEnPassantLegale(bianco)) {
            return true;
        }

        return esisteArroccoLegale(bianco);
    }

    /**
     * Controlla se il colore indicato può fare almeno una cattura en passant legale.
     * @param bianco true per il bianco, false per il nero
     * @return true se esiste un en passant legale
     */
    private boolean esisteEnPassantLegale(boolean bianco) {
        if (pedoneEnPassant == null || pedoneEnPassant.isBianco() == bianco) return false;

        int rigaPedoneAvversario = pedoneEnPassant.getRiga();
        int colonnaPedoneAvversario = pedoneEnPassant.getColonna();
        int direzione = bianco ? -1 : 1;
        int rigaArrivo = rigaPedoneAvversario + direzione;

        for (int colonnaPartenza : new int[] {colonnaPedoneAvversario - 1, colonnaPedoneAvversario + 1}) {
            if (!coordinateValide(rigaPedoneAvversario, colonnaPartenza)) continue;

            Pezzo pezzo = mappa[rigaPedoneAvversario][colonnaPartenza].getPezzoContenuto();
            if (pezzo instanceof Pedone && pezzo.isBianco() == bianco
                    && isEnPassantValido(pezzo, rigaPedoneAvversario, colonnaPartenza, rigaArrivo, colonnaPedoneAvversario)
                    && !lasciaReSottoScacco(pezzo, rigaPedoneAvversario, colonnaPartenza, rigaArrivo, colonnaPedoneAvversario)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Controlla se il colore indicato può arroccare da almeno un lato.
     * @param bianco true per il bianco, false per il nero
     * @return true se è possibile arroccare
     */
    private boolean esisteArroccoLegale(boolean bianco) {
        return arroccoLegale(bianco, false) || arroccoLegale(bianco, true);
    }

    /**
     * Verifica tutte le condizioni dell'arrocco per un colore e un lato specifici.
     * @param bianco true per il bianco, false per il nero
     * @param latoLungo true per arrocco lungo, false per arrocco corto
     * @return true se l'arrocco richiesto è legale
     */
    private boolean arroccoLegale(boolean bianco, boolean latoLungo) {
        int rigaRe = bianco ? 7 : 0;
        if (!coordinateValide(rigaRe, 4)) return false;

        Pezzo pezzoRe = mappa[rigaRe][4].getPezzoContenuto();
        if (!(pezzoRe instanceof Re) || pezzoRe.isBianco() != bianco) return false;

        Re re = (Re) pezzoRe;
        if (re.haMosso() || isSottoScacco(bianco)) return false;

        int colonnaTorre = latoLungo ? 0 : 7;
        int primaCasellaLibera = latoLungo ? 1 : 5;
        int ultimaCasellaLibera = latoLungo ? 3 : 6;
        int colonnaReDestinazione = latoLungo ? 2 : 6;
        int passo = latoLungo ? -1 : 1;

        Pezzo pezzoTorre = mappa[rigaRe][colonnaTorre].getPezzoContenuto();
        if (!(pezzoTorre instanceof Torre) || pezzoTorre.isBianco() != bianco) return false;

        Torre torre = (Torre) pezzoTorre;
        if (torre.haMosso()) return false;

        for (int col = primaCasellaLibera; col <= ultimaCasellaLibera; col++) {
            if (!mappa[rigaRe][col].isVuota()) return false;
        }

        for (int col = 4 + passo; col != colonnaReDestinazione + passo; col += passo) {
            if (casellaAttaccata(rigaRe, col, !bianco)) return false;
        }

        return true;
    }

    /**
     * Controlla se il colore di turno può arroccare dal lato richiesto.
     * @param latoLungo true per arrocco lungo, false per arrocco corto
     * @return true se il colore di turno può arroccare
     */
    public boolean puoArroccare(boolean latoLungo) {
        return arroccoLegale(attaccaBianco, latoLungo);
    }

    /**
     * Legge da file le otto righe della scacchiera e costruisce la matrice di caselle.
     * @param s scanner posizionato all'inizio della scacchiera
     * @return mappa 8x8 letta dal file
     * @throws FileNonValidoException se il formato della scacchiera non è valido
     */
    protected Casella[][] leggiScacchiera(Scanner s) {
        Casella[][] nuovaMappa = new Casella[8][8];
        for (int i = 0; i < 8; i++) {
            if (!s.hasNextLine()) {
                throw new FileNonValidoException("Il file deve contenere 8 righe di scacchiera.");
            }

            String riga = s.nextLine().replace(" ", "").trim();
            if (riga.length() != 8) {
                throw new FileNonValidoException("Ogni riga della scacchiera deve contenere esattamente 8 caselle.");
            }

            for (int j = 0; j < 8; j++) {
                nuovaMappa[i][j] = creaCasellaDaChar(riga.charAt(j), i, j, nuovaMappa);
            }
        }
        return nuovaMappa;
    }

    /**
     * Converte un carattere del file nel pezzo corrispondente o in una casella vuota.
     * @param c carattere che rappresenta il pezzo
     * @param riga riga della casella
     * @param colonna colonna della casella
     * @param mappa mappa a cui il pezzo deve fare riferimento
     * @return casella creata dal carattere
     * @throws FileNonValidoException se il carattere non rappresenta un pezzo valido
     */
    protected Casella creaCasellaDaChar(char c, int riga, int colonna, Casella[][] mappa) {
        switch (c) {
            case 't': case 'T': return new Casella(new Torre(c, riga, colonna, mappa));
            case 'c': case 'C': return new Casella(new Cavallo(c, riga, colonna, mappa));
            case 'a': case 'A': return new Casella(new Alfiere(c, riga, colonna, mappa));
            case 'd': case 'D': return new Casella(new Regina(c, riga, colonna, mappa));
            case 'r': case 'R': return new Casella(new Re(c, riga, colonna, mappa));
            case 'p': case 'P': return new Casella(new Pedone(c, riga, colonna, mappa));
            case '.': return new Casella();
            default: throw new FileNonValidoException("Carattere non valido nella scacchiera: " + c);
        }
    }

    /**
     * Converte la riga del turno letta da file nel relativo valore booleano.
     * @param rigaTurno testo del turno, BIANCO oppure NERO
     * @return true se il turno è del bianco, false se è del nero
     * @throws FileNonValidoException se il turno non è valido
     */
    protected boolean leggiTurno(String rigaTurno) {
        String turno = rigaTurno.trim().toUpperCase();
        if (turno.equals("BIANCO")) return true;
        if (turno.equals("NERO")) return false;
        throw new FileNonValidoException("Turno non valido: " + rigaTurno);
    }

    /**
     * Valida le coordinate di una casella prima di usarle nella mappa.
     * @param riga riga da controllare
     * @param colonna colonna da controllare
     * @throws MossaNonValidaException se le coordinate sono fuori dalla scacchiera
     */
    protected void validaCoordinate(int riga, int colonna) {
        if (!coordinateValide(riga, colonna)) {
            throw new MossaNonValidaException("Coordinate non valide: (" + riga + ", " + colonna + ")");
        }
    }

    /**
     * Controlla se due coordinate sono comprese dentro la scacchiera 8x8.
     * @param riga riga da controllare
     * @param colonna colonna da controllare
     * @return true se le coordinate sono valide
     */
    protected boolean coordinateValide(int riga, int colonna) {
        return riga >= 0 && riga < 8 && colonna >= 0 && colonna < 8;
    }

    public Casella[][] getMappa() {
        return mappa;
    }
    
    public void setMappa(Casella[][] mappa) {
        this.mappa = mappa;
    }

    public boolean isAttaccaBianco() {
        return attaccaBianco;
    }

    /**
     * Associa un bot alla partita e gli passa il riferimento alla partita corrente.
     * @param bot bot da collegare alla partita
     */
    public void setBot(Bot bot) {
        this.bot = bot;

        if (bot != null) {
            bot.setPartita(this);
        }
    }

    public Pedone getPedoneEnPassant() {
        return pedoneEnPassant;
    }

    public void setPedoneEnPassant(Pedone pedoneEnPassant) {
        this.pedoneEnPassant = pedoneEnPassant;
    }
}
