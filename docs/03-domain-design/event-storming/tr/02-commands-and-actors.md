# Komutlar ve Aktörler Kataloğu

**Döküman:** `docs/03-domain-design/event-storming/tr/02-commands-and-actors.md`
**Aşama:** Stratejik Tasarım
**İlgili Dökümanlar:** [`01-domain-events.md`](./01-domain-events.md)
**İlgili Gereksinimler:** İş Gereksinimleri Dokümanı (Tüm Bölümler)

---

## Genel Bakış

Bu katalog, TechNest platformu için Event Storming oturumunda belirlenen Komutları ve Aktörleri tanımlar. Bir Komut, sistemin durumunu değiştirme yönündeki açık bir niyeti temsil eder — her zaman bir Aktör tarafından gönderilir ve kabul edilirse bir veya birden fazla Domain Event üretir.

Domain Event'lerin *zaten gerçekleşmiş ve geri alınamaz* bir gerçeği ifade etmesinin aksine, Komutlar *talep edilen* bir eylemi tanımlar. Bir Komut reddedilebilir; Domain Event ise reddedilemez.

Her Komut, aktörün amacını net biçimde ortaya koyacak şekilde emir kipinde isimlendirilmiştir (`RegisterCustomer`, `CancelOrder`). Komutlar, Event Storming zaman çizelgesinde Domain Event'lerin hemen öncesinde yer alır ve bir sonraki tasarım adımında Aggregate'lerin temel girdisini oluşturur.

---

## Aktörler

TechNest platformuyla etkileşime giren beş farklı aktör vardır. Her aktör, kendine özgü bir yetki sınırı içinde çalışır ve bu sınırın dışındaki Komutları gönderemez.

| Aktör | Tür | Açıklama |
|---|---|---|
| **Customer** | İnsan | TechNest mağazasında ürün inceleyen ve satın alma yapan kayıtlı bir müşteri. Satın alma tamamlamak, adres kaydetmek ve sipariş geçmişini görüntülemek için kimlik doğrulaması yapması gerekir. |
| **Admin** | İnsan | Arka ofis yönetim paneli üzerinden katalog, sipariş, promosyon ve envanter yöneten TechNest çalışanı. Rol tabanlı erişimle ayrı bir oturum açar (UR-07). |
| **System** | Otomatik | Gelen Domain Event'lere ve dış sistem callback'lerine tepki veren platform içi mantık — örneğin ödeme onayının ardından sipariş oluşturmak veya bildirim e-postası göndermek. |
| **Background Job** | Otomatik | Kullanıcı oturumlarından bağımsız olarak düzenli aralıklarla çalışan zamanlanmış süreçler — örneğin süresi dolmuş stok rezervasyonlarını tarayıp serbest bırakmak. |
| **Payment Gateway (İyzico)** | Harici Sistem | Kart işlemlerini gerçekleştiren ve ödeme sonuçlarını (başarı, başarısızlık, iptal) imzalı callback olarak platforma ileten harici ödeme sağlayıcısı (PAY-01, PAY-03). |

---

## Komut Kataloğu

Komutlar, Domain Event Kataloğu'ndaki yedi alan sınırıyla aynı yapıda gruplandırılmıştır.

---

### 1. Kimlik ve Müşteri Profili

