package code.Partite;

import code.Exception.*;
import code.Pezzi.*;
import code.Util.*;

import java.io.*;
import java.util.Scanner;

/**
 * Rappresenta una partita normale di scacchi, con salvataggio su file, cronologia delle mosse, patta per mosse e patta per ripetizione.
 */
public class Partita extends PartitaAstratta {

    private int ultimaMossaPerPatta;

    private String percorsoDirectoryPartita = null;
    private int numeroFileMossa = 0;
    private int numeroPartita = -1;
    private boolean partitaSalvata = false;
    private boolean salvataggioRealeEsistente = false;
    private int primaMossaTemporaneaNonSalvata = 0;

    private static final String CARTELLA_PARTITE = "Scuola/Progettini/Scacchi/FilePartite";
    private static final String CARTELLA_TEMPORANEI = "Scuola/Progettini/Scacchi/FilePartite/FileTemporanei";

    /**
     * Crea una nuova partita standard e salva subito la posizione iniziale nella cronologia temporanea.
     */
    public Partita() {
        super();
        inizializzaPartitaStandard();
        salvaPerOgniRound();
    }

    /**
     * Carica una partita salvata da file e prepara la cronologia temporanea.
     * @param percorsoFile percorso del file di salvataggio
     * @throws IOException se il file non può essere letto
     */
    public Partita(String percorsoFile) throws IOException {
        caricaMappaDaFile(percorsoFile);
        collegaTemporaneiAlFileReale(percorsoFile);

        partitaSalvata = true;
        salvataggioRealeEsistente = true;
        primaMossaTemporaneaNonSalvata = numeroFileMossa;
    }

    /**
     * Prepara la posizione iniziale classica degli scacchi con tutti i pezzi nelle caselle corrette.
     */
    private void inizializzaPartitaStandard() {
        mappa[0][0] = new Casella(new Torre('t', 0, 0, mappa));
        mappa[0][1] = new Casella(new Cavallo('c', 0, 1, mappa));
        mappa[0][2] = new Casella(new Alfiere('a', 0, 2, mappa));
        mappa[0][3] = new Casella(new Regina('d', 0, 3, mappa));
        mappa[0][4] = new Casella(new Re('r', 0, 4, mappa));
        mappa[0][5] = new Casella(new Alfiere('a', 0, 5, mappa));
        mappa[0][6] = new Casella(new Cavallo('c', 0, 6, mappa));
        mappa[0][7] = new Casella(new Torre('t', 0, 7, mappa));

        for (int i = 0; i < 8; i++) {
            mappa[1][i] = new Casella(new Pedone('p', 1, i, mappa));
            mappa[6][i] = new Casella(new Pedone('P', 6, i, mappa));
        }

        mappa[7][0] = new Casella(new Torre('T', 7, 0, mappa));
        mappa[7][1] = new Casella(new Cavallo('C', 7, 1, mappa));
        mappa[7][2] = new Casella(new Alfiere('A', 7, 2, mappa));
        mappa[7][3] = new Casella(new Regina('D', 7, 3, mappa));
        mappa[7][4] = new Casella(new Re('R', 7, 4, mappa));
        mappa[7][5] = new Casella(new Alfiere('A', 7, 5, mappa));
        mappa[7][6] = new Casella(new Cavallo('C', 7, 6, mappa));
        mappa[7][7] = new Casella(new Torre('T', 7, 7, mappa));

        attaccaBianco = true;
        mosse = 0;
    }

    /**
     * Esegue la mossa del bot aggiornando anche patta, turno, cronologia ed en passant.
     * @param turnoBianco true se deve muovere il bot bianco, false per il bot nero
     */
    @Override
    public void muoviPezzoConBot(boolean turnoBianco) {
        if (turnoBianco != attaccaBianco) {
            throw new MossaNonValidaException("Non è il turno del bot.");
        }

        Casella[][] mappaPrima = mappa;
        Casella[][] migliore = bot.trovaPosizioneMigliore(turnoBianco);

        if (migliore == null || migliore == mappa) {
            return;
        }

        if (botHaMossoPedoneOCatturato(mappaPrima, migliore, turnoBianco)) {
            ultimaMossaPerPatta = mosse;
        }

        mappa = migliore;
        pedoneEnPassant = bot.getPedoneEnPassantMigliore();

        attaccaBianco = !attaccaBianco;
        dopoMossa(turnoBianco);
    }

