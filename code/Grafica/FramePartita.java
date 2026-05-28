package code.Grafica;

import code.Main;
import code.Partite.Partita;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

/**
 * Finestra grafica della partita normale, con pulsanti di salvataggio, home e navigazione nella cronologia.
 */
public class FramePartita extends FrameScacchiAstratto {

    private static final Color COLORE_BARRA = new Color(248, 241, 230);
    private static final Color COLORE_BOTTONE = new Color(236, 219, 195);
    private static final Color COLORE_BOTTONE_HOVER = new Color(225, 202, 172);
    private static final Color COLORE_TESTO = new Color(48, 36, 25);
    private static final Color COLORE_BORDO = new Color(174, 139, 104);

    private final Partita partitaNormale;
    private JButton home;
    private JButton salvaPartita;
    private JButton indietro;
    private JButton avanti;

    /**
     * Crea una finestra per una nuova partita, con o senza bot.
     * @param conBot true se la partita è contro il bot
     */
    public FramePartita(boolean conBot) {
        this(new Partita(), conBot);
    }

    /**
     * Crea una finestra caricando una partita da file e impostando manualmente la presenza del bot.
     * @param percorsoFile file di salvataggio
     * @param conBot true se la partita è contro il bot
     * @throws IOException se il file non può essere letto
     */
    public FramePartita(String percorsoFile, boolean conBot) throws IOException {
        this(new Partita(percorsoFile), conBot);
    }

    /**
     * Crea una finestra caricando una partita da file e usando il valore del bot salvato nel file.
     * @param percorsoFile file di salvataggio
     * @throws IOException se il file non può essere letto
     */
    public FramePartita(String percorsoFile) throws IOException {
        this(new Partita(percorsoFile));
    }

    private FramePartita(Partita partita) {
        this(partita, partita.isConBot());
    }

    private FramePartita(Partita partita, boolean conBot) {
        super("Scacchi", partita, conBot);
        this.partitaNormale = partita;
        aggiungiPulsantiAlti();
        aggiungiControlloChiusura();
    }

