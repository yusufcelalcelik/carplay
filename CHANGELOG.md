# Fiorino Auto değişiklik geçmişi

Bu dosya Fiorino Auto geliştirmesini izler. Türetilen projenin özgün geçmişi, yazar atıflarıyla [CHANGELOG_UPSTREAM.md](CHANGELOG_UPSTREAM.md) dosyasında korunur.

## 1.5.5 Preview — 10 Kasım 2026

- Ayarlar → Teşhis altına Wi-Fi log görüntüleyici eklendi. Açılınca ekranda adres ve 6 haneli PIN çıkar; aynı ağdaki bilgisayarın tarayıcısından canlı log izlenir ve indirilir. Varsayılan kapalıdır, yalnızca log okur, 30 dakika sonra kendiliğinden kapanır, yanlış PIN denemeleri kilitlenir.

## 1.5.4 Preview — 10 Kasım 2026

- Android Auto Bluetooth dinleyicisi yeniden açılırken deneme sınırı artık gerçekten işliyor: dinleyici en az 30 saniye ayakta kalmadıkça deneme hakkı yenilenmiyor.
- Birincil dinleyici düşünce hâlâ açık olan ikincil radyo dinleyicileri tekrar başlatılmıyor. Bluetooth kapalıyken uyarı yalnızca deneme hakkı bittiğinde çıkıyor; rutin Bluetooth sıfırlamasında banner görünmüyor.
- PX3 uygulama içi Bluetooth açma yolu artık yalnızca tek bir yazılım parmak izine bağlı değil (PX3 + HCT işaretleri). Bluetooth kapanırken açma isteği bekleyip yeniden deniyor, "kontrol ediliyor" mesajı takılı kalmıyor, çift diyalog açılmıyor.
- Bluetooth açıldıktan sonra kablosuz mod, eski bir uyarı kaydına bakmadan yeniden başlatılıyor.

## 1.5.3 Preview — 9 Kasım 2026

- HCT PX3 Android 7.1.2'de takılan sistem Bluetooth penceresinin yerine uygulama içinde onay ve en fazla sekiz saniye bekleme. Fabrika CAR-KIT bağlantısı ve Android Auto'nun Bluetooth servisi ayrı ayrı açıklanır.
- Native Android Auto, Wi-Fi ağ bilgileri hazır olduğunda hazırlık adımından çıkar. Android Bluetooth kaydı ilk denemede açılmazsa sınırlı yeniden deneme yapılır; kayıt yine açılamazsa ana ekranda uyarı gösterilir.
- Fiziksel PX3'te kablolu bağlantı önceki sürümde doğrulandı. Bu sürümün kablosuz bağlantısı araçta yeniden denenmeli.

## 1.5.2 Preview — 8 Kasım 2026

- Android Auto'da hıza göre ses artışı, uyumlu Microntek carservice varsa teybin amfi komutunu kullanır. Ses kademesi, üst sınır ve sessiz durum doğrulandıktan sonra ayar yapılır.
- Ses ayarlarında GPS hızı ve kullanılan ses yolu gösterilir. Eski GPS sürücüsü monoton zaman damgası vermediğinde güncel konum ölçümünün saat damgası kullanılır.

## 1.5.1 Preview — 7 Kasım 2026

- Hıza göre teyp sesinde 30–150 km/sa arası referans hız ve 0–6 ek ses kademesi ayrı ayarlanabilir. Kayıtlı ayarlarda varsayılan referans 90 km/sa olarak korunur.
- Hata ayıklama günlüğü, hız telafisinde istenen ve teybin Android medya kanalında okunan ses seviyesini gösterir.
- Native Android Auto, Android Bluetooth kapalıysa sistem onayını ister. Bluetooth açılamazsa ana ekranda bağlantı uyarısı ve ayarlara geçiş gösterir. Microntek arayüzünün denetim sonucu günlüğe eklenir.
- Telefon ve bağlantı ayarlarından Android Auto araç adı ve erişilebilir Android Bluetooth adı Fiorino Auto yapılabilir. İsteğe bağlı yalnız veri ayarı, uygulamanın yardımcı görüşme profilini ve Bluetooth uyandırmasını kapatır.

## 1.5.0 Preview — 6 Kasım 2026

- Ayrı USB aksesuar / kablosuz Android Auto köprüsü: eski Android telefon araçta USB ile kalır, sürücü telefonu Wi-Fi/Bluetooth üzerinden bağlanır. Şeffaf AAP/TLS aktarımı, medya yeniden kodlama yok.
- Köprüde sürücü seçimi, USB takılışında sor/son telefonu dene tercihi, bildirimden telefon değiştirme ve durdurma.
- Önceki USB işini kapatmadan ikinci oturum açmama; Android 10+ yeni ağ kimliği, normal AA/CarPlay alıcısının otomatik tetikleriyle sahiplik ayrımı.
- Ford Kuga’da gerçek test bekliyor; kablosuz iki telefon geçişinde USB yeniden takılması gerekebilir. iPhone → fabrika CarPlay köprüsü eklenmedi.
- CarPlay ekranında keşif testinin gelişmiş ayarlara taşınması; alıcı adresinin normal bağlantı ayarlarından kaydedilmesi ve ilk bağlantı adres girişi sonrası devam.
- Yerel test/derleme sonuçları sürüm notunda belirtilir; fiziksel araç doğrulaması yerine sayılmaz.

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
