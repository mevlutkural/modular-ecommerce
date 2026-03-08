# Müşteri Brifingi — TechNest E-Ticaret Platformu

**Kimden:** Kemal Aydın, Kurucu — TechNest  
**Kime:** Geliştirme Ekibi  
**Tarih:** 14 Ocak 2025  
**Konu:** Özel e-ticaret platformu — tam proje brifingi

---

## Biz Kimiz

Küçük bir ekibiz — şu an için sadece ben ve yarı zamanlı bir depo görevlimiz var, ancak planımız büyümek. Yaklaşık üç yıldır ağırlıklı olarak Trendyol ve Hepsiburada üzerinden tüketici elektroniği satıyorum. İşler fena gitmiyor ama bu platformlar üzerinden yapabileceklerimin sınırına dayandım ve artık gerçekten bana ait olan bir şeyler inşa etmeye başlamak istiyorum.

Markamız TechNest. Telefon, laptop, tablet, aksesuar ve çevre birimleri satıyoruz. Biz üretim yapmıyoruz. Ürünleri distribütörlerden, anlaşma sağlayabildiğimiz durumlarda ise bazen doğrudan markalardan tedarik ediyoruz. Bu pazarda kar marjları oldukça düşük, tam da bu yüzden her şeye %15-20 komisyon ödemeden doğrudan bir satış kanalı istiyorum.

---

## Neden Sadece Shopify Veya Benzeri Bir Platform Kullanmıyoruz?

Açıkçası bu çok haklı bir soru ve sizinle iletişime geçmeden önce birkaç hafta bu konuyu düşündüm. Shopify birçok insan için harika. Ancak tam olarak neye ihtiyacım olduğunu haritalandırmaya çalıştığımda sürekli duvara tosladım.

En büyük sorun: promosyonlar. Çok fazla kampanya yapıyorum — sezonluk indirimler, kategori bazlı fırsatlar, paket teklifleri... Shopify'ın kendi yerleşik indirim sistemi şaşırtıcı derecede kısıtlı. Örneğin; "Laptop kategorisinde 5.000 TL üzeri siparişlerde %10 indirim, ancak müşteri zaten bir indirim çeki kodu kullanıyorsa bu indirim uygulanmaz" gibi bir kural koymak istediğinizde, ya Shopify Functions kullanmanız gerekiyor (ki bu da kod yazmak demek) ya da her ay sizden ücret alan ve yine de tam olarak istediğiniz kuralları destekleyemeyebilen üçüncü taraf bir uygulama kurmanız gerekiyor.

İkinci konu, sepet ve stok etkileşimi etrafında dönüyor. Daha önce iki müşterinin aynı ürünün son kalan adetine aynı anda baktığı, ikisinin de sepete eklediği, birinin ödeme yaptığı ve diğerinin ödeme aşamasında kötü bir sürprizle karşılaştığı durumlar yaşadım. Bunu çok daha şık bir şekilde ele almak istiyorum — ideal olarak bir müşteri ürünü sepetine ekleyip ödeme adımına doğru ilerlediğinde stoku onun için ayırmak ve eğer satın almazsa bir süre sonra serbest bırakmak istiyorum. Bunun için hazır ve temiz bir çözüm bulamadım.

Bir de dürüst olmak gerekirse, uygulama abonelik ücretleri bir noktadan sonra çok birikiyor. SEO uygulaması, yorum uygulaması, indirim uygulaması, envanter uygulaması derken... Bir bakmışsınız geliştirmeden ettiğiniz tasarruftan daha fazlasını her ay SaaS ücreti olarak ödüyorsunuz. Bunun yerine bir kez yatırım yapıp platformun sahibi olmayı tercih ederim.

---

## Neye İhtiyacımız Var?

### Mağaza (Vitrin)

Müşteriye dönük kısım oldukça standart:

- Kategoriler, arama ve filtreler (marka, fiyat, özellik) içeren ürün kataloğu
- Ürün detay sayfaları — iyi görsel desteği, özellik tablosu, varyant seçimi, stok durumu
- Sepet ve ödeme (checkout) akışı
- Hesap oluşturma, sipariş geçmişi, adres yönetimi
- E-posta bildirimleri — sipariş onayı, kargo güncellemeleri

Ürün varyantları önemli. Bir telefon listelendiğinde, her biri kendi stok miktarına ve potansiyel olarak kendi fiyatına sahip olan farklı depolama alanı boyutlarını ve renklerini yönetebilmesi gerekir. Bazı platformlar bunu sonradan akla gelmiş bir eklenti gibi ele alıyor. Öyle olmamalı.

Yönetici (Admin) tarafında:

- Ürün ve kategori yönetimi
- Sipariş yönetimi — görüntüleme, filtreleme, durum güncelleme
- Stok azalma uyarılarıyla temel envanter takibi
- Temel satış gösterge panosu (dashboard) — gelir, sipariş hacmi gibi veriler

### Promosyonlar

Doğru yapılmasını istediğim temel şeylerden biri bu. Sistem şunları desteklemeli:

- Yüzdelik ve sabit tutarlı (TL) indirimler
- Sepet seviyesinde (sipariş toplamı eşikleri) ve ürün/kategori seviyesinde indirimler
- Kategoriye özel promosyonlar
- Belirli müşteriler için tek kullanımlık olanlar da dahil olmak üzere kupon kodları
- Promosyon birleştirme kuralları — bazı promosyonlar birleştirilebilir, diğerleri birleştirilemez. Bunu sistemi zorlamadan kendim yapılandırabilmek istiyorum.
- Başlangıç ve bitiş tarihleri olan zamanlanmış promosyonlar

İstemediğim şey ise yeni bir kampanya türü çıktığında bir yazılımcıya ihtiyaç duymak. Gerekli altyapı orada olmalı ve ben sadece onu yapılandırabilmeliyim.