| Komut | Aktör | Ön Koşul | Üretilen Event(ler) | İş Kuralları |
|---|---|---|---|---|
| `RegisterCustomer` | Customer | E-posta adresi daha önce kayıtlı değil. Girdi (ad, e-posta, şifre) sunucu tarafı doğrulamayı geçiyor. | `CustomerRegistered` | Şifre bcrypt hash olarak saklanır — asla düz metin olarak kaydedilmez (NFR-05). Kayıt başarılı olduğunda müşteri ayrı bir adım gerekmeksizin otomatik olarak sisteme giriş yapar. İlk sürümde e-posta doğrulaması zorunlu değildir; hesap hemen aktif olur. |
| `LoginCustomer` | Customer | Hesap mevcut. Gönderilen kimlik bilgileri kayıtlı hesapla eşleşiyor. | *(Domain Event üretilmez — sadece oturum durumu değişir)* | Kimlik bilgisi hatasında, şifrenin mi yoksa e-postanın mı yanlış olduğunu belirtmeyen genel bir hata mesajı döndürülür; bu sayede hesap keşif saldırıları engellenir. Başarıda access token (30 dakika geçerli) ve refresh token (7 gün geçerli) düzenlenir (NFR-06). |
| `LogoutCustomer` | Customer | Müşteriye ait aktif bir oturum mevcut. | *(Domain Event üretilmez — sadece oturum durumu değişir)* | Oturum token'ı sunucu tarafında geçersiz kılınır. Müşteri ana sayfaya yönlendirilir. |
| `RequestPasswordReset` | Customer | — *(Dışarıdan ön koşul zorunlu tutulmaz)* | `CustomerPasswordResetRequested` | Gönderilen e-posta kayıtlı değilse sistem yine de genel bir başarı mesajı döndürür; bu, saldırganın belirli bir e-postanın hesabının olup olmadığını öğrenmesini engeller. Sıfırlama token'ı tek kullanımlıktır ve 30 dakika sonra sona erer. |
| `ResetPassword` | Customer | Sıfırlama token'ı geçerli, kullanılmamış ve süresi dolmamış. | `CustomerPasswordReset` | Yeni şifre bcrypt ile hash'lenir (NFR-05). İşlem başarılı olduğunda token tüketilir ve bir daha kullanılamaz. Hesaba ait tüm aktif oturumlar hemen geçersiz kılınır. |
| `UpdateCustomerProfile` | Customer | Müşteri kimlik doğrulamasını tamamlamış. | *(Bu aşamada Domain Event üretilmez)* | Düzenlenebilir alanlar: ad soyad, e-posta adresi, şifre. E-posta değişikliği kaydedilmeden önce benzersizlik kontrolü yapılır. |
| `AddDeliveryAddress` | Customer | Müşteri kimlik doğrulamasını tamamlamış. | `CustomerAddressAdded` | Bir hesaba birden fazla adres kaydedilebilir. Bir adres varsayılan olarak işaretlenebilir. Hem teslimat hem de fatura adresleri desteklenir. |
| `RemoveDeliveryAddress` | Customer | Müşteri kimlik doğrulamasını tamamlamış. Adres, açık veya işlemdeki bir siparişte kullanılmıyor. | *(Bu aşamada Domain Event üretilmez)* | Geçmiş siparişlerle ilişkilendirilmiş adresler, müşteri profilinden kaldırılsa dahi o sipariş kayıtlarında korunmaya devam eder. |

---

### 2. Katalog ve Envanter

| Komut | Aktör | Ön Koşul | Üretilen Event(ler) | İş Kuralları |
|---|---|---|---|---|
| `AddProductToCatalog` | Admin | Zorunlu alanlar doldurulmuş: ürün adı, en az bir kategori, benzersiz SKU'ya sahip en az bir varyant, temel fiyat ve başlangıç stok miktarı. | `ProductAddedToCatalog` | Ürünler varsayılan olarak devre dışı durumda oluşturulur; açıkça aktif edilmeden mağazada görünmez (PC-09). Her varyantın SKU'su katalog genelinde benzersiz olmalıdır. Ürün en az bir kategoriye atanmak zorundadır (PC-01). |
| `UpdateProductDetails` | Admin | Ürün katalogda mevcut. | *(Bu aşamada Domain Event üretilmez)* | Düzenlenebilir alanlar: ürün adı, zengin metin açıklaması, teknik özellikler tablosu, görseller, marka/üretici, kategori atamaları (PC-02, PC-03, PC-10). Aktif ürünlerdeki değişiklikler mağazaya anında yansır. |
| `UpdateProductPrice` | Admin | Ürün varyantı mevcut. | `ProductPriceUpdated` | Önceki ve yeni fiyatın her ikisi de izlenebilirlik amacıyla event payload'ına eklenir. Fiyat değişikliği daha önce verilmiş siparişleri etkilemez. |
| `ActivateProduct` | Admin | Ürün mevcut. En az bir varyantın stok miktarı sıfırdan fazla. | `ProductAddedToCatalog` | Ürün mağazada hemen görünür hale gelir. Daha önce devre dışı bırakılmış bir ürün yeniden aktif edildiğinde katalog erişilebilirliğini bildirmek amacıyla aynı event yayımlanır. |
| `DeactivateProduct` | Admin | Ürün şu anda aktif durumda. | *(Bu aşamada Domain Event üretilmez)* | Ürün mağazadan anında kaldırılır. Devre dışı bırakma silme işlemi değildir; devam eden siparişler etkilenmez. |
| `AdjustStock` | Admin | Ürün varyantı mevcut. Düzeltme için bir neden belirtilmiş. | `ProductStockAdjusted` | Düzeltme pozitif (stok ekleme) veya negatif (hasar silme, düzeltme) olabilir. Negatif düzeltme mevcut rezervasyonların altına stoku düşürecekse işlem engellenmez, ancak yönetim panelinde uyarı gösterilir — mevcut rezervasyonlar iptal edilmez, önceliği korunur (IR-07). Güncel mevcut stok, yapılandırılan düşük stok eşiğinin altına düşerse ek olarak `LowStockThresholdReached` event'i de yayımlanır (IR-08). |

