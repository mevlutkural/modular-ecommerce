# Domain Event Kataloğu

**Belge:** `docs/03-domain-design/event-storming/tr/01-domain-events.md`  
**Aşama:** Stratejik Tasarım  
**İlgili Gereksinimler:** BRD (Tüm Bölümler)

---

## Genel Bakış

Bu katalog, TechNest platformunun Event Storming oturumu sırasında belirlenen değişmez iş gerçeklerini (Domain Event) tanımlar. Bu olaylar, Bounded Context (Sınırlı Bağlam) sınırları arası iletişimi ve nihai tutarlılığı (eventual consistency) tetikleyen her bir durum değişikliğini temsil eder.

## Event Kataloğu

### 1. Identity & Customer Profile

| Domain Event | Emitter | Tetikleyici / Koşul | Payload |
|---|---|---|---|
| `CustomerRegistered` | Identity | Yeni müşteri hesabı başarıyla açıldı. | `CustomerId`, `EmailAddress` |
| `CustomerPasswordResetRequested` | Identity | Geçerli şifre sıfırlama bağlantısı oluşturuldu. | `CustomerId`, `ResetTokenUrl` |
| `CustomerPasswordReset` | Identity | Şifre bağlantı aracılığıyla başarıyla güncellendi. | `CustomerId` |
| `CustomerAddressAdded` | Identity | Yeni kargo/fatura adresi kaydedildi. | `CustomerId`, `AddressId` |

### 2. Catalog & Inventory

| Domain Event | Emitter | Tetikleyici / Koşul | Payload |
|---|---|---|---|
| `ProductAddedToCatalog` | Catalog | Yeni ürün ilanı aktif hale geldi. | `ProductId`, `BasePrice` |
| `ProductPriceUpdated` | Catalog | Bir ürünün taban fiyatı değişti. | `ProductId`, `OldPrice`, `NewPrice` |
| `ProductStockAdjusted` | Inventory | Yönetici fiziksel stoğu manuel düzenledi. | `VariantId`, `Delta`, `Reason` |
| `LowStockThresholdReached` | Inventory | Mevcut stok önceden tanımlı minimum seviyenin altına düştü. | `VariantId`, `RemainingQuantity` |

### 3. Promotions & Marketing

| Domain Event | Emitter | Tetikleyici / Koşul | Payload |
|---|---|---|---|
| `PromotionCreated` | Promotions | Yönetici tarafından yeni indirim kuralı tanımlandı. | `PromotionId`, `Type`, `Scope` |
| `PromotionActivated` | Promotions | Başlangıç tarihi geldi veya aktif edildi. | `PromotionId` |
| `PromotionDeactivated` | Promotions | Bitiş tarihi geçti veya deaktif edildi. | `PromotionId` |

### 4. Cart & Checkout

| Domain Event | Emitter | Tetikleyici / Koşul | Payload |
|---|---|---|---|
| `CheckoutInitiated` | Cart | Müşteri geçerli ürünlerle ödeme sayfasına geçti. | `CartId`, `CustomerId` |
| `StockReserved` | Inventory | Talep edilen miktarlar ödeme için bloke edildi. | `SessionId`, `VariantIds`, `Quantities`, `ExpiryTime` |
| `StockReservationFailed` | Inventory | Bir veya daha fazla ürün için yetersiz stok saptandı. | `SessionId`, `FailedVariantIds` |
| `StockReservationExpired` | Inventory | Ödeme yapılmadan rezerve süresi doldu. | `SessionId`, `VariantIds` |
| `StockReservationReleased` | Inventory | Müşteri ödemeden döndü veya ödeme başarısız. | `SessionId`, `Reason` |

### 5. Order & Payment

| Domain Event | Emitter | Tetikleyici / Koşul | Payload |
|---|---|---|---|
| `PromotionAppliedToCart` | Promotions | Kurallar değerlendirildi ve indirim sepete yansıtıldı. | `CartId`, `PromotionIds`, `DiscountAmount` |
| `PaymentInitiated` | Payment | Müşteri ödeme ağ geçidine yönlendirildi (sepet bazlı). | `CartId`, `PaymentReference`, `TotalAmount` |
| `PaymentConfirmed` | Payment | Ağ geçidi başarılı işlemi onayladı. | `CartId`, `TransactionId`, `AmountPaid` |
| `PaymentFailed` | Payment | Ağ geçidi işlemi reddetti veya başarısız buldu. | `CartId`, `ErrorCode`, `Reason` |
| `OrderCreated` | Sales | Sipariş kaydı resmi olarak ödeme onayından sonra oluşturuldu. | `OrderId`, `CustomerId`, `TotalAmount` |
| `StockPermanentlyDeducted`| Inventory | Geçici rezervasyon fiziksel düşüme çevrildi. | `OrderId`, `VariantIds`, `Quantities` |

### 6. Fulfillment

| Domain Event | Emitter | Tetikleyici / Koşul | Payload |
|---|---|---|---|
| `OrderProcessingStarted` | Fulfillment | Yönetici sipariş hazırlığına başladı. | `OrderId` |
| `OrderShipped` | Fulfillment | Sipariş kargo firmasına teslim edildi. | `OrderId`, `TrackingNumber`, `Carrier` |
| `OrderDelivered` | Fulfillment | Kargo firması teslimatı onayladı. | `OrderId`, `DeliveryDate` |
| `OrderCancelled` | Sales/Fulfillment | Sipariş yönetici tarafından sonlandırıldı. | `OrderId`, `AdminId`, `Reason` |
| `RefundInitiated` | Payment | Ağ geçidi aracılığıyla nakit iadesi talep edildi. | `OrderId`, `RefundReference`, `Amount` |
| `RefundCompleted` | Payment | Ağ geçidi ödemenin iade edildiğini onayladı. | `OrderId`, `TransactionId` |

### 7. Notifications

| Domain Event | Emitter | Tetikleyici / Koşul | Payload |
|---|---|---|---|
| `EmailDispatched` | Notifications | E-posta, posta sağlayıcısına başarıyla iletildi. | `EmailId`, `Recipient`, `TemplateType` |
| `EmailDeliveryFailed` | Notifications | Posta sağlayıcısı e-postayı reddetti. | `EmailId`, `Recipient`, `Reason` |
