# Bounded Context'ler

**Döküman:** `docs/03-domain-design/event-storming/tr/04-bounded-contexts.md`
**Aşama:** Stratejik Tasarım
**İlgili Dökümanlar:** [`01-domain-events.md`](./01-domain-events.md) · [`02-commands-and-actors.md`](./02-commands-and-actors.md) · [`03-aggregates.md`](./03-aggregates.md)
**İlgili Gereksinimler:** İş Gereksinimleri Dokümanı (Tüm Bölümler)

---

## Genel Bakış

Bu döküman, TechNest platformu için Bounded Context'leri tanımlar. Bounded Context, belirli bir alan modelinin geçerli olduğu açık bir sınırdır. Bu sınır içinde her terim, kavram ve kural tek, net bir anlama sahiptir. Aynı kelime, farklı bir Bounded Context'te tamamen farklı bir şey ifade edebilir — bu tasarım gereğidir.

Bounded Context'ler, modüler ayrıştırmanın temel birimleridir. TechNest'in mimarisinde platform bir **modüler monolith** olarak inşa edilmiştir: tüm Bounded Context'ler tek bir dağıtılabilir uygulama içinde yaşar ve tek bir veritabanını paylaşır; ancak kodda doğrudan modüller arası bağımlılığa izin verilmeyecek şekilde yapısal olarak ayrı modüller olarak uygulanır.

**Bu neden önemlidir:**
- Her modül bağımsız olarak geliştirilebilir, test edilebilir ve anlaşılabilir.
- Ayrı servislere geçiş gerekirse (ileride ihtiyaç duyulursa) bu adım, tam bir yeniden yazım yerine kademeli bir süreç olur.
- İş kuralları kendi sınırlarında kalır — Promosyonlar bağlamındaki bir değişiklik, Envanter bağlamını yanlışlıkla bozamaz.

---

## Context Map Özeti

TechNest platformu için altı Bounded Context belirlenmiştir:

