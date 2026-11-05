# Android Auto / CarPlay özellik ve doğrulama matrisi

İnceleme: **5 Kasım 2026**. Hedef: Fiorino'nun Android 7.1 etiketli, kullanıcı bildirimine göre Rockchip PX3 / 2 GB RAM teybi (MCU ve donanım envanteri bekliyor); masa başında Note 9 alıcı, Note 12 Android gönderici ve gerçek iPhone 14 Pro Max. Araç ekranı ve CAN verisi henüz bağlı değil. Aşağıdaki tablo mevcut alıcının kapsamını ve test durumunu ayırır; gelecekteki özellikleri çalışıyormuş gibi göstermez.

## Somut cihaz kanıtı

Yerel deneysel doğrulama girdili 1.4.0 testinde gerçek iPhone ile kontrol oturumu kuruldu ve video kareleri Note 9'da çizildi. 14:57:45.651 başlangıç, 14:57:46.620 Bluetooth bağlantısı, 14:57:56.097 kontrol oturumu, 14:57:58.193 ilk çizilen kare: bu **tek soğuk denemede** toplam yaklaşık 12,54 s, RFCOMM bağlantısı 659 ms. Bu değer görüntü/dokunma gecikmesi veya tekrarlı performans ortalaması değildir.

İlk görüntü iPhone'un Siri etkinleştirme uyarısıydı. Kullanıcı Siri'yi açtıktan sonra CarPlay Haritalar arayüzü görüntülendi. Boş harita döşemeleriyle aynı anda kullanıcı iPhone'un hattı/hücresel interneti olmadığını doğruladı; internetten rota ve döşeme indirme testi yapılmadı. Gerçek arama, müzik çıkışı, mikrofon kalitesi ve çoklu dokunma için ayrı testler gerekir.

Note 12'de kurulu Android Auto: **17.9.664054-release**. Bu, test cihazında okunan sürümdür; dünyadaki en son dağıtım olduğu iddia edilmez. Kullanıcı bu tur AA eşleştirmesini doğruladı; önceki çalışmada Note 9 alıcı / Note 12 gönderici AA görüntüsü alınmıştı. Yeni iki protokollü servis geçişi ayrıca doğrulanır.

## Özellikler

| Özellik | Kaynak / kapsam | Bu projedeki durum |
|---|---|---|
| Kablosuz CarPlay keşfi ve eşleşme | Aksesuar iAP2/CarPlay SDP; iPhone araç listesi | Gerçek iPhone ile doğrulandı. Keşif testi kendi başına medya oturumu açmaz. |
| Kablosuz CarPlay kontrol ve görüntü | Yerel deneysel kimlik + Wi-Fi + AirPlay | Gerçek kontrol oturumu ve Haritalar görüntüsü doğrulandı. Apple onaylı ürün veya genel kimlik dağıtımı anlamına gelmez. |
| Android Auto kablosuz / USB | Mevcut AA alıcısı | Mevcut özellikler korunur; gerçek PX3 / otomatik kontak testi bekler. |
| Ortak telefon seçimi | Alıcı uygulaması | Tek APK; MAC bazında Android/iPhone seçimi, aktif protokol sahipliği, yanlış iPhone AA hedefini eleme. |
| Açılış / uyandırma / tekrar bağlanma | Alıcı uygulaması, Android arka plan kuralları | 1.4.3 kaynakta foreground CarPlay hizmeti, kayıtlı iPhone, Bluetooth/boot/unlock tetikleri, 2–60 s sınırlı geri çekilme. Aktif/kurulan oturum yeniden başlatılmaz. Foreground hizmeti ve kayıtlı iPhone bağlantısı Note9'da doğrulandı; gerçek teyp boot/kopma döngüsü bekler. |
| Tam ekran / yüzey ömrü | Alıcı uygulaması | 1.4.3 kaynakta ilk kareden sonra kontroller gizlenir; menüyle geri açılır. Activity arka plana geçince oturum yerine yalnız ekran yüzeyi ayrılır. |
| Daha büyük AA yazıları | AA oturum DPI tanıtımı | 140/160/190/220/260 ve özel DPI. 260 seçimi boyut sınırı geçersiz kılmasını açar; yeni oturum gerekir ve yerleşim değişebilir. |
| Navigasyon, müzik, mesaj ve arama uygulamaları | Telefonun desteklenen uygulamaları | Telefonun sunduğu projeksiyon taşınır. Her uygulama ve ses yolu henüz ayrı ayrı doğrulanmadı. |
| Siri / telefon asistanı ve mikrofon | Telefon + ses kanalları + izinler | Siri etkinleştirme engeli gerçek cihazda çözüldü. Arka plan mikrofonuna modern Android ek kısıtlar uygular; araçta ses testi bekler. |
| CarPlay widget / Live Activities / yeni MiniPlayer | iPhone/iOS ve uygulaması | Güncel iOS tarafından üretilir; alıcı video/dokunma taşır. Tek tek cihaz testleri bekler. APK'dan iOS yetkisi veya yeni uygulama kategorisi verilemez. |
| Çoklu dokunma | Alıcı giriş protokolü | İki temas gönderimi kaynakta mevcut. iPhone üzerinde iki parmaklı harita testi bekler. |
| Dinamik CarPlay boyutlandırma / ikinci ekran | Araç/alıcı görüşmesi ve ek video yüzeyleri | 1.4.3 ana CarPlay tuvali en çok800×480 / 30FPS, ekran oranına göre hesaplanır. Oturum başında ekran oranı seçilir; gerçek zamanlı yeniden görüşme veya gösterge paneli entegrasyonu tamamlanmadı. |
| Uzamsal / çok kanallı ses | Destekleyen araç ses sistemi | Donanım ve uçtan uca test olmadan destek ilan edilmez. |
| CarPlay Ultra gösterge ve araç verileri | Otomobil üreticisi / MFi / araç entegrasyonu | Doğrulanmadı. iPhone'daki Ultra bağlantı yazısı, hız/devir göstergesi veya diğer Ultra işlevlerinin çalıştığını kanıtlamaz. Klima/araç kumandaları kullanıcının kapsamı dışındadır. |
| Park halinde video, dijital araç anahtarı | Ayrı araç yetenekleri, donanım ve onay | Genel telefon projeksiyonuna eşit değildir; bu kurulumda açılmış sayılmaz. Video-in-car varsayılan kapalıdır; park verisi kaynağı yok. |

