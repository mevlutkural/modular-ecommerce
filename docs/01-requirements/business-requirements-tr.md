# İş Gereksinimleri Belgesi
## TechNest E-Ticaret Platformu

| | |
|---|---|
| **Belge Versiyonu** | 1.0 |
| **Durum** | Taslak — Müşteri İncelemesi Bekleniyor |
| **Hazırlayan** | Geliştirme Ekibi |
| **Talep Eden** | Kemal Aydın, Kurucu — TechNest |
| **Tarih** | 21 Ocak 2025 |
| **Kaynak Belge** | Müşteri Brifingi — TechNest E-Ticaret Platformu (14 Ocak 2025) |

---

## İçindekiler

1. [Yönetici Özeti](#1-yönetici-özeti)
2. [İş Hedefleri](#2-iş-hedefleri)
3. [Paydaşlar](#3-paydaşlar)
4. [Kapsam](#4-kapsam)
5. [İş Gereksinimleri](#5-iş-gereksinimleri)
   - 5.1 Kullanıcı Yönetimi ve Kimlik Doğrulama
   - 5.2 Ürün Kataloğu
   - 5.3 Alışveriş Sepeti ve Ödeme
   - 5.4 Sipariş Yönetimi
   - 5.5 Promosyon Motoru
   - 5.6 Envanter ve Stok Rezervasyonu
   - 5.7 Bildirimler ve Denetim Kaydı
   - 5.8 Yönetici Paneli
   - 5.9 Ödeme Entegrasyonu
6. [Fonksiyonel Olmayan Gereksinimler](#6-fonksiyonel-olmayan-gereksinimler)
7. [Varsayımlar ve Kısıtlamalar](#7-varsayımlar-ve-kısıtlamalar)
8. [Terimler Sözlüğü](#8-terimler-sözlüğü)

---

## 1. Yönetici Özeti

TechNest, halihazırda yalnızca üçüncü taraf pazar yerleri (Trendyol, Hepsiburada) üzerinden faaliyet gösteren bir tüketici elektroniği bayisidir. İşletme, işlem başına %15–20 arasında değişen komisyon bağımlılığını azaltmak ve müşteri ilişkileri, fiyatlandırma stratejisi ile iş mantığı üzerinde tam kontrol sahibi olmak amacıyla kendi e-ticaret kanalını kurmayı hedeflemektedir.

Bu belge, TechNest e-ticaret platformunun iş gereksinimlerini tanımlamaktadır. Platform; tam müşteri satın alma döngüsünü desteklemenin yanı sıra iki operasyonel açıdan kritik yetkinliği de karşılamalıdır: karmaşık indirim kurallarını destekleyen yapılandırılabilir bir promosyon motoru ve eş zamanlı ödeme oturumlarında aşırı satışı önleyen bir stok rezervasyon mekanizması.

Platform, TechNest'in uzun vadeli dijital varlığının temeli olarak tasarlanmıştır. Bu aşamada alınan kararlar gelecekteki büyümeyi kısıtlamamalıdır.

---

## 2. İş Hedefleri

| ID | Hedef | Başarı Ölçütü |
|---|---|---|
| BO-01 | Pazar yeri platformlarından bağımsız bir doğrudan satış kanalı oluşturmak | Platformun canlıya geçmesi ve gerçek siparişleri işlemeye başlaması |
| BO-02 | Doğrudan satışlarda pazar yeri komisyonunu ortadan kaldırmak | Doğrudan sipariş başına komisyon maliyeti = 0 |
| BO-03 | Geliştirici müdahalesi olmaksızın esnek, kural tabanlı promosyon kampanyaları oluşturabilmek | Yöneticinin kod dağıtımı gerektirmeden yeni bir kampanya oluşturup aktif edebilmesi |
| BO-04 | Eş zamanlı ödeme işlemleri sırasında aşırı satıştan kaynaklanan gelir kaybını önlemek | Stokta olmayan ürünler için onaylanmış sipariş sayısı = 0 |
| BO-05 | Operasyonel ve destek amaçlı tam sipariş geçmişi ve olay izlenebilirliği sağlamak | Herhangi bir sipariş olayının yönetici panelinden erişilebilir olması |
| BO-06 | Artımlı özellik geliştirmeyi destekleyen sürdürülebilir bir platform inşa etmek | Mevcut işlevlerde gerileme olmaksızın yeni özellik eklenebilmesi |

---

## 3. Paydaşlar

| Rol | Ad / Grup | İlgi Alanı |
|---|---|---|
| İşletme Sahibi | Kemal Aydın | Platformun genel başarısı, satış geliri, operasyonel verimlilik |
| Son Kullanıcılar | TechNest alıcıları | Güvenilir, hızlı ve anlaşılır bir alışveriş deneyimi |
| Depo Personeli | TechNest operasyonları | Doğru envanter verisi, net sipariş durumları |
| Geliştirme Ekibi | — | Açık, kararlı gereksinimler; teknik açıdan sağlam mimari |

---

## 4. Kapsam

### 4.1 Kapsam Dahilinde

- Müşteriye yönelik mağaza arayüzü (ürün gezinme, arama, sepet, ödeme, hesap yönetimi)
- Tam varyant desteğiyle ürün kataloğu (beden, renk, depolama kapasitesi vb.)
- Yönetici tarafından yapılandırılabilir kural tabanlı promosyon motoru
- Zaman aşımında otomatik serbest bırakma özellikli sepet düzeyinde stok rezervasyonu
- Yönetici ve müşteriler için sipariş yönetimi
- Ödeme entegrasyonu (birincil: İyzico; ikincil değerlendirme: Stripe)
- Otomatik işlemsel e-posta bildirimleri
- Yönetici panelinden erişilebilir sistem genelinde olay denetim kaydı
- Katalog, sipariş ve promosyon yönetimi için yönetici paneli
- Duyarlı web tasarımı (mobil tarayıcı desteği — yerel uygulama yok)

### 4.2 Kapsam Dışında

Aşağıdakiler mevcut çalışma kapsamından açıkça hariç tutulmuştur:

- Yerel iOS veya Android uygulamaları
- Çok satıcılı / pazar yeri işlevselliği (üçüncü taraf satıcılar)
- B2B veya toptan fiyatlandırma kademeleri
- Tedarikçi akışı entegrasyonu veya envanter içe aktarma araçları
- Gelişmiş analitik veya iş zekâsı raporlaması
- Sosyal giriş (OAuth)
- Lojistik sağlayıcı API entegrasyonu (gelecek faz)
- ERP entegrasyonu (gelecek faz)

---

## 5. İş Gereksinimleri

Gereksinimler önceliğe göre sınıflandırılmıştır:
- **[ZORUNLU]** — Lansman için vazgeçilmez
- **[ÖNERİLEN]** — Yüksek değerli; zaman elverirse teslim edilecek
- **[İSTEĞE BAĞLI]** — Arzu edilen; gerekirse lansman sonrasına ertelenir

---

### 5.1 Kullanıcı Yönetimi ve Kimlik Doğrulama

| ID | Gereksinim | Öncelik |
|---|---|---|
| UR-01 | Müşteriler e-posta ve şifre ile kayıt olabilmelidir | ZORUNLU |
| UR-02 | Müşteriler giriş yapabilmeli ve çıkış yapabilmelidir | ZORUNLU |
| UR-03 | Müşteriler e-posta aracılığıyla şifrelerini sıfırlayabilmelidir | ZORUNLU |
| UR-04 | Müşteriler profillerini (ad, e-posta, şifre) yönetebilmelidir | ZORUNLU |
| UR-05 | Müşteriler birden fazla teslimat adresi kaydedip yönetebilmelidir | ZORUNLU |
| UR-06 | Misafir ödeme (hesap oluşturmadan satın alma) | İSTEĞE BAĞLI |
| UR-07 | Yönetici kullanıcılar rol tabanlı erişimle ayrı kimlik doğrulaması yapmalıdır | ZORUNLU |

---

### 5.2 Ürün Kataloğu

| ID | Gereksinim | Öncelik |
|---|---|---|
| PC-01 | Ürünler hiyerarşik bir kategori yapısıyla düzenlenebilmelidir | ZORUNLU |
| PC-02 | Her ürün birden fazla görsel desteklemelidir | ZORUNLU |
| PC-03 | Her ürün zengin metin açıklaması ve yapılandırılmış teknik özellik tablosu desteklemelidir | ZORUNLU |
| PC-04 | Ürünler birden fazla varyantı desteklemelidir (ör. renk, depolama kapasitesi) | ZORUNLU |
| PC-05 | Her varyantın kendine ait stok miktarı, fiyatı ve SKU'su olmalıdır | ZORUNLU |
| PC-06 | Müşteriler ürünleri kategori, marka, teknik özellik ve fiyat aralığına göre filtreleyebilmelidir | ZORUNLU |
| PC-07 | Müşteriler ürünleri anahtar kelimeyle arayabilmelidir | ZORUNLU |
| PC-08 | Ürün listeleri gerçek zamanlı stok durumunu göstermelidir (Stokta Var / Stokta Yok) | ZORUNLU |
| PC-09 | Yönetici, ürünleri silmeksizin aktif veya pasif duruma alabilmelidir | ZORUNLU |
| PC-10 | Ürünler marka/üretici alanı desteklemelidir | ÖNERİLEN |

---

### 5.3 Alışveriş Sepeti ve Ödeme

| ID | Gereksinim | Öncelik |
|---|---|---|
| CC-01 | Müşteriler sepete ürün (ve belirli varyantlar) ekleyebilmelidir | ZORUNLU |
| CC-02 | Müşteriler sepetteki ürünlerin miktarlarını güncelleyebilmeli ve ürünleri kaldırabilmelidir | ZORUNLU |
| CC-03 | Giriş yapmış müşteriler için sepet oturumlar arasında kalıcı olmalıdır | ÖNERİLEN |
| CC-04 | Müşteriler ödeme sırasında promosyon kodu uygulayabilmelidir | ZORUNLU |
| CC-05 | Sepet; ürün bazlı fiyat dökümünü ve uygulanan indirimleri göstermelidir | ZORUNLU |
| CC-06 | Ödeme akışı teslimat adresi toplamalı ve kayıtlı adresler arasından seçim yapılmasına izin vermelidir | ZORUNLU |
| CC-07 | Müşteriler ödeme akışı içinden ödeme ağ geçidine yönlendirilmelidir | ZORUNLU |
| CC-08 | Başarılı ödeme sonrasında müşteriye onay sayfası gösterilmeli ve sipariş onay e-postası gönderilmelidir | ZORUNLU |
| CC-09 | Talep edilen miktar mevcut stoğu aşıyorsa sepet bunu açıkça bildirmelidir | ZORUNLU |

---

### 5.4 Sipariş Yönetimi

| ID | Gereksinim | Öncelik |
|---|---|---|
| OM-01 | Onaylanan her siparişe benzersiz ve okunabilir bir sipariş numarası atanmalıdır | ZORUNLU |
| OM-02 | Müşteriler sipariş geçmişlerini ve bireysel sipariş detaylarını görüntüleyebilmelidir | ZORUNLU |
| OM-03 | Yönetici tüm siparişleri görüntüleyebilmeli; durum, tarih ve müşteriye göre filtreleyebilmelidir | ZORUNLU |
| OM-04 | Yönetici sipariş durumunu güncelleyebilmelidir (ör. İşlemde → Kargoya Verildi → Teslim Edildi) | ZORUNLU |
| OM-05 | Durum değişiklikleri uygun müşteri bildirimlerini tetiklemelidir (bkz. Bölüm 5.7) | ZORUNLU |
| OM-06 | Sipariş detayları; satın alınan ürünleri, fiyatlandırmayı, uygulanan indirimleri ve teslimat adresini içermelidir | ZORUNLU |
| OM-07 | Yönetici bir siparişi manuel olarak iptal edebilmeli ve stok serbest bırakma işlemini tetikleyebilmelidir | ÖNERİLEN |

**Sipariş Durum Yaşam Döngüsü:**

```
ÖDEME BEKLENİYOR → ÖDEME ONAYLANDI → İŞLEMDE → KARGOYA VERİLDİ → TESLİM EDİLDİ
                                               ↘
                                           İPTAL EDİLDİ
```

---

### 5.5 Promosyon Motoru

Bu, temel bir iş gereksinimidir. Sistem; kod değişikliği veya dağıtım gerektirmeksizin tamamen yönetici paneli üzerinden yapılandırılabilen esnek, kural tabanlı promosyonları desteklemelidir.

| ID | Gereksinim | Öncelik |
|---|---|---|
| PE-01 | Yönetici; başlangıç tarihi, bitiş tarihi ve aktif durumu belirlenmiş promosyonlar oluşturabilmelidir | ZORUNLU |
| PE-02 | Sistem yüzde bazlı indirimleri desteklemelidir (ör. %20 indirim) | ZORUNLU |
| PE-03 | Sistem sabit tutarlı indirimleri desteklemelidir (ör. 500 TL indirim) | ZORUNLU |
| PE-04 | İndirimler sepet düzeyinde (sipariş toplamı) veya ürün/kategori düzeyinde uygulanabilmelidir | ZORUNLU |
| PE-05 | Sistem sepet değeri eşiklerini desteklemelidir (ör. 5.000 TL üzeri siparişlerde indirim uygulanır) | ZORUNLU |
| PE-06 | Sistem kategori kapsamlı promosyonları desteklemelidir (ör. tüm laptoplarda %10 indirim) | ZORUNLU |
| PE-07 | Sistem tek kullanımlık ve çok kullanımlık promosyon kodlarını (kuponlar) desteklemelidir | ZORUNLU |
| PE-08 | Yönetici bir promosyonu birleştirilemez olarak işaretleyebilmelidir (diğer aktif promosyonlarla birleşemez) | ZORUNLU |
| PE-09 | Müşterinin birden fazla uygun promosyonu olduğunda sistem birleştirme kurallarını doğru biçimde uygulamalıdır | ZORUNLU |
| PE-10 | Yönetici bir promosyonu maksimum toplam kullanım sayısıyla sınırlayabilmelidir | ÖNERİLEN |
| PE-11 | Yönetici bir promosyon kodunu belirli bir müşteri hesabıyla kısıtlayabilmelidir | ZORUNLU |

**İfade edilebilir olması gereken örnek iş kuralı:**
> "Elektronik kategorisinde 10.000 TL üzeri tüm siparişlerde %10 indirim, ancak bu promosyon aktif herhangi bir kupon koduyla birleştirilemez."

---

### 5.6 Envanter ve Stok Rezervasyonu

| ID | Gereksinim | Öncelik |
|---|---|---|
| IR-01 | Her ürün varyantı doğru, gerçek zamanlı kullanılabilir stok miktarını korumalıdır | ZORUNLU |
| IR-02 | Müşteri ödeme akışını başlattığında ilgili stok geçici olarak rezerve edilmelidir | ZORUNLU |
| IR-03 | Ödeme, yapılandırılabilir zaman aşımı süresi içinde tamamlanmazsa rezerve edilen stok otomatik olarak serbest bırakılmalıdır (yapılandırılabilir; varsayılan: 15 dakika) | ZORUNLU |
| IR-04 | Başarılı ödeme onayı alındığında rezerve edilen stok kalıcı olarak düşülmelidir | ZORUNLU |
| IR-05 | Ödeme başarısız olursa veya iptal edilirse rezerve edilen stok derhal serbest bırakılmalıdır | ZORUNLU |
| IR-06 | Sistem, iki müşterinin aynı son ürünü eş zamanlı satın almasını engellemelidir | ZORUNLU |
| IR-07 | Yönetici stok miktarlarını manuel olarak düzenleyebilmelidir | ZORUNLU |
| IR-08 | Yönetici, bir varyantın stoğu yapılandırılabilir bir eşiğin altına düştüğünde bildirim veya gösterge paneli uyarısı almalıdır | ÖNERİLEN |

**Stok Durum Modeli:**

```
MEVCUT → [müşteri ödeme akışını başlatır] → REZERVE
REZERVE → [ödeme onaylanır]               → SATILDI (düşüldü)
REZERVE → [zaman aşımı / ödeme başarısız] → MEVCUT (serbest bırakıldı)
```

---

### 5.7 Bildirimler ve Denetim Kaydı

#### Müşteri Bildirimleri (E-posta)

| ID | Tetikleyici Olay | Öncelik |
|---|---|---|
| NT-01 | Hesap kaydı | ZORUNLU |
| NT-02 | Şifre sıfırlama talebi | ZORUNLU |
| NT-03 | Sipariş onaylandı (ödeme başarılı) | ZORUNLU |
| NT-04 | Sipariş durumu Kargoya Verildi olarak güncellendi (varsa takip bilgisi dahil) | ZORUNLU |
| NT-05 | Ödeme başarısız | ZORUNLU |
| NT-06 | Sipariş iptal edildi | ÖNERİLEN |

#### Denetim Kaydı

Her önemli sistem olayı; zaman damgası, sorumlu aktör (müşteri, yönetici veya sistem) ve ilgili bağlamla birlikte kaydedilmelidir. Bu kayıt yönetici panelinden erişilebilir olmalıdır.

| ID | Gereksinim | Öncelik |
|---|---|---|
| AT-01 | Tüm sipariş yaşam döngüsü olayları sorgulanabilir bir denetim kaydına işlenmelidir | ZORUNLU |
| AT-02 | Yönetici eylemleri (durum değişiklikleri, ürün düzenlemeleri, promosyon değişiklikleri) kaydedilmelidir | ZORUNLU |
| AT-03 | Ödeme olayları (başlatıldı, onaylandı, başarısız) kaydedilmelidir | ZORUNLU |
| AT-04 | Stok rezervasyon ve serbest bırakma olayları kaydedilmelidir | ZORUNLU |
| AT-05 | Denetim kayıtları değiştirilemez olmalıdır — düzenlenemez veya silinemez | ZORUNLU |
| AT-06 | Yönetici herhangi bir siparişe ait tam olay zaman çizelgesini görüntüleyebilmelidir | ZORUNLU |

---

### 5.8 Yönetici Paneli

| ID | Gereksinim | Öncelik |
|---|---|---|
| AP-01 | Yönetici ürün ve varyant oluşturabilmeli, düzenleyebilmeli ve pasifleştirebilmelidir | ZORUNLU |
| AP-02 | Yönetici ürün kategorilerini yönetebilmelidir | ZORUNLU |
| AP-03 | Yönetici siparişleri görüntüleyebilmeli, filtreleyebilmeli ve yönetebilmelidir | ZORUNLU |
| AP-04 | Yönetici promosyonları oluşturabilmeli ve yönetebilmelidir (bkz. Bölüm 5.5) | ZORUNLU |
| AP-05 | Yönetici envanter seviyelerini görüntüleyebilmeli ve manuel olarak düzenleyebilmelidir | ZORUNLU |
| AP-06 | Yönetici herhangi bir siparişe ait denetim kaydını görüntüleyebilmelidir | ZORUNLU |
| AP-07 | Yönetici paneli temel bir gösterge panosu sunmalıdır: toplam gelir, sipariş sayısı, son siparişler | ÖNERİLEN |
| AP-08 | Yönetici paneli teknik olmayan bir personel tarafından eğitim materyali gerekmeksizin kullanılabilmelidir | ZORUNLU |

---

### 5.9 Ödeme Entegrasyonu

| ID | Gereksinim | Öncelik |
|---|---|---|
| PAY-01 | Platform, birincil ödeme ağ geçidi olarak İyzico ile entegre edilmelidir | ZORUNLU |
| PAY-02 | Ödeme; güvenli yönlendirme veya gömülü form aracılığıyla işlenmelidir — platform kart verisi depolamaz | ZORUNLU |
| PAY-03 | Platform, ödeme başarısı, başarısızlığı ve iptal geri çağrılarını doğru biçimde işlemelidir | ZORUNLU |
| PAY-04 | Uluslararası ölçeklenebilirlik için ikincil seçenek olarak Stripe entegrasyonu değerlendirilebilir | İSTEĞE BAĞLI |

---

## 6. Fonksiyonel Olmayan Gereksinimler

### 6.1 Performans

| ID | Gereksinim |
|---|---|
| NFR-01 | Ürün listeleme ve detay sayfaları normal yük altında 2 saniye içinde yüklenmelidir |
| NFR-02 | Ödeme akışı eş zamanlı kullanıcı oturumları sırasında duyarlı kalmalıdır |
| NFR-03 | Stok rezervasyon çakışmaları belirleyici biçimde çözümlenmelidir — aşırı satışla sonuçlanan yarış koşulları olmamalıdır |

### 6.2 Güvenlik

| ID | Gereksinim |
|---|---|
| NFR-04 | Tüm veri iletimi HTTPS üzerinden şifrelenmelidir |
| NFR-05 | Müşteri şifreleri güvenli bir karma algoritmasıyla (ör. bcrypt) saklanmalıdır |
| NFR-06 | Yönetici ve müşteri oturumları uygun token süresi dolumu ile güvenli biçimde yönetilmelidir |
| NFR-07 | Platform hiçbir aşamada ham ödeme kartı verisi depolamamalıdır |
| NFR-08 | Enjeksiyon saldırılarını önlemek amacıyla kullanıcı tarafından sağlanan tüm verilere girdi doğrulaması uygulanmalıdır |

### 6.3 Ölçeklenebilirlik ve Sürdürülebilirlik

| ID | Gereksinim |
|---|---|
| NFR-09 | Mimari; ilgisiz sistem bölümlerinde değişiklik gerekmeksizin yeni özellik ve modül eklenmesine izin vermelidir |
| NFR-10 | Kod tabanına, yeni bir geliştiricinin projeye adaptasyonunu kolaylaştıracak yeterli dokümantasyon eşlik etmelidir |
| NFR-11 | Sistem bulut ortamına dağıtılabilmeli ve web katmanında yatay olarak ölçeklenebilmelidir |

### 6.4 Kullanılabilirlik

| ID | Gereksinim |
|---|---|
| NFR-12 | Mağaza arayüzü mobil tarayıcılarda tam kullanılabilir olmalıdır (duyarlı tasarım) |
| NFR-13 | Yönetici paneli teknik geçmişi olmayan personel tarafından kolayca kullanılabilmelidir |
| NFR-14 | Müşterilere sunulan hata mesajları açık ve yönlendirici olmalıdır |

---

## 7. Varsayımlar ve Kısıtlamalar

### Varsayımlar

- Platform, lansman aşamasında yalnızca Türkiye pazarına hizmet verecektir; çok para birimi ve çok dil desteği bu aşamada gerekli değildir.
- Kargo maliyeti hesaplama ve lojistik taşıyıcı entegrasyonu kapsam dışındadır; kargo ücretleri başlangıç sürümünde manuel olarak veya sabit fiyat üzerinden yönetilecektir.
- TechNest ürün içeriklerini (açıklamalar, görseller, teknik özellikler) sağlayacaktır; içerik üretimi platformun sorumluluğunda değildir.
- E-posta teslimatı üçüncü taraf bir e-posta servis sağlayıcısı aracılığıyla gerçekleştirilecektir (ör. AWS SES, Resend).
- Başlangıç altyapısı bulut tabanlı olacaktır; geliştirme ekibi bir öneri sunacaktır.

### Kısıtlamalar

- Bütçe öz finansmanlıdır; kullanılabilir zaman diliminde uygulanabilir bir ürün teslim etmek için kapsam yönetilmelidir.
- Hedef teslim süresi: proje başlangıcından itibaren 4–6 ay.
- Sistem İyzico ile entegre edilmelidir; ödeme sağlayıcısı anlaşma olmaksızın değiştirilemez.
- Denetim kaydı tasarım gereği değiştirilemezdir — kayıtları silmek veya değiştirmek için herhangi bir yönetici işlevi bulunmamaktadır.

---

## 8. Terimler Sözlüğü

| Terim | Tanım |
|---|---|
| **Varyant** | Bir veya daha fazla özniteliğiyle (ör. renk, depolama) ayırt edilen, satın alınabilir belirli bir ürün versiyonu. Her varyantın kendine ait SKU'su, stok miktarı ve fiyatı vardır. |
| **Promosyon** | Ödeme sırasında otomatik olarak veya kod aracılığıyla uygulanabilen yapılandırılmış bir indirim kuralı. |
| **Kupon Kodu** | Müşteri tarafından girilen ve belirli bir promosyonu etkinleştiren kod. |
| **Rezervasyon** | Müşteri ödeme sürecini başlattığında stok üzerine konulan geçici bloke; aynı birimlerin başka bir müşteriye eş zamanlı satılmasını engeller. |
| **Denetim Kaydı** | Sipariş, ürün ve kullanıcı hesabı gibi varlıklara ilişkin sistem olaylarının değiştirilemez, kronolojik kaydı. |
| **Yönetici** | Arka ofis yönetim arayüzüne erişimi olan TechNest personeli. |
| **SKU** | Stok Takip Birimi — her ürün varyantı için benzersiz tanımlayıcı. |
| **ZORUNLU / ÖNERİLEN / İSTEĞE BAĞLI** | MoSCoW yöntemine dayalı öncelik sınıflandırması. ZORUNLU = lansman için gerekli; ÖNERİLEN = yüksek öncelikli ancak engelleyici değil; İSTEĞE BAĞLI = arzu edilen, gerekirse ertelenir. |