---

### 3. Promosyonlar ve Pazarlama

| Komut | Aktör | Ön Koşul | Üretilen Event(ler) | İş Kuralları |
|---|---|---|---|---|
| `CreatePromotion` | Admin | Zorunlu promosyon özellikleri girilmiş: ad, indirim türü (yüzde veya sabit tutar), indirim değeri, kapsam (sepet geneli veya kategori bazlı), başlangıç tarihi, bitiş tarihi, aktiflik bayrağı, birleştirilebilirlik bayrağı. | `PromotionCreated` | Kapsam kategori bazlıysa hedef kategori belirtilmek zorundadır. Kupon kodu (tanımlanmışsa) aktif tüm promosyonlar içinde benzersiz olmalıdır. Promosyona toplam maksimum kullanım sayısı ve/veya belirli bir müşteri kısıtlaması eklenebilir (PE-10, PE-11). Sistem, şu gibi kuralları ifade edebilmelidir: *"Elektronik kategorisinde 10.000 TL üzeri siparişlerde %10 indirim, aktif kupon kodlarıyla birleştirilemez"* (PE-01 ile PE-11). |
| `UpdatePromotion` | Admin | Promosyon mevcut ve bitiş tarihi geçmemiş. | *(Bu aşamada Domain Event üretilmez)* | Aktif promosyondaki değişiklikler hemen geçerli olur. Ödeme sürecinde olan ve bu promosyonu uygulamış müşterilerin uygunluğu sipariş onayı sırasında yeniden değerlendirilir. |
| `ActivatePromotion` | Admin | Promosyon mevcut ve şu anda pasif durumda. | `PromotionActivated` | Promosyon, yeni oturumlardaki tüm ödeme hesaplamaları için hemen uygun hale gelir. |
| `DeactivatePromotion` | Admin | Promosyon şu anda aktif. | `PromotionDeactivated` | Promosyon, yeni ödeme oturumları için değerlendirme dışı bırakılır. Bu promosyonu zaten uygulamış ve ödeme sürecinde olan müşterilerin indirimi sipariş onayında yeniden değerlendirilir; artık uygun değilse kaldırılır ve müşteriye bildirilir. |
| `ApplyVoucherCode` | Customer | Müşteri kimlik doğrulamasını tamamlamış. Aktif bir ödeme oturumu mevcut. Bir kupon kodu girilmiş. | `PromotionAppliedToCart` *(uygunsa)* | Tam değerlendirme mantığı uygulanır: kupon aktif promosyonlara karşı doğrulanır, uygunluk kuralları kontrol edilir ve birleştirme kuralları işletilir (bkz. [Promosyon Değerlendirme Süreci](../../../02-business-processes/tr/promotion-evaluation-process.md)). Kod geçersizse, süresi dolmuşsa veya bu müşteri/sepet için uygun değilse açık bir hata mesajı döndürülür ve indirim uygulanmaz. |
| `EvaluateCartPromotions` | System | Ödeme başlatılmış ve stok rezervasyonu onaylanmış. | `PromotionAppliedToCart` *(herhangi bir promosyon uygunsa)* | Kupon kodu girilmese bile ödeme başlatıldığında otomatik olarak tetiklenir. Uygun tüm otomatik promosyonları uygular. Birleştirme kuralları tam olarak işletilir — sistem müşteriye en yüksek toplam indirimi sağlayan sonucu seçer. |