    /**
     * Capisce se la mossa del bot è stata una mossa di pedone o una cattura, utile per la regola delle cinquanta mosse.
     * @param prima scacchiera prima della mossa
     * @param dopo scacchiera dopo la mossa
     * @param coloreBot colore che ha mosso
     * @return true se la mossa azzera il conteggio della patta per mosse
     */
    private boolean botHaMossoPedoneOCatturato(Casella[][] prima, Casella[][] dopo, boolean coloreBot) {
        int pezziPrima = 0;
        int pezziDopo = 0;

        boolean pedoneMosso = false;

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Pezzo pezzoPrima = prima[i][j].getPezzoContenuto();
                Pezzo pezzoDopo = dopo[i][j].getPezzoContenuto();

                if (pezzoPrima != null) {
                    pezziPrima++;
                }

                if (pezzoDopo != null) {
                    pezziDopo++;
                }

                if (pezzoPrima instanceof Pedone && pezzoPrima.isBianco() == coloreBot) {
                    if (pezzoDopo == null || pezzoDopo != pezzoPrima) {
                        pedoneMosso = true;
                    }
                }
            }
        }

        boolean cattura = pezziDopo < pezziPrima;

        return pedoneMosso || cattura;
    }

    /**
     * Ricostruisce il riferimento al pedone vulnerabile all'en passant partendo dalle coordinate salvate nel file.
     * @param riga riga del pedone
     * @param colonna colonna del pedone
     */
    private void impostaPedoneEnPassant(int riga, int colonna) {
        if (riga == -1 && colonna == -1) {
            pedoneEnPassant = null;
            return;
        }

        if (riga < 0 || riga >= 8 || colonna < 0 || colonna >= 8) {
            throw new FileNonValidoException("Coordinate EN_PASSANT fuori dalla scacchiera.");
        }

        Pezzo pezzo = mappa[riga][colonna].getPezzoContenuto();

        if (!(pezzo instanceof Pedone)) {
            throw new FileNonValidoException("EN_PASSANT indica una casella senza pedone.");
        }

        pedoneEnPassant = (Pedone) pezzo;
    }

    private boolean reHaMosso(boolean bianco) {
        int riga = bianco ? 7 : 0;
        Pezzo pezzo = mappa[riga][4].getPezzoContenuto();

        if (pezzo instanceof Re) {
            return ((Re) pezzo).haMosso();
        }

        return true;
    }

    private boolean torreHaMosso(boolean bianco, boolean latoLungo) {
        int riga = bianco ? 7 : 0;
        int colonna = latoLungo ? 0 : 7;

        Pezzo pezzo = mappa[riga][colonna].getPezzoContenuto();

        if (pezzo instanceof Torre) {
            return ((Torre) pezzo).haMosso();
        }

        return true;
    }

    /**
     * Ripristina dai dati del salvataggio lo stato di movimento di re e torri, necessario per l'arrocco.
     */
    private void impostaStatoArrocco(
            boolean reBiancoHaMosso,
            boolean torreBiancaLungaHaMosso,
            boolean torreBiancaCortaHaMosso,
            boolean reNeroHaMosso,
            boolean torreNeraLungaHaMosso,
            boolean torreNeraCortaHaMosso
    ) {
        impostaReHaMosso(true, reBiancoHaMosso);
        impostaTorreHaMosso(true, true, torreBiancaLungaHaMosso);
        impostaTorreHaMosso(true, false, torreBiancaCortaHaMosso);

        impostaReHaMosso(false, reNeroHaMosso);
        impostaTorreHaMosso(false, true, torreNeraLungaHaMosso);
        impostaTorreHaMosso(false, false, torreNeraCortaHaMosso);
    }

    private void impostaReHaMosso(boolean bianco, boolean haMosso) {
        int riga = bianco ? 7 : 0;
        Pezzo pezzo = mappa[riga][4].getPezzoContenuto();

        if (pezzo instanceof Re) {
            ((Re) pezzo).setHaMosso(haMosso);
        }
    }

    private void impostaTorreHaMosso(boolean bianco, boolean latoLungo, boolean haMosso) {
        int riga = bianco ? 7 : 0;
        int colonna = latoLungo ? 0 : 7;

        Pezzo pezzo = mappa[riga][colonna].getPezzoContenuto();

        if (pezzo instanceof Torre) {
            ((Torre) pezzo).setHaMosso(haMosso);
        }
    }

    /**
     * Controlla che nella mappa caricata siano presenti sia il re bianco sia il re nero.
     * @throws FileNonValidoException se uno dei due re manca
     */
    private void validaRePresenti() {
        if (trovaRe(true) == null) {
            throw new FileNonValidoException("Nel file manca il Re bianco.");
        }

        if (trovaRe(false) == null) {
            throw new FileNonValidoException("Nel file manca il Re nero.");
        }
    }

    /**
     * Carica da file una partita completa, inclusi turno, mosse, en passant, arrocco e bot.
     * @param percorsoFile percorso del file da caricare
     * @throws IOException se il file non può essere letto
     * @throws FileNonValidoException se il file non rispetta il formato previsto
     */
    public void caricaMappaDaFile(String percorsoFile) throws IOException {
        try (Scanner s = new Scanner(new File(percorsoFile))) {
            Casella[][] nuovaMappa = leggiScacchiera(s);

            if (!s.hasNextLine()) {
                throw new FileNonValidoException("Turno mancante nel file.");
            }

            this.mappa = nuovaMappa;
            this.attaccaBianco = leggiTurno(s.nextLine());

            if (!s.hasNextLine()) {
                throw new FileNonValidoException("Mosse mancanti nel file.");
            }

            try {
                this.mosse = Integer.parseInt(s.nextLine().trim());
            } catch (NumberFormatException e) {
                throw new FileNonValidoException("Numero mosse non valido.");
            }

            if (mosse < 0) {
                throw new FileNonValidoException("Il numero della mossa non può essere negativo.");
            }

            this.ultimaMossaPerPatta = 0;
            this.pedoneEnPassant = null;

            while (s.hasNextLine()) {
                String riga = s.nextLine().trim();

                if (riga.isBlank()) {
                    continue;
                }

                String[] parti = riga.split("\\s+");

                if (parti[0].equals("ULTIMA_MOSSA_PATTA")) {
                    if (parti.length != 2) {
                        throw new FileNonValidoException("Riga ULTIMA_MOSSA_PATTA non valida.");
                    }

                    try {
                        ultimaMossaPerPatta = Integer.parseInt(parti[1]);
                    } catch (NumberFormatException e) {
                        throw new FileNonValidoException("Valore ULTIMA_MOSSA_PATTA non valido.");
                    }
                }

                else if (parti[0].equals("EN_PASSANT")) {
                    if (parti.length != 3) {
                        throw new FileNonValidoException("Riga EN_PASSANT non valida.");
                    }

                    try {
                        int rigaPedone = Integer.parseInt(parti[1]);
                        int colonnaPedone = Integer.parseInt(parti[2]);
                        impostaPedoneEnPassant(rigaPedone, colonnaPedone);
                    } catch (NumberFormatException e) {
                        throw new FileNonValidoException("Coordinate EN_PASSANT non valide.");
                    }
                }

                else if (parti[0].equals("ARROCCO")) {
                    if (parti.length != 7) {
                        throw new FileNonValidoException("Riga ARROCCO non valida.");
                    }

                    impostaStatoArrocco(
                        Boolean.parseBoolean(parti[1]),
                        Boolean.parseBoolean(parti[2]),
                        Boolean.parseBoolean(parti[3]),
                        Boolean.parseBoolean(parti[4]),
                        Boolean.parseBoolean(parti[5]),
                        Boolean.parseBoolean(parti[6])
                    );
                }

                else if (parti[0].equals("BOT")) {
                    if (parti.length < 2 || parti.length > 3) {
                        throw new FileNonValidoException("Riga BOT non valida.");
                    }

                    if (parti[1].equals("false")) {
                        if (parti.length != 2) {
                            throw new FileNonValidoException("Riga BOT non valida.");
                        }

                        bot = null;
                    }

                    else if (parti[1].equals("true")) {
                        if (parti.length != 3) {
                            throw new FileNonValidoException("Difficoltà BOT mancante.");
                        }

                        try {
                            setBot(new Bot(Integer.parseInt(parti[2])));
                        } catch (NumberFormatException e) {
                            throw new FileNonValidoException("Difficoltà BOT non valida.");
                        }
                    }

                    else {
                        throw new FileNonValidoException("Valore BOT non valido.");
                    }
                }
            }

            validaRePresenti();
        }
    }

    /**
     * Salva su file lo stato completo della partita, compresi turno, mosse, en passant, arrocco e bot.
     * @param percorsoFile percorso del file di destinazione
     * @throws IOException se il file non può essere scritto
     */
    public void salvaSuFile(String percorsoFile) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(percorsoFile))) {
            for (int i = 0; i < mappa.length; i++) {
                for (int j = 0; j < mappa[i].length; j++) {
                    Casella casella = mappa[i][j];

                    if (casella != null && casella.getPezzoContenuto() != null) {
                        writer.print(casella.getPezzoContenuto().getNome());
                    } else {
                        writer.print(".");
                    }

                    if (j < mappa[i].length - 1) {
                        writer.print(" ");
                    }
                }
                writer.println();
            }

            writer.println(attaccaBianco ? "BIANCO" : "NERO");
            writer.println(mosse);

            writer.println("ULTIMA_MOSSA_PATTA " + ultimaMossaPerPatta);

            if (pedoneEnPassant == null) {
                writer.println("EN_PASSANT -1 -1");
            } else {
                writer.println("EN_PASSANT " + pedoneEnPassant.getRiga() + " " + pedoneEnPassant.getColonna());
            }

            writer.println(
                "ARROCCO "
                + reHaMosso(true) + " "
                + torreHaMosso(true, true) + " "
                + torreHaMosso(true, false) + " "
                + reHaMosso(false) + " "
                + torreHaMosso(false, true) + " "
                + torreHaMosso(false, false)
            );
            boolean conBot = (bot == null) ? false : true;
            writer.println("BOT " + conBot + ((conBot) ? " " + bot.getProfondita() : ""));
        }
    }

    /**
     * Esegue una mossa del giocatore aggiornando regole speciali, patta, turno e cronologia temporanea.
     * @param rigaPartenza riga di partenza
     * @param colonnaPartenza colonna di partenza
     * @param rigaArrivo riga di arrivo
     * @param colonnaArrivo colonna di arrivo
     * @throws MossaNonValidaException se la mossa non è legale
     */
    @Override
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

        if (pezzo instanceof Pedone || mappa[rigaArrivo][colonnaArrivo].getPezzoContenuto() != null) ultimaMossaPerPatta = mosse;
        pezzo.muovi(rigaArrivo, colonnaArrivo);

        pedoneEnPassant = null;
        if (pezzo instanceof Pedone pedone && distanzaRighe == 2) {
            pedoneEnPassant = pedone;
        }

        attaccaBianco = !attaccaBianco;
        dopoMossa(coloreCheHaMosso);
    }

    /**
     * Controlla la patta legata al numero di mosse senza mosse di pedone o catture.
     * @return true se la partita è patta per mosse
     */
    public boolean pattaPerMosse() {
        if (mosse - ultimaMossaPerPatta > 50) {
            return true;
        }
        return false;
    }

    /**
     * Controlla se la posizione attuale si è già ripetuta almeno tre volte nella cronologia.
     * @return true se la partita è patta per tripla ripetizione
     */
    public boolean pattaPerRipetizione() {
        if (percorsoDirectoryPartita == null) {
            return false;
        }

        File cartellaPartita = new File(percorsoDirectoryPartita);
        File[] fileMosse = cartellaPartita.listFiles();

        if (fileMosse == null) {
            return false;
        }

        String posizioneAttuale = creaChiavePosizioneAttuale();
        int ripetizioni = 0;

        for (int i = 0; i < fileMosse.length; i++) {
            File file = fileMosse[i];

            if (file.getName().startsWith("Mossa") && file.getName().endsWith(".txt")) {
                try {
                    String posizioneLetta = leggiChiavePosizioneDaFile(file);

                    if (posizioneAttuale.equals(posizioneLetta)) {
                        ripetizioni++;
                    }
                } catch (Exception e) {
                }
            }
        }

        return ripetizioni >= 3;
    }

    /**
     * Salva una posizione ridotta usata per cronologia e controllo della ripetizione.
     * @param percorsoFile file temporaneo da scrivere
     * @throws IOException se il file non può essere scritto
     */
    private void salvaSuFileTemporaneo(String percorsoFile) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(percorsoFile))) {
            for (int i = 0; i < mappa.length; i++) {
                for (int j = 0; j < mappa[i].length; j++) {
                    Casella casella = mappa[i][j];

                    if (casella != null && casella.getPezzoContenuto() != null) {
                        writer.print(casella.getPezzoContenuto().getNome());
                    } else {
                        writer.print(".");
                    }

                    if (j < mappa[i].length - 1) {
                        writer.print(" ");
                    }
                }
                writer.println();
            }

            writer.println(attaccaBianco ? "BIANCO" : "NERO");
            writer.println(getEnPassantPerRipetizione());
            writer.println(getArroccoPerRipetizione());
        }
    }

    /**
     * Crea una chiave testuale della posizione corrente considerando pezzi, turno, en passant e arrocco.
     * @return chiave univoca della posizione attuale
     */
    private String creaChiavePosizioneAttuale() {
        String chiave = "";

        for (int i = 0; i < mappa.length; i++) {
            for (int j = 0; j < mappa[i].length; j++) {
                Casella casella = mappa[i][j];

                if (casella != null && casella.getPezzoContenuto() != null) {
                    chiave += casella.getPezzoContenuto().getNome();
                } else {
                    chiave += ".";
                }
            }

            chiave += "/";
        }

        if (attaccaBianco) {
            chiave += "BIANCO";
        } else {
            chiave += "NERO";
        }

        chiave += "/";
        chiave += getEnPassantPerRipetizione();
        chiave += "/";
        chiave += getArroccoPerRipetizione();

        return chiave;
    }

    /**
     * Legge da un file temporaneo la chiave della posizione salvata.
     * @param file file temporaneo da leggere
     * @return chiave della posizione letta
     * @throws IOException se il file non può essere letto
     */
    private String leggiChiavePosizioneDaFile(File file) throws IOException {
        try (Scanner scanner = new Scanner(file)) {
            String chiave = "";

            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    chiave += scanner.next();
                }

                chiave += "/";
            }

            String turno = scanner.next();
            int rigaEnPassant = scanner.nextInt();
            int colonnaEnPassant = scanner.nextInt();

            String arrocco = "";

            for (int i = 0; i < 6; i++) {
                arrocco += scanner.next();

                if (i < 5) {
                    arrocco += " ";
                }
            }

            chiave += turno + "/";
            chiave += rigaEnPassant + " " + colonnaEnPassant + "/";
            chiave += arrocco;

            return chiave;
        }
    }

    /**
     * Restituisce la parte di chiave relativa all'en passant, considerando solo i casi realmente catturabili.
     * @return stringa usata nella chiave di ripetizione
     */
    private String getEnPassantPerRipetizione() {
        if (pedoneEnPassant == null || !esisteCatturaEnPassantPossibile()) {
            return "-1 -1";
        }

        return pedoneEnPassant.getRiga() + " " + pedoneEnPassant.getColonna();
    }

    /**
     * Controlla se il pedone vulnerabile all'en passant può essere davvero catturato dal colore di turno.
     * @return true se esiste una cattura en passant possibile
     */
    private boolean esisteCatturaEnPassantPossibile() {
        int rigaPedone = pedoneEnPassant.getRiga();
        int colonnaPedone = pedoneEnPassant.getColonna();
        int rigaArrivo;

        if (attaccaBianco) {
            rigaArrivo = rigaPedone - 1;
        } else {
            rigaArrivo = rigaPedone + 1;
        }

        if (rigaArrivo < 0 || rigaArrivo >= 8) {
            return false;
        }

        int[] spostamenti = {-1, 1};

        for (int i = 0; i < spostamenti.length; i++) {
            int colonnaPedoneCheCattura = colonnaPedone + spostamenti[i];

            if (colonnaPedoneCheCattura >= 0 && colonnaPedoneCheCattura < 8) {
                Pezzo pezzo = mappa[rigaPedone][colonnaPedoneCheCattura].getPezzoContenuto();

                if (pezzo instanceof Pedone && pezzo.isBianco() == attaccaBianco) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Restituisce la parte di chiave relativa ai diritti di arrocco dei due colori.
     * @return stringa usata nella chiave di ripetizione
     */
    private String getArroccoPerRipetizione() {
        return reHaMosso(true) + " "
                + torreHaMosso(true, true) + " "
                + torreHaMosso(true, false) + " "
                + reHaMosso(false) + " "
                + torreHaMosso(false, true) + " "
                + torreHaMosso(false, false);
    }

    /**
     * Salva la posizione corrente nella cartella temporanea della partita e aggiorna l'indice della cronologia.
     */
    private void salvaPerOgniRound() {
        File cartellaPrincipale = new File(CARTELLA_TEMPORANEI);

        try {
            if (!cartellaPrincipale.exists()) {
                cartellaPrincipale.mkdirs();
            }

            if (percorsoDirectoryPartita == null) {
                numeroPartita = trovaProssimoNumeroPartita();
                File cartellaPartita = new File(cartellaPrincipale + "/Partita" + numeroPartita);
                cartellaPartita.mkdirs();
                percorsoDirectoryPartita = cartellaPartita.getPath();
                numeroFileMossa = 0;
            }

            File fileMossa = new File(percorsoDirectoryPartita + "/Mossa" + numeroFileMossa + ".txt");
            salvaSuFileTemporaneo(fileMossa.getPath());
            numeroFileMossa++;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Collega una partita salvata realmente alla cartella temporanea usata per la sua cronologia.
     * @param percorsoFile file reale della partita salvata
     * @throws IOException se non è possibile preparare i temporanei
     */
    private void collegaTemporaneiAlFileReale(String percorsoFile) throws IOException {
        File fileReale = new File(percorsoFile);
        numeroPartita = leggiNumeroPartitaDaNome(fileReale.getName());

        File cartellaTemporanei = new File(CARTELLA_TEMPORANEI);
        if (!cartellaTemporanei.exists()) {
            cartellaTemporanei.mkdirs();
        }

        File cartellaPartita = new File(cartellaTemporanei, "Partita" + numeroPartita);
        if (!cartellaPartita.exists()) {
            cartellaPartita.mkdirs();
            percorsoDirectoryPartita = cartellaPartita.getPath();
            numeroFileMossa = 0;
            salvaPerOgniRound();
            return;
        }

        percorsoDirectoryPartita = cartellaPartita.getPath();
        numeroFileMossa = trovaProssimoNumeroMossa(cartellaPartita);
    }

    /**
     * Calcola il prossimo numero disponibile per creare una nuova cartella di temporanei.
     * @return numero della prossima partita temporanea
     */
    private int trovaProssimoNumeroPartita() {
        int numero = 1;

        while (true) {
            File fileReale = new File(CARTELLA_PARTITE, "Partita" + numero + ".txt");
            File cartellaTemporanea = new File(CARTELLA_TEMPORANEI, "Partita" + numero);

            if (!fileReale.exists() && !cartellaTemporanea.exists()) {
                return numero;
            }

            numero++;
        }
    }

    /**
     * Trova il prossimo numero di mossa disponibile in una cartella di cronologia.
     * @param cartellaPartita cartella contenente i file MossaX.txt
     * @return prossimo indice disponibile
     */
    private int trovaProssimoNumeroMossa(File cartellaPartita) {
        int prossimo = 0;
        File[] fileMosse = cartellaPartita.listFiles();

        if (fileMosse == null) {
            return prossimo;
        }

        for (File file : fileMosse) {
            if (!file.isFile()) {
                continue;
            }

            String nome = file.getName();
            if (!nome.startsWith("Mossa") || !nome.endsWith(".txt")) {
                continue;
            }

            try {
                int n = Integer.parseInt(nome.substring("Mossa".length(), nome.length() - ".txt".length()));
                if (n >= prossimo) {
                    prossimo = n + 1;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return prossimo;
    }

    /**
     * Estrae il numero della partita dal nome di un file di salvataggio.
     * @param nomeFile nome del file da analizzare
     * @return numero trovato, oppure -1 se non presente
     */
    private int leggiNumeroPartitaDaNome(String nomeFile) {
        if (!nomeFile.startsWith("Partita") || !nomeFile.endsWith(".txt")) {
            return trovaProssimoNumeroPartita();
        }

        try {
            return Integer.parseInt(nomeFile.substring("Partita".length(), nomeFile.length() - ".txt".length()));
        } catch (NumberFormatException e) {
            return trovaProssimoNumeroPartita();
        }
    }

    /**
     * Carica dalla cronologia temporanea una posizione precedente o successiva senza perdere il riferimento alla partita.
     * @param indiceMossa indice della mossa da caricare
     * @throws IOException se il file temporaneo non può essere letto
     */
    public void caricaMossaTemporanea(int indiceMossa) throws IOException {
        if (percorsoDirectoryPartita == null) {
            return;
        }

        File fileMossa = new File(percorsoDirectoryPartita, "Mossa" + indiceMossa + ".txt");
        if (!fileMossa.exists()) {
            throw new FileNonValidoException("Mossa non trovata nella cronologia.");
        }

        try (Scanner scanner = new Scanner(fileMossa)) {
            Casella[][] nuovaMappa = leggiScacchiera(scanner);

            if (!scanner.hasNextLine()) {
                throw new FileNonValidoException("Turno mancante nella mossa della cronologia.");
            }

            mappa = nuovaMappa;
            attaccaBianco = leggiTurno(scanner.nextLine());
            mosse = indiceMossa;
            numeroFileMossa = indiceMossa + 1;
            pedoneEnPassant = null;

            if (scanner.hasNextLine()) {
                String[] enPassant = scanner.nextLine().trim().split("\\s+");
                if (enPassant.length == 2) {
                    try {
                        impostaPedoneEnPassant(Integer.parseInt(enPassant[0]), Integer.parseInt(enPassant[1]));
                    } catch (NumberFormatException e) {
                        throw new FileNonValidoException("EN_PASSANT non valido nella cronologia.");
                    }
                }
            }

            if (scanner.hasNextLine()) {
                String[] arrocco = scanner.nextLine().trim().split("\\s+");
                if (arrocco.length == 6) {
                    impostaStatoArrocco(
                        Boolean.parseBoolean(arrocco[0]),
                        Boolean.parseBoolean(arrocco[1]),
                        Boolean.parseBoolean(arrocco[2]),
                        Boolean.parseBoolean(arrocco[3]),
                        Boolean.parseBoolean(arrocco[4]),
                        Boolean.parseBoolean(arrocco[5])
                    );
                }
            }

            validaRePresenti();
        }
    }

    /**
     * Riconosce nella cronologia se il movimento mostrato è un arrocco e restituisce il movimento del re.
     * @param prima posizione precedente
     * @param dopo posizione successiva
     * @param biancoCheHaMosso colore che ha mosso
     * @return coordinate del movimento del re oppure null
     */
    private int[] trovaMovimentoArroccoCronologia(char[][] prima, char[][] dopo, boolean biancoCheHaMosso) {
        int riga = biancoCheHaMosso ? 7 : 0;
        char re = biancoCheHaMosso ? 'R' : 'r';
        char torre = biancoCheHaMosso ? 'T' : 't';

        if (prima[riga][4] != re) {
            return new int[] {-1, -1, -1, -1};
        }

        if (prima[riga][7] == torre && dopo[riga][6] == re && dopo[riga][5] == torre) {
            return new int[] {riga, 4, riga, 6};
        }

        if (prima[riga][0] == torre && dopo[riga][2] == re && dopo[riga][3] == torre) {
            return new int[] {riga, 4, riga, 2};
        }

        return new int[] {-1, -1, -1, -1};
    }

    /**
     * Legge solo i caratteri dei pezzi da una mossa temporanea della cronologia.
     * @param indiceMossa indice della mossa da leggere
     * @return matrice 8x8 con i caratteri dei pezzi
     * @throws IOException se il file non può essere letto
     */
    private char[][] leggiPezziMossaTemporanea(int indiceMossa) throws IOException {
        File fileMossa = new File(percorsoDirectoryPartita, "Mossa" + indiceMossa + ".txt");
        if (!fileMossa.exists()) {
            throw new FileNonValidoException("Mossa non trovata nella cronologia.");
        }

        char[][] pezzi = new char[8][8];

        try (Scanner scanner = new Scanner(fileMossa)) {
            for (int riga = 0; riga < 8; riga++) {
                if (!scanner.hasNextLine()) {
                    throw new FileNonValidoException("Scacchiera non valida nella cronologia.");
                }

                String linea = scanner.nextLine().replace(" ", "").trim();
                if (linea.length() != 8) {
                    throw new FileNonValidoException("Riga scacchiera non valida nella cronologia.");
                }

                for (int colonna = 0; colonna < 8; colonna++) {
                    pezzi[riga][colonna] = linea.charAt(colonna);
                }
            }
        }

        return pezzi;
    }

    /**
     * Legge il turno salvato in una mossa temporanea della cronologia.
     * @param indiceMossa indice della mossa da leggere
     * @return true se il turno salvato è del bianco
     * @throws IOException se il file non può essere letto
     */
    private boolean leggiTurnoMossaTemporanea(int indiceMossa) throws IOException {
        File fileMossa = new File(percorsoDirectoryPartita, "Mossa" + indiceMossa + ".txt");
        if (!fileMossa.exists()) {
            throw new FileNonValidoException("Mossa non trovata nella cronologia.");
        }

        try (Scanner scanner = new Scanner(fileMossa)) {
            for (int i = 0; i < 8; i++) {
                if (!scanner.hasNextLine()) {
                    throw new FileNonValidoException("Scacchiera non valida nella cronologia.");
                }
                scanner.nextLine();
            }

            if (!scanner.hasNextLine()) {
                throw new FileNonValidoException("Turno mancante nella cronologia.");
            }

            return leggiTurno(scanner.nextLine());
        }
    }

    private boolean isPezzoDelColore(char pezzo, boolean bianco) {
        return bianco ? Character.isUpperCase(pezzo) : Character.isLowerCase(pezzo);
    }

    /**
     * Crea o aggiorna il file reale della partita e lo collega ai temporanei della cronologia.
     * @return file reale in cui è stata salvata la partita
     * @throws IOException se il salvataggio fallisce
     */
    public File salvaPartitaReale() throws IOException {
        File cartella = new File(CARTELLA_PARTITE);
        if (!cartella.exists()) {
            cartella.mkdirs();
        }

        if (numeroPartita < 1) {
            numeroPartita = trovaProssimoNumeroPartita();
        }

        File fileDaSalvare = new File(cartella, "Partita" + numeroPartita + ".txt");
        salvaSuFile(fileDaSalvare.getPath());

        partitaSalvata = true;
        salvataggioRealeEsistente = true;

        primaMossaTemporaneaNonSalvata = numeroFileMossa;

        return fileDaSalvare;
    }

    /**
     * Elimina la cartella temporanea della partita se l'utente chiude senza salvare.
     */
    public void eliminaTemporaneiSeNonSalvata() {
        if (partitaSalvata) {
            return;
        }

        if (percorsoDirectoryPartita == null) {
            return;
        }

        File cartellaPartita = new File(percorsoDirectoryPartita);

        if (!salvataggioRealeEsistente) {
            eliminaCartella(cartellaPartita);
            return;
        }

        eliminaMosseTemporaneeDa(cartellaPartita, primaMossaTemporaneaNonSalvata);
    }

    /**
     * Rimuove dalla cronologia tutte le mosse successive a quella indicata, quando si torna indietro e si gioca una nuova linea.
     * @param cartellaPartita cartella della cronologia
     * @param numeroIniziale prima mossa da eliminare
     */
    private void eliminaMosseTemporaneeDa(File cartellaPartita, int numeroIniziale) {
        File[] fileMosse = cartellaPartita.listFiles();
        if (fileMosse == null) {
            return;
        }

        for (File file : fileMosse) {
            if (!file.isFile()) {
                continue;
            }

            String nome = file.getName();
            if (!nome.startsWith("Mossa") || !nome.endsWith(".txt")) {
                continue;
            }

            try {
                int numero = Integer.parseInt(nome.substring("Mossa".length(), nome.length() - ".txt".length()));
                if (numero >= numeroIniziale) {
                    file.delete();
                }
            } catch (NumberFormatException ignored) {
            }
        }

        numeroFileMossa = trovaProssimoNumeroMossa(cartellaPartita);
    }

    /**
     * Elimina ricorsivamente una cartella o un file temporaneo.
     * @param file file o cartella da eliminare
     */
    private void eliminaCartella(File file) {
        if (file == null || !file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            File[] contenuto = file.listFiles();
            if (contenuto != null) {
                for (File f : contenuto) {
                    eliminaCartella(f);
                }
            }
        }

        file.delete();
    }

    /**
     * Aggiorna numero di mosse e salvataggio temporaneo dopo ogni mossa valida.
     * @param coloreCheHaMosso colore che ha appena mosso
     */
    @Override
    protected void dopoMossa(boolean coloreCheHaMosso) {
        mosse++;
        salvaPerOgniRound();

        partitaSalvata = false;
    }

    public int getMosse() {
        return mosse + 1;
    }
 
    public boolean isConBot() {
        return bot != null;
    }

    public int getIndiceCronologiaAttuale() {
        return numeroFileMossa - 1;
    }

    public int getUltimoIndiceCronologia() {
        if (percorsoDirectoryPartita == null) {
            return 0;
        }

        File cartellaPartita = new File(percorsoDirectoryPartita);
        return trovaProssimoNumeroMossa(cartellaPartita) - 1;
    }

    /**
     * Indica se la scacchiera mostrata corrisponde all'ultima posizione della cronologia.
     * @return true se si è sull'ultima mossa
     */
    public boolean isUltimaMossaCronologia() {
        return getIndiceCronologiaAttuale() == getUltimoIndiceCronologia();
    }

    /**
     * Ricostruisce il movimento eseguito tra due posizioni consecutive della cronologia.
     * @param indiceMossa indice della posizione di arrivo
     * @return array con riga/colonna di partenza e arrivo, oppure null se non individuato
     * @throws IOException se la cronologia non può essere letta
     */
    public int[] getMovimentoMossaCronologia(int indiceMossa) throws IOException {
        if (indiceMossa <= 0 || percorsoDirectoryPartita == null) {
            return new int[] {-1, -1, -1, -1};
        }

        char[][] prima = leggiPezziMossaTemporanea(indiceMossa - 1);
        char[][] dopo = leggiPezziMossaTemporanea(indiceMossa);
        boolean turnoDopo = leggiTurnoMossaTemporanea(indiceMossa);
        boolean biancoCheHaMosso = !turnoDopo;

        int[] arrocco = trovaMovimentoArroccoCronologia(prima, dopo, biancoCheHaMosso);
        if (arrocco[0] != -1) {
            return arrocco;
        }

        int rigaPartenza = -1;
        int colonnaPartenza = -1;
        int rigaArrivo = -1;
        int colonnaArrivo = -1;

        for (int riga = 0; riga < 8; riga++) {
            for (int colonna = 0; colonna < 8; colonna++) {
                char pezzoPrima = prima[riga][colonna];
                char pezzoDopo = dopo[riga][colonna];

                if (pezzoPrima == pezzoDopo) {
                    continue;
                }

                if (pezzoPrima != '.' && isPezzoDelColore(pezzoPrima, biancoCheHaMosso)) {
                    rigaPartenza = riga;
                    colonnaPartenza = colonna;
                }

                if (pezzoDopo != '.' && isPezzoDelColore(pezzoDopo, biancoCheHaMosso)) {
                    rigaArrivo = riga;
                    colonnaArrivo = colonna;
                }
            }
        }

        if (rigaPartenza == -1 || rigaArrivo == -1) {
            return new int[] {-1, -1, -1, -1};
        }

        return new int[] {rigaPartenza, colonnaPartenza, rigaArrivo, colonnaArrivo};
    }

}
