# Context Map

**Döküman:** `docs/03-domain-design/event-storming/tr/05-context-map.md`
**Aşama:** Stratejik Tasarım
**İlgili Dökümanlar:** [`04-bounded-contexts.md`](./04-bounded-contexts.md)
**İlgili Gereksinimler:** NFR-09, NFR-10, NFR-11

---

## Genel Bakış

Context Map, TechNest platformundaki tüm Bounded Context'ler arasındaki kesin ilişkileri tanımlar. Bounded Contexts dökümanı her bağlamın neye sahip olduğunu açıklarken, bu döküman onların birbiriyle nasıl ilişki kurduğunu açıklar — bağımlılığın yönü, entegrasyon deseni ve her sınır boyunca iletişimi yöneten kurallar.

Bu ilişkileri anlamak, implementasyona başlamadan önce zorunludur. Bir sınırdaki yanlış entegrasyon deseni, ileride düzeltilmesi en pahalı hatalardan biridir: modüller arasında sıkı bağ üretir, testi zorlaştırır ve her bağlamın bağımsız gelişmesini engeller.

---

## Kullanılan İlişki Desenleri

TechNest context map'inde dört DDD ilişki deseni kullanılmaktadır:

| Desen | Sembol | Anlamı |
|---|---|---|
| **Published Language** | `PL` | Yukarı yönlü bağlam, aşağı yönlü bağlamların bağımlı olduğu stabil, versiyonlanmış bir event veya API sözleşmesi yayımlar. Yukarı yönlü bağlam kimin dinlediğini bilmez. |
| **Open Host Service** | `OHS` | Yukarı yönlü bağlam, diğer bağlamların senkron olarak çağırabileceği iyi tanımlanmış bir sorgu arayüzü sunar. Sağlayıcı arayüzü stabil tutmaktan sorumludur. |
| **Customer–Supplier** | `CS` | Aşağı yönlü bağlam (Customer), yukarı yönlü bağlamın (Supplier) yol haritasını etkileyebilir. İki taraf arasında açık bir iş birliği vardır. |
| **Conformist** | `CF` | Aşağı yönlü bağlam, yukarı yönlü modeli herhangi bir çeviri yapmadan olduğu gibi kabul eder. Burada TechNest'in İyzico entegrasyonunda kullanılır. |

---

