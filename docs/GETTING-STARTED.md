# Kurulum ve kullanım

## Önce paketi seçin

İndirme kaynağı: [Fiorino Auto Releases](https://github.com/yusufcelalcelik/carplay/releases).

Genel APK, Android Auto alıcısı ile CarPlay arayüz/motorunu içerir; CarPlay aksesuar kimliği içermez. Yeni cihaza genel APK kurmak tek başına CarPlay bağlantısı sağlamaz. Yerel deneysel kimlikli paket ayrı hazırlanır ve genel release'e konulmaz. Gizli girdileri kaynak depoya veya hata kaydına eklemeyin.

Aynı uygulamayı güncellerken mevcut verileri korumak için aynı paket kimliği ve imza gerekir. Bu projenin ana paket kimliği `com.yusufcelik.fiorinoheadunit` olur. Ayrı `carplay-lab` kurulumu normal kullanım için gerekli değildir.

## Android Auto

1. Telefonda Android Auto, alıcıda Bluetooth ve Wi-Fi açık olsun. Alıcının Bluetooth/konum ve mikrofon izinlerini tamamlayın.
2. **Ayarlar → Fiorino ayarları → Native Android Auto kurulumu** bölümünden Native alıcıyı etkinleştirin.
3. **Kablosuz Android Auto eşleştir** seçeneği üç dakikalık görünürlük/dinleme penceresi açar. Telefonda Android Auto'nun kablosuz araç bağlama akışını kullanın ve iki cihazdaki sistem onaylarını tamamlayın.
4. Ortak telefon seçicisinden Android telefonu kaydedin. OEM Bluetooth modülü Android'e açık değilse ilk eşleştirme teybin fabrika Bluetooth uygulamasını gerektirebilir.
5. Ayarlardan çıkıp bağlantıyı başlatın. Native AA, telefondaki geliştirici **Headunit Server** işlemini normal kullanımda gerektirmez.

Telefonun kablosuz araç listesinde her üretici teybinin görüneceği doğrulanmış değildir. Sorun bildirirken tam alıcı modeli/MCU ve telefon sürümünü yazın.

### USB

USB Host desteği bulunan alıcı portu ve veri taşıyan kablo kullanın; telefondaki Android Auto/USB onaylarını tamamlayın. Yalnız şarj portu veya kablosu görüntü taşımaz. USB CarPlay bu sürümün doğrulanmış kullanım yolu değildir.

### Self Mode / Bağımsız Mod

Aynı Android cihazı hem Android Auto telefonu hem alıcı gibi denemek içindir. Normal iki cihazlı kablosuz araç bağlantısıyla aynı akış değildir; geliştirici Headunit Server/yerel bağlantı adımları bu test modunda görülebilir.

## CarPlay

1. Android 7.0+ alıcıda CarPlay kurulum girdisinin hazır olduğundan emin olun. Eksik/geçersiz kimlikte uygulama bağlantıyı reddeder; keşif testinin başarılı olması bu kontrolün yerine geçmez.
2. Alıcı ve iPhone'da Wi-Fi/Bluetooth açık olsun. iPhone'da Siri etkin olmalıdır.
3. İlk eşleştirmede **Yeni telefon eşleştir → CarPlay** akışını açın, alıcı görünürlüğünü onaylayın. iPhone'da **Ayarlar → Genel → CarPlay** ekranından alıcıyı seçip onayları tamamlayın.
4. Kayıtlı iPhone'u seçin. Alıcı adresi otomatik okunamıyorsa **Alıcı adresi** bölümüne **teybin kendi Bluetooth MAC adresini** bir kez kaydedin; iPhone'un adresini kullanmayın.
5. **Kayıtlı iPhone'a otomatik bağlan** seçeneğini açık bırakın. Sonraki uygulama açılışı doğrudan bu telefona gider. **Telefon değiştir** ortak seçiciyi açar.

CarPlay bağlantısı foreground hizmeti tarafından tutulur. Görüntü ekranının arkaya geçmesi tek başına oturumu kapatmaz. Kopma sonrası yeniden denemeler aralıklı yapılır. Modern Android'de arka plandan ekran açmak veya mikrofon kullanmak sınırlanabilir; bildirimden uygulamayı açmak gerekebilir. Gerçek teyp kontak/uyku davranışı cihazda test edilmelidir.

**Durdur**, otomatik denemeyi de bekletir; yeniden açıkça bağlantı başlatmak bu bekletmeyi kaldırır.

## Görüntü ve yazı boyutu

- Android Auto için **Fiorino ayarları → Arayüz boyutu / DPI** altında hazır veya özel değer seçin. **Dev — 260 DPI**, boyut sınırı geçersiz kılmasını da açar. Telefonun yerleşimi değişebilir; yeni değer sonraki AA oturumunda uygulanır.
- AA çözünürlük/otomatik kalite değişiklikleri mevcut oturumu keserek uygulanmaz; sonraki bağlantı içindir.
- CarPlay, oturum başlangıcında alıcı ekranının yatay oranına uygun tuval seçer. İlk görüntüden sonra büyük kontroller gizlenir; köşedeki menü düğmesi geri açar. Çentiğin fiziksel kamera deliği yazılımla yok olmaz.

## 2,4 GHz teyp

AA için **Kablosuz bağlantı profili → 2,4 GHz araç — 480p / 30 FPS** seçeneği H.264/AAC ve ilgili bant tercihlerini kaydeder. Otomatik 720p yükseltmesi bu profilde kapatılır. CarPlay alıcısı 2,4 GHz kanal 6 kullanır.

Google'ın standart kablosuz AA telefon gereksinimlerinde [5 GHz Wi-Fi desteği](https://support.google.com/androidauto/answer/6348019) bulunur. Yalnız 2,4 GHz destekleyen teypteki özel profil bütün telefon/sürücü kombinasyonları için garanti değildir. Bandın gerçek uygulandığı araç üzerinde kontrol edilir.

## Ses, mikrofon ve aramalar

AA medya kısma ve mikrofon ayarları uygulamada bulunur. İlk olarak telefon-teyp Bluetooth HFP görüşmesini, ardından projeksiyon açıkken gelen/giden aramayı deneyin. Asistan mikrofonu seçimi, OEM HFP görüşme mikrofonunu mutlaka değiştirmez.

AA'daki hıza göre ses ayarı alıcının Android `STREAM_MUSIC` seviyesini kullanır; telefona AVRCP ses ayarı göndermez. Bu seviye her OEM teybin fiziksel MCU/amfi kontrolüyle aynı olmayabilir. Siri/Gemini'nin doğal dil komutuyla fiziksel teyp sesini yönetmesi henüz uygulanmış/doğrulanmış değildir.

## Kayıtlı cihazları temizleme

**Cihazları yönet → Uygulamadaki telefon tercihlerini sıfırla**, onayınızdan sonra oturumları durdurur; AA telefon seçimleri ve CarPlay eşleşme yetkilendirmelerini kaldırır. Alıcı MAC adresi, ekran/ses tercihleri ve alıcı provizyonu korunur.

Bu işlem sistem Bluetooth bond'larını veya iPhone'daki araç kaydını topluca silmez. Sistem eşleşmeleri için aynı menüde **Bluetooth eşleşmelerini yönet**, iPhone için gerekirse kendi CarPlay ayarlarındaki aracı unutma seçeneği kullanılır.

## Sık karşılaşılan durumlar

| Belirti | Kontrol |
| --- | --- |
| Araç keşifte görünüyor ama görüntü gelmiyor | Keşif ile medya doğrulaması ayrı. Paket/provizyon durumunu ve ekrandaki hata aşamasını kontrol edin. |
| CarPlay Siri istiyor | iPhone Ayarlar aramasında Siri'yi bulun, etkinleştirme kurulumunu tamamlayın. |
| Harita boş ızgara | Telefonun internet erişimini veya indirilmiş çevrimdışı haritayı kontrol edin. CarPlay Wi-Fi bağlantısı tek başına internet sağlamaz. |
| Yanlış telefon uyandırılıyor | Ortak seçicide doğru telefon/türü kaydedin; eski tercihleri cihaz yönetiminden sıfırlayabilirsiniz. |
| ADB kurulumu reddediliyor | Bazı MIUI cihazları USB kurulumunu engeller. APK'yı İndirilenler'e kopyalayıp cihazdan kurun; bu cihaz izni zorla aşılmaz. |
| Güncelleme bağlantısı farklı projeye gidiyor | 1.4.4'te uygulama içi güncelleme kaynağında upstream adresi bulunur. Şimdilik bu deponun Releases sayfasını kullanın; düzeltme yol haritasındadır. |

## Gerçek araçta doğrulama

Önce araç sabitken normal Bluetooth görüşmesi, projeksiyon, menü/dokunma ve ses giriş/çıkışını kontrol edin. Ardından birkaç kontak/uyku dönüşü ve bağlantı kopması sonrası toparlanmayı kaydedin. Karşı tarafın duyduğu mikrofonu ve navigasyon anonsu bittikten sonra müzik seviyesini ayrıca değerlendirin.

[Özellik matrisi](FEATURE-COMPATIBILITY-2026.md) · [CarPlay teknik test kaydı](CARPLAY-LAB.md) · [Donanım ve direksiyon tuşları](HARDWARE-AND-STEERING.md)
