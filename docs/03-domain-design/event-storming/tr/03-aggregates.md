# Aggregate Kataloğu

**Döküman:** `docs/03-domain-design/event-storming/tr/03-aggregates.md`
**Aşama:** Stratejik Tasarım
**İlgili Dökümanlar:** [`01-domain-events.md`](./01-domain-events.md) · [`02-commands-and-actors.md`](./02-commands-and-actors.md)
**İlgili Gereksinimler:** İş Gereksinimleri Dokümanı (Tüm Bölümler)

---

## Genel Bakış

Bu katalog, TechNest platformu için belirlenen Aggregate'leri tanımlar. Aggregate, tutarlılık birimi olarak tek bir bütün gibi ele alınan bir alan nesneleri kümesidir. İlgili Komutlar için bir kapı bekçisi görevi görür — komutun geçerli olup olmadığına karar verir, iş kurallarını uygular ve sonuç olarak Domain Event üretir.

Her Aggregate kendi durumunun tam sahibidir. Hiçbir Aggregate, başka bir Aggregate'in iç durumunu doğrudan okuyamaz veya değiştiremez. Aggregate'ler arası iletişim yalnızca Domain Event'ler aracılığıyla gerçekleşir.

**Uygulanan temel tasarım ilkeleri:**

- Aggregate sınırı, tek bir işlem içinde atomik olarak uygulanması gereken iş kuralları etrafına çizilir.
- Aggregate'ler mümkün olduğunca küçük tutulur — yalnızca aynı işlem içinde tutarlı olması gereken şeyler dahil edilir.
- Her Komut tam olarak bir Aggregate'i hedefler. Her Domain Event tam olarak bir Aggregate tarafından üretilir.

---

## Aggregate Özeti

