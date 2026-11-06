# USB kablosuz adaptör modu — 1.5.0 önizleme

## Ne yapar?

Eski Android telefon araçta kalır ve **aracın kendi kablolu Android Auto USB girişine** veri kablosuyla takılır. Sürücünün Android telefonu eski telefona Bluetooth/Wi-Fi üzerinden bağlanır. Fiorino Auto, AAP verisini USB aksesuar arabirimi ile TCP arasında taşır; görüntüyü eski telefonda çözüp yeniden kodlamaz. Görüntü/ses ve TLS uçları sürücü telefonu ile aracın kendi Android Auto sistemidir.

Bu mod Android teypte çalışan mevcut AA/CarPlay **alıcı modundan ayrıdır**. Tek APK içindedir, aynı anda alıcı ve köprü çalıştırılmaz. Ford Kuga, kullanıcının hedef aracıdır; model yılı/SYNC sürümü henüz bilinmiyor ve araç testi yapılmadı. Araçta çalışan kablolu Android Auto olması gerekir.

**iPhone → fabrika CarPlay köprüsü bu sürümde yoktur.** Mevcut CarPlay motoru iPhone görüntüsünü Android alıcıda açar; bu, araca iPhone gibi davranan bir CarPlay göndericisi değildir. Aracın yalnız CarPlay desteklemesi AA USB köprüsü için yeterli değildir.

## İlk kurulum

1. Eski telefona 1.5.0 önizlemeyi kurun. Wi-Fi, Bluetooth ve konumu açın. USB aksesuar modu ve Wi-Fi Direct donanım desteği gerekir.
2. Uygulamada **Ayarlar → Fiorino → Native Android Auto kurulumu → USB kablosuz adaptör modu** ekranını açın.
3. Eşlerin iki Android telefonunu eski telefonla bir kez Bluetooth üzerinden eşleştirin. Sonraki seçimlerde eşleşmeleri silmek gerekmez.
4. Eski telefonu aracın **Android Auto veri USB girişine** takın. Şarj kablosu/girişi yeterli değildir. USB uygulama seçicisinde **Fiorino Auto** ve USB erişim onayını seçin. Telefonun kendi Android Auto uygulaması USB’yi açıyorsa bu uygulamanın USB varsayılanını sistem ayarlarından temizleyip kabloyu yeniden takın; uygulama paketleri otomatik devre dışı bırakılmaz.
5. **Sürücü telefonunu seç / değiştir** ile bağlanacak Android telefonu seçin. Sürücü telefonundaki ilk Android Auto onayını tamamlayın.
6. Varsayılan tercih her USB takılışında telefon seçimidir. **USB takılınca son telefonu bağla** tercihi açılırsa son seçilen telefon denenir. Ekrandaki ve bildirimdeki “Telefon değiştir” seçeneği her zaman kullanılabilir.

USB takılma olayı uygulamaya yönlendirildiğinde otomatik giriş çalışır. Android’in uygulama seçimi, izin onayı ve kilit ekranı gereklilikleri atlanmaz. İşlem sonlandırılırsa servis kendiliğinden körlemesine tekrar başlamaz; yeni USB takılma veya açık başlatma gerekir.

## Telefon değiştirme ve sınırlar

- Telefon seçimi kaydedilir; mevcut veri ve Bluetooth kanalları kapatılır. Eski USB okuma işinin bitmesi beklenir; bitmezse ikinci USB okuma işi açılmaz.
- Önceki ağ kaldırılıp yenisi kurulur. Android 10+ desteklenen API ile yeni SSID/parola kullanır; daha eski Android’de sistemin grup oluşturma davranışı geçerlidir, yeni parola garantisi yoktur.
- Bluetooth kontrol kanalı yalnız seçilen eşleşmiş telefon için kabul edilir. Bir oturumda yalnız bir USB/TCP veri çifti açılır.
- **Kabloyu çıkarmadan sorunsuz iki telefon geçişi henüz doğrulanmadı.** Şeffaf aktarım, aracın TLS oturumunun tarafı değildir ve şifreli protokole “telefon değiştir” mesajı eklemez. Bazı teypler yeni oturum için fiziksel USB bağlantısının yenilenmesini ister. Arayüz gerektiğinde kabloyu çıkarıp takmayı söyler; uygulama root/USB gadget reset yetkisi varsaymaz.
- 90 saniyede ilk TCP bağlantısı gelmezse kaynaklar kapanır. Veri oturumundaki TCP okuması 30 saniye ilerlemezse oturum sonlanır. Hata/bitme bildiriminden yeni başlatma veya USB takılma kullanılabilir; genel donanım uyumluluğu iddiası yoktur.
- Arama sesi, aracın telefon Bluetooth eşleşmesi ve SYNC davranışı gerçek araçta ayrıca test edilmelidir. Bu sürüm HFP arama sesini başka bir telefona köprülemez.

## Doğrulama

Yerel yazılım testleri: parçalı/birleşik TCP okumaları, bütün AAP fragment bayrakları, 0–65535 bayt gövdeler, eksik çerçevenin USB’ye yazılmaması ve normal alıcı/köprü tercih ayrımı. Bunlar gerçek USB, Bluetooth el sıkışması, SYNC bağlantısı veya iki sürücü geçişini doğrulamaz.

Araçta test sırası: önce tek telefonla kablolu Android Auto’nun çalıştığını doğrulayın; ardından köprüyle ilk görüntü, dokunma, navigasyon sesi, mikrofon/arama ve en son iki telefon arasında geçiş/kontak döngüsü deneyin. Hata raporuna telefon modeli/Android sürümü, araç yılı/SYNC sürümü ve ekrandaki aşamayı ekleyin. Ağ parolası veya kişisel kimlik paylaşmayın.

## Teknik kaynaklar

- [Android USB accessory API](https://developer.android.com/develop/connectivity/usb/accessory): araç USB host, eski Android telefon aksesuar/peripheral rolündedir. USB host alıcı koduyla aynı yön değildir.
- [AAWirelessGateway](https://github.com/nisargjhaveri/AAWirelessGateway): yedek Android telefonu USB/kablosuz Android Auto köprüsü olarak kullanan birincil proje örneği; uygulamaya kaynak kodu alınmadı.
- [aa-proxy-rs](https://github.com/aa-proxy/aa-proxy-rs): ayrı Linux tabanlı USB/kablosuz proxy örneği; Android APK’ya eklenmiş bir bağımlılık değildir.