    /**
     * Aggiunge nella barra alta i pulsanti home, salvataggio e navigazione della cronologia.
     */
    private void aggiungiPulsantiAlti() {
        home = creaBottone("⌂ Home", 104, 30);
        salvaPartita = creaBottone("💾 Salva", 112, 30);
        indietro = creaBottoneFreccia("←");
        avanti = creaBottoneFreccia("→");

        home.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tornaHome();
            }
        });

        salvaPartita.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                salvaPartita();
            }
        });

        indietro.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cambiaMossaCronologia(-1);
            }
        });

        avanti.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cambiaMossaCronologia(1);
            }
        });

        stato.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        stato.setForeground(COLORE_TESTO);
        stato.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));

        info.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        info.setForeground(new Color(90, 75, 60));
        info.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        JPanel pannelloAlto = new JPanel(new BorderLayout());
        pannelloAlto.setBackground(COLORE_BARRA);
        pannelloAlto.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, COLORE_BORDO),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JPanel pannelloTesti = new JPanel();
        pannelloTesti.setOpaque(false);
        pannelloTesti.setLayout(new BoxLayout(pannelloTesti, BoxLayout.Y_AXIS));
        stato.setAlignmentX(Component.CENTER_ALIGNMENT);
        info.setAlignmentX(Component.CENTER_ALIGNMENT);
        pannelloTesti.add(Box.createVerticalGlue());
        pannelloTesti.add(stato);
        pannelloTesti.add(info);
        pannelloTesti.add(Box.createVerticalGlue());

        JPanel rigaFrecce = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        rigaFrecce.setOpaque(false);
        rigaFrecce.add(indietro);
        rigaFrecce.add(avanti);

        JPanel rigaAzioni = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        rigaAzioni.setOpaque(false);
        rigaAzioni.add(home);
        rigaAzioni.add(salvaPartita);

        JPanel pannelloComandi = new JPanel();
        pannelloComandi.setOpaque(false);
        pannelloComandi.setLayout(new BoxLayout(pannelloComandi, BoxLayout.Y_AXIS));
        rigaFrecce.setAlignmentX(Component.CENTER_ALIGNMENT);
        rigaAzioni.setAlignmentX(Component.CENTER_ALIGNMENT);
        pannelloComandi.add(rigaFrecce);
        pannelloComandi.add(Box.createVerticalStrut(5));
        pannelloComandi.add(rigaAzioni);

        JPanel pannelloDestra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pannelloDestra.setOpaque(false);
        pannelloDestra.add(pannelloComandi);

        Dimension dimensioneComandi = pannelloDestra.getPreferredSize();
        JPanel spaziatoreSinistra = new JPanel();
        spaziatoreSinistra.setOpaque(false);
        spaziatoreSinistra.setPreferredSize(dimensioneComandi);

        pannelloAlto.add(spaziatoreSinistra, BorderLayout.WEST);
        pannelloAlto.add(pannelloTesti, BorderLayout.CENTER);
        pannelloAlto.add(pannelloDestra, BorderLayout.EAST);

        getContentPane().remove(0);
        add(pannelloAlto, BorderLayout.NORTH);

        aggiornaPulsantiCronologia();
        revalidate();
        repaint();
    }

    /**
     * Crea un bottone con stile coerente per la barra superiore.
     * @param testo testo o simbolo del bottone
     * @param larghezza larghezza preferita
     * @param altezza altezza preferita
     * @return bottone configurato
     */
    private JButton creaBottone(String testo, int larghezza, int altezza) {
        JButton bottone = new JButton(testo);
        bottone.setFocusable(false);
        bottone.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        bottone.setForeground(COLORE_TESTO);
        bottone.setBackground(COLORE_BOTTONE);
        bottone.setOpaque(true);
        bottone.setContentAreaFilled(true);
        bottone.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLORE_BORDO, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        bottone.setCursor(new Cursor(Cursor.HAND_CURSOR));
        bottone.setPreferredSize(new Dimension(larghezza, altezza));

        bottone.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (bottone.isEnabled()) {
                    bottone.setBackground(COLORE_BOTTONE_HOVER);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                bottone.setBackground(COLORE_BOTTONE);
            }
        });

        return bottone;
    }

    /**
     * Crea un bottone freccia per muoversi nella cronologia.
     * @param testo simbolo della freccia
     * @return bottone della cronologia
     */
    private JButton creaBottoneFreccia(String testo) {
        JButton bottone = creaBottone(testo, 52, 30);
        bottone.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        return bottone;
    }

    /**
     * Chiede conferma e torna al menu iniziale, eliminando i temporanei se la partita non è stata salvata.
     */
    private void tornaHome() {
        partitaNormale.eliminaTemporaneiSeNonSalvata();
        dispose();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    Main.main(new String[0]);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(
                        null,
                        "Errore durante il ritorno alla home:\n" + ex.getMessage(),
                        "Errore",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });
    }

    /**
     * Salva la partita reale e mostra all'utente l'esito dell'operazione.
     */
    private void salvaPartita() {
        if (!partitaNormale.isUltimaMossaCronologia()) {
            JOptionPane.showMessageDialog(
                FramePartita.this,
                "Torna all'ultima mossa prima di salvare la partita.",
                "Cronologia",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            File fileDaSalvare = partitaNormale.salvaPartitaReale();

            JOptionPane.showMessageDialog(
                FramePartita.this,
                "Partita salvata in:\n" + fileDaSalvare.getAbsolutePath(),
                "Salvataggio completato",
                JOptionPane.INFORMATION_MESSAGE
            );

            String[] scelte = {"Continua", "Esci"};

            int scelta = JOptionPane.showOptionDialog(
                null,
                "Vuoi continuare a giocare?",
                "Salvataggio partita",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                new ImageIcon("code/Immagini/Pedone.png"),
                scelte,
                scelte[0]
            );

            if (scelta <= 0) return;
            if (scelta == 1) {
                System.exit(0);
            }

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                FramePartita.this,
                "Errore durante il salvataggio:\n" + ex.getMessage(),
                "Errore",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Sposta la visualizzazione avanti o indietro nella cronologia delle mosse.
     * @param direzione -1 per indietro, +1 per avanti
     */
    private void cambiaMossaCronologia(int direzione) {
        int nuovaMossa = partitaNormale.getIndiceCronologiaAttuale() + direzione;
        int ultimaMossa = partitaNormale.getUltimoIndiceCronologia();

        if (nuovaMossa < 0 || nuovaMossa > ultimaMossa) {
            return;
        }

        try {
            partitaNormale.caricaMossaTemporanea(nuovaMossa);
            rigaSelezionata = -1;
            colonnaSelezionata = -1;
            impostaMovimentoDaCronologia(nuovaMossa);

            finito = false;
            erroreVisibile = false;
            riattivaScacchiera();

            aggiornaGrafica();
            aggiornaPulsantiCronologia();

            if (nuovaMossa == ultimaMossa) {
                controllaFine(!partitaNormale.isAttaccaBianco());
            } else {
                aggiornaMessaggioCronologia();
            }
        } catch (IOException | RuntimeException ex) {
            mostraErrore(ex.getMessage());
        }
    }

    /**
     * Imposta l'evidenziazione della mossa caricata dalla cronologia.
     * @param indiceMossa indice della mossa caricata
     */
    private void impostaMovimentoDaCronologia(int indiceMossa) {
        try {
            int[] movimento = partitaNormale.getMovimentoMossaCronologia(indiceMossa);

            if (movimento == null || movimento.length != 4 || movimento[0] == -1) {
                rigaMovimentoPartenza = -1;
                colonnaMovimentoPartenza = -1;
                rigaMovimentoArrivo = -1;
                colonnaMovimentoArrivo = -1;
                mostraUltimoMovimento = false;
                return;
            }

            salvaUltimoMovimento(movimento[0], movimento[1], movimento[2], movimento[3]);
        } catch (IOException | RuntimeException ex) {
            rigaMovimentoPartenza = -1;
            colonnaMovimentoPartenza = -1;
            rigaMovimentoArrivo = -1;
            colonnaMovimentoArrivo = -1;
            mostraUltimoMovimento = false;
        }
    }

    /**
     * Abilita o disabilita le frecce della cronologia in base alla posizione corrente.
     */
    private void aggiornaPulsantiCronologia() {
        if (indietro == null || avanti == null || salvaPartita == null) return;

        int corrente = partitaNormale.getIndiceCronologiaAttuale();
        int ultimo = partitaNormale.getUltimoIndiceCronologia();

        indietro.setEnabled(corrente > 0);
        avanti.setEnabled(corrente < ultimo);
        salvaPartita.setEnabled(corrente == ultimo);
    }

    /**
     * Aggiorna la scritta della barra superiore quando si sta guardando una posizione vecchia.
     */
    private void aggiornaMessaggioCronologia() {
        if (partitaNormale.isUltimaMossaCronologia()) {
            return;
        }

        stato.setForeground(Color.BLACK);
        stato.setText("Cronologia: mossa " + partitaNormale.getMosse()
                + " di " + (partitaNormale.getUltimoIndiceCronologia() + 1));
    }

    /**
     * Gestisce la chiusura della finestra chiedendo cosa fare dei temporanei non salvati.
     */
    private void aggiungiControlloChiusura() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                partitaNormale.eliminaTemporaneiSeNonSalvata();
                System.exit(0);
            }
        });
    }

    /**
     * Blocca le mosse quando si sta visualizzando una posizione vecchia della cronologia.
     * @return true se la posizione attuale è giocabile
     */
    @Override
    protected boolean inputAbilitato() {
        return partitaNormale.isUltimaMossaCronologia();
    }

    /**
     * Controlla scacco matto, stallo e patte della partita normale.
     * @param coloreCheHaMosso colore che ha appena mosso
     */
    @Override
    protected void controllaFine(boolean coloreCheHaMosso) {
        aggiornaPulsantiCronologia();

        String risultato = partitaNormale.checkWin();

        if (partitaNormale.pattaPerMosse()) {
            finisciConMessaggio("Patta per la regola delle 50 mosse");
            return;
        }

        if (partitaNormale.pattaPerRipetizione()) {
            finisciConMessaggio("Patta per triplice ripetizione");
            return;
        }

        if (risultato.equals("Nessuno")) {
            if (partitaNormale.isSottoScacco(partitaNormale.isAttaccaBianco())) {
                stato.setForeground(Color.RED);
                stato.setText("Scacco al " + nomeColore(partitaNormale.isAttaccaBianco()) + "!");
            }
            return;
        }

        if (risultato.equals("Patta BIANCO")) {
            finisciConMessaggio("Patta per stallo, il bianco non può muoversi");
        } else if (risultato.equals("Patta NERO")) {
            finisciConMessaggio("Patta per stallo, il nero non può muoversi");
        } else {
            finisciConMessaggio("Scacco matto! Vince il " + risultato);
        }
    }

    /**
     * Aggiorna nella barra superiore il numero di mossa corrente.
     */
    @Override
    protected void aggiornaInfoExtra() {
        if (info == null) return;

        info.setForeground(Color.DARK_GRAY);
        info.setText("Mossa: " + partitaNormale.getMosse());
    }

}
