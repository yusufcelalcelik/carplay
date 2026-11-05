# Teyp donanımı ve direksiyon kumandası incelemesi

## Bilinen durum

5 Kasım 2026'da paylaşılan kasa fotoğrafı ve kullanıcı açıklaması:

| Gözlem | Anlamı / sınırı |
| --- | --- |
| Android 7.1 etiketi | Kasadaki yazı; çalışma zamanı SDK/ROM bilgisi henüz alınmadı. |
| `MTA701H…` seri/kart ailesi | Olası anakart ailesi için ipucu; seri etiketini tek başına kesin ürün/MCU modeli saymıyoruz. |
| `K1/TX`, `K2/RX`, GND, ACC, B+ | Kumanda girişlerini araştırmak için somut etiket bilgisi; araç pinout'u veya CAN-H/CAN-L eşlemesi değildir. |
| Araç: 2017 Fiorino Pop | Premium direksiyon ve kumanda sonradan takılmış. Fabrika çıkışlı kumandalı tesisat bulunduğu varsayılmaz. |
| Tuş öğrenme ekranı hep boş | Beklenen öğrenme yolunda sinyal görülmediği bildirildi. Android, MCU ve tesisat nedeni henüz ayrılmadı. |
| Mute basışında bazen kısa kesilme | Tekrarlanabilirliği doğrulanmadı; kumandanın çalıştığına veya arızanın yazılım olduğuna kanıt değil. |

Fotoğrafın tamamı ve kişiye özgü seri numarası bu belgeye eklenmedi.

## Model hakkında elde edilen kanıt

