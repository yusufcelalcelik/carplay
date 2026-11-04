# Fiorino Auto

Fiat Fiorino'daki Android 7.1 / Rockchip PX3 veya PX5 teyp için Android Auto alıcısı. Open Headunit'in `ae74788d8312dd7a735775ddc21370f1741a7af1` sürümünden türetilmiştir. AGPL-3.0 lisansı ve önceki yazarların telif bildirimleri korunmuştur. Önceki proje belgeleri [README_UPSTREAM.md](README_UPSTREAM.md) dosyasındadır.

## Kurulum

1. APK'yı **teybe** yükleyin. Telefonda Android Auto bulunmalı.
2. Mikrofon, yakın cihazlar/konum ve gerekli otomatik başlatma izinlerini kurulum ekranından verin. Teybin pil optimizasyonundan uygulamayı çıkarın.
3. Teybin kendi Bluetooth uygulamasından telefonu eşleştirin. **Telefondaki** bu Bluetooth kaydında Telefon aramaları açık olsun.
4. Kablosuz modda Native AA seçilidir. Kurulum ekranından telefonu seçip ilk bağlantıyı onaylayın. Sonraki açılışlarda otomatik bağlantı etkinleşir. OEM Bluetooth modülü standart Android arayüzünde görünmeyebilir. Uygulamanın HCT/harici Bluetooth seçenekleri cihazın firmware desteğine bağlıdır.
5. USB kullanımı için telefonun veri kablosunu teybin USB Host portuna bağlayın, USB erişim iznini verin. Bazı portlar yalnızca şarj sağlar.
6. Ayarlar → **Fiorino ayarları** altında anons, mikrofon, kalite ve DPI ayarlarını yapın.

## Bu sürümdeki eklemeler

- İlk çalıştırmada 480p / 30 FPS / H.264, Türkçe dil ve kontak/ekran uyanışında otomatik başlatma.
- Dinamik ses odağı modunda ortak çıkış ve gerçek çalınan navigasyon/asistan sesine göre medya kısma. Başlangıç seviyesi %30, kullanıcı tarafından %0–100 ayarlanabilir. Android sistem ses düğmesinin seviyesi değiştirilmez.
- Kullanılmayan navigasyon kanalı müziği kısmaz. Bitiş bildirimi kaybolsa da ses tamponu boşalınca kısma kalkar. Yeni oturum kendi ses durumuyla başlar. Diğer uygulamaların veya OEM donanımının uyguladığı ses kısması bu denetimin dışındadır.
- Otomatik mikrofon, açılabilen VOICE_COMMUNICATION, MIC, DEFAULT veya VOICE_RECOGNITION kaynağını dener. 16 kHz açılamıyorsa 48 kHz kayıt denenir ve Android Auto'nun 16 kHz formatına dönüştürülür. Sessizlik giriş değiştirmek için tek başına yeterli değildir. Elle giriş ve örnekleme hızı ayarı korunur.
- Kısa mikrofon testi ses dosyası kaydetmeden tepe seviyesini gösterir. Bluetooth SCO ve HFP görüşme mikrofonu gerçek görüşmeyle ayrıca denenmelidir.
- Otomatik görüntü kalitesi yeterli uzunluktaki kablosuz oturum sonunda kare kaybı ve render atlamalarını değerlendirir. %5 ve üzeri kayıpta sonraki bağlantı 480p olur. En fazla %1 kayıplı üç oturum sonunda 720p denenir. Manuel seçim otomatiği kapatır. Protokol doğrudan video bitrate kontrolü sunmaz. Kalite değişimi mevcut oturumu kesmez.
- DPI hazır değerleri, özel DPI ve mevcut gelişmiş görüntü ayarları.

## 1.1 — Hıza göre teyp sesi ve uygulamadan eşleştirme

Hıza göre ses ayarı yalnızca teybin Android medya sesine uygulanır. Telefonun ses ayarını değiştiren Wi-Fi veya AVRCP komutu gönderilmez. Teybin canlı GPS hızı kullanılır. 90 km/sa hızda varsayılan artış üç kademedir, 0–6 arasında seçilebilir. Geçiş iki saniyede en fazla bir kademedir. GPS verisi beş saniyeden eskiyse ek ses kaldırılır. Elle sessize alma korunur ve ses düğmesiyle yapılan değişiklik yeni temel seviye kabul edilir. Görüşme sırasında ses yönetimi bekler. OEM amplifikatörü Android medya seviyesini kullanmıyorsa fiziksel ana ses ayrıca cihazda incelenmelidir.