---

### 4. Sepet ve Ödeme Başlatma

| Komut | Aktör | Ön Koşul | Üretilen Event(ler) | İş Kuralları |
|---|---|---|---|---|
| `AddItemToCart` | Customer | Müşteri kimlik doğrulamasını tamamlamış. Ürün varyantı aktif. Varyantın en az 1 birim mevcut stoğu var. | *(Domain Event üretilmez — sadece sepet durumu değişir)* | Varyant zaten sepetteyse miktar artırılır. Giriş yapmış müşteriler için sepet, tarayıcı oturumları arasında korunur (CC-03). Sepete ürün eklemek stok rezervasyonu başlatmaz — rezervasyon yalnızca ödeme adımı başlatıldığında gerçekleşir. |
| `UpdateCartItemQuantity` | Customer | Ürün sepette mevcut. İstenen miktar sıfırdan büyük. | *(Domain Event üretilmez — sadece sepet durumu değişir)* | İstenen miktar mevcut stoğu aşıyorsa satır içi hata gösterilir ve miktar güncellenmez (CC-09). Bu aşamada stok rezervasyonu yapılmaz. |
| `RemoveItemFromCart` | Customer | Ürün sepette mevcut. | *(Domain Event üretilmez — sadece sepet durumu değişir)* | Tüm ürünlerin çıkarılması boş bir sepetle sonuçlanır. Boş sepet ödeme adımına geçemez. |
| `InitiateCheckout` | Customer | Sepette en az bir ürün var. Müşteri kimlik doğrulamasını tamamlamış. Tüm sepet ürünleri için yeterli mevcut stok mevcut. | `CheckoutInitiated`, ardından `StockReserved` veya `StockReservationFailed` | Stok Rezervasyon Süreci, sepetteki tüm ürünler için atomik olarak tetiklenir — ya tüm ürünler rezerve edilir ya da hiçbiri edilmez. Kısmi rezervasyona izin verilmez (IR-02). Herhangi bir ürün rezerve edilemezse ödeme adımı engellenir ve müşteriye hangi ürünlerin mevcut olmadığı bildirilir (CC-09). Başarıda 15 dakikalık rezervasyon penceresi başlar (IR-03). Bkz. [Stok Rezervasyon Süreci](../../../02-business-processes/tr/stock-reservation-process.md). |
| `SelectDeliveryAddress` | Customer | Aktif bir ödeme oturumu mevcut. Stok rezervasyonu aktif. Müşteri kimlik doğrulamasını tamamlamış. | `CustomerAddressAdded` *(yalnızca yeni bir adres kaydedilirse)* | Müşteri daha önce kayıtlı bir adresi seçer ya da yeni bir adres girer. Ödeme sırasında girilen yeni adresler müşteri profiline kaydedilir. Adres seçimi rezervasyon süresini uzatmaz. |
| `ConfirmCheckout` | Customer | Aktif ödeme oturumu mevcut. Stok rezervasyonu aktif ve süresi dolmamış. Teslimat adresi seçilmiş. Promosyon değerlendirmesi tamamlanmış. | `PaymentInitiated` | Müşteri İyzico ödeme sayfasına yönlendirilir (PAY-01, CC-07). Platform kart verilerini hiçbir aşamada işlemez veya kaydetmez (PAY-02). Promosyon değerlendirmesi bu noktada kilitlenir — ödeme onaylanana kadar başka kupon kodu girişi kabul edilmez. |

---

### 5. Sipariş ve Ödeme

