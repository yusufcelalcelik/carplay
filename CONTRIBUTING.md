# Katkı rehberi

Önce [README](README.md), [özellik matrisi](docs/FEATURE-COMPATIBILITY-2026.md) ve [yerel build rehberini](docs/BUILDING.md) okuyun. Kullanım belgeleri Türkçedir; kod sembolleri mevcut İngilizce düzeni izler.

## Bir değişiklik hazırlarken

1. Sorunu, mevcut davranışı ve beklenen sonucu açıklayın. Yeni bir özellik için telefon uygulaması, alıcı protokolü ve araç donanımı kapsamlarını ayırın.
2. Küçük, incelenebilir değişiklikler yapın. İlgisiz biçimlendirme veya tüm upstream'i yenileme işini aynı değişikliğe katmayın.
3. Android Auto minSDK 21 ve CarPlay'in API 24 kapısını koruyun. Daha yeni API'ler için çalışma zamanı kontrolü gerekir.
4. Bağlantı kapanışı, ses odağı, yüzey sahipliği ve yeniden deneme değişikliklerini anlamlı hedefli testlerle doğrulayın. Gerçek cihaz sonucu ile JVM/emülatör testini ayrı belirtin.
5. Kullanıcıya görünen davranış değiştiyse kurulum notunu ve CHANGELOG'u güncelleyin.

## Doğrulama

Build ve testler yerel Gradle ile yapılır; cloud build/Actions eklemeyin. Komutlar [BUILDING.md](docs/BUILDING.md) içindedir. Yalnız belge değişikliklerinde ilgili bağlantıları, kaynak yollarını ve komutları kontrol etmek yeterlidir; tüm APK testlerini tekrar koşmak gerekmez.

PR açıklaması sorunu ve son davranışı önden anlatmalı; test komutunu/sonucunu, kullanılan cihazı ve kalan fiziksel testleri içermelidir. “Bağlandı” ifadesinde keşif, Bluetooth eşleşmesi, kontrol oturumu ve görüntü/ses aşamalarını ayırın. Desteklenmeyen Ultra/araç özelliklerini çalışıyor diye ilan etmeyin.

## Kaynaklar ve özel girdiler

- Upstream telif/lisans bildirimlerini koruyun; motor değişikliklerini `carplay-engine/UPSTREAM.md` altında kaydedin.
- Gerçek aksesuar kimliği, özel anahtar, keystore, parola veya özel kimlikli APK paylaşmayın.
- Hata kaydında cihaz/MCU, Android sürümü, bağlantı türü, tekrarlama adımları ve gerekli kısa log kesiti yeterlidir. Kişisel ekran içeriğini, telefon/MAC adreslerini ve Wi-Fi parolalarını çıkarın.
- Yeni araçların pinlerini yalnız görünüş veya kablo rengi benzerliğine göre belgelemeyin. Ölçüm/kılavuz ve kesin model ayrımını yazın.

İstek ve hata kayıtları için [GitHub Issues](https://github.com/yusufcelalcelik/carplay/issues) kullanılır.
