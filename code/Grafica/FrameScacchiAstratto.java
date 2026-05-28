package code.Grafica;

import code.Exception.MossaNonValidaException;
import code.Partite.PartitaAstratta;
import code.Pezzi.Pedone;
import code.Util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Finestra grafica comune per partita ed esercizio. Disegna la scacchiera, gestisce i click, evidenzia le mosse e aggiorna lo stato a video.
 */
public abstract class FrameScacchiAstratto extends JFrame implements MouseListener {

    protected static final int DIMENSIONE_SCACCHIERA = 8;
    protected static final int DIMENSIONE_CASELLA = 100;

    protected static final Color COLORE_CHIARO = new Color(240, 217, 181);
    protected static final Color COLORE_SCURO = new Color(181, 136, 99);
    protected static final Color COLORE_SELEZIONE = new Color(246, 246, 105);
    protected static final Color COLORE_MOSSA = new Color(120, 200, 120);
    protected static final Color COLORE_CATTURA = new Color(220, 80, 80);
    protected static final Color COLORE_ERRORE = new Color(220, 80, 80);
    protected static final Color COLORE_ARROCCO = new Color(120, 170, 230);
    protected static final Color COLORE_MOVIMENTO = new Color(189, 189, 79);

    protected final JLabel[][] labels;
    protected final PartitaAstratta partita;
    protected final JLabel stato;
    protected final JLabel info;

    protected int rigaSelezionata = -1;
    protected int colonnaSelezionata = -1;

    protected int rigaMovimentoPartenza = -1;
    protected int colonnaMovimentoPartenza = -1;
    protected int rigaMovimentoArrivo = -1;
    protected int colonnaMovimentoArrivo = -1;
    protected boolean mostraUltimoMovimento = true;

    protected boolean erroreVisibile = false;
    protected boolean finito = false;
    protected Timer timerErrore;
    protected boolean conBot;

    /**
     * Costruisce la finestra base collegandola alla partita e preparando le impostazioni principali.
     * @param titolo titolo della finestra
     * @param partita partita o esercizio da mostrare
     * @param conBot true se è attivo il bot
     */
    protected FrameScacchiAstratto(String titolo, PartitaAstratta partita, boolean conBot) {
        super(titolo);

        this.partita = partita;
        this.labels = new JLabel[DIMENSIONE_SCACCHIERA][DIMENSIONE_SCACCHIERA];
        this.stato = new JLabel("", SwingConstants.CENTER);
        this.info = new JLabel("", SwingConstants.CENTER);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(true);

        creaPannelloAlto();
        creaScacchiera();
        this.conBot = conBot;
    }

    /**
     * Rende visibile la finestra e inizializza la grafica della scacchiera.
     */
    public void avviaFrame() {
        aggiornaGrafica();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Crea la barra superiore con lo stato del turno e le informazioni aggiuntive.
     */
    protected void creaPannelloAlto() {
        stato.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        stato.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel pannelloAlto = new JPanel(new GridLayout(2, 1));
        pannelloAlto.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        info.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));

