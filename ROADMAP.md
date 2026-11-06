# Yol haritası

Durum: 6 Kasım 2026. Kaynakta bulunan işlev ile gerçek araç doğrulaması farklı izlenir.

## Kaynakta tamamlananlar

- [x] Tek APK içinde Android Auto ve CarPlay alıcıları; ortak telefon seçimi.
- [x] Kayıtlı iPhone'a doğrudan açılış ve CarPlay foreground oturum hizmeti.
- [x] Sınırlı geri çekilmeyle tekrar bağlantı; AA/USB/Self ile sahiplik/temizlik sırası.
- [x] Ekran oranı, tam ekran/çentik düzeni ve AA DPI seçenekleri.
- [x] Uygulama telefon tercihlerini sıfırlama ve sistem Bluetooth yönetimine geçiş.
- [x] Kimliksiz genel release ile özel yerel test girdilerinin ayrılması.

## Yakın doğrulama ve bakım

- [ ] Gerçek 2017 Fiorino Pop'a takılı teybin MCU, SoC ve tesisatını doğrulamak.
- [ ] Sonradan takılan Premio direksiyonun sinyal yolunu ölçmek; varsa MCU/Android tuşlarını eşlemek.
- [ ] PX3/Android 7.1 üzerinde kontak/uyku ve tekrar bağlantı testleri.
- [ ] Gerçek arama, mikrofon, navigasyon anonsu ve ses toparlama testi.
- [ ] Uzun oturumda kare kaybı, sıcaklık, dokunma ve bağlantı süresi dağılımını ölçmek.
- [ ] Uygulama içi güncelleme denetimini bu fork'un Releases kaynağına taşımak. 1.4.4'te upstream adresi kaldığı için şimdilik bu deponun Releases sayfası kullanılmalı.

## Ayrı araştırma gerektirenler

- [ ] Siri/Gemini doğal dil komutuyla fiziksel teyp ses kontrolü. Kullanıcı ses testini sonraya bıraktı; çalışıyor diye ilan edilmiyor.
- [ ] Daha fazla telefonda CarPlay widget/Live Activity, medya tuşu, çoklu dokunma ve ses yolu doğrulaması.
- [ ] Desteklenebilir CarPlay provizyon/sağlayıcı yolu; deneysel girdiden genel dağıtım yetkisi çıkarılmıyor.
- [ ] Kesin kart/MCU ve geri dönüş planı bilindikten sonra Android firmware yükseltme uygunluğu.

Klima/araç kumandaları kullanıcı kapsamı dışındadır. CAN donanımı olmayan Android cihazda CAN menüsü seçmek, fiziksel CAN arabirimi oluşturmaz. CarPlay Ultra'nın bütün araç/gösterge işlevleri bu yol haritasında tamamlanmış kabul edilmez.

## 1.5 USB köprü doğrulaması

- [x] İzole USB aksesuar ↔ TCP Android Auto veri yolu ve sürücü seçimi.
- [x] CarPlay alıcı adresinin normal bağlantı ayarlarına alınması.
- [ ] Ford Kuga USB/SYNC ilk görüntü, kontrol ve görüşme testi.
- [ ] İki Android telefon arasında USB yeniden takmadan geçişin araç bazında doğrulanması.
- [ ] iPhone → fabrika CarPlay gönderici/köprü yolu; mevcut alıcı motoruyla aynı özellik değildir.