## Güncel belgelerden çıkan sınırlar

Apple, alıcı/araç üreticileri için kablosuz bağlantı, multitouch, dinamik ekran boyutları, sürekli Siri, ikinci ekran ve uygun donanımda Spatial Audio başlıklarını listeliyor. Ultra tüm sürücü ekranları ve üretici tasarımıyla bütünleşiyor; araç sistemi entegrasyonu ve MFi yolunu ayrı tanımlıyor. Bu, Android APK'ya bir SDK bağımlılığı ekleyerek bütün Ultra işlevlerini kazanmakla aynı şey değildir. [Apple CarPlay geliştirici sayfası](https://developer.apple.com/carplay/).

WWDC26, iOS 27 için yeni liste/Now Playing sunumları, sesli konuşma ve uygun araçta park edilmiş video uygulamalarını anlatıyor. Rota paylaşımı destekleyen araç ve kullanıcı onayı ister. Bu API'ler esasen iPhone uygulamalarına yöneliktir; alıcıdaki görüntü protokolünü desteklemek iPhone uygulaması yetkilerini vermez. [WWDC26: Rev up your CarPlay app](https://developer.apple.com/videos/play/wwdc2026/212/).

Google'ın kategori tablosu Android Auto ile Android Automotive OS'yi ayırıyor: medya, iletişim, navigasyon, POI, IoT, hava durumu ve park edilmiş uygulamaların platform/test kanalı kapsamları farklıdır. Android 7.1 teyp üzerine alıcı APK kurmak, teybi Android Automotive OS'ye dönüştürmez. [Android for Cars](https://developer.android.com/training/cars).

Car App Library sayfasında 7 Ekim 2026 tarihli **1.9.0-alpha03** incelendi: tab, bölüm başlığı ve stil API değişiklikleri bulunuyor. Bunlar alıcının telefon projeksiyonu protokol sürümüyle aynı şey değildir; sırf sürüm numarası büyüsün diye alfa SDK'ya geçilmedi. [Car App sürüm notları](https://developer.android.com/jetpack/androidx/releases/car-app#1.9.0-alpha03).

Google'ın resmî kablosuz AA koşulları telefon tarafında 5 GHz Wi-Fi desteği ve uyumlu Android sürümü ister. Bizim 2,4 GHz profili, yalnızca bu bandı destekleyen teyp için uygulama düzeyinde bir uyumluluk denemesidir; PX3'te resmî sertifikalı kablosuz AA uyumu veya tüm telefonlarda çalışma garantisi değildir. [Google AA gereksinimleri](https://support.google.com/androidauto/answer/6348019).

## Forum kontrolü ve araştırma sınırı

Apple'ın güncel CarPlay forum dizininde iOS 27 harita paneli yerleşimi, CarPlay bağlantısında arka plan tetikleme ve araç tarafında video yeteneği hakkında kullanıcı raporları incelendi. Bunlar tek başına Apple'ın onayladığı çözüm sayılmadı. Video yeteneğiyle ilgili tekil başlık tarayıcı doğrulamasında açılamadı; doğrulanmamış yanıt kullanılamadı. [Forum dizini](https://developer.apple.com/forums/topics/app-and-system-services/app-and-system-services-carplay), [video yeteneği sorusu](https://developer.apple.com/forums/thread/854477).

DiPlay'ın güncel açık sorun başlıklarında Wi-Fi Direct meşgul durumu ve tekrar bağlanma raporları da görüldü; başka cihazların raporları bizim PX3 ölçümü yerine kullanılmadı. [DiPlay sorunları](https://github.com/shihabal3amri/DiPlay/issues).

Bu inceleme güncel kamuya açık dokümanlarla sınırlıdır; kapalı MFi/üretici protokol paketleri incelenmiş veya bütün özellikler uçtan uca test edilmiş değildir. Google'ın 2026 blog arama kaydı bulundu fakat tam makale gövdesi alınamadığından teknik sonuçlar buna dayandırılmadı.

Son1.4.4tesliminde4.811ana/16CarPlaytest geçti; Note9kullanıcı son görünümü doğruladı. Testtelefona kurulan yerel deneysel APK ile GitHub'daki kimliksiz genel APK aynı provizyon içeriğine sahip değildir. Siri/Gemini'nin fiziksel teyp ses komutu testi kullanıcı isteğiyle sonraya bırakıldı; Ultra ve gerçek araç test sınırları geçerliliğini korur.