Dağıtıcı tarafından yayımlanmış bir Android 5.1 kılavuzu aynı MTA701H etiket ailesini ve aynı ana soket düzenini gösteriyor; K1/TX ve K2/RX hatlarını direksiyon kumandası 1/2 olarak açıklıyor. **Bu, benzer donanım ailesine ilişkin birincil bir bağlantı belgesidir; kullanıcının cihazının tam servis kılavuzu değildir.** [Kılavuz, ana soket sayfası](https://www.silim.com/notice/NOTICES%20DES%20PRODUITS/Autoradios%20ISO%20et%20Double%20Din%20Kit%20cle%20extraction%20autoradio/Witson-Android/W2-S190/Notice%20demarrage%20Android%205.1.pdf).

MT-A701H anakartlı farklı bir ünitenin birinci el onarım kaydında HCT-PX3-Rev7.0 modülü ve RK3188 anlatılıyor. Bu kayıt **PX3/RK3188 olasılığını güçlendirir**, bizim kartın kesin tanısı değildir. Bağlantı şeması, kart revizyonu ve MCU doğrulanmadan PX3/PX5 ROM'u seçilemez. [Onarım kaydı](https://remont-aud.net/forum/125-95439-1).

## CAN bus dönüştürücü olmadan mümkün mü?

Burada araştırılan parça genellikle **CAN bus dekoderi/direksiyon arayüzü** olarak adlandırılır; Modbus ile aynı protokol değildir.

| Gerçek sinyal yolu | Yazılımla yapılabilecek |
| --- | --- |
| Direksiyonun dirençli tuşları doğru kontak/tesisat üzerinden teybin uygun KEY girişine ulaşıyor | MCU'nun tuş öğrenmesi ve ardından Android/uygulama eşlemesiyle harici CAN kutusu gerekmeyebilir. |
| Tuş bilgisi araçta CAN üzerinden geliyor; teypte uyumlu fiziksel alıcı/dekoder yolu yok | Sadece APK/root/fabrika menüsü eksik elektriksel arabirimi oluşturmaz. Uygun donanım yolu gerekir. |
| MCU tuşu görüyor ama Android'e standart key event olarak vermiyor | OEM yayın/servis yolu varsa uygulama entegrasyonu araştırılır; `getevent` boşluğu tek başına tesisat kopukluğu kanıtı değildir. |
| Sonradan takılan direksiyonun gerekli kontak veya kablo yolu eksik | Android ayarıyla tamamlanamaz; önce tesisatın doğrulanması gerekir. |

Fabrika menüsünde araç/CAN dekoder listesi olması, kasada dahili CAN alıcısı olduğunu kanıtlamaz. Bu menü bağlı kutunun protokolünü veya MCU çalışma biçimini seçiyor olabilir. Fotoğraftaki `TX/RX` yazısını doğrudan araç CAN hattı diye yorumlamayın.

Connects2'nin **CTSFA004.2** kılavuzu Fiorino225/2008+ dahil belirli VDO/Mini ISO sistemleri için CAN tabanlı kumanda arayüzü tanımlar ve UConnect'i hariç tutar. Bu, bazı Fiorino kurulumlarının CAN yolu kullandığını gösterir; 2017 Pop'a sonradan takılan direksiyonun birebir aynı tesisata sahip olduğunu göstermez. [Üretici kılavuzu](https://downloads.connects2.com/C2/files/InstallationGuides/CTSFA004.2.pdf).

InCarTec ayrıca 2019+ orijinal 7 inç Daichi/M300 ekranlı Fiorino için farklı bir arayüz tanımlar. Ürünün Çin teybe dirençli patch çıkışı sunması, aracın giriş tarafının doğrudan analog olduğu anlamına gelmez. Yıl, orijinal radyo ve donanım birlikte belirlenmelidir. [Üretici açıklaması](https://incartec.co.uk/product/Fiat-Fiorino-OEM-Daichi-7-Navigation-Screen-Steering-wheel-audio-control-interface-29-734).

Bu kaynaklar satın alma önerisi veya bu araca kesin parça uyumu ilanı değildir.

## Birlikte debug planı

### 1. Gerçek teybi tanı

Teybin **Hakkında / About** ekranından tam MCU sürümü, sistem/derleme numarası, Android sürümü ve varsa CPU/RAM bilgisi alınır. Android etiketi veya USB'ye bağlı test telefonunun bilgisi bunun yerine kullanılamaz.

Teyp ADB ile bağlandığında, bilgisayarda:

```sh
adb devices -l
python3 tools/inspect_headunit.py --serial RECEIVER_SERIAL --output headunit-info.json
```

Araç yalnız seçilmiş cihazdan sınırlı sistem özelliklerini, CPU alanlarını, MCU ile ilişkili paket adlarını ve giriş aygıtı envanterini okur. Ayar/firmware yazmaz, root istemez, Android/MCU sürümünü tahmin etmez. Yeni JSON dosyası oluşturur; mevcut dosyanın üzerine yazmaz. Python3 ve ADB gerektirir.

### 2. Tekrarlanabilir tuş denemesi

Araç sabitken aynı tuşa birkaç kez basın; her basışın saatini, native tuş öğrenme ekranındaki tepkiyi ve sistem ses göstergesini not edin. Mute tepkisi her basışla eşleşiyor mu, yoksa rastlantısal mı ayırt edilir. Mevcut fabrika ayarlarının fotoğrafını almadan rastgele CAN tipleri arasında dolaşmayın.

Fiorino Auto'nun **Tuş Haritası** ekranı Android Auto eylemleri için giriş eşlemesini gösterir. Kaynakta standart medya tuşu ile `com.microntek.irkeyUp/irkeyDown` dahil OEM olay alıcısı bulunur. Bu desteğin cihazdaki sinyal yoluyla uyumu, özellikle CarPlay'e eylem aktarımı, ayrıca test edilir.

ADB üzerinden canlı giriş incelemesi:

```sh
adb -s RECEIVER_SERIAL shell getevent -lp
adb -s RECEIVER_SERIAL shell getevent -lt
```

İkinci komutta yalnız test edilecek tuşlara basıp `Ctrl+C` ile durdurun. ROM izin vermiyorsa hata kaydedilir; otomatik root/izin aşma yapılmaz. Touch/başka klavye kullanımı aynı akışa karışabileceği için logu bu kısa denemeyle sınırlayın.

`getevent`'te hiçbir şey yoksa hemen “kablo kopuk” sonucu çıkarılmaz: bazı MCU'lar tuşu kendi işler veya Android'e OEM mesajı yollar. Üç gözlem birlikte değerlendirilir: native öğrenme ekranı, Android girişleri, uygulamanın OEM tuş logları.

### 3. Tesisat doğrulaması

Yazılım tarafı sinyal görmüyorsa oto elektrik uzmanı, doğru araç/kart şemasıyla direksiyon tuş grubu, spiral kontak ve radyo soketi arasındaki yolu; anahtarların dirençli mi veri yolu tabanlı mı olduğunu doğrulamalı. “Kablolar bağlı” ifadesi doğru hat/şase/işlevi kanıtlamaz. Bu belgede doğrulanmamış araç pin numarası veya rastgele köprüleme tarifi yoktur. CAN-H/CAN-L ve besleme hatlarını KEY girişine deneme amaçlı bağlamayın. Direksiyon/airbag bölgesi için sökme tarifi verilmez.

Sonuç analog KEY yolunu doğrularsa CAN kutusuz öğrenme denenebilir. Sonuç CAN veya eksik tesisat gösterirse Android kodu yerine uygun donanım/bağlantı adımı gerekir.

## İleride Android yükseltme

Şimdilik firmware seçimi veya flash işlemi yapılmaz. Önce MCU ailesi/üreticisi, SoC, anakart revizyonu, RAM, ekran/touch, Bluetooth/radyo modülü, mevcut sistem ve geri dönüş yöntemi bilinmeli. Android sistem imajı ile MCU firmware'i ayrı bileşenlerdir. Aynı kasa/Android etiketi, uyumlu ROM anlamına gelmez.

Hedef önce çalışan kurulumun envanteri ve yedeği, sonra gerçekten aynı donanıma uygun güncelleme kaynağının doğrulanmasıdır. Yalnız “Android7.1/PX3 gibi” bilgisiyle Android sürümü yükseltme veya MCU değişimi önerilmez.
