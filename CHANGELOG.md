# Fiorino Auto değişiklik geçmişi

Bu dosya Fiorino Auto geliştirmesini izler. Türetilen projenin özgün geçmişi, yazar atıflarıyla [CHANGELOG_UPSTREAM.md](CHANGELOG_UPSTREAM.md) dosyasında korunur.

## 1.4.4 Preview — 5 Kasım 2026

- CarPlay ekranında Android Decor sistem paylarını kaldıran kenardan kenara düzen; API 30+ cutout ALWAYS ve önceki platformlar için layout flagları.
- İlk kareden sonra tam ekran geçişinde aynı kenar ayarlarının korunması.
- Yerel genel APK build'i, 4.811 ana uygulama ve 16 ortak CarPlay testi başarılı.
- Gerçek Note 9 / iPhone 14 Pro Max üzerinde kontrol ve görüntü; kullanıcı son görünümü doğruladı. Gerçek PX3, görüşme/mikrofon ve Siri/Gemini teyp sesi testleri bekler.

## 1.4.3 geliştirme adımı

- Kayıtlı telefonları yönetme, uygulama eşleşme tercihlerini onayla sıfırlama ve sistem Bluetooth ayarlarına geçiş.
- Sıfırlamada alıcı adresi, görüntü/ses ayarları ve alıcı provizyonunun korunması.
- Tam ekranda uygulama kenar paylarının kaldırılması; küçük codec blok yuvarlamasını dolduran görüntü/dokunma politikası.

## 1.4.2 geliştirme adımı

- Ekran oranına göre, en çok 800×480 / 30 FPS CarPlay tuvali.
- Kurulmuş otomatik CarPlay tercihinde kayıtlı iPhone'a doğrudan açılış; açık telefon değiştirme akışının korunması.

## 1.4.1 geliştirme adımı

- CarPlay kontrol ve medyasını Activity'den bağımsız foreground hizmetine taşıma.
- Boot/Bluetooth/uyanış tetikleri; 2–60 saniye aralıklı tekrar bağlantı ve kullanıcı durdurma tercihi.
- Protokol değişiminde önceki CarPlay temizliğini bekleme; AA/USB sahiplik korumaları.
- Tam ekran menüsü, bir kez alıcı adresi kaydı ve 260 DPI AA seçeneği.

## 1.4.0 Preview

- Android Auto ve CarPlay motor/arayüzünü tek ana APK'da birleştirme.
- Ortak telefon/tür seçicisi; iPhone servis kaydını yanlış AA hedeflerinden ayırma.
- Genel kimliksiz APK ile özel yerel CarPlay test paketini ayıran dağıtım kontrolü.

## 1.3.x ve önceki geliştirme

- Native AA tercihini eski bağlantı bayraklarına karşı koruma; ayarlarda üç dakikalık eşleştirme penceresi.
- 2,4 GHz AA profili, H.264/AAC, DPI/kalite ayarları ve aktif ağın gereksiz yeniden kurulmasını engelleme.
- Ortak ayar menüsü, ses kısma/toparlama, otomatik mikrofon ve GPS hızına göre alıcının Android medya seviyesi.

Ayrıntılı CarPlay cihaz kayıtları [CARPLAY-LAB.md](docs/CARPLAY-LAB.md) içindedir. “Geliştirme adımı” girdileri, her numaranın ayrı genel release olarak yayımlandığı anlamına gelmez.