| Komut | Aktör | Ön Koşul | Üretilen Event(ler) | İş Kuralları |
|---|---|---|---|---|
| `HandlePaymentSuccess` | System *(İyzico callback aracılığıyla)* | Ödeme sağlayıcısı, geçerli bir referansla işlemi onaylamış. Bu oturuma ait stok rezervasyonu hâlâ aktif ve süresi dolmamış. | `PaymentConfirmed`, `OrderCreated`, `StockPermanentlyDeducted` | Bu üç event, tek bir atomik mantıksal işlem olarak üretilir. Siparişe benzersiz ve insan tarafından okunabilir bir sipariş numarası atanır (OM-01). İlk durum `PAYMENT_CONFIRMED` olarak ayarlanır. Uygulanan promosyonlar, indirim tutarları ve ödenen nihai fiyat sipariş kaydıyla birlikte saklanır (OM-06). **Uç durum:** callback geldiğinde rezervasyonun süresi dolmuşsa ödeme otomatik olarak iade edilir ve sipariş oluşturulmaz. |
| `HandlePaymentFailure` | System *(İyzico callback aracılığıyla)* | Ödeme sağlayıcısı işlemi reddetmiş. Bu oturuma ait stok rezervasyonu mevcut. | `PaymentFailed`, `StockReservationReleased` | Rezerve edilen stok anında mevcut envantera geri döner. Müşteriye ödeme hata mesajı ve tekrar deneme seçeneği gösterilir (CC-07). |
| `HandlePaymentCancellation` | System *(İyzico callback aracılığıyla)* | Müşteri ödeme sayfasında işlemi iptal etmiş. Bu oturuma ait stok rezervasyonu mevcut. | `StockReservationReleased` | Rezerve edilen stok anında mevcut envantera geri döner. Müşteri sepetine yönlendirilir. |

---

### 6. Sipariş Karşılama

| Komut | Aktör | Ön Koşul | Üretilen Event(ler) | İş Kuralları |
|---|---|---|---|---|
| `StartOrderProcessing` | Admin | Sipariş `PAYMENT_CONFIRMED` durumunda. | `OrderProcessingStarted` | Admin, siparişin hazırlık aşamasına alındığını onaylar. Sipariş durumu `PROCESSING` olarak güncellenir. İşlem ve işlemi yapan admin kaydı denetim günlüğüne eklenir (AT-02). |
| `ShipOrder` | Admin | Sipariş `PROCESSING` durumunda. | `OrderShipped` | Sipariş durumu `SHIPPED` olarak güncellenir. Takip numarası ve kargo firması isteğe bağlı olarak eklenebilir. Takip numarası girilmişse müşteriye gönderilen kargo bildirim e-postasına dahil edilir (NT-04). İşlem, takip bilgileri ve admin kaydı denetim günlüğüne eklenir. |
| `ConfirmOrderDelivery` | Admin | Sipariş `SHIPPED` durumunda. | `OrderDelivered` | Sipariş durumu `DELIVERED` olarak güncellenir. Sipariş tamamlanmış sayılır; başka durum geçişi mümkün değildir. İşlem denetim günlüğüne kaydedilir. |
| `CancelOrder` | Admin | Sipariş `PAYMENT_CONFIRMED` veya `PROCESSING` durumunda. İptal nedeni belirtilmiş. | `OrderCancelled`, `RefundInitiated` | Sipariş `SHIPPED` veya `DELIVERED` durumuna geçtikten sonra iptal edilemez (OM-07). İptal işleminde: (1) kalıcı olarak düşülmüş stok mevcut envantera geri döner; (2) İyzico üzerinden iade başlatılır; (3) müşteriye iptal bildirim e-postası gönderilir (NT-06); (4) iptal nedeni ve işlemi yapan admin kaydı denetim günlüğüne eklenir (AT-02). |
| `CompleteRefund` | System *(İyzico callback aracılığıyla)* | `RefundInitiated` event'i yayımlanmış. Ödeme sağlayıcısı iadeyi onaylamış. | `RefundCompleted` | Tutarın müşterinin ödeme yöntemine geri yüklendiğini doğrular. Gateway referans numarasıyla birlikte denetim günlüğüne kaydedilir. |

---

### 7. Bildirimler ve Arka Plan İşleri

