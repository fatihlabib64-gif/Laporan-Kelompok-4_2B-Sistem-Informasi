import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class SistemBioskop {
    private static final Scanner input = new Scanner(System.in);
    private static final String FILM_FILE = "films.csv";
    private static final String TRAN_FILE = "transactions.csv";
    private static final String APP_TITLE = "SISTEM PEMESANAN TIKET BIOSKOP";
    private static final int UI_WIDTH = 86;
    private static final int ROLE_KASIR = 1;
    private static final int ROLE_ADMIN = 2;
    private static final String PASSWORD_KASIR = "123456";
    private static final String PASSWORD_ADMIN = "654321";

    private static final boolean USE_COLOR = !"1".equals(System.getenv("NO_COLOR"));
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String BOLD = "\u001B[1m";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter
            .ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter
            .ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    private static final int ROWS = 5;
    private static final int COLS = 10;
    private static final String CANCEL = "C";
    private static final String BACK = "B";

    private static final List<Film> daftarFilm = new ArrayList<>();
    private static final List<Transaksi> daftarTransaksi = new ArrayList<>();

    // Menjalankan aplikasi dengan memuat data, membuka login, lalu masuk ke menu sesuai role.
    public static void main(String[] args) {
        loadData();
        if (daftarFilm.isEmpty()) {
            seedDataAwal();
            rebuildTicketStats();
            saveData();
        }
        reconcileSeatsFromTransactions();

        while (true) {
            int role = menuLogin();
            if (role == 0) {
                saveData();
                success("Data disimpan. Program selesai.");
                return;
            }
            menuUtama(role);
        }
    }

    // =========================================================
    // LOGIN
    // =========================================================
    // Menampilkan menu awal untuk memilih login kasir, login admin, atau keluar program.
    private static int menuLogin() {
        while (true) {
            printHeader("LOGIN " + APP_TITLE);
            printMenuOption("1", "Login Kasir");
            printMenuOption("2", "Login Admin");
            printMenuOption("0", "Keluar Program");

            String pilih = readTextWithCmd("Pilih menu login: ");
            switch (pilih) {
                case "1" -> {
                    if (prosesLogin("KASIR", PASSWORD_KASIR)) {
                        return ROLE_KASIR;
                    }
                }
                case "2" -> {
                    if (prosesLogin("ADMIN", PASSWORD_ADMIN)) {
                        return ROLE_ADMIN;
                    }
                }
                case "0" -> {
                    return 0;
                }
                default -> error("Pilihan login tidak valid.");
            }
        }
    }

    // Memvalidasi password role yang dipilih dan memberi kesempatan kembali ke menu login.
    private static boolean prosesLogin(String roleName, String passwordBenar) {
        while (true) {
            printHeader("LOGIN " + roleName);
            info("Password harus berupa angka, maksimal 6 digit.");
            String password = readTextWithCmd("Masukkan password [B kembali]: ");
            if (isBack(password)) {
                return false;
            }
            if (!validPasswordFormat(password)) {
                error("Password hanya boleh berisi angka dengan panjang 1 sampai 6 digit.");
                continue;
            }
            if (password.equals(passwordBenar)) {
                success("Login " + roleName.toLowerCase(Locale.ROOT) + " berhasil.");
                return true;
            }
            error("Password salah.");
        }
    }

    // Menampilkan menu utama sesuai hak akses kasir atau admin.
    private static void menuUtama(int role) {
        while (true) {
            printHeader(APP_TITLE + " - " + namaRole(role));
            if (role == ROLE_ADMIN) {
                printMenuOption("1", "Kelola Film");
            } else {
                printMenuOption("1", "Lihat Daftar Film");
            }
            printMenuOption("2", "Transaksi Tiket Baru");
            printMenuOption("3", "Riwayat Transaksi");
            printMenuOption("4", "Laporan Transaksi");
            printMenuOption("5", "Cari / Urut Film");
            printMenuOption("0", "Kembali ke Menu Login");

            String pilih = readTextWithCmd("Pilih menu: ");
            switch (pilih) {
                case "1" -> {
                    if (role == ROLE_ADMIN) {
                        menuKelolaFilm();
                    } else {
                        tampilkanDaftarFilm(true);
                    }
                }
                case "2" -> transaksiTiketBaru();
                case "3" -> menuRiwayatTransaksi();
                case "4" -> menuLaporan();
                case "5" -> menuCariUrutFilm();
                case "0" -> {
                    saveData();
                    warn("Kembali ke menu login.");
                    return;
                }
                default -> error("Pilihan tidak valid.");
            }
        }
    }

    // Mengubah kode role menjadi label yang tampil pada header menu.
    private static String namaRole(int role) {
        if (role == ROLE_ADMIN) return "ADMIN";
        return "KASIR";
    }

    // =========================================================
    // MENU KELOLA FILM
    // =========================================================
    // Menampilkan menu admin untuk mengelola data film dan jadwal tayang.
    private static void menuKelolaFilm() {
        while (true) {
            printHeader("KELOLA FILM");
            printMenuOption("1", "Lihat Daftar Film");
            printMenuOption("2", "Tambah Film");
            printMenuOption("3", "Edit Film");
            printMenuOption("4", "Hapus Film");
            printMenuOption("5", "Kelola Jadwal Tayang");
            printMenuOption("6", "Lihat Semua Film Termasuk Nonaktif");
            printMenuOption("0", "Kembali");

            String pilih = readTextWithCmd("Pilih menu: ");
            switch (pilih) {
                case "1" -> tampilkanDaftarFilm(true);
                case "2" -> tambahFilm();
                case "3" -> editFilm();
                case "4" -> hapusFilm();
                case "5" -> kelolaJadwalTayang();
                case "6" -> tampilkanDaftarFilm(false);
                case "0" -> { return; }
                default -> error("Pilihan tidak valid.");
            }
        }
    }

    // =========================================================
    // RIWAYAT & LAPORAN
    // =========================================================
    // Menampilkan submenu riwayat transaksi dan pencarian invoice berdasarkan ID.
    private static void menuRiwayatTransaksi() {
        while (true) {
            printHeader("RIWAYAT TRANSAKSI");
            printMenuOption("1", "Lihat Semua Transaksi");
            printMenuOption("2", "Cari Transaksi by ID");
            printMenuOption("0", "Kembali");

            String pilih = readTextWithCmd("Pilih menu: ");
            switch (pilih) {
                case "1" -> tampilkanRiwayatTransaksi();
                case "2" -> cariTransaksiById();
                case "0" -> { return; }
                default -> error("Pilihan tidak valid.");
            }
        }
    }

    // Menghitung total transaksi, tiket, pendapatan, dan film terlaris.
    private static void menuLaporan() {
        printHeader("LAPORAN TRANSAKSI");

        int totalTransaksi = daftarTransaksi.size();
        int totalTiket = 0;
        double totalPendapatan = 0;

        Map<String, Integer> tiketPerFilm = new HashMap<>();
        for (Transaksi t : daftarTransaksi) {
            totalTiket += t.jumlahTiket;
            totalPendapatan += t.totalBayar;
            tiketPerFilm.put(t.kodeFilm, tiketPerFilm.getOrDefault(t.kodeFilm, 0) + t.jumlahTiket);
        }

        Film filmTerlaris = null;
        int maxTiket = 0;
        for (Film f : daftarFilm) {
            int jumlah = tiketPerFilm.getOrDefault(f.kodeFilm, 0);
            if (filmTerlaris == null || jumlah > maxTiket) {
                filmTerlaris = f;
                maxTiket = jumlah;
            }
        }

        System.out.println(label("Total transaksi") + totalTransaksi);
        System.out.println(label("Total tiket") + totalTiket);
        System.out.println(label("Pendapatan") + "Rp" + formatRupiah(totalPendapatan));
        if (filmTerlaris != null && maxTiket > 0) {
            System.out.println(label("Film terlaris") + filmTerlaris.judul + " (" + maxTiket + " tiket)");
        } else {
            System.out.println(label("Film terlaris") + "belum ada data.");
        }
    }

    // Menampilkan menu pencarian dan sorting film aktif tanpa library sorting/searching.
    private static void menuCariUrutFilm() {
        while (true) {
            printHeader("CARI / URUT FILM");
            tampilkanDaftarFilm(true);
            printMenuOption("1", "Cari film berdasarkan kode atau judul");
            printMenuOption("2", "Cari film berdasarkan genre");
            printMenuOption("3", "Urutkan film aktif berdasarkan kode");
            printMenuOption("4", "Urutkan film aktif berdasarkan judul");
            printMenuOption("5", "Urutkan film aktif berdasarkan tiket terjual");
            printMenuOption("0", "Kembali");

            String pilih = readTextWithCmd("Pilih menu: ");
            switch (pilih) {
                case "1" -> cariFilmByKeyword();
                case "2" -> cariFilmByGenre();
                case "3" -> tampilkanFilmTerurut("URUT KODE FILM", 1);
                case "4" -> tampilkanFilmTerurut("URUT JUDUL FILM", 2);
                case "5" -> tampilkanFilmTerurut("URUT TIKET TERJUAL", 3);
                case "0" -> { return; }
                default -> error("Pilihan tidak valid.");
            }
        }
    }

    // Searching film aktif berdasarkan kode atau judul menggunakan Linear Search manual.
    private static void cariFilmByKeyword() {
        String keyword = readTextWithCmd("Masukkan kata kunci [C batal]: ");
        if (isCancel(keyword) || isBack(keyword)) return;
        String key = keyword.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) {
            warn("Kata kunci tidak boleh kosong.");
            return;
        }

        List<Film> hasil = new ArrayList<>();
        for (Film film : daftarFilm) {
            if (!film.aktif) continue;
            boolean cocok = containsIgnoreCaseManual(film.kodeFilm, key)
                    || containsIgnoreCaseManual(film.judul, key);
            if (cocok) hasil.add(film);
        }
        System.out.println("----------------------------------- HASIL PENCARIAN ----------------------------------");
        tampilkanDaftarFilm(hasil, true);
    }

    // Searching film aktif berdasarkan genre menggunakan Linear Search manual.
    private static void cariFilmByGenre() {
        Map<String, Integer> genreCount = new LinkedHashMap<>();
        for (Film film : daftarFilm) {
            if (!film.aktif) continue;
            genreCount.put(film.genre, genreCount.getOrDefault(film.genre, 0) + 1);
        }
        if (genreCount.isEmpty()) {
            warn("Belum ada film aktif.");
            return;
        }

        System.out.println("----------------------------------- GENRE TERSEDIA -----------------------------------");
        int no = 1;
        for (Map.Entry<String, Integer> entry : genreCount.entrySet()) {
            System.out.printf("%2d. %-20s %d film%n", no++, entry.getKey(), entry.getValue());
        }

        String genre = readTextWithCmd("Masukkan genre [C batal]: ");
        if (isCancel(genre) || isBack(genre)) return;
        String key = genre.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) {
            warn("Genre tidak boleh kosong.");
            return;
        }

        List<Film> hasil = new ArrayList<>();
        for (Film film : daftarFilm) {
            if (film.aktif && containsIgnoreCaseManual(film.genre, key)) {
                hasil.add(film);
            }
        }
        System.out.println("------------------------------- HASIL PENCARIAN GENRE --------------------------------");
        tampilkanDaftarFilm(hasil, true);
    }

    // Menyalin film aktif lalu mengurutkannya secara manual sesuai mode pilihan.
    private static void tampilkanFilmTerurut(String judul, int modeUrut) {
        List<Film> hasil = new ArrayList<>();
        for (Film film : daftarFilm) {
            if (film.aktif) hasil.add(film);
        }
        urutkanFilmManual(hasil, modeUrut);
        if ("URUT KODE FILM".equals(judul)) {
            System.out.println("----------------------------------- URUT KODE FILM -----------------------------------");
        } else if ("URUT JUDUL FILM".equals(judul)) {
            System.out.println("----------------------------------- URUT JUDUL FILM ----------------------------------");
        } else if ("URUT TIKET TERJUAL".equals(judul)) {
            System.out.println("----------------------------------- URUT TIKET FILM ----------------------------------");
        } else {
            System.out.println("------------------------- " + judul + " " + repeat("-", Math.max(1, UI_WIDTH - judul.length() - 4)));
        }
        tampilkanDaftarFilm(hasil, true);
    }

    // Sorting film menggunakan Selection Sort manual.
    private static void urutkanFilmManual(List<Film> data, int modeUrut) {
        for (int i = 0; i < data.size() - 1; i++) {
            int indeksPilihan = i;
            for (int j = i + 1; j < data.size(); j++) {
                if (perluTukarUrutan(data.get(indeksPilihan), data.get(j), modeUrut)) {
                    indeksPilihan = j;
                }
            }
            if (indeksPilihan != i) {
                Film sementara = data.get(i);
                data.set(i, data.get(indeksPilihan));
                data.set(indeksPilihan, sementara);
            }
        }
    }

    // Menentukan prioritas pertukaran data untuk setiap mode sorting film.
    private static boolean perluTukarUrutan(Film filmSaatIni, Film filmKandidat, int modeUrut) {
        if (modeUrut == 1) {
            return bandingkanTextManual(filmSaatIni.kodeFilm, filmKandidat.kodeFilm) > 0;
        }
        if (modeUrut == 2) {
            return bandingkanTextManual(filmSaatIni.judul, filmKandidat.judul) > 0;
        }
        if (modeUrut == 3) {
            if (filmKandidat.tiketTerjual != filmSaatIni.tiketTerjual) {
                return filmKandidat.tiketTerjual > filmSaatIni.tiketTerjual;
            }
            return bandingkanTextManual(filmSaatIni.judul, filmKandidat.judul) > 0;
        }
        return false;
    }

    // Membandingkan dua teks secara manual tanpa helper sorting bawaan.
    private static int bandingkanTextManual(String kiri, String kanan) {
        String a = kiri == null ? "" : kiri.toLowerCase(Locale.ROOT);
        String b = kanan == null ? "" : kanan.toLowerCase(Locale.ROOT);
        int batas = Math.min(a.length(), b.length());
        for (int i = 0; i < batas; i++) {
            char ca = a.charAt(i);
            char cb = b.charAt(i);
            if (ca != cb) {
                return ca - cb;
            }
        }
        return a.length() - b.length();
    }

    // Searching substring secara manual untuk menggantikan contains.
    private static boolean containsIgnoreCaseManual(String sumber, String kataKunci) {
        if (sumber == null || kataKunci == null) return false;
        String teks = sumber.toLowerCase(Locale.ROOT);
        String dicari = kataKunci.toLowerCase(Locale.ROOT);
        if (dicari.isEmpty()) return true;
        if (dicari.length() > teks.length()) return false;

        for (int i = 0; i <= teks.length() - dicari.length(); i++) {
            boolean cocok = true;
            for (int j = 0; j < dicari.length(); j++) {
                if (teks.charAt(i + j) != dicari.charAt(j)) {
                    cocok = false;
                    break;
                }
            }
            if (cocok) return true;
        }
        return false;
    }

    // =========================================================
    // TRANSAKSI TIKET BARU
    // =========================================================
    // Mengatur alur transaksi tiket dari pilih film sampai invoice pembayaran.
    private static void transaksiTiketBaru() {
        if (!adaFilmAktif()) {
            warn("Tidak ada film aktif.");
            return;
        }

        printHeader("TRANSAKSI TIKET BARU");

        String tanggal = readTanggal("Masukkan tanggal transaksi (dd/MM/yyyy) [C batal]: ");
        if (tanggal == null) return;

        Film film = null;
        JadwalTayang jadwal = null;
        int jumlahTiket = 0;
        List<String> kursiDipilih = new ArrayList<>();

        int step = 1;
        while (true) {
            if (step == 1) {
                tampilkanDaftarFilm(true);
                String kode = readTextWithCmd("Pilih kode film [C batal]: ");
                if (isCancel(kode)) {
                    warn("Transaksi dibatalkan.");
                    return;
                }
                film = cariFilmByKodeAktif(kode);
                if (film == null) {
                    error("Film tidak ditemukan atau tidak aktif.");
                    continue;
                }
                if (film.jadwal.isEmpty()) {
                    warn("Film ini belum memiliki jadwal tayang.");
                    continue;
                }
                step = 2;
                continue;
            }

            if (step == 2) {
                tampilkanJadwalFilm(film);
                String pil = readTextWithCmd("Pilih nomor jadwal [B kembali, C batal]: ");
                if (isCancel(pil)) {
                    warn("Transaksi dibatalkan.");
                    return;
                }
                if (isBack(pil)) {
                    step = 1;
                    continue;
                }
                Integer idx = parseIntOrNull(pil);
                if (idx == null || idx < 1 || idx > film.jadwal.size()) {
                    error("Pilihan jadwal tidak valid.");
                    continue;
                }
                jadwal = film.jadwal.get(idx - 1);
                step = 3;
                continue;
            }

            if (step == 3) {
                int tersedia = hitungKursiTersedia(jadwal);
                String q = readTextWithCmd("Masukkan jumlah tiket [B kembali, C batal]: ");
                if (isCancel(q)) {
                    warn("Transaksi dibatalkan.");
                    return;
                }
                if (isBack(q)) {
                    step = 2;
                    continue;
                }
                Integer qty = parseIntOrNull(q);
                if (qty == null || qty < 1) {
                    error("Jumlah tiket harus minimal 1.");
                    continue;
                }
                if (qty > tersedia) {
                    warn("Kursi tersedia hanya " + tersedia + ".");
                    continue;
                }
                jumlahTiket = qty;
                kursiDipilih.clear();
                step = 4;
                continue;
            }

            if (step == 4) {
                while (kursiDipilih.size() < jumlahTiket) {
                    tampilkanDenahKursi(jadwal, kursiDipilih);
                    System.out.println("Kursi sementara : " + (kursiDipilih.isEmpty() ? "-" : String.join(", ", kursiDipilih)));
                    String seat = readTextWithCmd("Pilih kursi ke-" + (kursiDipilih.size() + 1) + " [B kembali, C batal]: ");
                    if (isCancel(seat)) {
                        warn("Transaksi dibatalkan.");
                        return;
                    }
                    if (isBack(seat)) {
                        kursiDipilih.clear();
                        step = 3;
                        break;
                    }
                    seat = seat.toUpperCase(Locale.ROOT);
                    if (!validSeatCode(seat)) {
                        error("Format kursi salah. Contoh: A1, C10.");
                        continue;
                    }
                    if (jadwal.isOccupied(seat) || kursiSudahDipilih(kursiDipilih, seat)) {
                        error("Kursi sudah terisi atau sudah dipilih.");
                        continue;
                    }
                    kursiDipilih.add(seat);
                }
                if (step == 3) continue;
                step = 5;
                continue;
            }

            if (step == 5) {
                double hargaPerTiket = film.hargaTiket;
                double total = hargaPerTiket * jumlahTiket;
                LocalTime jamMulai = jadwal.getJamMulai();
                LocalTime jamSelesai = jadwal.getJamSelesai(film.durasiMenit);

                System.out.println();
                System.out.println("-------------------------------- RINGKASAN TRANSAKSI ---------------------------------");
                System.out.println(label("Tanggal") + tanggal);
                System.out.println(label("Film") + film.judul);
                System.out.println(label("Genre") + film.genre);
                System.out.println(label("Studio") + film.studio);
                System.out.println(label("Jam mulai") + jamMulai.format(TIME_FMT));
                System.out.println(label("Jam selesai") + jamSelesai.format(TIME_FMT));
                System.out.println(label("Jumlah tiket") + jumlahTiket);
                System.out.println(label("Kursi") + String.join(", ", kursiDipilih));
                System.out.println(label("Harga/tiket") + "Rp" + formatRupiah(hargaPerTiket));
                System.out.println(label("Total") + color("Rp" + formatRupiah(total), GREEN + BOLD));
                System.out.println();

                String lanjut = readTextWithCmd("Tekan ENTER untuk lanjut ke pembayaran, atau [B kembali, C batal]: ");
                if (isCancel(lanjut)) {
                    warn("Transaksi dibatalkan.");
                    return;
                }
                if (isBack(lanjut)) {
                    kursiDipilih.clear();
                    step = 4;
                    continue;
                }
                step = 6;
                continue;
            }

            if (step == 6) {
                double hargaPerTiket = film.hargaTiket;
                double total = hargaPerTiket * jumlahTiket;
                double uangBayar;
                while (true) {
                    String bayarTxt = readTextWithCmd("Masukkan uang pembayaran [B kembali, C batal]: Rp");
                    if (isCancel(bayarTxt)) {
                        warn("Transaksi dibatalkan.");
                        return;
                    }
                    if (isBack(bayarTxt)) {
                        step = 5;
                        break;
                    }
                    Double bayar = parseDoubleOrNull(bayarTxt);
                    if (bayar == null || bayar < total) {
                        error("Uang tidak mencukupi. Minimal Rp" + formatRupiah(total));
                        continue;
                    }
                    uangBayar = bayar;
                    double kembalian = uangBayar - total;
                    step = 7;

                    String idTrans = generateNextTransactionId();
                    String jamMulaiStr = jadwal.getJamMulai().format(TIME_FMT);
                    String jamSelesaiStr = jadwal.getJamSelesai(film.durasiMenit).format(TIME_FMT);
                    Transaksi trx = new Transaksi(
                            idTrans,
                            tanggal,
                            film.kodeFilm,
                            film.judul,
                            film.genre,
                            film.studio,
                            jamMulaiStr,
                            jamSelesaiStr,
                            film.durasiMenit,
                            hargaPerTiket,
                            jumlahTiket,
                            new ArrayList<>(kursiDipilih),
                            total,
                            uangBayar,
                            kembalian
                    );
                    daftarTransaksi.add(trx);
                    for (String seat : kursiDipilih) {
                        jadwal.occupy(seat);
                    }
                    film.tiketTerjual += jumlahTiket;
                    saveData();
                    System.out.println();
                    printInvoice(trx);
                    success("Pembayaran berhasil. Kursi sudah dikunci.");
                    return;
                }
                if (step == 5) continue;
            }

            // step 7 is finalization only; no cancel options
        }
    }

    // =========================================================
    // KELOLA FILM
    // =========================================================
    // Menampilkan daftar film dari data utama sesuai status aktif yang diminta.
    private static void tampilkanDaftarFilm(boolean aktifSaja) {
        tampilkanDaftarFilm(daftarFilm, aktifSaja);
    }

    // Menampilkan daftar film dari sumber data tertentu dalam format tabel.
    private static void tampilkanDaftarFilm(List<Film> sumberFilm, boolean aktifSaja) {
        System.out.println(MAGENTA + "-------------------------------------DAFTAR FILM--------------------------------------" + RESET);
        if (sumberFilm.isEmpty()) {
            warn("Belum ada film.");
            return;
        }

        int no = 1;
        boolean adaData = false;
        System.out.printf("%-3s %-6s %-28s %-12s %-7s %-7s %-12s%n",
                "No", "Kode", "Judul", "Genre", "Studio", "Durasi", "Status");
        System.out.println(repeat("-", UI_WIDTH));
        for (Film film : sumberFilm) {
            if (aktifSaja && !film.aktif) continue;
            adaData = true;
            System.out.printf("%-3d %-6s %-28s %-12s %-7d %-7s %-12s%n",
                    no++,
                    film.kodeFilm,
                    truncate(film.judul, 28),
                    truncate(film.genre, 12),
                    film.studio,
                    film.durasiMenit + "m",
                    film.aktif ? "Aktif" : "Nonaktif");
            System.out.println("    Harga   : Rp" + formatRupiah(film.hargaTiket)
                    + " | Terjual: " + film.tiketTerjual + " tiket");
            if (!film.jadwal.isEmpty()) {
                System.out.print("    Jadwal  : ");
                for (int i = 0; i < film.jadwal.size(); i++) {
                    JadwalTayang j = film.jadwal.get(i);
                    System.out.print(j.jamMulai + " (" + hitungKursiTersedia(j) + " kursi)");
                    if (i < film.jadwal.size() - 1) System.out.print(", ");
                }
                System.out.println();
            } else {
                System.out.println("    Jadwal  : belum ada");
            }
            System.out.println();
        }
        if (!adaData) {
            warn("Tidak ada film yang sesuai filter.");
        }
    }

    // Menambahkan film baru beserta jadwal tayang awal.
    private static void tambahFilm() {
        printHeader("TAMBAH FILM");

        String kode;
        while (true) {
            kode = readTextWithCmd("Kode film (format F001) [C batal]: ");
            if (isCancel(kode) || isBack(kode)) return;
            if (!validFilmId(kode)) {
                error("Format kode film harus F diikuti 3 digit, contoh F001.");
                continue;
            }
            if (cariFilmByKode(kode) != null) {
                error("Kode film sudah dipakai.");
                continue;
            }
            break;
        }

        String judul = readNonEmptyWithBackCancel("Judul film [B kembali, C batal]: ");
        if (judul == null) return;

        String genre = readNonEmptyWithBackCancel("Genre film [B kembali, C batal]: ");
        if (genre == null) return;

        Integer studio = readIntWithBackCancel("Studio [B kembali, C batal]: ", 1, 99);
        if (studio == null) return;

        Integer durasi = readIntWithBackCancel("Durasi film (menit) [B kembali, C batal]: ", 1, 1000);
        if (durasi == null) return;

        Double harga = readDoubleWithBackCancel("Harga tiket [B kembali, C batal]: ", 1, 1000000000);
        if (harga == null) return;

        Integer jumlahJadwal = readIntWithBackCancel("Jumlah jadwal tayang (1-5) [B kembali, C batal]: ", 1, 5);
        if (jumlahJadwal == null) return;

        List<JadwalTayang> jadwalBaru = new ArrayList<>();
        int idx = 1;
        while (idx <= jumlahJadwal) {
            String waktu = readTextWithCmd("Jam mulai jadwal ke-" + idx + " (HH:mm) [B kembali, C batal]: ");
            if (isCancel(waktu)) return;
            if (isBack(waktu)) {
                if (!jadwalBaru.isEmpty()) {
                    jadwalBaru.remove(jadwalBaru.size() - 1);
                    idx--;
                } else {
                    return;
                }
                continue;
            }
            if (!validTime(waktu)) {
                error("Format jam harus HH:mm, contoh 19:00.");
                continue;
            }
            if (jadwalSudahAda(jadwalBaru, waktu)) {
                error("Jam sudah dimasukkan.");
                continue;
            }
            jadwalBaru.add(new JadwalTayang(waktu));
            idx++;
        }

        Film film = new Film(kode.toUpperCase(Locale.ROOT), judul, genre, studio, durasi, harga, true);
        film.jadwal.addAll(jadwalBaru);
        daftarFilm.add(film);
        saveData();
        success("Film berhasil ditambahkan.");
    }

    // Mengubah data film aktif melalui salinan sementara sebelum disimpan.
    private static void editFilm() {
        printHeader("EDIT FILM");
        tampilkanDaftarFilm(true);

        String kode = readTextWithCmd("Masukkan kode film yang diedit [C batal]: ");
        if (isCancel(kode) || isBack(kode)) return;
        Film film = cariFilmByKodeAktif(kode);
        if (film == null) {
            error("Film tidak ditemukan atau tidak aktif.");
            return;
        }

        Film temp = film.copy();
        while (true) {
            System.out.println();
            System.out.println("--------------------------------- DATA FILM SAAT INI ---------------------------------");
            printMenuOption("1", "Judul   : " + temp.judul);
            printMenuOption("2", "Genre   : " + temp.genre);
            printMenuOption("3", "Studio  : " + temp.studio);
            printMenuOption("4", "Durasi  : " + temp.durasiMenit + " menit");
            printMenuOption("5", "Harga   : Rp" + formatRupiah(temp.hargaTiket));
            printMenuOption("6", "Jadwal  : " + temp.jadwal.size() + " jadwal");
            printMenuOption("7", "Simpan perubahan");
            printMenuOption("0", "Batal edit");
            String pilih = readTextWithCmd("Pilih field yang ingin diubah: ");
            if (isCancel(pilih)) return;

            switch (pilih) {
                case "1" -> {
                    String v = readNonEmptyWithBackCancel("Judul baru [B kembali, C batal]: ");
                    if (v == null) return;
                    temp.judul = v;
                }
                case "2" -> {
                    String v = readNonEmptyWithBackCancel("Genre baru [B kembali, C batal]: ");
                    if (v == null) return;
                    temp.genre = v;
                }
                case "3" -> {
                    Integer v = readIntWithBackCancel("Studio baru [B kembali, C batal]: ", 1, 99);
                    if (v == null) return;
                    temp.studio = v;
                }
                case "4" -> {
                    Integer v = readIntWithBackCancel("Durasi baru (menit) [B kembali, C batal]: ", 1, 1000);
                    if (v == null) return;
                    temp.durasiMenit = v;
                }
                case "5" -> {
                    Double v = readDoubleWithBackCancel("Harga baru [B kembali, C batal]: ", 1, 1000000000);
                    if (v == null) return;
                    temp.hargaTiket = v;
                }
                case "6" -> editJadwalFilm(temp, false);
                case "7" -> {
                    film.judul = temp.judul;
                    film.genre = temp.genre;
                    film.studio = temp.studio;
                    film.durasiMenit = temp.durasiMenit;
                    film.hargaTiket = temp.hargaTiket;
                    film.jadwal = temp.jadwal;
                    saveData();
                    success("Perubahan film berhasil disimpan.");
                    return;
                }
                case "0" -> {
                    warn("Edit dibatalkan.");
                    return;
                }
                default -> error("Pilihan tidak valid.");
            }
        }
    }

    // Membuka editor jadwal dengan penyimpanan otomatis.
    private static void editJadwalFilm(Film film) {
        editJadwalFilm(film, true);
    }

    // Mengelola tambah, ubah, dan hapus jadwal tayang dengan validasi kursi terisi.
    private static void editJadwalFilm(Film film, boolean autoSave) {
        while (true) {
            System.out.println();
            System.out.println("------------------------------------ JADWAL FILM -------------------------------------");
            if (film.jadwal.isEmpty()) {
                System.out.println("Belum ada jadwal.");
            } else {
                for (int i = 0; i < film.jadwal.size(); i++) {
                    JadwalTayang j = film.jadwal.get(i);
                    System.out.println((i + 1) + ". " + j.jamMulai + " - "
                            + j.getJamSelesai(film.durasiMenit).format(TIME_FMT)
                            + " | Terisi: " + j.countOccupied() + "/" + (ROWS * COLS));
                }
            }
            if (!autoSave) {
                info("Perubahan jadwal di mode edit film disimpan setelah memilih 'Simpan perubahan'.");
            }
            printMenuOption("1", "Tambah jadwal");
            printMenuOption("2", "Ubah jadwal kosong");
            printMenuOption("3", "Hapus jadwal kosong");
            printMenuOption("0", "Kembali");
            String pilih = readTextWithCmd("Pilih menu: ");
            if (isCancel(pilih) || "0".equals(pilih)) return;

            switch (pilih) {
                case "1" -> {
                    String waktu = readTextWithCmd("Jam mulai baru (HH:mm) [B kembali, C batal]: ");
                    if (isCancel(waktu) || isBack(waktu)) continue;
                    if (!validTime(waktu)) {
                        error("Format jam harus HH:mm.");
                        continue;
                    }
                    boolean duplikat = jadwalSudahAda(film.jadwal, waktu);
                    if (duplikat) {
                        error("Jam sudah ada.");
                        continue;
                    }
                    film.jadwal.add(new JadwalTayang(waktu));
                    saveIfNeeded(autoSave);
                    success(autoSave ? "Jadwal berhasil ditambahkan." : "Jadwal sementara berhasil ditambahkan.");
                }
                case "2" -> {
                    if (film.jadwal.isEmpty()) {
                        warn("Tidak ada jadwal untuk diubah.");
                        continue;
                    }
                    Integer idx = readIntWithBackCancel("Nomor jadwal yang diubah [B kembali, C batal]: ", 1, film.jadwal.size());
                    if (idx == null) return;
                    JadwalTayang target = film.jadwal.get(idx - 1);
                    if (target.countOccupied() > 0) {
                        warn("Jadwal sudah memiliki kursi terisi, jadi tidak boleh diubah.");
                        continue;
                    }
                    String waktu = readTextWithCmd("Jam baru (HH:mm) [B kembali, C batal]: ");
                    if (isCancel(waktu) || isBack(waktu)) continue;
                    if (!validTime(waktu)) {
                        error("Format jam harus HH:mm.");
                        continue;
                    }
                    boolean duplikat = false;
                    for (int i = 0; i < film.jadwal.size(); i++) {
                        if (i != idx - 1 && film.jadwal.get(i).jamMulai.equals(waktu)) {
                            duplikat = true;
                            break;
                        }
                    }
                    if (duplikat) {
                        error("Jam sudah ada.");
                        continue;
                    }
                    target.jamMulai = waktu;
                    saveIfNeeded(autoSave);
                    success(autoSave ? "Jadwal berhasil diubah." : "Jadwal sementara berhasil diubah.");
                }
                case "3" -> {
                    if (film.jadwal.isEmpty()) {
                        warn("Tidak ada jadwal untuk dihapus.");
                        continue;
                    }
                    Integer idx = readIntWithBackCancel("Nomor jadwal yang dihapus [B kembali, C batal]: ", 1, film.jadwal.size());
                    if (idx == null) return;
                    JadwalTayang target = film.jadwal.get(idx - 1);
                    if (target.countOccupied() > 0) {
                        warn("Jadwal sudah memiliki kursi terisi, jadi tidak boleh dihapus.");
                        continue;
                    }
                    film.jadwal.remove((int) idx - 1);
                    saveIfNeeded(autoSave);
                    success(autoSave ? "Jadwal berhasil dihapus." : "Jadwal sementara berhasil dihapus.");
                }
                default -> error("Pilihan tidak valid.");
            }
        }
    }

    // Melakukan soft delete film agar data transaksi lama tetap aman.
    private static void hapusFilm() {
        printHeader("HAPUS FILM");
        tampilkanDaftarFilm(true);
        String kode = readTextWithCmd("Masukkan kode film yang ingin dihapus [C batal]: ");
        if (isCancel(kode) || isBack(kode)) return;
        Film film = cariFilmByKodeAktif(kode);
        if (film == null) {
            error("Film tidak ditemukan atau sudah nonaktif.");
            return;
        }
        film.aktif = false;
        saveData();
        success("Film berhasil dihapus.");
    }

    // Membuka pengelolaan jadwal tayang untuk film aktif yang dipilih admin.
    private static void kelolaJadwalTayang() {
        printHeader("KELOLA JADWAL TAYANG");
        tampilkanDaftarFilm(true);
        String kode = readTextWithCmd("Masukkan kode film [C batal]: ");
        if (isCancel(kode) || isBack(kode)) return;
        Film film = cariFilmByKodeAktif(kode);
        if (film == null) {
            error("Film tidak ditemukan atau tidak aktif.");
            return;
        }
        editJadwalFilm(film);
    }

    // =========================================================
    // TAMPILAN TRANSAKSI
    // =========================================================
    // Menampilkan seluruh riwayat transaksi dalam bentuk tabel ringkas.
    private static void tampilkanRiwayatTransaksi() {
        printHeader("RIWAYAT TRANSAKSI");
        if (daftarTransaksi.isEmpty()) {
            warn("Belum ada transaksi.");
            return;
        }
        System.out.printf("%-4s %-8s %-12s %-6s %-8s %-18s %-12s%n",
                "No", "ID", "Tanggal", "Film", "Jam", "Kursi", "Total");
        System.out.println(repeat("-", UI_WIDTH));
        for (int i = 0; i < daftarTransaksi.size(); i++) {
            Transaksi t = daftarTransaksi.get(i);
            System.out.printf("%-4d %-8s %-12s %-6s %-8s %-18s Rp%-10s%n",
                    i + 1,
                    t.idTransaksi,
                    t.tanggal,
                    t.kodeFilm,
                    t.jamMulai,
                    truncate(String.join(", ", t.kursiDipilih), 18),
                    formatRupiah(t.totalBayar));
        }
    }

    // Searching transaksi berdasarkan ID menggunakan Linear Search manual.
    private static void cariTransaksiById() {
        String kode = readTextWithCmd("Masukkan ID transaksi (format TRX001) [C batal]: ");
        if (isCancel(kode) || isBack(kode)) return;
        if (!validTransaksiId(kode)) {
            error("Format ID transaksi tidak valid.");
            return;
        }
        Transaksi t = cariTransaksiByIdInternal(kode);
        if (t == null) {
            error("Transaksi tidak ditemukan.");
            return;
        }
        printInvoice(t);
    }

    // Mencetak invoice transaksi yang sudah berhasil dibayar.
    private static void printInvoice(Transaksi t) {
        System.out.println();
        printHeader("INVOICE");
        System.out.println(label("ID Transaksi") + t.idTransaksi);
        System.out.println(label("Tanggal") + t.tanggal);
        System.out.println(label("Film") + t.judulFilm);
        System.out.println(label("Genre") + t.genreFilm);
        System.out.println(label("Studio") + t.studio);
        System.out.println(label("Jam mulai") + t.jamMulai);
        System.out.println(label("Jam selesai") + t.jamSelesai);
        System.out.println(label("Durasi") + t.durasiFilm + " menit");
        System.out.println(label("Kursi") + String.join(", ", t.kursiDipilih));
        System.out.println(label("Jumlah tiket") + t.jumlahTiket);
        System.out.println(label("Harga/tiket") + "Rp" + formatRupiah(t.hargaPerTiket));
        System.out.println(label("Total bayar") + color("Rp" + formatRupiah(t.totalBayar), GREEN + BOLD));
        System.out.println(label("Tunai") + "Rp" + formatRupiah(t.uangBayar));
        System.out.println(label("Kembalian") + "Rp" + formatRupiah(t.kembalian));
        System.out.println(repeat("=", UI_WIDTH));
    }

    // =========================================================
    // UTILITAS TAMPILAN CONSOLE
    // =========================================================
    // Menampilkan header halaman dengan garis pembatas.
    private static void printHeader(String title) {
        System.out.println();
        System.out.println(color(repeat("=", UI_WIDTH), BLUE));
        System.out.println(color(center(title, UI_WIDTH), BOLD + CYAN));
        System.out.println(color(repeat("=", UI_WIDTH), BLUE));
    }

    // Menampilkan opsi menu dengan kunci navigasi berwarna kuning.
    private static void printMenuOption(String key, String text) {
        System.out.println(color(String.format(" %2s", key), YELLOW + BOLD) + ". " + text);
    }

    // Membuat label data invoice dan laporan agar rata kiri.
    private static String label(String label) {
        return color(String.format("%-14s", label), CYAN) + ": ";
    }

    // Menampilkan pesan berhasil.
    private static void success(String message) {
        System.out.println(color("[OK] " + message, GREEN));
    }

    // Menampilkan pesan peringatan.
    private static void warn(String message) {
        System.out.println(color("[!] " + message, YELLOW));
    }

    // Menampilkan pesan kesalahan.
    private static void error(String message) {
        System.out.println(color("[X] " + message, RED));
    }

    // Menampilkan pesan informasi.
    private static void info(String message) {
        System.out.println(color("[i] " + message, CYAN));
    }

    // Memberi warna ANSI saat mode warna aktif.
    private static String color(String text, String ansi) {
        if (!USE_COLOR) return text;
        return ansi + text + RESET;
    }

    // Meratakan teks ke tengah berdasarkan lebar UI.
    private static String center(String text, int width) {
        if (text.length() >= width) return text;
        int left = (width - text.length()) / 2;
        return repeat(" ", left) + text;
    }

    // Membuat teks berulang untuk garis pembatas.
    private static String repeat(String text, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(text);
        }
        return sb.toString();
    }

    // Memotong teks panjang agar tetap muat di tabel.
    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        if (maxLength <= 3) return text.substring(0, maxLength);
        return text.substring(0, maxLength - 3) + "...";
    }

    // Menyimpan data hanya ketika mode autosave aktif.
    private static void saveIfNeeded(boolean autoSave) {
        if (autoSave) saveData();
    }

    // =========================================================
    // UTILITAS DATA FILM / TRANSAKSI
    // =========================================================
    // Searching film berdasarkan kode menggunakan Linear Search manual.
    private static Film cariFilmByKode(String kode) {
        if (kode == null) return null;
        String key = kode.trim().toUpperCase(Locale.ROOT);
        for (Film film : daftarFilm) {
            if (film.kodeFilm.equalsIgnoreCase(key)) {
                return film;
            }
        }
        return null;
    }

    // Searching film aktif berdasarkan kode menggunakan Linear Search manual.
    private static Film cariFilmByKodeAktif(String kode) {
        Film film = cariFilmByKode(kode);
        if (film != null && film.aktif) return film;
        return null;
    }

    // Searching transaksi berdasarkan ID menggunakan Linear Search manual.
    private static Transaksi cariTransaksiByIdInternal(String id) {
        if (id == null) return null;
        for (Transaksi t : daftarTransaksi) {
            if (t.idTransaksi.equalsIgnoreCase(id.trim())) return t;
        }
        return null;
    }

    // Menampilkan jadwal tayang milik film beserta sisa kursi.
    private static void tampilkanJadwalFilm(Film film) {
        System.out.println();
        System.out.println("------------------------- JADWAL FILM: " + film.judul + " " + repeat("-", Math.max(1, UI_WIDTH - ("JADWAL FILM: " + film.judul).length() - 4)));
        if (film.jadwal.isEmpty()) {
            warn("Belum ada jadwal.");
            return;
        }
        for (int i = 0; i < film.jadwal.size(); i++) {
            JadwalTayang j = film.jadwal.get(i);
            System.out.println((i + 1) + ". " + j.jamMulai + " - "
                    + j.getJamSelesai(film.durasiMenit).format(TIME_FMT)
                    + " | Kursi tersedia: " + hitungKursiTersedia(j));
        }
    }

    // Menghitung jumlah kursi yang masih tersedia pada jadwal tayang.
    private static int hitungKursiTersedia(JadwalTayang jadwal) {
        return ROWS * COLS - jadwal.countOccupied();
    }

    // Menampilkan denah kursi dan status kursi sementara.
    private static void tampilkanDenahKursi(JadwalTayang jadwal, List<String> kursiDipilih) {
        System.out.println();
        System.out.println(color("------------- LAYAR -------------", BLUE + BOLD));
        System.out.print("    ");
        for (int c = 1; c <= COLS; c++) {
            System.out.print(String.format("%4d", c));
        }
        System.out.println();
        for (int r = 0; r < ROWS; r++) {
            char row = (char) ('A' + r);
            System.out.print(row + "   ");
            for (int c = 0; c < COLS; c++) {
                String code = "" + row + (c + 1);
                boolean terisi = jadwal.isOccupied(code) || kursiSudahDipilih(kursiDipilih, code);
                System.out.print(terisi ? color(" [X]", RED) : color(" [ ]", GREEN));
            }
            System.out.println();
        }
        System.out.println("Keterangan: " + color("[ ]", GREEN) + " Tersedia, " + color("[X]", RED) + " Terisi / Dipilih");
    }

    // Mengecek apakah masih ada film aktif untuk transaksi.
    private static boolean adaFilmAktif() {
        for (Film film : daftarFilm) {
            if (film.aktif) {
                return true;
            }
        }
        return false;
    }

    // Searching jadwal yang sama menggunakan Linear Search manual.
    private static boolean jadwalSudahAda(List<JadwalTayang> daftarJadwal, String waktu) {
        for (JadwalTayang jadwal : daftarJadwal) {
            if (jadwal.jamMulai.equals(waktu)) {
                return true;
            }
        }
        return false;
    }

    // Searching kursi pilihan sementara menggunakan Linear Search manual.
    private static boolean kursiSudahDipilih(List<String> daftarKursi, String kursi) {
        for (String item : daftarKursi) {
            if (item.equalsIgnoreCase(kursi)) {
                return true;
            }
        }
        return false;
    }

    // Memvalidasi kode kursi sesuai format baris A-E dan nomor 1-10.
    private static boolean validSeatCode(String seat) {
        if (seat == null) return false;
        seat = seat.trim().toUpperCase(Locale.ROOT);
        return seat.matches("^[A-E](10|[1-9])$");
    }

    // Memvalidasi kode film dengan format F diikuti 3 digit.
    private static boolean validFilmId(String id) {
        return id != null && id.trim().toUpperCase(Locale.ROOT).matches("^F\\d{3}$");
    }

    // Memvalidasi ID transaksi dengan format TRX diikuti angka.
    private static boolean validTransaksiId(String id) {
        return id != null && id.trim().toUpperCase(Locale.ROOT).matches("^TRX\\d{3,}$");
    }

    // Memvalidasi password login agar hanya angka maksimal 6 digit.
    private static boolean validPasswordFormat(String password) {
        if (password == null) return false;
        String teks = password.trim();
        if (teks.isEmpty() || teks.length() > 6) return false;
        for (int i = 0; i < teks.length(); i++) {
            char c = teks.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    // Memvalidasi jam dengan format HH:mm.
    private static boolean validTime(String waktu) {
        if (waktu == null) return false;
        try {
            LocalTime.parse(waktu.trim(), TIME_FMT);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    // Membaca tanggal transaksi sampai formatnya valid atau pengguna membatalkan.
    private static String readTanggal(String prompt) {
        while (true) {
            String s = readTextWithCmd(prompt);
            if (isCancel(s)) return null;
            if (isBack(s)) return null;
            if (!validTanggal(s)) {
                error("Format tanggal harus dd/MM/yyyy, contoh 27/01/2026.");
                continue;
            }
            return s.trim();
        }
    }

    // Memvalidasi tanggal dengan format dd/MM/yyyy.
    private static boolean validTanggal(String tgl) {
        if (tgl == null) return false;
        try {
            LocalDate.parse(tgl.trim(), DATE_FMT);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    // Membaca teks wajib isi dengan opsi kembali atau batal.
    private static String readNonEmptyWithBackCancel(String prompt) {
        while (true) {
            String s = readTextWithCmd(prompt);
            if (isCancel(s) || isBack(s)) return null;
            if (s.trim().isEmpty()) {
                error("Input tidak boleh kosong.");
                continue;
            }
            return s.trim();
        }
    }

    // Membaca angka bulat dalam batas minimum dan maksimum.
    private static Integer readIntWithBackCancel(String prompt, int min, int max) {
        while (true) {
            String s = readTextWithCmd(prompt);
            if (isCancel(s) || isBack(s)) return null;
            Integer val = parseIntOrNull(s);
            if (val == null) {
                error("Masukkan angka yang valid.");
                continue;
            }
            if (val < min || val > max) {
                error("Nilai harus di antara " + min + " dan " + max + ".");
                continue;
            }
            return val;
        }
    }

    // Membaca nominal uang dalam batas minimum dan maksimum.
    private static Double readDoubleWithBackCancel(String prompt, double min, double max) {
        while (true) {
            String s = readTextWithCmd(prompt);
            if (isCancel(s) || isBack(s)) return null;
            Double val = parseDoubleOrNull(s);
            if (val == null) {
                error("Masukkan angka uang yang valid. Contoh: 50000 atau 50.000.");
                continue;
            }
            if (val < min || val > max) {
                error("Nilai harus di antara " + formatRupiah(min) + " dan " + formatRupiah(max) + ".");
                continue;
            }
            return val;
        }
    }

    // Mengubah teks menjadi integer dan mengembalikan null jika gagal.
    private static Integer parseIntOrNull(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    // Mengubah teks nominal menjadi double dan mengembalikan null jika gagal.
    private static Double parseDoubleOrNull(String s) {
        try {
            String normalized = normalizeNumberInput(s);
            if (normalized.isEmpty()) return null;
            return Double.parseDouble(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    // Menormalkan input uang seperti Rp50.000 menjadi angka yang bisa dihitung.
    private static String normalizeNumberInput(String s) {
        if (s == null) return "";
        String value = s.trim()
                .replaceAll("(?i)rp", "")
                .replace(" ", "");
        if (value.isEmpty()) return "";

        boolean hasDot = containsCharManual(value, '.');
        boolean hasComma = containsCharManual(value, ',');
        if (hasDot && hasComma) {
            int lastDot = value.lastIndexOf('.');
            int lastComma = value.lastIndexOf(',');
            if (lastComma > lastDot) {
                return value.replace(".", "").replace(',', '.');
            }
            return value.replace(",", "");
        }
        if (hasDot && value.matches("\\d{1,3}(\\.\\d{3})+")) {
            return value.replace(".", "");
        }
        if (hasComma && value.matches("\\d{1,3}(,\\d{3})+")) {
            return value.replace(",", "");
        }
        if (hasComma) {
            return value.replace(',', '.');
        }
        return value;
    }

    // Searching karakter tertentu secara manual untuk menggantikan contains.
    private static boolean containsCharManual(String value, char target) {
        if (value == null) return false;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == target) {
                return true;
            }
        }
        return false;
    }

    // Membaca input pengguna sambil memberi highlight pada kunci navigasi.
    private static String readTextWithCmd(String prompt) {
        System.out.print(highlightNavigationKeys(prompt));
        return readLine().trim();
    }

    // Memberi warna kuning pada tombol navigasi seperti B, C, dan angka di dalam prompt.
    private static String highlightNavigationKeys(String prompt) {
        if (!USE_COLOR) return prompt;
        StringBuilder sb = new StringBuilder();
        boolean dalamKurung = false;
        sb.append(BOLD);
        for (int i = 0; i < prompt.length(); i++) {
            char c = prompt.charAt(i);
            if (c == '[' || c == '(') {
                dalamKurung = true;
                sb.append(c);
                continue;
            }
            if (c == ']' || c == ')') {
                dalamKurung = false;
                sb.append(c);
                continue;
            }
            if (dalamKurung && isNavigationKeyAt(prompt, i)) {
                sb.append(YELLOW).append(BOLD).append(c).append(RESET).append(BOLD);
            } else {
                sb.append(c);
            }
        }
        sb.append(RESET);
        return sb.toString();
    }

    // Mengecek posisi karakter yang layak dianggap kunci navigasi.
    private static boolean isNavigationKeyAt(String text, int index) {
        char c = text.charAt(index);
        boolean keyChar = c == 'B' || c == 'b' || c == 'C' || c == 'c' || (c >= '0' && c <= '9');
        if (!keyChar) return false;

        char before = index == 0 ? ' ' : text.charAt(index - 1);
        char after = index >= text.length() - 1 ? ' ' : text.charAt(index + 1);
        return isKeyBoundaryBefore(before) && isKeyBoundaryAfter(after);
    }

    // Mengecek batas kiri agar huruf di tengah kata tidak ikut disorot.
    private static boolean isKeyBoundaryBefore(char c) {
        return c == '[' || c == '(' || c == ' ' || c == ',' || c == '/';
    }

    // Mengecek batas kanan agar huruf di tengah kata tidak ikut disorot.
    private static boolean isKeyBoundaryAfter(char c) {
        return c == ']' || c == ')' || c == ' ' || c == ',' || c == '/' || c == '-';
    }

    // Membaca satu baris input dan menutup program aman saat input habis.
    private static String readLine() {
        try {
            if (!input.hasNextLine()) {
                System.out.println();
                saveData();
                info("Input selesai. Program ditutup otomatis.");
                System.exit(0);
            }
            return input.nextLine();
        } catch (Exception e) {
            return "";
        }
    }

    // Mengecek perintah batal dari pengguna.
    private static boolean isCancel(String s) {
        return s != null && s.trim().equalsIgnoreCase(CANCEL);
    }

    // Mengecek perintah kembali dari pengguna.
    private static boolean isBack(String s) {
        return s != null && s.trim().equalsIgnoreCase(BACK);
    }

    // Memformat angka menjadi gaya rupiah dengan titik ribuan.
    private static String formatRupiah(double value) {
        return String.format(Locale.ROOT, "%,.0f", value).replace(',', '.');
    }

    // Membuat ID transaksi berikutnya berdasarkan angka terbesar yang sudah ada.
    private static String generateNextTransactionId() {
        int max = 0;
        for (Transaksi t : daftarTransaksi) {
            String id = t.idTransaksi.toUpperCase(Locale.ROOT);
            if (id.startsWith("TRX")) {
                String num = id.substring(3);
                try {
                    max = Math.max(max, Integer.parseInt(num));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return String.format("TRX%03d", max + 1);
    }

    // Menghitung ulang jumlah tiket terjual tiap film dari riwayat transaksi.
    private static void rebuildTicketStats() {
        for (Film film : daftarFilm) {
            film.tiketTerjual = 0;
        }
        for (Transaksi t : daftarTransaksi) {
            Film film = cariFilmByKode(t.kodeFilm);
            if (film != null) {
                film.tiketTerjual += t.jumlahTiket;
            }
        }
    }

    // Menyelaraskan status kursi berdasarkan transaksi yang tersimpan.
    private static void reconcileSeatsFromTransactions() {
        boolean changed = false;
        for (Transaksi t : daftarTransaksi) {
            Film film = cariFilmByKode(t.kodeFilm);
            if (film == null) continue;
            JadwalTayang jadwal = findJadwalByJam(film, t.jamMulai);
            if (jadwal == null) continue;
            for (String seat : t.kursiDipilih) {
                if (!validSeatCode(seat)) continue;
                if (!jadwal.isOccupied(seat)) {
                    jadwal.occupy(seat);
                    changed = true;
                }
            }
        }
        if (changed) {
            saveFilms();
            info("Status kursi diselaraskan dari riwayat transaksi.");
        }
    }

    // Searching jadwal berdasarkan jam mulai menggunakan Linear Search manual.
    private static JadwalTayang findJadwalByJam(Film film, String jamMulai) {
        if (film == null || jamMulai == null) return null;
        for (JadwalTayang jadwal : film.jadwal) {
            if (jadwal.jamMulai.equals(jamMulai.trim())) {
                return jadwal;
            }
        }
        return null;
    }

    // =========================================================
    // LOAD / SAVE CSV
    // =========================================================
    // Menyimpan seluruh data film dan transaksi ke CSV.
    private static void saveData() {
        saveFilms();
        saveTransactions();
    }

    // Menyimpan data film, jadwal, dan status kursi ke films.csv.
    private static void saveFilms() {
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(FILM_FILE)))) {
            for (Film film : daftarFilm) {
                StringBuilder sb = new StringBuilder();
                sb.append(escape(film.kodeFilm)).append(';')
                  .append(escape(film.judul)).append(';')
                  .append(escape(film.genre)).append(';')
                  .append(film.studio).append(';')
                  .append(film.durasiMenit).append(';')
                  .append(film.hargaTiket).append(';')
                  .append(film.aktif).append(';');
                List<String> scheduleParts = new ArrayList<>();
                for (JadwalTayang j : film.jadwal) {
                    scheduleParts.add(j.serialize());
                }
                sb.append(String.join("|", scheduleParts));
                pw.println(sb);
            }
        } catch (IOException e) {
            System.out.println("Gagal menyimpan file film: " + e.getMessage());
        }
    }

    // Menyimpan riwayat transaksi ke transactions.csv.
    private static void saveTransactions() {
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(TRAN_FILE)))) {
            for (Transaksi t : daftarTransaksi) {
                pw.println(String.join(";",
                        escape(t.idTransaksi),
                        escape(t.tanggal),
                        escape(t.kodeFilm),
                        escape(t.judulFilm),
                        escape(t.genreFilm),
                        String.valueOf(t.studio),
                        escape(t.jamMulai),
                        escape(t.jamSelesai),
                        String.valueOf(t.durasiFilm),
                        String.valueOf(t.hargaPerTiket),
                        String.valueOf(t.jumlahTiket),
                        escape(String.join(",", t.kursiDipilih)),
                        String.valueOf(t.totalBayar),
                        String.valueOf(t.uangBayar),
                        String.valueOf(t.kembalian)
                ));
            }
        } catch (IOException e) {
            System.out.println("Gagal menyimpan file transaksi: " + e.getMessage());
        }
    }

    // Memuat data film dan transaksi dari CSV lalu menghitung ulang statistik.
    private static void loadData() {
        daftarFilm.clear();
        daftarTransaksi.clear();
        loadFilms();
        loadTransactions();
        rebuildTicketStats();
    }

    // Memuat data film dari films.csv.
    private static void loadFilms() {
        File file = new File(FILM_FILE);
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(";", -1);
                if (parts.length < 7) continue;
                Film film = new Film(
                        unescape(parts[0]),
                        unescape(parts[1]),
                        unescape(parts[2]),
                        Integer.parseInt(parts[3]),
                        Integer.parseInt(parts[4]),
                        Double.parseDouble(parts[5]),
                        Boolean.parseBoolean(parts[6])
                );
                if (parts.length >= 8 && !parts[7].isBlank()) {
                    String[] jadwalTokens = parts[7].split("\\|");
                    for (String token : jadwalTokens) {
                        JadwalTayang j = JadwalTayang.deserialize(token);
                        if (j != null) film.jadwal.add(j);
                    }
                }
                daftarFilm.add(film);
            }
        } catch (Exception e) {
            System.out.println("Gagal memuat film, memakai data default. " + e.getMessage());
            daftarFilm.clear();
        }
    }

    // Memuat data transaksi dari transactions.csv.
    private static void loadTransactions() {
        File file = new File(TRAN_FILE);
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(";", -1);
                if (parts.length < 15) continue;
                List<String> kursi = new ArrayList<>();
                String kursiPart = unescape(parts[11]);
                if (!kursiPart.isBlank()) kursi.addAll(Arrays.asList(kursiPart.split(",")));
                Transaksi t = new Transaksi(
                        unescape(parts[0]),
                        unescape(parts[1]),
                        unescape(parts[2]),
                        unescape(parts[3]),
                        unescape(parts[4]),
                        Integer.parseInt(parts[5]),
                        unescape(parts[6]),
                        unescape(parts[7]),
                        Integer.parseInt(parts[8]),
                        Double.parseDouble(parts[9]),
                        Integer.parseInt(parts[10]),
                        kursi,
                        Double.parseDouble(parts[12]),
                        Double.parseDouble(parts[13]),
                        Double.parseDouble(parts[14])
                );
                daftarTransaksi.add(t);
            }
        } catch (Exception e) {
            System.out.println("Gagal memuat transaksi, memulai tanpa riwayat. " + e.getMessage());
            daftarTransaksi.clear();
        }
    }

    // Membersihkan teks sebelum ditulis ke CSV.
    private static String escape(String s) {
        return s == null ? "" : s.replace(";", ",").replace("\n", " ").trim();
    }

    // Membersihkan teks setelah dibaca dari CSV.
    private static String unescape(String s) {
        return s == null ? "" : s.trim();
    }

    // =========================================================
    // DATA AWAL
    // =========================================================
    // Membuat data awal ketika CSV belum berisi film.
    private static void seedDataAwal() {
        Film f1 = new Film("F001", "Tunggu Aku Sukses Nanti", "Drama", 5, 120, 40000, true);
        f1.jadwal.add(new JadwalTayang("11:30"));
        f1.jadwal.add(new JadwalTayang("14:00"));

        Film f2 = new Film("F002", "Senin Harga Naik", "Comedy", 2, 98, 45000, true);
        f2.jadwal.add(new JadwalTayang("15:00"));
        f2.jadwal.add(new JadwalTayang("17:00"));

        Film f3 = new Film("F003", "Ayah, Ini Arahnya Kemana?", "Drama", 1, 103, 50000, true);
        f3.jadwal.add(new JadwalTayang("19:00"));
        f3.jadwal.add(new JadwalTayang("21:00"));

        daftarFilm.add(f1);
        daftarFilm.add(f2);
        daftarFilm.add(f3);
    }

    // =========================================================
    // MODEL DATA
    // =========================================================
    // Menyimpan data film, status aktif, jadwal, dan jumlah tiket terjual.
    private static class Film {
        String kodeFilm;
        String judul;
        String genre;
        int studio;
        int durasiMenit;
        double hargaTiket;
        boolean aktif;
        List<JadwalTayang> jadwal = new ArrayList<>();
        int tiketTerjual;

        // Membuat objek film baru.
        Film(String kodeFilm, String judul, String genre, int studio, int durasiMenit, double hargaTiket, boolean aktif) {
            this.kodeFilm = kodeFilm;
            this.judul = judul;
            this.genre = genre;
            this.studio = studio;
            this.durasiMenit = durasiMenit;
            this.hargaTiket = hargaTiket;
            this.aktif = aktif;
        }

        // Menyalin film agar proses edit tidak langsung mengubah data asli.
        Film copy() {
            Film f = new Film(kodeFilm, judul, genre, studio, durasiMenit, hargaTiket, aktif);
            for (JadwalTayang j : jadwal) {
                f.jadwal.add(j.copy());
            }
            f.tiketTerjual = tiketTerjual;
            return f;
        }
    }

    // Menyimpan jam tayang dan denah kursi untuk satu jadwal.
    private static class JadwalTayang {
        String jamMulai;
        boolean[][] kursi = new boolean[ROWS][COLS];

        // Membuat jadwal tayang baru dengan kursi kosong.
        JadwalTayang(String jamMulai) {
            this.jamMulai = jamMulai.trim();
        }

        // Menyalin jadwal beserta status kursinya.
        JadwalTayang copy() {
            JadwalTayang j = new JadwalTayang(jamMulai);
            for (int i = 0; i < ROWS; i++) {
                j.kursi[i] = Arrays.copyOf(kursi[i], COLS);
            }
            return j;
        }

        // Mengubah teks jam mulai menjadi LocalTime.
        LocalTime getJamMulai() {
            return LocalTime.parse(jamMulai, TIME_FMT);
        }

        // Menghitung jam selesai berdasarkan durasi film.
        LocalTime getJamSelesai(int durasiMenit) {
            return getJamMulai().plusMinutes(durasiMenit);
        }

        // Mengecek apakah kursi sudah terisi.
        boolean isOccupied(String seatCode) {
            int[] rc = parseSeat(seatCode);
            return rc != null && kursi[rc[0]][rc[1]];
        }

        // Mengunci kursi setelah pembayaran berhasil.
        void occupy(String seatCode) {
            int[] rc = parseSeat(seatCode);
            if (rc != null) kursi[rc[0]][rc[1]] = true;
        }

        // Menghitung jumlah kursi yang sudah terisi.
        int countOccupied() {
            int count = 0;
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    if (kursi[r][c]) count++;
                }
            }
            return count;
        }

        // Mengubah jadwal dan kursi menjadi format teks CSV.
        String serialize() {
            StringBuilder bits = new StringBuilder();
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    bits.append(kursi[r][c] ? '1' : '0');
                }
            }
            return jamMulai + "#" + bits;
        }

        // Mengubah teks CSV menjadi objek jadwal tayang.
        static JadwalTayang deserialize(String token) {
            if (token == null || token.isBlank()) return null;
            String[] p = token.split("#", -1);
            if (p.length < 2) return null;
            String jam = p[0].trim();
            if (!jam.matches("^\\d{2}:\\d{2}$")) return null;
            JadwalTayang j = new JadwalTayang(jam);
            String bits = p[1].trim();
            int idx = 0;
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    if (idx < bits.length()) {
                        j.kursi[r][c] = bits.charAt(idx) == '1';
                    }
                    idx++;
                }
            }
            return j;
        }
    }

    // Menyimpan data transaksi yang sudah final.
    private static class Transaksi {
        String idTransaksi;
        String tanggal;
        String kodeFilm;
        String judulFilm;
        String genreFilm;
        int studio;
        String jamMulai;
        String jamSelesai;
        int durasiFilm;
        double hargaPerTiket;
        int jumlahTiket;
        List<String> kursiDipilih;
        double totalBayar;
        double uangBayar;
        double kembalian;

        // Membuat objek transaksi baru setelah pembayaran berhasil.
        Transaksi(String idTransaksi, String tanggal, String kodeFilm, String judulFilm, String genreFilm, int studio,
                  String jamMulai, String jamSelesai, int durasiFilm, double hargaPerTiket, int jumlahTiket,
                  List<String> kursiDipilih, double totalBayar, double uangBayar, double kembalian) {
            this.idTransaksi = idTransaksi;
            this.tanggal = tanggal;
            this.kodeFilm = kodeFilm;
            this.judulFilm = judulFilm;
            this.genreFilm = genreFilm;
            this.studio = studio;
            this.jamMulai = jamMulai;
            this.jamSelesai = jamSelesai;
            this.durasiFilm = durasiFilm;
            this.hargaPerTiket = hargaPerTiket;
            this.jumlahTiket = jumlahTiket;
            this.kursiDipilih = kursiDipilih;
            this.totalBayar = totalBayar;
            this.uangBayar = uangBayar;
            this.kembalian = kembalian;
        }
    }

    // Mengubah kode kursi seperti A1 menjadi indeks baris dan kolom.
    private static int[] parseSeat(String seatCode) {
        if (seatCode == null) return null;
        seatCode = seatCode.trim().toUpperCase(Locale.ROOT);
        if (!validSeatCode(seatCode)) return null;
        int row = seatCode.charAt(0) - 'A';
        int col = Integer.parseInt(seatCode.substring(1)) - 1;
        return new int[]{row, col};
    }
}
