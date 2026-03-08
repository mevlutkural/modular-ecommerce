# İş Süreçleri — Genel Bakış

**Dizin:** `docs/02-business-processes/`  
**Son Güncelleme:** Mart 2025  
**İlgili Belgeler:** [İş Gereksinimleri Belgesi](../../01-requirements/business-requirements-tr.md)

---

## Amaç

Bu dizin, TechNest e-ticaret platformunun temel iş süreçlerini BPMN tarzı akış diyagramları olarak belgelemektedir. İki temel amacı vardır:

1. **İş mantığını doğrulamak** — kodlama (implementasyon) başlamadan önce iş akışları modellenerek, sistemin paydaşların beklentilerine uygun davranacağından emin olunması sağlanır.
2. **Geliştirme sürecinde referans oluşturmak** — geliştiriciler ve mimarlar, teknik kararlar alırken bu akışlara başvurabilir.

Diyagramlar, taşınabilirlik açısından doğrudan Markdown dosyalarına gömülü [Mermaid](https://mermaid.js.org/) kullanılarak oluşturulmuştur.

---

## Belgelenen Süreçler

| Belge | Açıklama | İlgili BRD Bölümleri |
|---|---|---|
| [Kullanıcı Kaydı](./user-registration.md) | Hesap oluşturma, e-posta doğrulama ve ilk giriş | UR-01, UR-02, UR-03 |
| [Sipariş Süreci](./order-process.md) | Ürün sayfasından teslimata kadar uçtan uca müşteri yolculuğu; admin sipariş yönetimini de kapsar | OM-01 – OM-07, NT-03 – NT-06, AT-01 |
| [Stok Rezervasyon Süreci](./stock-reservation-process.md) | Ödeme başlatıldığında tetiklenen stok blokesi, zaman aşımına bağlı serbest bırakma ve ödeme sonrası kalıcı düşüm | IR-01 – IR-08 |
| [Promosyon Değerlendirme Süreci](./promotion-evaluation-process.md) | Kural değerlendirme, birleştirme mantığı ve ödeme sırasında indirim uygulaması | PE-01 – PE-11 |

---

## Diyagramlar Nasıl Okunur?

- **Yuvarlak köşeli dikdörtgenler** — Olaylar (gerçekleşen şeyler)
- **Dikdörtgenler** — Görevler veya aktiviteler (sistemin ya da kullanıcının yaptığı şeyler)
- **Eşkenar Dörtgenler (Baklava dilimleri)** — Karar noktaları (dallanma mantığı)
- **Kulvarlar (Swimlanes)** — İlgili aktörler: Müşteri, Sistem, Yönetici, Ödeme Ağ Geçidi

Mermaid diyagramları düz metin editörlerinde görüntülenemeyebilir. Mermaid destekli bir Markdown görüntüleyici kullanın (ör. GitHub, Mermaid eklentili VSCode, Obsidian).

---

## Süreçler Arasındaki İlişkiler

Aşağıdaki dört süreç birbirinden bağımsız değildir — müşterinin satın alma yolculuğunda kritik anlarda birbirleriyle etkileşime girer:

```
[Kullanıcı Kaydı] ──► Müşteri kimlik doğrulaması gerçekleşir
                              │
                              ▼
                      [Sipariş Süreci]
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
     [Stok Rezervasyon    [Promosyon      [Ödeme Akışı]
      Süreci]              Değerlendirme   (Sipariş Süreci
                           Süreci]           içinde)
```

Tamamlanmış bir satın alımla sonuçlanan müşteri oturumu, bu dört süreci sırayla tetikler.