Fiorino ayarları → Telefon eşleştirme ve arka plan bağlantısı ekranında teyp 180 saniye görünür yapılabilir. Telefonun Bluetooth aramasından teybi seçip sistem eşleştirmesini onaylayın. Ardından uygulamadan eşleşen telefonu seçin. Telefon, Native AA ve açılış/uyanış sonrası otomatik bağlantı için kaydedilir. Ayrı OEM Bluetooth modülü Android'e açılmıyorsa ilk eşleştirme fabrika uygulamasından yapılır. Desteklenen OEM eşleşme kayıtları seçim ekranına dahil edilir.

Telefonun Bluetooth kaydında **Telefon aramaları açık**, **Medya sesi kapalı** olsun. Android Auto medya aktarımı Wi-Fi üzerinden yapılırken Bluetooth medya ses eşitlemesi böylece devre dışı kalır. Teybin Bluetooth modülünün davranışı fiziksel araçta doğrulanmalıdır.

Çözünürlük bağlantı devam ederken değişmez. Otomatik kalite, bir oturumun ölçümlerini sonraki bağlantıda kullanır. DPI ve manuel çözünürlük değişiklikleri de yeniden bağlantıda uygulanır.

Derlemeler yerel Gradle/Android SDK ile yapılır. Bulut derleme kurulmamıştır. Depoda otomatik GitHub Actions derleme iş akışı bulunmaz.

## Arama sesi

Hedef, aramaların araç hoparlörleri ve araç mikrofonundan yapılmasıdır. Bunun için telefon-teyp HFP bağlantısı gerekir. Önce Android Auto kapalıyken teybin Bluetooth uygulamasında gelen/giden arama deneyin. Sonra Android Auto açıkken tekrar deneyin. Teybin OEM modülü görüşme mikrofonunu Android uygulamalarına açmayabilir. Asistan mikrofon ayarı HFP görüşmesinin mikrofonunu değiştirmez.

## Araçta doğrulama

- Telefon kilitliyken beş tam açılış ve beş uykudan dönüş. Her seferinde bağlanma süresini kontrol edin.
- Rota başlamadan bağlantı kurun. Müzik normal ses düzeyinde başlamalı.
- Tek ve peş peşe navigasyon anonsları. Sonrasında ses eski seviyesine dönmeli.
- Anons sırasında ses düğmesini çevirin. Yeni kullanıcı seviyesi korunmalı.
- Anons sırasında çağrı alın. Karşılıklı sesi ve arama sonrasında müziği kontrol edin.
- Anons sırasında bağlantıyı kesin, yeniden bağlanın. Kısık ses taşınmamalı.
- 30 dakika navigasyon ve müzik. Dokunma, ses kesintisi, sıcaklık ve yeniden bağlantı davranışını kontrol edin.

Gerçek teyp ve telefon üzerinde bu kontroller henüz yapılmadı. APK bir önizleme sürümüdür. CarPlay henüz uygulanmadı. Bağlantı ve oturum ayrımı kaynak projedeki transport yapısında korunur, CarPlay için ayrı protokol/donanım incelemesi gerekir.

## Derleme

JDK 21, Android SDK 36, CMake 3.22.1 ve Android NDK gerekir. Bu çalışma NDK 28.2.13676358 ile derlenmiştir. SDK yolunu `ANDROID_HOME` ile belirtin.

```sh
./gradlew :app:assembleGithubDebug :app:testGithubDebugUnitTest -PfiorinoNdkVersion=28.2.13676358
./gradlew :app:assembleGithubRelease -PfiorinoNdkVersion=28.2.13676358
```

Uygulama kimliği `com.yusufcelik.fiorinoheadunit`. APK hem armeabi-v7a hem arm64-v8a içerir. İmzalı release için git tarafından dışlanan `key.properties` dosyasında storeFile, storePassword, keyAlias ve keyPassword alanlarını tanımlayın. Güncellemeler için aynı imza anahtarını saklayın. Anahtarlar depoya eklenmemelidir.

## Kaynaklar

- https://github.com/andreknieriem/open-headunit
- https://headunit.andrerinas.com/guides/wireless/
- https://headunit.andrerinas.com/guides/settings/
- https://developer.android.com/media/optimize/audio-focus

## Ekran görüntüleri

1024×600 yerel Android 11 emülatöründen alınmıştır. Android Auto telefon yansıtması ve gerçek araç bağlantısı bu görsellerde açık değildir.

![Ana ekran](docs/screenshots/01-home.png)
![Ses ve hız ayarları](docs/screenshots/02-sound-speed.png)
![Bluetooth eşleştirme](docs/screenshots/04-bluetooth.png)
![Görüntü ayarları](docs/screenshots/05-display.png)