        pannelloAlto.add(stato);
        pannelloAlto.add(info);
        add(pannelloAlto, BorderLayout.NORTH);
    }

    /**
     * Crea graficamente le 64 caselle della scacchiera e collega i listener del mouse.
     */
    private void creaScacchiera() {
        JPanel scacchiera = new JPanel(new GridLayout(DIMENSIONE_SCACCHIERA, DIMENSIONE_SCACCHIERA));
        scacchiera.setPreferredSize(new Dimension(
                DIMENSIONE_CASELLA * DIMENSIONE_SCACCHIERA,
                DIMENSIONE_CASELLA * DIMENSIONE_SCACCHIERA
        ));

        for (int riga = 0; riga < DIMENSIONE_SCACCHIERA; riga++) {
            for (int colonna = 0; colonna < DIMENSIONE_SCACCHIERA; colonna++) {
                JLabel label = new JLabel("", SwingConstants.CENTER);
                label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 48));
                label.setOpaque(true);
                label.putClientProperty("riga", riga);
                label.putClientProperty("colonna", colonna);
                label.addMouseListener(this);

                labels[riga][colonna] = label;
                scacchiera.add(label);
            }
        }

        add(scacchiera, BorderLayout.CENTER);
    }

    /**
     * Ridisegna tutta la scacchiera in base alla mappa corrente e mantiene evidenziato l'ultimo movimento.
     */
    protected void aggiornaGrafica() {
        Casella[][] mappa = partita.getMappa();

        for (int riga = 0; riga < DIMENSIONE_SCACCHIERA; riga++) {
            for (int colonna = 0; colonna < DIMENSIONE_SCACCHIERA; colonna++) {
                JLabel label = labels[riga][colonna];
                label.setBackground(coloreBase(riga, colonna));
                label.setBorder(null);

                Pezzo pezzo = mappa[riga][colonna].getPezzoContenuto();
                label.setText(pezzo == null ? "" : simboloPezzo(pezzo.getNome()));
            }
        }

        evidenziaUltimoMovimento();

        if (rigaSelezionata != -1 && colonnaSelezionata != -1 && !finito) {
            labels[rigaSelezionata][colonnaSelezionata].setBackground(COLORE_SELEZIONE);
            evidenziaMossePossibili(rigaSelezionata, colonnaSelezionata);
        }

        aggiornaInfoExtra();

        if (!erroreVisibile && !finito) {
            stato.setForeground(Color.BLACK);
            stato.setText("Turno: " + nomeColore(partita.isAttaccaBianco()));
        }
    }

    /**
     * Colora le caselle di partenza e arrivo dell'ultima mossa eseguita.
     */
    private void evidenziaUltimoMovimento() {
        if (!mostraUltimoMovimento) return;

        if (coordinateMovimentoValide(rigaMovimentoPartenza, colonnaMovimentoPartenza)) {
            labels[rigaMovimentoPartenza][colonnaMovimentoPartenza].setBackground(COLORE_MOVIMENTO);
            labels[rigaMovimentoPartenza][colonnaMovimentoPartenza].setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        }

        if (coordinateMovimentoValide(rigaMovimentoArrivo, colonnaMovimentoArrivo)) {
            labels[rigaMovimentoArrivo][colonnaMovimentoArrivo].setBackground(COLORE_MOVIMENTO);
            labels[rigaMovimentoArrivo][colonnaMovimentoArrivo].setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        }
    }

    private boolean coordinateMovimentoValide(int riga, int colonna) {
        return riga >= 0 && riga < DIMENSIONE_SCACCHIERA
                && colonna >= 0 && colonna < DIMENSIONE_SCACCHIERA;
    }

    /**
     * Salva le coordinate dell'ultima mossa da evidenziare graficamente.
     * @param rigaPartenza riga iniziale
     * @param colonnaPartenza colonna iniziale
     * @param rigaArrivo riga finale
     * @param colonnaArrivo colonna finale
     */
    protected void salvaUltimoMovimento(int rigaPartenza, int colonnaPartenza, int rigaArrivo, int colonnaArrivo) {
        rigaMovimentoPartenza = rigaPartenza;
        colonnaMovimentoPartenza = colonnaPartenza;
        rigaMovimentoArrivo = rigaArrivo;
        colonnaMovimentoArrivo = colonnaArrivo;
        mostraUltimoMovimento = true;
    }

    /**
     * Evidenzia sulla scacchiera le mosse possibili del pezzo selezionato, distinguendo mosse e catture.
     * @param riga riga del pezzo selezionato
     * @param colonna colonna del pezzo selezionato
     */
    private void evidenziaMossePossibili(int riga, int colonna) {
        Pezzo pezzo = partita.getMappa()[riga][colonna].getPezzoContenuto();
        if (pezzo == null) return;

        Casella[][] mosse = pezzo.mossePossibili();
        Casella[][] mappa = partita.getMappa();

        for (int i = 0; i < DIMENSIONE_SCACCHIERA; i++) {
            for (int j = 0; j < DIMENSIONE_SCACCHIERA; j++) {
                boolean enPassant = partita.isEnPassantValido(pezzo, riga, colonna, i, j);

                if (mosse[i][j] == null && !enPassant) continue;
                if (partita.lasciaReSottoScacco(pezzo, riga, colonna, i, j)) continue;

                Pezzo pezzoDestinazione = mappa[i][j].getPezzoContenuto();

                if (enPassant || (pezzoDestinazione != null && pezzoDestinazione.isBianco() != pezzo.isBianco())) {
                    labels[i][j].setBackground(COLORE_CATTURA);
                    labels[i][j].setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
                } else {
                    labels[i][j].setBackground(COLORE_MOSSA);
                    labels[i][j].setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
                }
            }
        }
        evidenziaArroccoSePossibile(pezzo);
    }

    /**
     * Evidenzia le caselle di arrocco quando il re selezionato può arroccare.
     * @param pezzo pezzo selezionato
     */
    private void evidenziaArroccoSePossibile(Pezzo pezzo) {
        if (Character.toUpperCase(pezzo.getNome()) != 'R') return;

        int rigaRe = pezzo.isBianco() ? 7 : 0;
        if (rigaSelezionata != rigaRe || colonnaSelezionata != 4) return;

        if (puoArroccare(false)) {
            labels[rigaRe][6].setBackground(COLORE_ARROCCO);
            labels[rigaRe][6].setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
        }

        if (puoArroccare(true)) {
            labels[rigaRe][2].setBackground(COLORE_ARROCCO);
            labels[rigaRe][2].setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
        }
    }

    /**
     * Chiede alla partita se l'arrocco richiesto è disponibile e protegge la grafica da errori runtime.
     * @param latoLungo true per arrocco lungo, false per arrocco corto
     * @return true se l'arrocco è possibile
     */
    public boolean puoArroccare(boolean latoLungo) {
        try {
            return partita.puoArroccare(latoLungo);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private Color coloreBase(int riga, int colonna) {
        return (riga + colonna) % 2 == 0 ? COLORE_CHIARO : COLORE_SCURO;
    }

    /**
     * Converte il carattere interno di un pezzo nel simbolo Unicode mostrato sulla scacchiera.
     * @param nome carattere del pezzo
     * @return simbolo grafico del pezzo
     */
    private String simboloPezzo(char nome) {
        switch (nome) {
            case 'R': return "♔";
            case 'D': return "♕";
            case 'T': return "♖";
            case 'A': return "♗";
            case 'C': return "♘";
            case 'P': return "♙";
            case 'r': return "♚";
            case 'd': return "♛";
            case 't': return "♜";
            case 'a': return "♝";
            case 'c': return "♞";
            case 'p': return "♟";
            default: return String.valueOf(nome);
        }
    }

    /**
     * Gestisce la selezione di una casella e mostra le mosse del pezzo scelto.
     * @param riga riga cliccata
     * @param colonna colonna cliccata
     */
    private void selezionaCasella(int riga, int colonna) {
        if (finito) return;

        Pezzo pezzo = partita.getMappa()[riga][colonna].getPezzoContenuto();

        if (pezzo == null) {
            deseleziona();
            return;
        }

        if (conBot && !partita.isAttaccaBianco()) {
            mostraErrore("Aspetta la mossa del bot");
            deseleziona();
            return;
        }

        if (conBot && !pezzo.isBianco()) {
            mostraErrore("Tu giochi con il Bianco");
            deseleziona();
            return;
        }

        if (pezzo.isBianco() != partita.isAttaccaBianco()) {
            mostraErrore("Non è il turno di questo colore");
            deseleziona();
            return;
        }

        rigaSelezionata = riga;
        colonnaSelezionata = colonna;
        aggiornaGrafica();
    }

    /**
     * Prova a muovere il pezzo selezionato nella casella cliccata e aggiorna grafica, errori e bot.
     * @param rigaArrivo riga di arrivo
     * @param colonnaArrivo colonna di arrivo
     */
    private void muoviSuCasella(int rigaArrivo, int colonnaArrivo) {
        if (finito) return;

        try {
            boolean coloreCheHaMosso = partita.isAttaccaBianco();
            Pezzo pezzo = partita.getMappa()[rigaSelezionata][colonnaSelezionata].getPezzoContenuto();

            if (isTentativoArrocco(pezzo, rigaArrivo, colonnaArrivo)) {
                boolean latoLungo = colonnaArrivo == 2;

                if (!puoArroccare(latoLungo)) {
                    throw new MossaNonValidaException("Arrocco non valido");
                }

                partita.arrocca(latoLungo);
                salvaUltimoMovimento(rigaSelezionata, colonnaSelezionata, rigaArrivo, colonnaArrivo);
            } else {
                partita.muoviPezzo(rigaSelezionata, colonnaSelezionata, rigaArrivo, colonnaArrivo);
                dopoMossaNormale(pezzo);
                salvaUltimoMovimento(rigaSelezionata, colonnaSelezionata, rigaArrivo, colonnaArrivo);
            }

            deseleziona();
            controllaFine(coloreCheHaMosso);
            muoviBotSeServe();

        } catch (MossaNonValidaException | IllegalArgumentException e) {
            mostraErrore(e.getMessage());
            deseleziona();
        }
    }

    /**
     * Riconosce se il click dell'utente rappresenta un tentativo di arrocco del re.
     * @param pezzo pezzo selezionato
     * @param rigaArrivo riga cliccata
     * @param colonnaArrivo colonna cliccata
     * @return true se il movimento richiesto è un arrocco
     */
    private boolean isTentativoArrocco(Pezzo pezzo, int rigaArrivo, int colonnaArrivo) {
        if (pezzo == null) return false;
        if (Character.toUpperCase(pezzo.getNome()) != 'R') return false;

        return rigaArrivo == rigaSelezionata
                && colonnaSelezionata == 4
                && (colonnaArrivo == 2 || colonnaArrivo == 6);
    }

    /**
     * Fa muovere automaticamente il bot quando è il suo turno e la partita non è terminata.
     */
    private void muoviBotSeServe() {
        if (!conBot || finito || partita.isAttaccaBianco()) {
            return;
        }

        Timer timerBot = new Timer(300, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    char[][] mappaPrima = copiaNomiPezzi();

                    partita.muoviPezzoConBot(false);
                    salvaUltimoMovimentoBot(mappaPrima, false);

                    aggiornaGrafica();
                    controllaFine(false);
                } catch (RuntimeException ex) {
                    mostraErrore("Errore bot: " + ex.getMessage());
                }
            }
        });

        timerBot.setRepeats(false);
        timerBot.start();
    }

    /**
     * Copia solo i caratteri dei pezzi della scacchiera corrente, utile per riconoscere il movimento del bot.
     * @return matrice 8x8 con i nomi dei pezzi
     */
    private char[][] copiaNomiPezzi() {
        char[][] copia = new char[DIMENSIONE_SCACCHIERA][DIMENSIONE_SCACCHIERA];
        Casella[][] mappa = partita.getMappa();

        for (int riga = 0; riga < DIMENSIONE_SCACCHIERA; riga++) {
            for (int colonna = 0; colonna < DIMENSIONE_SCACCHIERA; colonna++) {
                Pezzo pezzo = mappa[riga][colonna].getPezzoContenuto();
                copia[riga][colonna] = pezzo == null ? '.' : pezzo.getNome();
            }
        }

        return copia;
    }

    /**
     * Confronta la mappa prima e dopo la mossa del bot per ricavare le caselle da evidenziare.
     * @param mappaPrima nomi dei pezzi prima della mossa
     * @param botBianco colore del bot
     */
    private void salvaUltimoMovimentoBot(char[][] mappaPrima, boolean botBianco) {
        if (salvaArroccoBotSePresente(mappaPrima, botBianco)) {
            return;
        }

        Casella[][] mappaDopo = partita.getMappa();
        int rigaPartenza = -1;
        int colonnaPartenza = -1;
        int rigaArrivo = -1;
        int colonnaArrivo = -1;

        for (int riga = 0; riga < DIMENSIONE_SCACCHIERA; riga++) {
            for (int colonna = 0; colonna < DIMENSIONE_SCACCHIERA; colonna++) {
                char prima = mappaPrima[riga][colonna];
                Pezzo pezzoDopo = mappaDopo[riga][colonna].getPezzoContenuto();
                char dopo = pezzoDopo == null ? '.' : pezzoDopo.getNome();

                if (prima != '.' && isColorePezzo(prima, botBianco) && prima != dopo) {
                    rigaPartenza = riga;
                    colonnaPartenza = colonna;
                }

                if (dopo != '.' && isColorePezzo(dopo, botBianco) && prima != dopo) {
                    rigaArrivo = riga;
                    colonnaArrivo = colonna;
                }
            }
        }

        if (coordinateMovimentoValide(rigaPartenza, colonnaPartenza)
                && coordinateMovimentoValide(rigaArrivo, colonnaArrivo)) {
            salvaUltimoMovimento(rigaPartenza, colonnaPartenza, rigaArrivo, colonnaArrivo);
        }
    }

    /**
     * Riconosce e salva l'evidenziazione corretta quando il bot ha appena arroccato.
     * @param mappaPrima posizione prima della mossa del bot
     * @param botBianco colore del bot
     * @return true se è stato riconosciuto un arrocco
     */
    private boolean salvaArroccoBotSePresente(char[][] mappaPrima, boolean botBianco) {
        int rigaRe = botBianco ? 7 : 0;
        char re = botBianco ? 'R' : 'r';
        char torre = botBianco ? 'T' : 't';
        Casella[][] mappaDopo = partita.getMappa();

        if (mappaPrima[rigaRe][4] != re) {
            return false;
        }

        if (mappaPrima[rigaRe][7] == torre
                && pezzoInCasella(mappaDopo, rigaRe, 6, re)
                && pezzoInCasella(mappaDopo, rigaRe, 5, torre)) {
            salvaUltimoMovimento(rigaRe, 4, rigaRe, 6);
            return true;
        }

        if (mappaPrima[rigaRe][0] == torre
                && pezzoInCasella(mappaDopo, rigaRe, 2, re)
                && pezzoInCasella(mappaDopo, rigaRe, 3, torre)) {
            salvaUltimoMovimento(rigaRe, 4, rigaRe, 2);
            return true;
        }

        return false;
    }

    private boolean pezzoInCasella(Casella[][] mappa, int riga, int colonna, char nome) {
        Pezzo pezzo = mappa[riga][colonna].getPezzoContenuto();
        return pezzo != null && pezzo.getNome() == nome;
    }

    private boolean isColorePezzo(char nome, boolean bianco) {
        return bianco ? Character.isUpperCase(nome) : Character.isLowerCase(nome);
    }

    /**
     * Aggiorna le informazioni specifiche della modalità mostrata nella barra superiore.
     */
    protected abstract void aggiornaInfoExtra();

    /**
     * Aggiorna grafica, controlli di fine partita e bot dopo una mossa manuale valida.
     * @param pezzoMosso pezzo appena mosso
     */
    private void dopoMossaNormale(Pezzo pezzoMosso) {
        if (pezzoMosso instanceof Pedone p) {
            Pezzo pezzo = partita.promuoviPedone(p);
            if (pezzo == null) return;
            partita.getMappa()[p.getRiga()][p.getColonna()].inserisciPezzo(pezzo);
        }
    }

    /**
     * Controlla le condizioni di fine partita specifiche della modalità.
     * @param coloreCheHaMosso colore che ha appena mosso
     */
    protected abstract void controllaFine(boolean coloreCheHaMosso);

    /**
     * Blocca la scacchiera e mostra il messaggio finale nello stato.
     * @param messaggio testo da visualizzare
     */
    protected void finisciConMessaggio(String messaggio) {
        finito = true;
        rigaSelezionata = -1;
        colonnaSelezionata = -1;
        erroreVisibile = false;

        if (timerErrore != null && timerErrore.isRunning()) {
            timerErrore.stop();
        }

        aggiornaGrafica();
        stato.setForeground(Color.BLACK);
        stato.setText(messaggio);
        bloccaScacchiera();
    }

    private void deseleziona() {
        rigaSelezionata = -1;
        colonnaSelezionata = -1;
        aggiornaGrafica();
    }

    /**
     * Mostra temporaneamente un messaggio di errore nella barra di stato e poi ripristina il turno.
     * @param messaggio errore da visualizzare
     */
    protected void mostraErrore(String messaggio) {
        if (finito) return;

        if (timerErrore != null && timerErrore.isRunning()) {
            timerErrore.stop();
        }

        erroreVisibile = true;

        stato.setText(messaggio != null && !messaggio.isBlank() ? messaggio : "Mossa invalida");
        stato.setForeground(COLORE_ERRORE);

        timerErrore = new Timer(1500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                erroreVisibile = false;

                if (!finito) {
                    stato.setForeground(Color.BLACK);
                    stato.setText("Turno: " + nomeColore(partita.isAttaccaBianco()));
                }
            }
        });

        timerErrore.setRepeats(false);
        timerErrore.start();
    }

    /**
     * Indica se la scacchiera deve accettare input dell'utente.
     * @return true se i click sono abilitati
     */
    protected boolean inputAbilitato() {
        return true;
    }

    /**
     * Gestisce il click sulla scacchiera selezionando un pezzo o tentando una mossa.
     * @param e evento del mouse
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        if (finito) return;

        if (!inputAbilitato()) {
            mostraErrore("Torna all'ultima mossa per continuare a giocare");
            return;
        }

        JLabel label = (JLabel) e.getSource();
        int riga = (int) label.getClientProperty("riga");
        int colonna = (int) label.getClientProperty("colonna");

        if (rigaSelezionata == -1 || colonnaSelezionata == -1) {
            selezionaCasella(riga, colonna);
            return;
        }

        if (riga == rigaSelezionata && colonna == colonnaSelezionata) {
            deseleziona();
            return;
        }

        Pezzo pezzoCliccato = partita.getMappa()[riga][colonna].getPezzoContenuto();

        if (pezzoCliccato != null && pezzoCliccato.isBianco() == partita.isAttaccaBianco()) {
            selezionaCasella(riga, colonna);
            return;
        }

        muoviSuCasella(riga, colonna);
    }

    /**
     * Disattiva tutte le caselle della scacchiera dopo la fine della partita.
     */
    private void bloccaScacchiera() {
        for (int riga = 0; riga < DIMENSIONE_SCACCHIERA; riga++) {
            for (int colonna = 0; colonna < DIMENSIONE_SCACCHIERA; colonna++) {
                labels[riga][colonna].removeMouseListener(this);
                labels[riga][colonna].setEnabled(true);
            }
        }
    }

    /**
     * Riattiva la scacchiera, usato quando si torna dalla cronologia a una posizione giocabile.
     */
    protected void riattivaScacchiera() {
        for (int riga = 0; riga < DIMENSIONE_SCACCHIERA; riga++) {
            for (int colonna = 0; colonna < DIMENSIONE_SCACCHIERA; colonna++) {
                labels[riga][colonna].setEnabled(true);

                boolean listenerPresente = false;
                MouseListener[] listeners = labels[riga][colonna].getMouseListeners();
                for (MouseListener listener : listeners) {
                    if (listener == this) {
                        listenerPresente = true;
                        break;
                    }
                }

                if (!listenerPresente) {
                    labels[riga][colonna].addMouseListener(this);
                }
            }
        }
    }

    /**
     * Converte un booleano colore in testo leggibile.
     * @param bianco true per bianco, false per nero
     * @return nome del colore
     */
    protected String nomeColore(boolean bianco) {
        return bianco ? "Bianco" : "Nero";
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (finito) return;

        JLabel label = (JLabel) e.getSource();
        label.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (finito) return;
        aggiornaGrafica();
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    public PartitaAstratta getPartita() {
        return partita;
    }
}
