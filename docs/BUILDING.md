# Yerel geliştirme ve derleme

## Araçlar

| Araç | Projede kullanılan |
| --- | --- |
| JDK | 21 |
| Android SDK | Platform 36 |
| Android NDK | 28.2.13676358 |
| CMake | 3.22.1 |
| Gradle | Wrapper 8.14.4 |
| Android Gradle Plugin / Kotlin | 8.13.2 / 2.2.21 |

Android SDK Manager üzerinden gerekli SDK/NDK/CMake bileşenlerini kurun. `JAVA_HOME` JDK 21'i, `ANDROID_HOME` SDK dizinini göstermeli. Aşağıdaki komutlar depo kökünde çalıştırılır. Aynı checkout'ta farklı kimlik girdileriyle paralel Gradle çalıştırmayın.

Build çıktıları yerelde üretilir. GitHub Actions veya başka bir cloud build kurulumu gerekmez; bu projede böyle bir iş akışı bulunmaz.

## Modüller

| Modül | Görev |
| --- | --- |
| `app` | Fiorino Auto ana APK, Android Auto, ortak telefon seçimi ve araç ayarları |
| `contract` | Modüller/yardımcı entegrasyonlar arasındaki sözleşmeler |
| `carplay-ui` | Ana APK'daki CarPlay ekranı, hizmet ve tekrar bağlantı politikası |
| `carplay-engine` | DiPlay'dan alınan protokol/medya motoru |
| `carplay-lab` | Aynı CarPlay arayüzüne ayrı deney giriş noktası |

## Geliştirme APK'sı

```sh
./gradlew :app:assembleGithubDebug \
  -PfiorinoNdkVersion=28.2.13676358 --console=plain
```

Çıktı `app/build/outputs/apk/github/debug/` altındadır. Debug imzası, release anahtarınızla aynı değildir; kurulu uygulamayı güncellemek için imza uyumu gerekir.

## Testler

Ana uygulama ve ortak CarPlay arayüzü:

```sh
./gradlew :app:testGithubDebugUnitTest :carplay-ui:testDebugUnitTest \
  -PfiorinoNdkVersion=28.2.13676358 --console=plain
```

5 Kasım 2026 / 1.4.4 doğrulaması: **4.811 ana + 16 CarPlay UI testi**. Bunlar motorun bütün upstream testlerinin sayısı değildir. XML raporları modüllerin `build/test-results/`, HTML raporları `build/reports/tests/` dizinindedir.

Motorun önceki 92 hedefli testini yeniden çalıştırmak için:

```sh
./gradlew :carplay-engine:testDebugUnitTest \
  --tests '*MfiAuthenticationClientTest' --tests '*Iap2*' \
  --tests '*AirPlayCryptoTest' --tests '*WifiP2p*' \
  --tests '*PairSetupTest' --tests '*PairVerifyTest' \
  -PfiorinoNdkVersion=28.2.13676358 --console=plain
```

Negatif provizyon testleri kimliksiz kaynak kurulumu varsayar. Bunları gerçek yerel kimlik girdisi eklenmiş derlemeyle çalıştırmayın.

## İmzalı genel APK

Release imzasını depo dışındaki bir keystore ile oluşturun. Depo kökünde git tarafından dışlanan `key.properties` dosyası kullanılır:

```properties
storeFile=/absolute/private/path/fiorino-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```

Bunlar örnek alan adlarıdır; gerçek parolaları belgeye veya terminal komutuna koymayın. Kurulu sürümün üzerine güncelleme için aynı imza anahtarı korunmalıdır.

Genel dağıtım için **korumalı görevi** kullanın:

```sh
# FIORINO_CARPLAY_AUTH_ASSETS bu işlem için ayarlı olmamalı.
./gradlew :app:assemblePublicGithubRelease \
  -PfiorinoNdkVersion=28.2.13676358 --console=plain
```

Çıktı `app/build/outputs/apk/github/release/` altındadır. Anahtar yapılandırılmadıysa imzasız çıktı oluşabilir; bu dosya kurulum adayı diye sunulmaz. Görev, yerel kimlik ortam girdisini reddeder ve APK'da `assets/offline-mfi/` bulunmadığını denetler. ARM32 ve ARM64 aynı APK'dadır.

APK yanında SHA256 yayımlayın; içeriği gerçek dosyadan hesaplayın. Release etiketi, kaynak commit'i, build tipi ve test sonucu sürüm notunda belirtilmelidir. `BuildConfig` kaynak tanımı `-dirty` içeriyorsa bunu gizlemeyin.

## Özel CarPlay test girdisi

Kaynak kod bir aksesuar kimliği üretmez veya Apple'a kabul ettirmez. Desteklenen yerel girdi yapısı şöyledir:

```text
/private/outside-repository/runtime-assets/
└── offline-mfi/
    ├── identity.pk8
    └── certificate.p7b
```

Girdi doğrulaması yalnız beklenen iki dosyayı ve boyut sınırını kabul eder. Anahtar/sertifika yapısının eşleşmesi, iPhone kabulünü veya yeniden dağıtım yetkisini kanıtlamaz. Teknik/provizyon ayrıntıları [CarPlay notlarında](CARPLAY-LAB.md).

Bu dizin açıkça `FIORINO_CARPLAY_AUTH_ASSETS` ortam değişkeniyle verildiğinde `:app:assembleGithubRelease` yerel test paketi üretir. Böyle bir APK, kimlik dosyaları ve imza keystore'u **genel Releases, kaynak depo veya issue ekine konulmaz**. Yerel test çıktısını genel APK'lardan ayrı tutun. Bu modda hızlı cihaz denemesi için R8 kapalıdır; genel release'te R8 açıktır.

## Cihaza aktarma

```sh
adb devices -l
adb -s RECEIVER_SERIAL push PATH_TO_APK /sdcard/Download/fiorino-auto.apk
```

USB kurulum izni varsa `adb install -r` kullanılabilir. Bazı MIUI sürümlerinde ADB kurulumu reddedilir; dosyayı cihazdaki Dosyalar/İndirilenler ekranından açıp kurulum onayını cihazda tamamlayın.

`RECEIVER_SERIAL` gerçekten teype ait olmalı. Test telefonunun donanım bilgisi, araç teybinin platformunu kanıtlamaz.