| Aggregate | Alan | İşlenen Komutlar | Üretilen Event'ler |
|---|---|---|---|
| [`Customer`](#1-customer) | Kimlik | 8 | 4 |
| [`Product`](#2-product) | Katalog | 5 | 3 |
| [`StockLedger`](#3-stockledger) | Envanter | 2 | 5 |
| [`Promotion`](#4-promotion) | Promosyonlar | 4 | 3 |
| [`Cart`](#5-cart) | Sepet | 5 | 1 |
| [`StockReservation`](#6-stockreservation) | Envanter | 3 | 4 |
| [`Order`](#7-order) | Satış | 4 | 5 |
| [`Payment`](#8-payment) | Ödeme | 3 | 4 |
| [`Fulfillment`](#9-fulfillment) | Sipariş Karşılama | 4 | 6 |
| [`Notification`](#10-notification) | Bildirimler | 1 | 2 |

---

## Aggregate Kataloğu

---

### 1. Customer

**Alan:** Kimlik ve Müşteri Profili

`Customer` aggregate'i, kayıtlı bir alıcının kimliğini ve kişisel verilerini yönetir. Kimlik doğrulama durumu, kimlik bilgileri ve kayıtlı adresler için tek yetkili kaynaktır.

**Aggregate Root:** `Customer`

**Kimlik:** `CustomerId`

#### Komutlar ve İş Kuralları

| Komut | Uygulanan İş Kuralı | Üretilen Event(ler) |
|---|---|---|
| `RegisterCustomer` | E-posta adresi tüm Customer kayıtları arasında benzersiz olmalı. Şifre minimum güç kurallarını karşılamalı. | `CustomerRegistered` |
| `LoginCustomer` | Kimlik bilgileri eşleşmeli. Hesap mevcut olmalı. | *(event üretilmez — oturum dışarıda düzenlenir)* |
| `LogoutCustomer` | Oturum aktif olmalı. | *(event üretilmez — oturum dışarıda geçersiz kılınır)* |
| `RequestPasswordReset` | Aggregate düzeyinde kural uygulanmaz — komut kabul edildiğinde her zaman token üretilir. E-posta kayıtlı olsa da olmasa da yanıt her zaman genel bir başarı mesajıdır. | `CustomerPasswordResetRequested` |
| `ResetPassword` | Sıfırlama token'ı mevcut olmalı, bu müşteriye ait olmalı, kullanılmamış ve süresi dolmamış olmalı. | `CustomerPasswordReset` |
| `UpdateCustomerProfile` | E-posta değiştiriliyorsa yeni e-posta başka bir hesaba kayıtlı olmamalı. | *(bu aşamada event üretilmez)* |
| `AddDeliveryAddress` | Bu aşamada kesin bir sınır uygulanmaz. | `CustomerAddressAdded` |
| `RemoveDeliveryAddress` | Adres, açık bir siparişte kullanılıyor olmamalı. | *(bu aşamada event üretilmez)* |

#### Durum

```
Customer {
  CustomerId
  FullName
  EmailAddress
  PasswordHash
  IsActive
  Addresses[]        → AddressId, Line1, Line2, City, PostalCode, IsDefault
  ResetToken         → TokenHash, ExpiresAt, IsUsed
  CreatedAt
}
```

#### Notlar

- `LoginCustomer` ve `LogoutCustomer` Domain Event üretmez — oturum yönetimi (token düzenleme, saklama, geçersiz kılma) aggregate sınırının dışında ele alınan bir konudur.
- Sıfırlama token'ı `Customer` aggregate'ine dahildir; çünkü geçerlilik kuralları (tek kullanım, süre, sahiplik) müşterinin kimlik durumunun birer iş kuralıdır.
- `UpdateCustomerProfile` için e-posta benzersizliği, komut aggregate'e ulaşmadan önce uygulama katmanında tüm `Customer` koleksiyonuna karşı kontrol edilmelidir — bu, aggregate'in içinde değil dışında gerçekleşen bir okumadır.

---

### 2. Product

**Alan:** Katalog

`Product` aggregate'i, satılabilir bir ürünün tanımını, fiyatını ve görünürlüğünü yönetir. Ürünün varyantlarına ve fiyatlarına sahiptir; ancak stok miktarlarına sahip **değildir** — stok, `StockLedger` aggregate'inin sorumluluğundadır.

**Aggregate Root:** `Product`

**Kimlik:** `ProductId`

#### Komutlar ve İş Kuralları

| Komut | Uygulanan İş Kuralı | Üretilen Event(ler) |
|---|---|---|
| `AddProductToCatalog` | Benzersiz SKU'ya sahip en az bir varyant sağlanmalı. Ürün en az bir kategoriye atanmalı. | `ProductAddedToCatalog` |
| `UpdateProductDetails` | Ürün mevcut olmalı. | *(bu aşamada event üretilmez)* |
| `UpdateProductPrice` | Ürün varyantı bu ürün içinde mevcut olmalı. Fiyat pozitif bir değer olmalı. | `ProductPriceUpdated` |
| `ActivateProduct` | Ürünün en az bir varyantı olmalı. | `ProductAddedToCatalog` |
| `DeactivateProduct` | Ürün şu anda aktif olmalı. | *(bu aşamada event üretilmez)* |

#### Durum

```
Product {
  ProductId
  Name
  Description          (zengin metin)
  SpecificationTable   (anahtar-değer çiftleri)
  Images[]             → ImageId, Url, SortOrder
  Brand
  Categories[]         → CategoryId
  IsActive
  Variants[] {
    VariantId
    SKU                (katalog genelinde benzersiz)
    Attributes         (örn. Renk: Siyah, Depolama: 256GB)
    Price
    IsActive
  }
  CreatedAt
  UpdatedAt
}
```

#### Notlar

- `Product` aggregate'i kasıtlı olarak stok verisinden arındırılmıştır. Tek bir üründe çok sayıda varyant olabilir; stok değişiklikleri yüksek frekanslı işlemlerdir ve ürün kaydını kilitlememelidir.
- `DeactivateProduct` bu aşamada Domain Event üretmez. Aşağı yönlü tepkiler gerekirse (örn. ürünü arama indeksinden kaldırma) sonraki bir tasarım iterasyonunda özel bir event tanımlanmalıdır.
- Varyant `SKU` benzersizliği katalog genelinde geçerli bir kuraldır — komut aggregate'e ulaşmadan önce uygulama katmanında kontrol edilir.

---

### 3. StockLedger

**Alan:** Envanter

`StockLedger` aggregate'i, tek bir ürün varyantının fiziksel miktarını takip eder. Mevcut, rezerve ve satılmış miktarlar için yetkili kaynaktır. `StockLedger`, siparişler veya müşteriler hakkında hiçbir şey bilmez — yalnızca miktarları ve bunlara uygulanan işlemleri bilir.

**Aggregate Root:** `StockLedger`

**Kimlik:** `VariantId` *(her ürün varyantı için bir StockLedger)*

#### Komutlar ve İş Kuralları

| Komut | Uygulanan İş Kuralı | Üretilen Event(ler) |
|---|---|---|
| `AdjustStock` | Düzeltme nedeni belirtilmeli. Sıfırın altına düşen negatif düzeltmelere uyarıyla izin verilir (bkz. kenar durumlar). | `ProductStockAdjusted`, *(isteğe bağlı olarak)* `LowStockThresholdReached` |
| `ReleaseExpiredStockReservation` | Rezervasyon mevcut olmalı ve süre sonu zaman damgası geçmiş olmalı. | `StockReservationExpired` |

#### Durum

```
StockLedger {
  VariantId
  TotalPhysical         (alınan toplam birim)
  TotalReserved         (aktif rezervasyonların toplamı)
  TotalSold             (kalıcı olarak düşülen birimler)
  LowStockThreshold     (yapılandırılabilir)

  Available = TotalPhysical - TotalReserved - TotalSold
}
```

#### Notlar

- `StockLedger`, `StockReservation`'dan kasıtlı olarak ayrılmıştır. Defter toplamları takip eder; `StockReservation` aggregate'i bireysel rezervasyon oturumlarının yaşam döngüsünü yönetir.
- `Available` miktarı türetilmiş bir değerdir — doğrudan saklanmaz, her zaman üç takip edilen toplamdan hesaplanır. Bu, tutarsızlığı önler.
- `LowStockThresholdReached`, bir düzeltme veya rezervasyon mevcut stoğu yapılandırılan minimumun altına düşürdüğünde `StockLedger` tarafından üretilen bir yan etki event'idir (IR-08). `StockLedger` tarafından yayımlanır ancak dışarıdan tüketilir (örn. admin panel uyarısı).

---

### 4. Promotion

**Alan:** Promosyonlar ve Pazarlama

`Promotion` aggregate'i, tek bir indirim kuralını tanımlar ve yönetir. İndirim türü, kapsam, uygunluk kriterleri, birleştirme kuralları ve kullanım sınırları dahil tüm yapılandırma özelliklerine sahiptir. Belirli bir sepete promosyonun uygulanıp uygulanmayacağına kendisi karar vermez; bu değerlendirme, aggregate'in durum verisi kullanılarak uygulama katmanında gerçekleştirilir.

**Aggregate Root:** `Promotion`

**Kimlik:** `PromotionId`

#### Komutlar ve İş Kuralları

| Komut | Uygulanan İş Kuralı | Üretilen Event(ler) |
|---|---|---|
| `CreatePromotion` | Kupon kodu (tanımlanmışsa) aktif tüm promosyonlar arasında benzersiz olmalı. Kapsam kategori bazlıysa hedef kategori belirtilmeli. Bitiş tarihi başlangıç tarihinden sonra olmalı. | `PromotionCreated` |
| `UpdatePromotion` | Promosyon bitiş tarihini geçmemiş olmalı. | *(bu aşamada event üretilmez)* |
| `ActivatePromotion` | Promosyon şu anda pasif olmalı. | `PromotionActivated` |
| `DeactivatePromotion` | Promosyon şu anda aktif olmalı. | `PromotionDeactivated` |

#### Durum

```
Promotion {
  PromotionId
  Name
  DiscountType          (PERCENTAGE | FIXED_AMOUNT)
  DiscountValue
  Scope                 (CART | CATEGORY)
  TargetCategoryId      (Scope = CATEGORY ise zorunlu)
  MinimumCartValue      (isteğe bağlı)
  VoucherCode           (isteğe bağlı; benzersiz)
  StartDate
  EndDate
  IsActive
  IsCombinable
  MaxTotalUses          (isteğe bağlı)
  TotalUsesCount        (mevcut kullanım sayısı)
  CustomerRestriction   (isteğe bağlı; CustomerId)
  CreatedAt
}
```

#### Notlar

- `Promotion` aggregate'i uygunluk değerlendirmesi yapmaz — yapılandırmasını dışa açar ve `EvaluateCartPromotions` / `ApplyVoucherCode` komutları, tüm uygun promosyonları mevcut sepet durumuna karşı değerlendirmek için uygulama katmanını kullanır.
- `TotalUsesCount`, değerlendirme sırasında değil sipariş oluşturulduğu anda artırılır. Bu, bir promosyonun yalnızca birden fazla müşteri tarafından aynı anda değerlendirildiği için engellenmesini önler.
- Aktif promosyonlar arasında kupon kodu benzersizliği, `CreatePromotion` komutu kabul edilmeden önce uygulama katmanında kontrol edilir.

---

### 5. Cart

**Alan:** Sepet ve Ödeme Başlatma

`Cart` aggregate'i, müşterinin geçerli alışveriş oturumunun içeriğini yönetir. Müşterinin satın almayı planladığı ürün varyantlarını ve miktarlarını takip eder. Sepet, işlem öncesi bir çalışma alanıdır — herhangi bir finansal taahhüt içermez ve stok rezervasyonu yapmaz.

**Aggregate Root:** `Cart`

**Kimlik:** `CartId`

#### Komutlar ve İş Kuralları

| Komut | Uygulanan İş Kuralı | Üretilen Event(ler) |
|---|---|---|
| `AddItemToCart` | Ürün varyantı aktif olmalı. İstenen miktar ≥ 1 olmalı. | *(event üretilmez — sadece sepet durumu değişir)* |
| `UpdateCartItemQuantity` | Ürün sepette mevcut olmalı. Yeni miktar ≥ 1 olmalı ve mevcut stoku aşmamalı (komut anında kontrol edilir, rezerve edilmez). | *(event üretilmez — sadece sepet durumu değişir)* |
| `RemoveItemFromCart` | Ürün sepette mevcut olmalı. | *(event üretilmez — sadece sepet durumu değişir)* |
| `InitiateCheckout` | Sepet boş olmamalı. Müşteri kimlik doğrulamasını tamamlamış olmalı. Tüm ürünlerin yeterli mevcut stoğu olmalı (komut anında kontrol edilir). | `CheckoutInitiated` |
| `ConfirmCheckout` | Aktif ödeme oturumu mevcut olmalı. Stok rezervasyonu aktif ve süresi dolmamış olmalı. Teslimat adresi belirlenmiş olmalı. | `PaymentInitiated` |

#### Durum

```
Cart {
  CartId
  CustomerId
  Items[] {
    CartItemId
    VariantId
    ProductName          (eklenme anındaki anlık görüntü)
    VariantAttributes    (eklenme anındaki anlık görüntü)
    UnitPrice            (eklenme anındaki anlık görüntü)
    Quantity
  }
  CheckoutSession {
    SessionId
    DeliveryAddressId
    AppliedPromotions[]  → PromotionId, DiscountAmount
    TotalDiscount
    GrandTotal
    ReservationExpiresAt
  }
  CreatedAt
  UpdatedAt
}
```

#### Notlar

- Fiyat ve ürün adı anlık görüntüleri, ürün ekleme anında saklanır; bu sayede ürün sonradan güncellenip devre dışı bırakılsa bile sepet tutarlı veri göstermeye devam eder.
- `CheckoutSession` alt nesnesi, `InitiateCheckout` kabul edildiğinde oluşturulur; rezervasyon süresi dolduğunda veya ödeme başarısız olduğunda silinir ya da sıfırlanır.
- Sepet, başarılı bir siparişten sonra silinmez — temizlenir ve müşterinin bir sonraki oturumu için hazır hale gelir.

---

### 6. StockReservation

**Alan:** Envanter

`StockReservation` aggregate'i, tek bir stok rezervasyon oturumunun yaşam döngüsünü yönetir. Hangi varyant miktarlarının tutulduğunu, ne zaman sona ereceğini ve sonucu (tamamlandı, serbest bırakıldı veya zaman aşımına uğradı) takip eder. `StockLedger` ile yakın koordinasyon içinde çalışır — rezervasyon oluşturulduğunda, serbest bırakıldığında veya tamamlandığında `StockLedger` toplamları buna göre güncellenir.

**Aggregate Root:** `StockReservation`

**Kimlik:** `SessionId`

#### Komutlar ve İş Kuralları

| Komut | Uygulanan İş Kuralı | Üretilen Event(ler) |
|---|---|---|
| `InitiateCheckout` *(rezervasyon kısmı)* | Her varyant için mevcut stok ≥ istenen miktar olmalı. Rezervasyon atomik olmalı — ya tüm varyantlar başarılı ya da hiçbiri. Aynı son birim için eş zamanlı rezervasyonlar veritabanı düzeyinde optimistic locking ile çözülmeli (NFR-03). | `StockReserved` veya `StockReservationFailed` |
| `HandlePaymentSuccess` *(düşüm kısmı)* | Rezervasyon hâlâ aktif ve süresi dolmamış olmalı. | `StockPermanentlyDeducted` |
| `HandlePaymentFailure` / `HandlePaymentCancellation` | Rezervasyon aktif olmalı. | `StockReservationReleased` |

#### Durum

```
StockReservation {
  SessionId
  CartId
  CustomerId
  Status              (ACTIVE | FULFILLED | RELEASED | EXPIRED)
  Items[] {
    VariantId
    Quantity
  }
  CreatedAt
  ExpiresAt           (CreatedAt + varsayılan 15 dakika)
  ResolvedAt
  ReleaseReason       (PAYMENT_FAILED | CUSTOMER_CANCELLED | TIMEOUT | null)
}
```

#### Notlar

- Rezervasyon penceresi varsayılan olarak 15 dakikadır ve sistem tarafından yapılandırılabilir (IR-03). Değer, rezervasyon oluşturulurken belirlenir ve o oturum boyunca değişmez.
- `HandlePaymentSuccess` rezervasyon süresi dolduktan sonra gelirse `StockReservation` aggregate'i komutu reddeder. Uygulama katmanı bu durumda iade başlatır ve sipariş oluşturmaz.
- `StockReservationFailed`, sepetteki en az bir varyantın yetersiz stoku varsa üretilir. Tüm rezervasyon girişimi reddedilir — kısmi durum yazılmaz.
- Eş zamanlılık, `StockLedger` üzerinde veritabanı düzeyinde optimistic locking ile yönetilir. Dağıtık kilit gerekmez (NFR-03).

---

### 7. Order

**Alan:** Satış

`Order` aggregate'i, tamamlanmış bir satın almanın merkezi kaydıdır. Yalnızca ödeme onaylandıktan sonra oluşturulur ve müşterinin yasal ve finansal taahhüdünü temsil eder. Neyin satın alındığının, hangi fiyata ve hangi promosyonlarla alındığının değiştirilemez kaydına sahiptir.

**Aggregate Root:** `Order`

**Kimlik:** `OrderId`

#### Komutlar ve İş Kuralları

| Komut | Uygulanan İş Kuralı | Üretilen Event(ler) |
|---|---|---|
| `HandlePaymentSuccess` *(sipariş oluşturma kısmı)* | Bu oturum için geçerli ve süresi dolmamış bir `StockReservation` mevcut olmalı. Ödeme referansı mevcut bir siparişle ilişkilendirilmemiş olmalı (idempotency koruması). | `OrderCreated` |
| `StartOrderProcessing` | Sipariş `PAYMENT_CONFIRMED` durumunda olmalı. | `OrderProcessingStarted` |
| `ShipOrder` | Sipariş `PROCESSING` durumunda olmalı. | `OrderShipped` |
| `ConfirmOrderDelivery` | Sipariş `SHIPPED` durumunda olmalı. | `OrderDelivered` |
| `CancelOrder` | Sipariş `PAYMENT_CONFIRMED` veya `PROCESSING` durumunda olmalı. İptal nedeni belirtilmiş olmalı. | `OrderCancelled` |

#### Durum

```
Order {
  OrderId
  OrderNumber           (okunabilir, örn. TN-2025-00142)
  CustomerId
  Status                (PAYMENT_CONFIRMED | PROCESSING | SHIPPED | DELIVERED | CANCELLED)
  Items[] {
    OrderItemId
    VariantId
    ProductName          (anlık görüntü)
    VariantAttributes    (anlık görüntü)
    UnitPrice            (anlık görüntü)
    Quantity
    LineTotal
  }
  DeliveryAddress        (anlık görüntü)
  AppliedPromotions[] {
    PromotionId
    PromotionName        (anlık görüntü)
    DiscountAmount
  }
  Subtotal
  TotalDiscount
  GrandTotal
  PaymentReference
  TrackingNumber         (isteğe bağlı; ShipOrder ile atanır)
  Carrier                (isteğe bağlı; ShipOrder ile atanır)
  CancellationReason     (isteğe bağlı; CancelOrder ile atanır)
  CancelledByAdminId     (isteğe bağlı; CancelOrder ile atanır)
  CreatedAt
  UpdatedAt
}
```

#### Sipariş Durumu Yaşam Döngüsü

```
PAYMENT_CONFIRMED → PROCESSING → SHIPPED → DELIVERED
        ↘               ↘
      CANCELLED       CANCELLED
```

#### Notlar

- Tüm ürün verisi (ad, özellikler, fiyat), sipariş oluşturulurken anlık görüntü olarak kaydedilir. Ürün kataloğundaki sonraki değişiklikler mevcut sipariş kayıtlarını geriye dönük olarak etkilemez.
- `HandlePaymentSuccess` üzerindeki idempotency koruması, İyzico'nun ödeme callback'ini birden fazla kez göndermesi durumunda mükerrer sipariş oluşturulmasını önler (ödeme entegrasyonlarında bilinen bir kenar durum).
- `TrackingNumber` ve `Carrier`, ayrı bir takip aggregate'inde değil, `Order` aggregate'i içinde `ShipOrder` işlendiğinde atanır.
- Sipariş `SHIPPED` veya `DELIVERED` durumuna geçtikten sonra `CancelOrder` komutu aggregate tarafından reddedilir.

---

### 8. Payment

**Alan:** Ödeme

`Payment` aggregate'i, bir ödeme oturumuyla ilişkili tek bir ödeme girişiminin yaşam döngüsünü takip eder. İyzico ile iletişim kurulan ve İyzico'dan alınan bilgilerin kaydıdır — ödeme kararı vermez, kaydeder.

**Aggregate Root:** `Payment`

**Kimlik:** `PaymentId` *(`CartId` ile ve başarı durumunda `OrderId` ile ilişkili)*

#### Komutlar ve İş Kuralları

| Komut | Uygulanan İş Kuralı | Üretilen Event(ler) |
|---|---|---|
| `ConfirmCheckout` *(ödeme başlatma kısmı)* | Sepet onaylanmış ödeme durumunda olmalı. Bu sepet için ödeme kaydı terminal bir durumda olmamalı (mükerrer ödeme girişimini önler). | `PaymentInitiated` |
| `HandlePaymentSuccess` | Ödeme `PENDING` durumunda olmalı. İşlem referansı daha önce kaydedilmemiş olmalı (idempotency). | `PaymentConfirmed` |
| `HandlePaymentFailure` | Ödeme `PENDING` durumunda olmalı. | `PaymentFailed` |
| `CompleteRefund` | Bu ödeme için iade başlatılmış olmalı. İade `COMPLETED` durumunda olmamalı. | `RefundCompleted` |

#### Durum

```
Payment {
  PaymentId
  CartId
  OrderId              (sipariş oluşturulduktan sonra atanır; öncesinde null)
  Status               (PENDING | CONFIRMED | FAILED | REFUND_INITIATED | REFUND_COMPLETED)
  TotalAmount
  GatewayReference     (İyzico işlem kimliği)
  GatewayErrorCode     (başarısızlıkta atanır)
  GatewayErrorReason   (başarısızlıkta atanır)
  RefundReference      (iade başlatılırken atanır)
  RefundAmount         (iade başlatılırken atanır)
  InitiatedAt
  ResolvedAt
}
```

#### Notlar

- Platform kart verisi saklamaz — `Payment` aggregate'i yalnızca gateway referanslarını ve sonuç meta verilerini kaydeder (PAY-02).
- `Payment` aggregate'i kasıtlı olarak sadedir. Karmaşık ödeme mantığı (yeniden deneme stratejileri, kısmi iade, taksit yönetimi) TechNest gereksinimleri geliştikçe gelecek aşamalara ertelenir.
- `PaymentInitiated`, müşteri İyzico'ya yönlendirildiğinde üretilir — bu noktada henüz para hareketi gerçekleşmemiştir. Ödeme girişiminin devam ettiğini bildirir.

---

### 9. Fulfillment

**Alan:** Sipariş Karşılama

`Fulfillment` aggregate'i, bir siparişin depo hazırlığından teslimatına kadar olan fiziksel yolculuğunu takip eder. Ödeme onaylandıktan sonra siparişin operasyonel durumunu temsil eder. `Order` ticari kaydın sahibiyken, `Fulfillment` operasyonel kaydın sahibidir.

> **Tasarım notu:** Bu aşamada `Fulfillment` durumu, sadelik amacıyla `Order` aggregate'ine gömülüdür (durum alanı, takip numarası). Karşılama mantığı büyüdükçe — örneğin kısmi sevkiyat, çoklu kargo firması, depo entegrasyonu gibi senaryolar gerekirse — gelecek bir iterasyonda bağımsız bir aggregate'e çıkarılmalıdır.

**Aggregate Root:** `Order` *(karşılama komutları bu aşamada Order aggregate'i üzerinde işlenir)*

**İşlenen komutlar:** `StartOrderProcessing`, `ShipOrder`, `ConfirmOrderDelivery`, `CancelOrder`

**Üretilen event'ler:** `OrderProcessingStarted`, `OrderShipped`, `OrderDelivered`, `OrderCancelled`, `RefundInitiated`, `RefundCompleted`

Tam durum ve iş kuralları için bkz. [`Order`](#7-order) aggregate'i.

---

### 10. Notification

**Alan:** Bildirimler

`Notification` aggregate'i, tek bir işlemsel e-postanın gönderimini ve sonucunu kaydeder. Denetim gereksinimini karşılamak için vardır (AT-01 ile AT-06) — gönderilen veya gönderilmeye çalışılan her e-posta izlenebilir olmalıdır.

**Aggregate Root:** `Notification`

**Kimlik:** `EmailId`

#### Komutlar ve İş Kuralları

| Komut | Uygulanan İş Kuralı | Üretilen Event(ler) |
|---|---|---|
| `DispatchEmail` | Tetikleyici bir domain event mevcut olmalı (örn. `CustomerRegistered`, `OrderCreated`). Şablon türü geçerli olmalı. Alıcı geçerli formatta bir e-posta adresi olmalı. | `EmailDispatched` veya `EmailDeliveryFailed` |

#### Durum

```
Notification {
  EmailId
  Recipient
  TemplateType          (WELCOME | PASSWORD_RESET | ORDER_CONFIRMED | ORDER_SHIPPED |
                         PAYMENT_FAILED | ORDER_CANCELLED)
  TriggeringEventType   (örn. CustomerRegistered, OrderCreated)
  TriggeringEntityId    (örn. CustomerId, OrderId)
  Status                (DISPATCHED | FAILED)
  ProviderReference     (AWS SES / Resend'den gelen harici gönderim kimliği)
  FailureReason         (başarısızlıkta atanır)
  SentAt
}
```

#### Notlar

- `Notification` aggregate'i e-postanın *ne zaman* gönderileceğine karar vermez. Bu karar, domain event'lere tepki veren event handler'lar tarafından verilir. Aggregate yalnızca girişimi ve sonucunu kaydeder.
- Yeniden deneme mantığı e-posta sağlayıcısına devredilir (örn. AWS SES dahili yeniden deneme). Platform bunu yeniden uygulamaz.
- `EmailDeliveryFailed`, gözlemlenebilirlik ve olası manuel müdahale için kaydedilir. Bu aşamada platform tarafından otomatik yeniden deneme başlatılmaz.

---

## Aggregate'ler Arası Etkileşim Haritası

Bu tablo, aggregate'lerin Domain Event'ler aracılığıyla nasıl iletişim kurduğunu gösterir. Hiçbir aggregate, başka bir aggregate'i doğrudan çağırmaz — yalnızca diğerinin yansıtılmış durumunu okur veya event'lerine tepki verir.

| Üreten Aggregate | Domain Event | Tüketen Aggregate / Handler |
|---|---|---|
| `Customer` | `CustomerRegistered` | `Notification` (hoşgeldiniz e-postası gönder) |
| `Customer` | `CustomerPasswordResetRequested` | `Notification` (sıfırlama linki e-postası gönder) |
| `Cart` | `CheckoutInitiated` | `StockReservation` (stok rezervasyonu dene) |
| `StockReservation` | `StockReserved` | `StockLedger` (rezerve toplamı artır) |
| `StockReservation` | `StockReservationFailed` | `Cart` (ödeme adımını engelle, müşteriyi bildir) |
| `StockReservation` | `StockReservationExpired` | `StockLedger` (rezerve toplamı azalt) |
| `StockReservation` | `StockReservationReleased` | `StockLedger` (rezerve toplamı azalt) |
| `Cart` | `PaymentInitiated` | `Payment` (ödeme girişimini kaydet) |
| `Payment` | `PaymentConfirmed` | `Order` (sipariş kaydı oluştur), `StockReservation` (düşüme dönüştür) |
| `Payment` | `PaymentFailed` | `StockReservation` (anında serbest bırak), `Notification` (ödeme başarısız e-postası) |
| `StockReservation` | `StockPermanentlyDeducted` | `StockLedger` (rezerveyi satışa dönüştür) |
| `Order` | `OrderCreated` | `Notification` (sipariş onay e-postası), `Promotion` (kullanım sayısını artır) |
| `Order` | `OrderShipped` | `Notification` (takip bilgisiyle kargo e-postası) |
| `Order` | `OrderCancelled` | `StockLedger` (stoğu mevvuta geri döndür), `Payment` (iade başlat), `Notification` (iptal e-postası) |
| `Payment` | `RefundCompleted` | `Order` (iade onayını kaydet) |
| `StockLedger` | `LowStockThresholdReached` | Admin panel uyarısı |
