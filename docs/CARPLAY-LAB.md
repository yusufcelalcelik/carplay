# Kablosuz CarPlay denemesi

## Durum

`carplay-lab`, ana Android Auto uygulamasından ayrı kurulabilen yerel bir alıcı prototipidir. Paket: `com.yusufcelik.fiorino.carplaylab`, sürüm `0.1.0-lab`. Android 7.0+ ve ARM32/ARM64 hedeflenir. Ana uygulama 1.3.2 olarak korunur.

DiPlay'ın `615b32ce0b40cd0b99ce94ba23a512001553684f` sürümündeki `shared/` motoru `carplay-engine` içine alındı. iAP2 Bluetooth başlangıcı, Wi-Fi Direct, AirPlay kontrolü, video/ses ve dokunma motoru bağlandı. Kaynak/lisans bilgisi [UPSTREAM.md](../carplay-engine/UPSTREAM.md) dosyasındadır. Ayrı lisanslı BYD HUD görselleri alınmadı. Upstream demo Android Auto servisi manifestten çıkarıldı.

**Henüz iPhone ile CarPlay oturumu doğrulanmadı.** Bu ekran çalışan CarPlay görüntüsü değildir. Kaynak APK hiçbir aksesuar kimliği içermez. Kimlik yoksa veya geçersizse ağ/CarPlay kurulumu başlatılmaz; kullanıcıya neden bildirilir. Yerel anahtarın sertifikasıyla eşleşmesi, iPhone'un onu kabul ettiğini kanıtlamaz.

![Yerel emülatörde gerçek prototip ekranı; CarPlay oturumu açık değil](screenshots/08-carplay-lab.png)

## Yerel derleme

Ana projeyle aynı JDK 21, SDK 36 ve NDK 28.2.13676358 kullanılır. Cloud build gerekmez.

```sh
./gradlew :carplay-lab:assembleDebug :carplay-lab:testDebugUnitTest \
  :carplay-engine:testDebugUnitTest \
  --tests '*MfiAuthenticationClientTest' --tests '*Iap2*' \
  --tests '*AirPlayCryptoTest' --tests '*WifiP2p*' \
  --tests '*PairSetupTest' --tests '*PairVerifyTest' \
  -PfiorinoNdkVersion=28.2.13676358
```

Çıktı: `carplay-lab/build/outputs/apk/debug/carplay-lab-debug.apk`. Debug imzasıdır. Motorun JDK HTTP sunucusu kullanan host testleri için yalnızca test derleme sınıf yoluna yerel JDK API'si eklenir; APK'ya eklenmez. İki SoftAP testindeki Android-hidden setter çağrıları SDK 36 ile derlenebilmesi için reflection kullanır.

İsteğe bağlı, açıkça sağlanmış yerel aksesuar girdileri:

```text
/absolute/private/runtime-assets/
└── offline-mfi/
    ├── identity.pk8
    └── certificate.p7b
```

Bu iki dosya dışındaki girdiler build sırasında reddedilir. Dosyalar, anahtarlar ve kimlik içeren özel APK genel kaynak deposuna veya genel release'e eklenmemelidir.

```sh
FIORINO_CARPLAY_AUTH_ASSETS=/absolute/private/runtime-assets \
  ./gradlew :carplay-lab:assembleDebug -PfiorinoNdkVersion=28.2.13676358
```

Sağlayıcı, yerel P-256 aksesuar imzası kullanır. Rastgele/self-signed test kimlikleri iPhone'a kabul edilmiş aksesuar kimliği olarak sunulmaz. Sentetik test girdileri yalnızca birim testlerinde üretilir. Upstream'in yayımladığı deneysel APK'daki Carlinkit kimliği bu projeye alınmadı; kaynak kod lisansı bu kimliği yeniden lisanslamaz.

## Cihaz denemesi

1. Diğer yansıtma uygulamalarını durdurun. Lab uygulaması yalnızca ekranda açıkken çalışır; arka planda/kontakta otomatik CarPlay desteği henüz eklenmedi.
2. Android tarafında Wi-Fi, Bluetooth, konum ve mikrofon izinlerini sağlayın.
3. “iPhone eşleştir” ile görünürlüğü açın; iPhone'daki sistem eşleştirmesini onaylayın. “Kayıtlı iPhone seç” ile hedefi saklayın.
4. Normal uygulama API'leri gerçek yerel Bluetooth adresini gizliyorsa ADB ile okunan gerçek adres, yalnızca lab kurulumunda `hostBluetoothMac` intent girdisiyle verilebilir. Yer tutucu `02:00:00:00:00:00` kabul edilmez. Adres kaynak koda sabit yazılmaz.
5. Doğrulanabilir yerel aksesuar girdisi ve gerçek Bluetooth adresi hazırken “CarPlay başlat” seçilir. Wi-Fi Direct kanal 6, 800×480 H.264, 30 FPS başlangıç ayarlarıdır. 2,4 GHz araçta gerçek performans henüz ölçülmedi.
6. iPhone'daki CarPlay onayını tamamlayın. Listede görünme, eşleşme, kontrol oturumu ve görüntü/ses ayrı aşamalardır. Her aşama cihazda doğrulanmalıdır.

Ekrandan ayrılma ve “Durdur” alıcıyı kapatır. Bu prototipte foreground servis, kontakta başlama, çağrı/ses yönlendirmesinin araç üzerinde doğrulanması ve ana uygulamaya aktarım tamamlanmış değildir.

## Bu adımda doğrulananlar

- Yerel debug APK derlendi ve imzası doğrulandı.
- 92 hedefli motor testi ve 2 Lab ön kontrol testi geçti. Motorun tüm upstream testleri çalıştırılmış sayılmaz.
- Android 11, 1024×600 yerel emülatöründe Lab kuruldu ve gerçek ekranı açıldı.
- Note 9'un Downloads klasörüne test APK'sı kopyalandı. Fiziksel Lab kurulum/açılışı henüz doğrulanmadı; cihaz son kontrolde kilitli/uykudaydı.
- Xcode cihaz envanterinde fiziksel iPhone çevrimdışıydı. Simülatör envanterinin görünmesi gerçek iPhone veya Bluetooth CarPlay testi değildir.

## Devam edilecekler

Önce geçerli yerel aksesuar provizyonu ve fiziksel iPhone erişimini çöz, gerçek el sıkışmasını kaydet. Bunlar olmadan “native CarPlay çalışıyor” veya “iPhone araç olarak tanıyor” sonucu bildirme. Kullanıcı uzaktayken fiziksel onay engel olursa uygulama içi Android Auto kablosuz eşleştirme akışı ve ölçülebilir performans iyileştirmeleriyle devam et. Note 12 kullanıcıyla dışarıda; alıcı olarak Note 9 kalacak.
