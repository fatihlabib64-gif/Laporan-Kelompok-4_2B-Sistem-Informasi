# Project Kelompok 4-2B-SI

Project akhir Mata Kuliah Algoritma dan Struktur Data dengan studi kasus Sistem Pemesanan Tiket Bioskop

Sistem Pemesanan Tiket Bioskop berbasis Java Console.

---

## Anggota Kelompok 4

1. [Belva Dhia Haqqina](https://github.com/Belvadh-bit)

2. [Muhammad Al-Fatih](https://github.com/NirCulus)

3. [Muhammad Fatih Labib](https://github.com/fatihlabib64-gif)


---

## Deskripsi

Project ini merupakan aplikasi pemesanan tiket bioskop berbasis Java yang berjalan melalui terminal/console. Sistem digunakan dari sudut pandang admin atau kasir untuk mengelola film, jadwal tayang, kursi, transaksi, dan riwayat pembelian tiket.

## Struktur File

```text
Project-Kelompok-4-2B-SI/
│
├── Laporan Kelompok 4_2B_Sistem Informasi.pdf
├── SistemBioskop.java
├── films.csv
├── transactions.csv
└── README.md
```
Keterangan:

`Laporan Kelompok 4_2B_Sistem Informasi.pdf` → laporan project

`SistemBioskop.java` → file utama program

`films.csv` → penyimpanan data film

`transactions.csv` → penyimpanan riwayat transaksi

`README.md` → dokumentasi singkat project

---

## Cara Menjalankan Program

### 1. Jalankan Program
Program dapat dijalankan secara lokal menggunakan IDE Java atau melalui terminal/command prompt.

### 2. Login ke Sistem

Masukkan password sesuai hak akses yang ingin digunakan.

- Password Kasir : 123456

- Password Admin : 654321

### 3. Gunakan Fitur Program

Setelah berhasil login, pengguna akan diarahkan ke menu utama sesuai hak akses masing-masing. Menu akan ditampilkan seperti berikut:

- Kasir
```text
  1. Lihat Daftar Film
  2. Transaksi Tiket Baru
  3. Riwayat Transaksi
  4. Laporan Transaksi
  5. Cari / Urut Film
  0. Kembali ke Menu Login
```

- Admin
```text
  1. Kelola Film
  2. Transaksi Tiket Baru
  3. Riwayat Transaksi
  4. Laporan Transaksi
  5. Cari / Urut Film
  0. Kembali ke Menu Login
```

##### Kelola Film (Menu Khusus Admin):

```text
  1. Lihat Daftar Film
  2. Tambah Film
  3. Edit Film
  4. Hapus Film (Soft Delete)
  5. Kelola Jadwal Tayang
  6. Lihat Semua Film Termasuk Nonaktif
  0. Kembali
```

### 4. Keluar Program dengan Benar

Untuk memastikan seluruh data tersimpan dengan aman, keluar program secara bertahap melalui menu Kembali hingga kembali ke halaman login utama, kemudian pilih opsi Keluar.

---

## Penyimpanan Data

Data program disimpan otomatis menggunakan file CSV:

* Film disimpan pada `films.csv`
* Transaksi disimpan pada `transactions.csv`

Perubahan data akan tetap tersimpan setelah program ditutup.

---

## Catatan

* Jangan menghapus file CSV saat program digunakan.
* Pastikan format file CSV tidak diubah secara manual.
* Jika file CSV kosong, sistem akan membuat data baru saat program berjalan.

---