| # | Bounded Context | Temel Sorumluluk | Aggregate'ler |
|---|---|---|---|
| 1 | [Identity](#1-identity-context) | Müşteri kimlik doğrulama ve profil yönetimi | `Customer` |
| 2 | [Catalog](#2-catalog-context) | Ürün tanımları, varyantlar, fiyatlandırma ve görünürlük | `Product` |
| 3 | [Inventory](#3-inventory-context) | Stok miktarları, rezervasyonlar ve kalıcı düşümler | `StockLedger`, `StockReservation` |
| 4 | [Promotions](#4-promotions-context) | İndirim kuralı yapılandırması ve değerlendirme | `Promotion` |
| 5 | [Commerce](#5-commerce-context) | Sepet, ödeme, sipariş ve ödeme yaşam döngüsü | `Cart`, `Order`, `Payment` |
| 6 | [Notifications](#6-notifications-context) | İşlemsel e-posta gönderimi ve teslimat takibi | `Notification` |

---

## 1. Identity Context

### Sorumluluk

Müşterinin kim olduğuyla ilgili her şeye sahiptir: nasıl kayıt olduğu, nasıl kimlik doğruladığı, kimlik bilgilerini nasıl yönettiği ve kişisel profilini ile kayıtlı adreslerini nasıl düzenlediği.

Identity kasıtlı olarak dar tutulmuştur. Siparişler, ürünler veya promosyonlar hakkında hiçbir şey bilmez. Tek odak noktası şu soruyu yanıtlamaktır: *"Bu kişi iddia ettiği kişi mi ve bu kişi hakkında ne biliyoruz?"*

### Aggregate'ler

- `Customer`

### Ortak Dil (Ubiquitous Language)

| Terim | Bu bağlamdaki anlamı |
|---|---|
| **Customer** | E-posta adresi, şifre hash'i ve en az bir olası teslimat adresine sahip kayıtlı kullanıcı. |
| **Session** | Başarılı girişten sonra düzenlenen, süre sınırlı kimlik doğrulama durumu (access token + refresh token çifti). |
| **Reset Token** | Mevcut şifreyi bilmeden şifre değiştirmeye olanak tanıyan, tek kullanımlık, süre sınırlı yetki belgesi. |
| **Address** | Müşteri profiline kaydedilen, adlandırılmış teslimat veya fatura konumu. |

### İşlenen Komutlar

`RegisterCustomer` · `LoginCustomer` · `LogoutCustomer` · `RequestPasswordReset` · `ResetPassword` · `UpdateCustomerProfile` · `AddDeliveryAddress` · `RemoveDeliveryAddress`

### Yayımlanan Event'ler

`CustomerRegistered` · `CustomerPasswordResetRequested` · `CustomerPasswordReset` · `CustomerAddressAdded`

### Tüketilen Event'ler

Hiçbiri. Identity tamamen yukarı yönlü (upstream) bir bağlamdır — diğer bağlamlardan gelen event'lere tepki vermez.

### Entegrasyon Noktaları

| Yön | Bağlam | Mekanizma | Amaç |
|---|---|---|---|
| Yayımlar → | Notifications | Domain Event (`CustomerRegistered`) | Hoşgeldiniz e-postasını tetikle |
| Yayımlar → | Notifications | Domain Event (`CustomerPasswordResetRequested`) | Şifre sıfırlama e-postasını tetikle |
| Okunur | Commerce | Müşteri kimlik sorgusu | Ödeme adımında kimlik doğrulama durumunu doğrula |
| Okunur | Commerce | Adres verisi sorgusu | Ödeme adımında kayıtlı adresleri önceden doldur |

### Temel İş Kuralları

- E-posta adresleri tüm müşteri kayıtları arasında global olarak benzersizdir.
- Şifreler bcrypt hash olarak saklanır; düz metin şifre hash işleminden sonra asla tutulmaz.
- Şifre sıfırlama isteklerinde, e-postanın kayıtlı olup olmadığından bağımsız olarak her zaman genel bir başarı yanıtı döndürülür.
- Başarılı bir şifre sıfırlamasının ardından, o hesaba ait tüm aktif oturumlar geçersiz kılınır.
- İlk sürümde e-posta doğrulaması zorunlu değildir — hesaplar kayıt anında hemen aktif olur.

---

## 2. Catalog Context

### Sorumluluk

TechNest'in sattığı ürünlerin tanımına sahiptir: ürünler, varyantları, fiyatları, görselleri, açıklamaları ve kategori atamaları. Ürünlerin mağazadaki görünürlüğünü yönetir — aktif mi yoksa devre dışı mı — ancak kaç adet stok olduğunu ya da ürünün daha önce sipariş edilip edilmediğini bilmez.

### Aggregate'ler

- `Product`

### Ortak Dil (Ubiquitous Language)

| Terim | Bu bağlamdaki anlamı |
|---|---|
| **Product** | TechNest'in sattığı, adı ve açıklaması olan, muhtemelen birden fazla varyantta sunulan ürün. |
| **Variant** | Bir veya daha fazla özellikle (örn. renk, depolama) farklılaşan, satın alınabilir bir ürün versiyonu. Her varyantın kendine ait SKU'su ve fiyatı vardır. |
| **SKU** | Katalog genelinde bir ürün varyantı için benzersiz dize tanımlayıcı. |
| **Category** | Ürünleri gruplamak ve mağazada gezinmeyi kolaylaştırmak için kullanılan hiyerarşik sınıflandırma. |
| **Aktif / Pasif** | Bir ürünün görünürlük durumu. Pasif ürünler müşterilere gizlenir ancak sistemde tutulur. |

### İşlenen Komutlar

`AddProductToCatalog` · `UpdateProductDetails` · `UpdateProductPrice` · `ActivateProduct` · `DeactivateProduct`

### Yayımlanan Event'ler

`ProductAddedToCatalog` · `ProductPriceUpdated`

### Tüketilen Event'ler

Bu aşamada hiçbiri. Catalog tamamen yukarı yönlü (upstream) bir bağlamdır.

### Entegrasyon Noktaları

| Yön | Bağlam | Mekanizma | Amaç |
|---|---|---|---|
| Yayımlar → | Inventory | Domain Event (`ProductAddedToCatalog`) | Her yeni varyant için `StockLedger` kaydı başlat |
| Okunur | Inventory | Varyant sorgusu | Stok düzenlemesi kabul edilmeden önce varyantın varlığını doğrula |
| Okunur | Commerce | Ürün/varyant veri sorgusu | Sepette ürün detaylarını göster; siparişe anlık görüntü al |
| Okunur | Promotions | Kategori sorgusu | Kategori bazlı promosyon oluştururken hedef kategorinin varlığını doğrula |

### Temel İş Kuralları

- Bir ürün en az bir kategoriye ait olmak zorundadır.
- Her varyant, katalog genelinde benzersiz bir SKU'ya sahip olmalıdır.
- Ürün varsayılan olarak pasif oluşturulur; mağazada görünmesi için açıkça aktif edilmesi gerekir.
- Ürünü devre dışı bırakmak, daha önce verilmiş siparişleri etkilemez.
- Fiyat değişiklikleri, izlenebilirlik amacıyla eski ve yeni değerler birlikte event olarak kaydedilir.

---

## 3. Inventory Context

### Sorumluluk

Her ürün varyantının fiziksel olarak kaç adet mevcut, geçici olarak rezerve veya kalıcı olarak satılmış olduğunun gerçeğine sahiptir. Eş zamanlı ödeme oturumlarında fazla satışı önleyen kritik iş kurallarını uygular.

Inventory, ürünleri adlarıyla ya da müşterileri adlarıyla tanımaz — yalnızca `VariantId` tanımlayıcıları ve miktar sayıları üzerinde çalışır.

### Aggregate'ler

- `StockLedger`
- `StockReservation`

### Ortak Dil (Ubiquitous Language)

| Terim | Bu bağlamdaki anlamı |
|---|---|
| **Mevcut (Available)** | Herhangi bir müşteri tarafından rezerve edilebilecek serbest adet. Hesaplama: `Toplam − Rezerve − Satılmış`. |
| **Rezerve (Reserved)** | Belirli bir aktif ödeme oturumu için geçici olarak tutulan adetler. Diğer müşterilere mevcut stok olarak görünmez. |
| **Satılmış (Sold)** | Ödeme onayının ardından kalıcı olarak düşülen adetler. Artık satış havuzunun parçası değildir. |
| **Rezervasyon** | Belirli bir ödeme oturumuna bağlı, belirli miktarda varyant üzerindeki süre sınırlı kilit. |
| **Düşüm (Deduction)** | Ödeme onayının ardından rezervasyonun kalıcı satış kaydına dönüştürülmesi. |
| **Zaman Aşımı (Timeout)** | Tamamlanmamış bir rezervasyonun otomatik olarak serbest bırakıldığı 15 dakikalık pencere. |

### İşlenen Komutlar

`AdjustStock` · `ReleaseExpiredStockReservation` · *(rezervasyon yaşam döngüsü komutları Commerce'ten domain event'ler aracılığıyla tetiklenir)*

### Yayımlanan Event'ler

`ProductStockAdjusted` · `LowStockThresholdReached` · `StockReserved` · `StockReservationFailed` · `StockReservationExpired` · `StockReservationReleased` · `StockPermanentlyDeducted`

### Tüketilen Event'ler

| Event | Üreten | Alınan Aksiyon |
|---|---|---|
| `CheckoutInitiated` | Commerce | Tüm sepet ürünleri için atomik stok rezervasyonu dene |
| `PaymentConfirmed` | Commerce | Aktif rezervasyonu kalıcı düşüme dönüştür |
| `PaymentFailed` | Commerce | Rezerve edilen stoğu anında serbest bırak |
| `StockReservationReleased` | Kendi kendine | `StockLedger` toplamlarını güncelle (rezerve toplamını azalt) |
| `StockPermanentlyDeducted` | Kendi kendine | `StockLedger` toplamlarını güncelle (rezerveyi satışa dönüştür) |

### Entegrasyon Noktaları

| Yön | Bağlam | Mekanizma | Amaç |
|---|---|---|---|
| Yayımlar → | Commerce | Domain Event (`StockReserved` / `StockReservationFailed`) | Ödeme akışını aç veya engelle |
| Yayımlar → | Commerce | Domain Event (`StockPermanentlyDeducted`) | Sipariş oluştuktan sonra stok düşümünü onayla |
| Yayımlar → | Notifications | Domain Event (`LowStockThresholdReached`) | Düşük stok admin uyarısını tetikle |
| Okur | Catalog | Varyant varlığı sorgusu | Düzeltme kabul edilmeden önce varyantın bilindiğini doğrula |

### Temel İş Kuralları

- Stok rezervasyonu tüm sepet ürünleri için atomiktir — ya hepsi ya hiçbiri, kısmi rezervasyona izin verilmez.
- Aynı son birim için iki eş zamanlı ödeme girişimi veritabanı düzeyinde optimistic locking ile çözülür; tam olarak biri başarılı olur.
- Rezervasyon penceresi varsayılan olarak 15 dakikadır ve sistem tarafından yapılandırılabilir.
- Mevcut stoğu sıfırın altına düşürecek negatif stok düzeltmesine uyarıyla izin verilir — mevcut rezervasyonlar iptal edilmeden önceliklendirilir.
- Ödeme başarı callback'i gelmeden önce stok rezervasyonu süresi dolmuşsa ödeme iade edilir ve düşüm gerçekleşmez.

---

## 4. Promotions Context

### Sorumluluk

İndirim kurallarının yapılandırmasına ve ödeme adımında belirli bir sepete hangi promosyonların uygulanacağını değerlendirme mantığına sahiptir. Tüm promosyon verilerinin — türler, kapsamlar, geçerlilik pencereleri, birleştirme kuralları ve kullanım takibi — yetkili kaynağıdır.

Promotions, müşterilerle doğrudan etkileşime girmez ve kullanıcı arayüzünde ne gösterileceğine karar vermez. Rolü tamamen şudur: *"Bu sepet durumu göz önüne alındığında hangi indirimler uygulanır ve ne kadar?"*

### Aggregate'ler

- `Promotion`

### Ortak Dil (Ubiquitous Language)

| Terim | Bu bağlamdaki anlamı |
|---|---|
| **Promotion** | Tanımlanmış türü, kapsamı, geçerlilik penceresi ve birleştirme davranışıyla yapılandırılmış indirim kuralı. |
| **Voucher Code** | Müşterinin belirli bir promosyonu etkinleştirmek için girdiği isteğe bağlı dize. |
| **Birleştirilebilir (Combinable)** | Diğer birleştirilebilir promosyonlarla yığınlanabilen promosyon. İndirimi diğerleriyle toplanır. |
| **Birleştirilemez (Non-combinable)** | Tek başına uygulanması gereken promosyon. Birleştirilebilirlerle birlikte uygulanabilir durumdaysa sistem, daha yüksek toplam indirimi veren senaryoyu seçer. |
| **Kapsam (Scope)** | Promosyonun tüm sepet toplamına (`CART`) mı yoksa yalnızca belirli bir kategorideki ürünlere mi (`CATEGORY`) uygulandığı. |
| **Uygun (Eligible)** | Belirli bir sepet ve müşteri için tüm geçerlilik, eşik değeri ve kullanım kontrollerinden geçen promosyon. |

### İşlenen Komutlar

`CreatePromotion` · `UpdatePromotion` · `ActivatePromotion` · `DeactivatePromotion` · `ApplyVoucherCode` · `EvaluateCartPromotions`

### Yayımlanan Event'ler

`PromotionCreated` · `PromotionActivated` · `PromotionDeactivated` · `PromotionAppliedToCart`

### Tüketilen Event'ler

| Event | Üreten | Alınan Aksiyon |
|---|---|---|
| `OrderCreated` | Commerce | Uygulanan promosyonlarda `TotalUsesCount` değerini artır |

### Entegrasyon Noktaları

| Yön | Bağlam | Mekanizma | Amaç |
|---|---|---|---|
| Yayımlar → | Commerce | Domain Event (`PromotionAppliedToCart`) | Sipariş özeti için indirim dökümünü sağla |
| Okur | Catalog | Kategori verisi sorgusu | Kategori bazlı promosyon oluştururken kategorinin var olduğunu doğrula |
| Okur | Identity | Müşteri verisi sorgusu | Promosyon değerlendirirken müşteri kısıtlamasını kontrol et |

### Temel İş Kuralları

- Kupon kodları, aktif tüm promosyonlar arasında benzersiz olmalıdır.
- Birleştirme algoritması: en iyi tek birleştirilemez indirimi, tüm birleştirilebilir indirimlerin toplamıyla karşılaştır; müşteriye daha yüksek toplam tasarrufu sağlayanı uygula.
- İki birleştirilemez promosyon eşit indirim değerine sahipse, daha önce oluşturulan (`CreatedAt`) seçilir — deterministik, rastgele değil.
- `TotalUsesCount`, değerlendirme anında değil sipariş oluşturulduğunda artırılır — eş zamanlı değerlendirmeler birbirini asla engellemez.
- Müşteri ödeme sürecindeyken devre dışı bırakılan promosyon, sipariş onayında hariç tutulur ve müşteriye bildirilir.
- İndirimlerden sonra sepet toplamı sıfırın altına inemez.

---

## 5. Commerce Context

### Sorumluluk

Commerce bağlamı, TechNest'in operasyonel çekirdeğidir. Müşterinin tüm satın alma yolculuğunu baştan sona yönetir — sepet oluşturmaktan onaylanmış, tamamlanmış veya iptal edilmiş bir siparişe kadar. Diğer bağlamları koordine eder (stok rezervasyonu için Inventory, indirimler için Promotions, müşteri verisi için Identity, ödeme sağlayıcı etkileşimi için Payment) ancak onların iş mantığının sahibi değildir.

Bu kasıtlı olarak en büyük ve en aktif bağlamdır. Şunlara sahiptir:
- Müşteriye yönelik alışveriş deneyimi (sepet)
- Ödeme akışı ve oturum durumu
- Her işlemin kalıcı kaydı (sipariş)
- Her ödeme girişiminin yaşam döngüsü

### Aggregate'ler

- `Cart`
- `Order`
- `Payment`

### Ortak Dil (Ubiquitous Language)

| Terim | Bu bağlamdaki anlamı |
|---|---|
| **Cart** | Müşterinin satın almayı planladığı ürünleri içeren, satın alma öncesi çalışma alanı. Herhangi bir finansal taahhüt içermez. |
| **Checkout Session** | "Ödemeye Geç"e tıklama ile ödemeyi tamamlama (veya vazgeçme) arasındaki aktif pencere. |
| **Order** | Yalnızca ödeme onaylandıktan sonra oluşturulan, tamamlanmış satın almanın değiştirilemez kaydı. |
| **Order Number** | Sipariş için insan tarafından okunabilir benzersiz tanımlayıcı (örn. `TN-2025-00142`). |
| **Grand Total** | Ödenen nihai tutar: ara toplam eksi uygulanan indirimler. Negatif olamaz. |
| **Order Status** | Siparişin mevcut yaşam döngüsü aşaması: `PAYMENT_CONFIRMED` → `PROCESSING` → `SHIPPED` → `DELIVERED` (veya `CANCELLED`). |
| **Snapshot** | Ürün, adres veya promosyon verisinin sipariş oluşturulduğu anda alınan kopyası; siparişi ileriki katalog değişikliklerinden yalıtır. |

### İşlenen Komutlar

`AddItemToCart` · `UpdateCartItemQuantity` · `RemoveItemFromCart` · `InitiateCheckout` · `SelectDeliveryAddress` · `ConfirmCheckout` · `HandlePaymentSuccess` · `HandlePaymentFailure` · `HandlePaymentCancellation` · `StartOrderProcessing` · `ShipOrder` · `ConfirmOrderDelivery` · `CancelOrder` · `CompleteRefund`

### Yayımlanan Event'ler

`CheckoutInitiated` · `PaymentInitiated` · `PaymentConfirmed` · `PaymentFailed` · `OrderCreated` · `OrderProcessingStarted` · `OrderShipped` · `OrderDelivered` · `OrderCancelled` · `RefundInitiated` · `RefundCompleted`

### Tüketilen Event'ler

| Event | Üreten | Alınan Aksiyon |
|---|---|---|
| `StockReserved` | Inventory | Ödeme adımının adres seçimine ilerlemesine izin ver |
| `StockReservationFailed` | Inventory | Ödeme adımını engelle; müşteriye mevcut olmayan ürünleri göster |
| `PromotionAppliedToCart` | Promotions | Sepeti indirim dökümüyle güncelle; sipariş özetini göster |
| `StockPermanentlyDeducted` | Inventory | Sipariş oluşturma sürecinde stok düşümünü onayla |

### Entegrasyon Noktaları

| Yön | Bağlam | Mekanizma | Amaç |
|---|---|---|---|
| Yayımlar → | Inventory | Domain Event (`CheckoutInitiated`) | Stok rezervasyonunu tetikle |
| Yayımlar → | Inventory | Domain Event (`PaymentConfirmed`) | Kalıcı stok düşümünü tetikle |
| Yayımlar → | Inventory | Domain Event (`PaymentFailed`, `OrderCancelled`) | Stok serbest bırakmayı tetikle |
| Yayımlar → | Promotions | Domain Event (`CheckoutInitiated`) | Promosyon değerlendirmesini tetikle |
| Yayımlar → | Notifications | Domain Event (`OrderCreated`, `OrderShipped`, `OrderCancelled`, `PaymentFailed`) | İşlemsel e-postaları tetikle |
| Okur | Identity | Müşteri ve adres verisi sorgusu | Müşteriyi doğrula; kayıtlı adresleri yükle |
| Okur | Catalog | Ürün ve varyant veri sorgusu | Sepet ürünlerini göster; siparişe anlık görüntü al |
| Dış çağrı | Payment Gateway (İyzico) | HTTP yönlendirme / webhook | Ödemeyi başlat; sonuç callback'ini al |

### Temel İş Kuralları

- Sepet aşamasında stok rezervasyonu yapılmaz — rezervasyon yalnızca ödeme başlatıldığında başlar.
- Sepet ürün verisi (ad, fiyat, özellikler), sipariş oluşturulurken anlık görüntü olarak kaydedilir. Sonraki katalog değişiklikleri mevcut siparişleri etkilemez.
- Sipariş yalnızca İyzico tarafından ödeme onaylandıktan sonra oluşturulur. Platform asla spekülatif sipariş oluşturmaz.
- Ödeme başarı callback'i geldiğinde stok rezervasyonu süresi dolmuşsa ödeme iade edilir ve sipariş oluşturulmaz.
- Sipariş durumu `SHIPPED` olduktan sonra iptal engellenir.
- İyzico'nun aynı event'i iki kez göndermesi durumunda mükerrer sipariş oluşturulmaması için `Payment` aggregate'i üzerinde idempotency uygulanır.

---

## 6. Notifications Context

### Sorumluluk

Platform tarafından gönderilen her işlemsel e-postanın gönderimini ve denetim kaydını yönetir. Diğer bağlamlardan gelen domain event'lere tepki verir — kendi başına iletişim başlatmaz.

Notifications tamamen aşağı yönlü (downstream) bir bağlamdır. Başka hiçbir bağlamanın dinlemediği event'ler yayımlamaz. Çıktısı her zaman dışarıya yöneliktir: e-posta servis sağlayıcısına ve denetim günlüğüne.

### Aggregate'ler

- `Notification`

### Ortak Dil (Ubiquitous Language)

| Terim | Bu bağlamdaki anlamı |
|---|---|
| **Notification** | Sonucuyla birlikte tek bir e-posta gönderim girişiminin kaydı. |
| **Template** | Belirli bir tetikleyici event türüne bağlı önceden tanımlanmış e-posta biçimi (örn. sipariş onayı, kargo bildirimi). |
| **Dispatch** | E-postanın teslimat için üçüncü taraf sağlayıcıya gönderilme eylemi. |
| **Provider Reference** | Gönderilen mesaj için e-posta servis sağlayıcısından (AWS SES / Resend) dönen benzersiz tanımlayıcı. |

### İşlenen Komutlar

`DispatchEmail`

### Yayımlanan Event'ler

`EmailDispatched` · `EmailDeliveryFailed`

### Tüketilen Event'ler

| Event | Üreten | Tetiklenen E-posta |
|---|---|---|
| `CustomerRegistered` | Identity | Hoşgeldiniz e-postası (NT-01) |
| `CustomerPasswordResetRequested` | Identity | Şifre sıfırlama linki e-postası (NT-02) |
| `OrderCreated` | Commerce | Sipariş onay e-postası (NT-03) |
| `OrderShipped` | Commerce | Takip bilgisiyle kargo bildirimi (NT-04) |
| `PaymentFailed` | Commerce | Ödeme başarısız bildirimi (NT-05) |
| `OrderCancelled` | Commerce | Sipariş iptal bildirimi (NT-06) |

### Entegrasyon Noktaları

| Yön | Bağlam | Mekanizma | Amaç |
|---|---|---|---|
| Tüketir | Identity | Domain Event'ler | Hesap ile ilgili e-postaları tetikle |
| Tüketir | Commerce | Domain Event'ler | Sipariş ve ödeme ile ilgili e-postaları tetikle |
| Tüketir | Inventory | Domain Event (`LowStockThresholdReached`) | Düşük stok admin uyarısını tetikle |
| Dış çağrı | E-posta Sağlayıcısı (AWS SES / Resend) | HTTP API | E-postayı alıcıya ilet |

### Temel İş Kuralları

- Her e-posta girişimi kaydedilir — başarılı gönderimlerin yanı sıra sağlayıcı düzeyindeki redler de.
- Yeniden deneme mantığı tamamen e-posta sağlayıcısına devredilir. Platform sonucu kaydeder ancak kendi yeniden deneme döngüsünü uygulamaz.
- Notifications bağlamı, e-postanın *hangi içeriği* barındıracağına karar vermez — bu, tetikleyici event türüyle ilişkili şablon tarafından belirlenir.

---

## Context Map

Aşağıdaki diyagram, altı Bounded Context arasındaki ilişkileri ve iletişim yönlerini göstermektedir.

```
                         ┌─────────────────────┐
                         │      IDENTITY        │
                         │  ─────────────────   │
                         │  Customer            │
                         └────────┬────────────-┘
                                  │ CustomerRegistered
                                  │ CustomerPasswordResetRequested
                    ┌─────────────┼──────────────────┐
                    ▼             │                  ▼
         ┌──────────────────┐     │      ┌─────────────────────┐
         │   NOTIFICATIONS  │     │      │      CATALOG         │
         │  ─────────────── │     │      │  ─────────────────   │
         │  Notification    │     │      │  Product             │
         └──────────────────┘     │      └────────┬────────────-┘
                    ▲             │               │ ProductAddedToCatalog
                    │             ▼               ▼
                    │    ┌────────────────┐  ┌─────────────────────┐
                    │    │   COMMERCE     │  │     INVENTORY        │
                    │    │  ──────────    │  │  ─────────────────   │
                    │    │  Cart          │◄─┤  StockLedger         │
  OrderCreated      │    │  Order         │  │  StockReservation    │
  OrderShipped      └────┤  Payment       ├─►│                      │
  OrderCancelled         │                │  └─────────────────────-┘
  PaymentFailed          └───────┬────────┘
                                 │
                                 │ CheckoutInitiated
                                 ▼
                         ┌─────────────────────┐
                         │     PROMOTIONS       │
                         │  ─────────────────   │
                         │  Promotion           │
                         └─────────────────────-┘
```

### İlişki Türleri

| Yukarı Yönlü Bağlam | Aşağı Yönlü Bağlam | İlişki | Notlar |
|---|---|---|---|
| Identity | Commerce | **Published Language** | Commerce müşteri/adres verisini okur; Identity stabil event'ler yayımlar |
| Identity | Notifications | **Published Language** | Notifications kayıt ve sıfırlama event'lerine abone olur |
| Catalog | Inventory | **Published Language** | Inventory, ürünler eklendiğinde stok kayıtlarını başlatır |
| Catalog | Commerce | **Open Host Service** | Commerce, sepet gösterimi ve sipariş anlık görüntüleri için ürün verisini okur |
| Inventory | Commerce | **Published Language** | Commerce, rezervasyon sonuçlarına ve düşüm onaylarına tepki verir |
| Commerce | Promotions | **Customer–Supplier** | Commerce promosyon değerlendirmesini yönetir; Promotions Commerce'e hizmet eder |
| Commerce | Notifications | **Published Language** | Notifications sipariş ve ödeme event'lerine abone olur |
| Inventory | Notifications | **Published Language** | Notifications düşük stok eşiği event'lerine abone olur |

---

## Modül Yapısı (Modüler Monolith)

TechNest kod tabanında her Bounded Context, doğrudan bir uygulama modülüne karşılık gelir. Modüller yalnızca şunlar aracılığıyla iletişim kurar:

1. **Domain Event'ler** — asenkron, gevşek bağlı, süreç içi event bus'a yayımlanır
2. **Sorgu arayüzleri** — bağlamlar arası veri okumaları için senkron, salt okunur çağrılar (örn. Commerce'in Catalog'dan ürün adlarını okuması)

Modül içleri arasında doğrudan import yasaktır. Bir modül, yalnızca genel bir API (arayüzler, DTO'lar, event türleri) üzerinden diğer modüllere erişim açabilir.

```
src/
├── identity/
│   ├── domain/          → Customer aggregate, domain event'ler
│   ├── application/     → Command handler'lar, query handler'lar
│   └── infrastructure/  → Kalıcılık, token düzenleme
│
├── catalog/
│   ├── domain/          → Product aggregate, domain event'ler
│   ├── application/
│   └── infrastructure/
│
├── inventory/
│   ├── domain/          → StockLedger, StockReservation aggregate'leri, domain event'ler
│   ├── application/
│   └── infrastructure/
│
├── promotions/
│   ├── domain/          → Promotion aggregate, değerlendirme mantığı, domain event'ler
│   ├── application/
│   └── infrastructure/
│
├── commerce/
│   ├── domain/          → Cart, Order, Payment aggregate'leri, domain event'ler
│   ├── application/
│   └── infrastructure/
│
└── notifications/
    ├── domain/          → Notification aggregate, domain event'ler
    ├── application/     → Event listener'lar, e-posta gönderimi
    └── infrastructure/  → E-posta sağlayıcı entegrasyonu (AWS SES / Resend)
```