## Context İlişki Haritası

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                                                                               │
│   [IDENTITY] ──PL──► [NOTIFICATIONS]                                          │
│       │                                                                       │
│       ├──OHS──► [COMMERCE]                                                    │
│                     │                                                         │
│                     ├──CS──► [PROMOTIONS] ──PL──► [COMMERCE]                 │
│                     │                                                         │
│                     ├──PL──► [INVENTORY] ──PL──► [COMMERCE]                  │
│                     │                                                         │
│                     └──PL──► [NOTIFICATIONS]                                  │
│                                                                               │
│   [CATALOG] ──PL──► [INVENTORY]                                               │
│       │                                                                       │
│       └──OHS──► [COMMERCE]                                                    │
│                                                                               │
│   [INVENTORY] ──PL──► [NOTIFICATIONS]                                         │
│                                                                               │
│   [COMMERCE] ──CF──► [İYZİCO (Harici)]                                        │
│                                                                               │
└───────────────────────────────────────────────────────────────────────────────┘
```

---

## İlişki Tanımları

### 1. Identity → Notifications `[PL]`

**Desen:** Published Language
**Yön:** Identity (yukarı yönlü) → Notifications (aşağı yönlü)
**Mekanizma:** Süreç içi event bus üzerinden Domain Event'ler

| Event | Tetikleyen | E-posta Şablonu |
|---|---|---|
| `CustomerRegistered` | Başarılı kayıt | `WELCOME` |
| `CustomerPasswordResetRequested` | Geçerli sıfırlama isteği | `PASSWORD_RESET` |

**Sözleşme kuralları:**
- Identity bu event'leri Notifications'ın varlığından habersiz olarak yayımlar.
- Event payload'ı her zaman `CustomerId` ve `EmailAddress` içermelidir. Alan eklemek kırıcı değildir; mevcut alanları kaldırmak veya yeniden adlandırmak kırıcı bir değişiklik sayılır ve versiyon güncellemesi gerektirir.
- Notifications asla Identity'ye geri çağrı yapmamalıdır. E-posta gövdesinde müşterinin adına ihtiyaç duyuluyorsa, ikincil bir sorgu ile çekilmek yerine event payload'ına dahil edilmelidir.

**Hata davranışı:**
- E-posta gönderimi başarısız olursa (`EmailDeliveryFailed`) bu kaydedilir ve asıl domain işlemi (kayıt, sıfırlama) geri alınmaz. Müşteri zaten kayıt edilmiştir; bildirim en iyi çaba temelinde çalışır.

---

### 2. Identity → Commerce `[OHS]`

**Desen:** Open Host Service
**Yön:** Identity (yukarı yönlü) → Commerce (aşağı yönlü)
**Mekanizma:** Süreç içi senkron sorgu

**Identity tarafından sunulan sorgular:**

| Sorgu | Amaç | Ne Zaman Çağrılır |
|---|---|---|
| `GetAuthenticatedCustomer(token)` | Oturum geçerliliğini doğrula ve `CustomerId` döndür | Her ödeme isteğinde |
| `GetCustomerAddresses(customerId)` | Önceden doldurmak için kayıtlı adresleri döndür | Ödeme adres seçim adımında |

**Sözleşme kuralları:**
- Identity bu sorgu arayüzlerini stabil tutmaktan sorumludur. Parametre veya dönüş tipi değişiklikleri geriye dönük uyumlu olmalı ya da versiyonlanmalıdır.
- Commerce, Identity'nin veritabanı tablolarına asla doğrudan erişmez. Tüm okumalar yayımlanan sorgu arayüzü üzerinden gerçekleşir.
- Bunlar modüler monolith içindeki süreç içi çağrılardır — HTTP çağrıları değil. Gecikme ihmal edilebilir düzeydedir.

---

### 3. Catalog → Inventory `[PL]`

**Desen:** Published Language
**Yön:** Catalog (yukarı yönlü) → Inventory (aşağı yönlü)
**Mekanizma:** Süreç içi event bus üzerinden Domain Event'ler

| Event | Tetikleyen | Inventory'deki Aksiyon |
|---|---|---|
| `ProductAddedToCatalog` | Yeni ürün aktif edildi | Her yeni varyant için `TotalPhysical = 0` ile `StockLedger` kaydı oluştur |

**Sözleşme kuralları:**
- Inventory bu event'e tepki vererek her aktif katalog varyantı için her zaman bir `StockLedger` girişinin bulunduğunu garanti eder.
- `ProductAddedToCatalog`, zaten `StockLedger` kaydı olan bir `VariantId` için alınırsa (örn. yeniden aktivasyon), mevcut kayıt korunur — mükerrer oluşturulmaz.
- Catalog, Inventory'nin bu event'i dinlediğinden habersizdir.

---

### 4. Catalog → Commerce `[OHS]`

**Desen:** Open Host Service
**Yön:** Catalog (yukarı yönlü) → Commerce (aşağı yönlü)
**Mekanizma:** Süreç içi senkron sorgu

**Catalog tarafından sunulan sorgular:**

| Sorgu | Amaç | Ne Zaman Çağrılır |
|---|---|---|
| `GetActiveVariant(variantId)` | Varyant adı, özellikleri, fiyatı, aktiflik durumunu döndür | Sepete ürün eklenirken |
| `GetProductSnapshot(variantId)` | Sipariş oluşturma için değiştirilemez anlık görüntü verisi döndür | Ödeme onayı sonrası sipariş oluşturulurken |
| `GetCategoryName(categoryId)` | Görüntüleme için kategori adını döndür | Sepet ürünü render edilirken |

**Sözleşme kuralları:**
- `GetProductSnapshot`, çağrı anındaki veriyi döndürür. Commerce bu anlık görüntüyü sipariş kaydında saklamakla sorumludur — ileride siparişin `VariantId`'sini kullanarak güncel fiyatları aramak için Catalog'u yeniden sorgulamaz.
- Catalog, bu sorgu yanıtlarının yapısını Commerce ile koordinasyon sağlamadan asla değiştirmemelidir.

---

### 5. Commerce ↔ Inventory `[PL]` (iki yönlü event'ler)

**Desen:** Published Language (her iki yönde)
**Yön:** Commerce ↔ Inventory
**Mekanizma:** Süreç içi event bus üzerinden Domain Event'ler

Bu, platformdaki en kritik entegrasyondur. Commerce ödemeyi başlatır, Inventory rezervasyonu gerçekleştirir ve sonuç Commerce'e geri akar.

**Commerce → Inventory:**

| Event | Tüketen | Inventory Aksiyonu |
|---|---|---|
| `CheckoutInitiated` | Inventory | Tüm sepet ürünleri için atomik stok rezervasyonu dene |
| `PaymentConfirmed` | Inventory | Aktif rezervasyonu kalıcı düşüme dönüştür |
| `PaymentFailed` | Inventory | Rezerve edilen stoğu anında serbest bırak |
| `OrderCancelled` | Inventory | Düşülen stoğu mevvuta geri döndür |

**Inventory → Commerce:**

| Event | Tüketen | Commerce Aksiyonu |
|---|---|---|
| `StockReserved` | Commerce | Ödeme adımının devam etmesine izin ver |
| `StockReservationFailed` | Commerce | Ödeme adımını engelle; mevcut olmayan ürünleri göster |
| `StockPermanentlyDeducted` | Commerce | Düşümün kaydedildiğini onayla; sipariş oluşturmayı tamamla |

**Sözleşme kuralları:**
- `CheckoutInitiated`, `CartId`, `CustomerId` ve `VariantId + Quantity` çiftlerinin tam listesini içermelidir. Inventory, sepet içeriği için Commerce'i sorgulamaz.
- Rezervasyon ya hepsi ya hiçbiri şeklindedir. Inventory asla yalnızca bazı ürünleri kapsayan kısmi bir `StockReserved` event'i göndermez.
- `StockPermanentlyDeducted`, `PaymentConfirmed` işlemiyle aynı mantıksal işlem içinde üretilmelidir. Commerce bu event'in yokluğunu hata durumu olarak değerlendirir.
- `PaymentConfirmed` callback'i rezervasyon süresi dolduktan sonra gelirse Inventory düşümü reddeder ve `StockReservationExpired` yayımlar. Commerce ardından iade başlatır.

---

### 6. Commerce → Promotions `[CS]`

**Desen:** Customer–Supplier
**Yön:** Commerce (Customer) → Promotions (Supplier)
**Mekanizma:** Domain Event'ler (asenkron değerlendirme tetikleyicisi) + senkron sorgu (sonuçlar için)

Commerce, promosyon değerlendirmesinin ne zaman gerçekleşeceğini yönetir. Promotions sonuçla yanıt verir.

**Commerce → Promotions:**

| Tetikleyici | Mekanizma | Promotions Aksiyonu |
|---|---|---|
| `CheckoutInitiated` event'i | Event bus | Bu sepet için tüm uygun promosyonları değerlendir |
| `ApplyVoucherCode` komutu | Promotions uygulama servisine doğrudan çağrı | Kuponu doğrula ve birleştirme kurallarıyla değerlendir |

**Promotions → Commerce:**

| Event | Tüketen | Commerce Aksiyonu |
|---|---|---|
| `PromotionAppliedToCart` | Commerce | Ödeme oturumunu indirim dökümüyle güncelle |

**Commerce → Promotions (sipariş oluşturulurken):**

| Event | Tüketen | Promotions Aksiyonu |
|---|---|---|
| `OrderCreated` | Promotions | Uygulanan her promosyonda `TotalUsesCount` değerini artır |

**Sözleşme kuralları:**
- Commerce, değerlendirme tetikleyicisinde tam sepet durumunu sağlar: `CartId`, `CustomerId`, `VariantId`, `CategoryId` ve `UnitPrice` ile ürün listesi, toplam sepet tutarı ve girilen kupon kodu (varsa). Promotions, sepet detayları için Commerce'i sorgulamaz.
- Promotions, değerlendirme başına tek bir `PromotionAppliedToCart` event'i döndürür ve kalem kalem döküm içerir. Commerce kendi indirim mantığını uygulamaz — hangi indirimlerin uygulanacağının tek otoritesi Promotions'tır.
- Bu ilişkide Customer konumundaki Commerce, Promotions'ın tasarımını etkileyebilecek gereksinimler ortaya koyabilir (örn. "yeni bir indirim türüne ihtiyacımız var"). Bu iş birliği açık ve planlanmış bir koordinasyondur — beklenmedik bir bağımlılık değil.

---

### 7. Commerce → Notifications `[PL]`

**Desen:** Published Language
**Yön:** Commerce (yukarı yönlü) → Notifications (aşağı yönlü)
**Mekanizma:** Süreç içi event bus üzerinden Domain Event'ler

| Event | Tetiklenen E-posta | Gerekli Payload |
|---|---|---|
| `OrderCreated` | Sipariş onayı (NT-03) | `OrderId`, `CustomerId`, `OrderNumber`, ürünler, toplamlar, `DeliveryAddress` |
| `OrderShipped` | Kargo bildirimi (NT-04) | `OrderId`, `CustomerId`, `TrackingNumber` (isteğe bağlı), `Carrier` (isteğe bağlı) |
| `PaymentFailed` | Ödeme başarısız (NT-05) | `CartId`, `CustomerId`, `ErrorReason` |
| `OrderCancelled` | İptal bildirimi (NT-06) | `OrderId`, `CustomerId`, `CancellationReason` |

**Sözleşme kuralları:**
- E-postayı render etmek için gereken tüm veriler event payload'ında bulunmalıdır. Notifications, ek sipariş verisi için Commerce'i sorgulamaz.
- Commerce, Notifications'ın hangi e-posta şablonunu kullanacağını veya e-postanın nasıl görüneceğini bilmez. Event türünden şablona eşleme, Notifications'ın iç konusudur.
- E-posta gönderiminin başarısız olması (`EmailDeliveryFailed`), sipariş kaydını etkilemez ve Commerce'de geri alma tetiklemez.

---

### 8. Inventory → Notifications `[PL]`

**Desen:** Published Language
**Yön:** Inventory (yukarı yönlü) → Notifications (aşağı yönlü)
**Mekanizma:** Süreç içi event bus üzerinden Domain Event'ler

| Event | Bildirim | Gerekli Payload |
|---|---|---|
| `LowStockThresholdReached` | Admin panel uyarısı | `VariantId`, `RemainingQuantity`, `Threshold` |

**Sözleşme kuralları:**
- Bu aşamada düşük stok uyarısı, ayrı bir e-posta yerine admin panel göstergesi olarak sunulur. `Notification` aggregate'i gönderimi kaydeder; admin paneli bu günlükten okur.
- İleride e-posta uyarısı eklenirse şablonu Notifications yönetir — Inventory değişmeden kalır.

---

### 9. Commerce → İyzico `[CF]`

**Desen:** Conformist
**Yön:** Commerce (aşağı yönlü, conformist) → İyzico (yukarı yönlü, harici)
**Mekanizma:** HTTP yönlendirme (ödeme başlatma) + imzalı webhook callback (ödeme sonucu)

TechNest, İyzico'nun entegrasyon modeline tamamen uyum sağlar. Çeviri katmanı yoktur — Commerce bu sınır için doğrudan İyzico'nun dilini kullanır.

**Giden (Commerce → İyzico):**

| Aksiyon | Mekanizma | Gönderilen Veri |
|---|---|---|
| Ödeme başlat | İyzico ödeme sayfasına HTTP yönlendirme | `CartId` referans olarak, `TotalAmount`, sepet ürünleri, müşteri fatura bilgisi |

**Gelen (İyzico → Commerce):**

| Callback | Commerce Komutu | Commerce Aksiyonu |
|---|---|---|
| Ödeme başarılı webhook | `HandlePaymentSuccess` | İmzayı doğrula, `CartId` referansını onayla, sipariş oluştur |
| Ödeme başarısız webhook | `HandlePaymentFailure` | Stoğu serbest bırak, müşteriyi bildir |
| Ödeme iptal webhook | `HandlePaymentCancellation` | Stoğu serbest bırak, müşteriyi sepete döndür |

**Sözleşme kuralları:**
- Gelen tüm İyzico webhook'ları, herhangi bir durum değişikliği yapılmadan önce İyzico'nun HMAC imzasıyla doğrulanmalıdır. Doğrulanamayan callback sessizce görmezden gelinir.
- Ödeme referansı olarak gönderilen `CartId` saklanmalı ve callback'te eşleştirilmelidir. Callback'teki `CartId` aktif bir ödeme oturumuyla eşleşmiyorsa callback reddedilir.
- TechNest bu akışın hiçbir aşamasında ham kart verisi saklamaz (PAY-02, NFR-07).
- Buradaki Conformist deseni bilinçli bir takas kararıdır: İyzico'nun modelini doğrudan benimsemek entegrasyon süresinden önemli ölçüde tasarruf sağlar, ancak tek bir sağlayıcıya daha sıkı bağlanma pahasına gelir. Ödeme sağlayıcısı değişirse, yalnızca Commerce'in altyapı katmanı güncellenmesi gerekir — başka hiçbir bağlam etkilenmez.

---

## Süreç İçi Event Bus Sözleşmesi

Bounded Context'ler arasındaki tüm domain event iletişimi, tek bir süreç içi event bus üzerinden akar. Aşağıdaki kurallar platform genelinde geçerlidir:

### Yayımlama Kuralları

1. Event'ler, üretici aggregate'in durumu kalıcı hale getirilmesinden **sonra** yayımlanır — önce değil.
2. Event'ler bir kez yayımlandıktan sonra değiştirilemez. Bir bağlam, yayımlanmış bir event'i geri alamaz veya değiştiremez.
3. Her event, idempotency ve sıralama için bir `occurredOn` zaman damgası (UTC) ve bir `eventId` (UUID) taşır.

### Tüketme Kuralları

1. Her event handler **idempotent** olmalıdır — aynı event'i iki kez işlemek, bir kez işlemekle aynı sonucu üretmelidir.
2. Event handler'lar, yayımlayan işlemi geri alan exception fırlatmamalıdır. Handler hataları bağımsız olarak kaydedilir ve yönetilir.
3. Bir handler, yayımlayan bağlama senkron olarak geri çağrı yapamaz. Tüm tepkiler kurala göre asenkrondur.

### Event Versiyonlama

Bu aşamada platform tek bir dağıtılabilir birim olarak çalışır — tüm modüller aynı event tipi tanımlarını paylaşır. Event sözleşmelerinde kırıcı değişiklikler (alan kaldırma veya yeniden adlandırma) aynı commit içinde tüm handler'ların güncellenmesini gerektirir. Kırıcı olmayan eklemeler (yeni isteğe bağlı alanlar) handler güncellemesi gerektirmez.

---

## Sorgu Arayüzü Sözleşmesi

Senkron bağlamlar arası okumalar (Open Host Service deseni) şu kurallara uyar:

1. Sorgular **salt okunurdur**. Bir sorgu çağrısı, sorgulanan bağlamda asla durum değişikliği tetiklemez.
2. Sorgu arayüzleri, modülün genel API'sında açık arayüzler olarak tanımlanır — doğrudan repository çağrıları olarak değil.
3. Sorgular domain aggregate'leri değil **DTO** (Data Transfer Object) döndürür. Çağıran, mutasyona uğratılabilecek canlı bir aggregate örneği asla almaz.
4. Sorgulanan kayıt yoksa, arayüz tiplendirilmiş `null` veya `Optional` döndürür — "bulunamadı" sonucu için asla exception fırlatmaz.

---

## Anti-Corruption Layer Değerlendirmeleri

Modüler monolith içinde, event'ler ve sorgular paylaşılan tip tanımları kullandığından dahili bounded context'ler arasında açık anti-corruption layer (ACL) gerekmez. Ancak bir harici ACL zorunludur:

### İyzico ACL (Commerce altyapı katmanında)

Commerce'in altyapı katmanındaki `PaymentGatewayAdapter` sınıfı, İyzico'nun veri modeli ile TechNest'in dahili alan modeli arasındaki tek çeviri noktası olarak görev yapar.

**Sorumluluklar:**
- TechNest'in `CartId` ve `TotalAmount` değerlerini İyzico'nun gerektirdiği istek biçimine çevirmek
- Callback'leri uygulama katmanına iletmeden önce İyzico webhook HMAC imzalarını doğrulamak
- İyzico'nun callback payload'ını `HandlePaymentSuccess`, `HandlePaymentFailure` veya `HandlePaymentCancellation` komutlarına eşlemek
- İyzico'nun API sözleşmesindeki gelecekteki değişiklikleri Commerce'in alan katmanına sızdırmadan emmek

Bu ACL, TechNest'in dışarıdan uyum sağladığı İyzico terminolojisi ve veri yapılarının dahili alan modeline asla sızmamasını garanti eder.