### Stok Rezervasyonu

Bir müşteri sepetine ürün ekleyip ödeme adımına geçtiğinde, o stokun makul bir süre — belki 10-15 dakika — onun için ayırt edilmesini istiyorum. Eğer alışverişi tamamlamazsa ürün tekrar satışa sunulabilmeli. Bu, daha önce bahsettiğim "iki kişi, son ürün" durumunu önleyecektir.

Bunun karmaşıklığı artırdığını anlıyorum ve bunda bir sorun görmüyorum. Ancak sürekli şikayet almaktansa bunu baştan doğru şekilde yapmayı tercih ederim.

### Sipariş Bildirimleri ve Denetim Kaydı (Audit Trail)

Sistemde gerçekleşen her önemli şeyin — sipariş verildiğinde, ödeme onaylandığında, ürün kargoya verildiğinde, bir yönetici sipariş durumunu değiştirdiğinde — kaydedilmesini istiyorum. Bunun sadece sipariş kaydının içinde değil, geriye dönüp bakabileceğim düzgün bir olay geçmişi şeklinde olmasını istiyorum.

Müşteriler için: Sadece doğru anlarda gelen otomatik e-postalar (sipariş onaylandı, ödeme başarısız, kargoya verildi, teslim edildi). Spam değil, sadece gerçekten önemli olanlar.

Bizim (admin) için: Bir siparişe ne olduğunun tam tarihçesini görebilmek istiyorum. Bir müşteri "siparişimin durumu değişmiş ama bana e-posta gelmedi" diye bizimle iletişime geçerse, zaman çizelgesine bakıp neyin, ne zaman ve nasıl olduğunu tam olarak anlayabilmeliyim.

---

## Neler İstemiyoruz?

- Yerel mobil uygulamalar — şimdilik mobil uyumlu (responsive) web sitesi yeterli.
- Çok satıcılı / pazar yeri özellikleri — sadece biz satış yapıyoruz.
- B2B veya toptan fiyatlandırma.
- Çok karmaşık analitik yapıları — bu aşama için temel raporlama yeterlidir.
- Sosyal ağlarla giriş (Social login) — şimdilik e-posta/şifre yeterli.

---

## Kullanıcılar

**Müşteriler** — Teknoloji alıcıları. Ürün özelliklerini kıyaslayacak, açıklamaları dikkatle okuyacaklar ve muhtemelen mobilde gezinecekler. Yavaş veya kafa karıştırıcı bir deneyime tahammülleri olmayacaktır.

**Yöneticiler (Biz)** — Temelde ben ve ileride depo/sipariş karşılama için işe alacağımız birisi. Çok teknik insanlar değiliz. Yönetici paneli (admin interface) basit şeyleri nasıl yapacağımı Google’da aratmak zorunda bırakmayacak kadar açık ve net olmalı.

---

## Teknik Notlar

Bu konuda çok katı fikirlerim yok ve çoğunlukla ekibin yönlendirmesine bırakacağım. Yazılımcı bir arkadaşım bir özellik eklemek istediğimizde anında çöken bir şey inşa etmediğimizden emin olmamı söyledi — buna "büyük bir çamur yumağı (big ball of mud)" dedi. Teknik terimi bilmiyorum ama sonucunu gördüm ve öyle bir şey istemiyorum.

**Performans önemlidir.** Site hızlı hissettirmeli. Kendi adıma yavaş e-ticaret sitelerinden alışveriş yapmayı bıraktığım çok olmuştur ve bunun kendi müşterilerimin başına gelmesini kabul edemem.

**Ödemeler.** Türkiye pazarı için en pratik seçenek olduğu için birincil ödeme altyapısı olarak İyzico'yu entegre etmek istiyorum. Artık Stripe'ın da Türk satıcıları desteklediğini biliyorum, ikincil bir seçenek ya da gelecekteki küresel büyüme için değerlendirilebilir. Her iki durumda da baştan itibaren gerçek çalışan bir sistem olmalı, test amaçlı konulmuş sahte ödeme butonları istemiyorum.

**Barındırma (Hosting).** Bu konuda pek bir şey bilmiyorum. Şu anki ölçeğimizde çok pahalıya mal olmayan ama satışlar arttığında ölçeklenebilen güvenilir bir şey istiyorum. Ekibin tavsiyesine uyacağım.

---

## Zaman Çizelgesi ve Bütçe

Bu işi kendi cebimden finanse ediyorum, bu yüzden gerçekçi olmalıyım. Hedefim 4-6 ay içinde canlıya çıkmak. Kapsama göre bunun zorlayıcı olabileceğini biliyorum. Kapsamı daraltmak gerekirse, vitrine veya promosyon sistemine dokunmaktansa yönetici paneli özelliklerinden kısmayı tercih ederim.

Öncelik sırası:
1. Temel mağaza (Vitrin) — kesinlikle olmalı
2. Promosyon motoru — kesinlikle olmalı, rekabet etme şeklimizin kilit bir parçası
3. Stok rezervasyonu — önemli, ancak gerekirse lansmanda daha basit bir yaklaşımla da çıkılabilir
4. Temel seviye ötesindeki yönetici özellikleri — lansman sonrası eklenebilir

---

## Uzun Vadeli Gelecek

Sadece tek seferlik bir iş peşinde değilim. İşler yolunda giderse büyüdükçe aynı ekiple çalışmaya devam etmek isterim. İlerisi için aklımda olanlar: mobil uygulama, lojistik entegrasyonları, ihtiyaç doğacak kadar büyürsek bir ERP bağlantısı.

Bunlardan herhangi birinin üzerinden geçmek için bir görüşme ayarlayabiliriz. Genelde akşamları müsait oluyorum.

— Kemal
