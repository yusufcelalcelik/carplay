# Fiorino Auto — veri işleme açıklaması

Güncelleme: **5 Kasım 2026**. Bu belge `com.yusufcelik.fiorinoheadunit` paketli Fiorino Auto kaynak sürümünü açıklar. Upstream Open Headunit belgesi, özgün atıflarıyla [PRIVACY_UPSTREAM.md](PRIVACY_UPSTREAM.md) dosyasında korunur; önceki geliştiricinin iletişim adresi bu fork'un destek adresi değildir.

## Yerel olarak işlenen bilgiler

Telefon projeksiyonu görüntü, ses ve giriş olayları alıcı ile telefon arasında işlenir. Uygulama telefon seçimi/MAC adresi, ekran/ses ayarları ve bağlantı tercihlerini cihazın uygulama alanında tutar. CarPlay, yerel AirPlay kimliğini ve eşleşmiş telefonların yetkilendirme kayıtlarını da saklar. Açıkça sağlanmış aksesuar provizyonu varsa bu, telefon eşleşmelerinden ayrı bir alıcı girdisidir.

Mikrofon izni, kullanılan projeksiyon/asistan ses yoluna ses aktarımı içindir. Mikrofon test ekranı seviye ölçer; bu test bir ses dosyası üretmez. Konum izinleri Android'in kablosuz keşif koşulları ve etkinleştirilen GPS/hız özellikleri için kullanılır. Foreground hizmetleri devam eden bağlantı ve ilgili medya/mikrofon kullanımı içindir.

## Ağ erişimi ve diğer hizmetler

Projede reklam veya analiz SDK'sı eklenmemiştir. Bununla birlikte “uygulama hiçbir ağ servisine erişmez” iddiası doğru olmaz: kablosuz telefon bağlantısı ağ kullanır; güncelleme kontrolü GitHub'a HTTPS isteği yapar. Mevcut 1.4.4 kontrol kaynağı upstream Open Headunit Releases adresidir; fork kaynağına taşıma işi [yol haritasında](ROADMAP.md) izlenir. GitHub, normal ağ isteğinde IP ve istemci başlıklarını görebilir.

Android Auto, Siri, harita/medya uygulamaları ve isteğe bağlı Google Nearby gibi bileşenlerin kendi veri işlemleri ilgili sağlayıcıların koşullarına tabidir. Bu alıcı, telefon hizmetlerinin internet üzerinden nasıl çalıştığını değiştirmez.

## Tanılama ve paylaşım

Ayar ve tanılama düzeyine göre loglar alıcıda tutulabilir. Bağlantı adları, adresler, zamanlar, sürüm ve oturum bilgileri içerebilirler. Log dışa aktarma/paylaşma kullanıcı işlemiyle yapılır; dışa aktardığınız dosyayı yayımlamadan önce kişisel bilgileri ve ağ sırlarını gözden geçirin. Proje issue kayıtlarına gerçek aksesuar anahtarı, sertifika girdisi, keystore veya özel test APK'sı eklemeyin.

## Kayıtların silinmesi

**Cihazları yönet**, uygulamanın telefon tercihlerini ve CarPlay yetkilendirmelerini onayla kaldırır. Alıcı adresi, ekran/ses ayarları ve alıcı donanım provizyonu korunur. Sistem Bluetooth eşleşmeleri ve iPhone'daki araç kaydı kendi sistem ayarlarından yönetilir. Uygulama kaldırıldığında Android'in uygulama veri alanı kaldırılır; dışa aktarılmış log/APK kopyaları ve sistem eşleşmeleri ayrıca yönetilir.

Sorular için [bu deponun Issues sayfasını](https://github.com/yusufcelalcelik/carplay/issues) kullanın.