| Komut | Aktör | Ön Koşul | Üretilen Event(ler) | İş Kuralları |
|---|---|---|---|---|
| `DispatchEmail` | System | E-posta bildirimi gerektiren bir Domain Event yayımlanmış (örn. `CustomerRegistered` → hoşgeldiniz e-postası, `OrderCreated` → sipariş onay e-postası, `OrderShipped` → kargo bildirim e-postası). | `EmailDispatched` veya `EmailDeliveryFailed` | E-posta, üçüncü taraf bir e-posta servis sağlayıcısı (örn. AWS SES, Resend) aracılığıyla gönderilir. Sonuç her koşulda kaydedilir — başarılı teslimat da sağlayıcı düzeyindeki red de denetim günlüğüne işlenir (NT-01 ile NT-06). Yeniden deneme mantığı platforma değil, e-posta sağlayıcısına devredilir. |
| `ReleaseExpiredStockReservation` | Background Job | Bir stok rezervasyon kaydı yapılandırılmış zaman aşımı penceresini geçmiş (varsayılan: 15 dakika, IR-03). | `StockReservationExpired` | Düzenli aralıklarla süresi dolmuş rezervasyonları tarayan zamanlanmış bir iş tarafından çalıştırılır. Rezerve edilen miktarlar mevcut stoğa geri döner. İlgili ödeme oturumu geçersiz kılınır — müşteri geri dönürse ödeme adımını baştan başlatması gerekir. Denetim günlüğü güncellenir (AT-04). |

---

## Aktör × Komut Referans Matrisi

Hangi aktörlerin hangi komutları göndermeye yetkili olduğunu gösteren hızlı başvuru tablosu.

| Alan | Komut | Customer | Admin | System | Background Job |
|---|---|:---:|:---:|:---:|:---:|
| Kimlik | `RegisterCustomer` | ✅ | | | |
| Kimlik | `LoginCustomer` | ✅ | | | |
| Kimlik | `LogoutCustomer` | ✅ | | | |
| Kimlik | `RequestPasswordReset` | ✅ | | | |
| Kimlik | `ResetPassword` | ✅ | | | |
| Kimlik | `UpdateCustomerProfile` | ✅ | | | |
| Kimlik | `AddDeliveryAddress` | ✅ | | | |
| Kimlik | `RemoveDeliveryAddress` | ✅ | | | |
| Katalog | `AddProductToCatalog` | | ✅ | | |
| Katalog | `UpdateProductDetails` | | ✅ | | |
| Katalog | `UpdateProductPrice` | | ✅ | | |
| Katalog | `ActivateProduct` | | ✅ | | |
| Katalog | `DeactivateProduct` | | ✅ | | |
| Katalog | `AdjustStock` | | ✅ | | |
| Promosyonlar | `CreatePromotion` | | ✅ | | |
| Promosyonlar | `UpdatePromotion` | | ✅ | | |
| Promosyonlar | `ActivatePromotion` | | ✅ | | |
| Promosyonlar | `DeactivatePromotion` | | ✅ | | |
| Promosyonlar | `ApplyVoucherCode` | ✅ | | | |
| Promosyonlar | `EvaluateCartPromotions` | | | ✅ | |
| Sepet | `AddItemToCart` | ✅ | | | |
| Sepet | `UpdateCartItemQuantity` | ✅ | | | |
| Sepet | `RemoveItemFromCart` | ✅ | | | |
| Sepet | `InitiateCheckout` | ✅ | | | |
| Sepet | `SelectDeliveryAddress` | ✅ | | | |
| Sepet | `ConfirmCheckout` | ✅ | | | |
| Ödeme | `HandlePaymentSuccess` | | | ✅ | |
| Ödeme | `HandlePaymentFailure` | | | ✅ | |
| Ödeme | `HandlePaymentCancellation` | | | ✅ | |
| Sipariş Karşılama | `StartOrderProcessing` | | ✅ | | |
| Sipariş Karşılama | `ShipOrder` | | ✅ | | |
| Sipariş Karşılama | `ConfirmOrderDelivery` | | ✅ | | |
| Sipariş Karşılama | `CancelOrder` | | ✅ | | |
| Sipariş Karşılama | `CompleteRefund` | | | ✅ | |
| Bildirimler | `DispatchEmail` | | | ✅ | |
| Bildirimler | `ReleaseExpiredStockReservation` | | | | ✅ |
