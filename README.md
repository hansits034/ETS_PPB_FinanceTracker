# Finance Tracker Android

Aplikasi pelacak keuangan pribadi berbasis Android menggunakan **Jetpack Compose**. Aplikasi ini memungkinkan pengguna untuk mencatat pemasukan dan pengeluaran harian, memantau tren keuangan melalui grafik interaktif, serta melihat ringkasan kategori pengeluaran secara intuitif.

## 🚀 Fitur Utama

- **Dashboard Keuangan:** Menampilkan saldo saat ini, total pendapatan, total pengeluaran, dan rata-rata pengeluaran harian.
- **Pencatatan Transaksi:** Tambah pemasukan atau pengeluaran dengan kategori, nominal, dan tanggal yang mudah diatur menggunakan *Date Picker*.
- **Visualisasi Data:**
    - **Line Chart:** Tren pergerakan saldo (Pendapatan vs Pengeluaran vs Selisih) selama 30 hari terakhir.
    - **Pie Chart:** Distribusi pengeluaran per kategori yang dinamis.
- **Riwayat Transaksi:** Pencarian riwayat transaksi dengan pengurutan kronologis (terbaru di atas) dan format tanggal `dd-MM-yyyy`.
- **Penyimpanan Lokal:** Data disimpan secara aman di memori internal perangkat menggunakan format `data.json`.

## 🛠️ Teknologi yang Digunakan

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material Design 3)
- **Data Storage:** JSON File I/O (`context.filesDir`)
- **JSON Parser:** Gson
- **Date Handling:** `java.time` (API level 26+)
- **Charts:** Native Android Canvas API

## 📂 Struktur Data (`data.json`)

Data disimpan dalam format array JSON. Contoh struktur:

```json
[
  {
    "id": 1773942904000,
    "date": "2026-03-19",
    "amount": 169000,
    "category": "Makanan",
    "description": "Makan Siang di Kantin",
    "type": "Pengeluaran"
  }
]
```
## ⚙️ Cara Menjalankan
1. Clone/Download proyek ini ke Android Studio.

2. Pastikan file data.json sudah berada di dalam folder app/src/main/assets/.

3. Jalankan aplikasi di Emulator atau perangkat Android (API Level 26+).

4. Saat aplikasi dibuka pertama kali, data dari assets/data.json akan otomatis disalin ke memori internal perangkat agar bisa dimodifikasi.

## 📝 Catatan Pengembang
- Jika ingin memperbarui data dummy secara manual, Anda bisa mengubah isi file app/src/main/assets/data.json.

- Untuk mereset data di aplikasi setelah mengubah file dummy, lakukan Clear Data melalui menu App Info di perangkat Anda sebelum menjalankan ulang aplikasi.

