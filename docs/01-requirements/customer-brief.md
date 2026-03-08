# Customer Brief — TechNest E-Commerce Platform

**From:** Kemal Aydın, Founder — TechNest  
**To:** Development Team  
**Date:** January 14, 2025  
**Subject:** Custom e-commerce platform — full project brief

---

## Who We Are

We're a small team — right now just me and a part-time warehouse guy, but the plan is to grow. I've been reselling consumer electronics for about three years, mostly through Trendyol and Hepsiburada. It's been going alright but I've hit a ceiling on what I can do through those platforms and I want to start building something I actually own.

The brand is TechNest. Phones, laptops, tablets, accessories, peripherals. We don't manufacture. We source from distributors, occasionally directly from brands when we can get a deal. Margins are thin in this market, which is exactly why I want a direct channel without paying 15-20% commission on everything.

---

## Why Not Just Use Shopify or a Similar Platform

Honestly, this is a fair question and I spent a couple of weeks thinking about it before reaching out. Shopify is fine for a lot of people. But I kept running into walls when I tried to map out what I actually need.

The biggest thing: promotions. I run a lot of campaigns — seasonal discounts, category-based deals, bundle offers. On Shopify, the native discount system is surprisingly limited. Things like "10% off orders over 5.000 TL in the laptop category, but the discount doesn't apply if the customer is already using a voucher code" — that kind of rule requires either Shopify Functions (which means writing code anyway) or a third-party app that charges me monthly and still might not support the exact rules I need.

The second thing is around how cart and stock interact. I've had situations where two customers were looking at the last unit of a product at the same time, both added it to their cart, one paid, the other got a nasty surprise at checkout. I want to handle that more gracefully — ideally hold the stock for a customer once it's in their cart and they're actively heading to checkout, with some kind of timeout if they abandon. That's not something I've found a clean off-the-shelf solution for.

And honestly, at some point the app subscriptions add up. SEO app, review app, discount app, inventory app — before you know it you're paying more in monthly SaaS fees than you saved on development. I'd rather invest once and own the platform.

---

## What We Need

### The Store

The customer-facing side is fairly standard:

- Product catalog with categories, search, and filters (by brand, price, specs)
- Product detail pages — good image support, specs table, variant selection, stock status
- Cart and checkout flow
- Account creation, order history, address management
- Email notifications — order confirmation, shipping updates

Product variants are important. A phone listing needs to handle different storage sizes and colors, each with its own stock count and potentially its own price. Some platforms treat this as an afterthought. It shouldn't be.

On the admin side:

- Product and category management
- Order management — view, filter, update status
- Inventory tracking with low-stock alerts
- Basic sales dashboard — revenue, order volume, that kind of thing

### Promotions

This is one of the core things I want done properly. The system should support:

- Percentage and fixed-amount discounts
- Cart-level discounts (order total thresholds) and product-level discounts
- Category-specific promotions
- Voucher codes, including single-use codes for specific customers
- Promotion stacking rules — some promotions can combine, others can't. I need to be able to configure this, not fight the system every time.
- Scheduled promotions with start and end dates

What I don't want is a system where every new campaign type requires a developer. The logic should be there, I should be able to configure it.

### Stock Reservation

When a customer adds something to their cart and moves to checkout, I'd like that stock to be held for them for a reasonable window — maybe 10-15 minutes. If they don't complete the purchase, it goes back to available. This prevents the "two people, last item" situation I mentioned earlier.

I understand this adds some complexity. I'm okay with that. I'd rather handle it properly than keep getting complaints.

### Order Notifications and Audit Trail

Every meaningful thing that happens in the system — an order is placed, payment is confirmed, an item ships, an admin changes an order status — I want that recorded. Not just in the order record itself but as a proper event history I can look back at.

For customers: automated emails at the right moments (order confirmed, payment failed, shipped, delivered). Not spam, just the ones that actually matter.

For us internally: I want to be able to see the full history of what happened to an order. If a customer contacts us saying "my order status changed but I never got an email," I want to be able to look at a timeline and understand exactly what happened and when.

---

## What We're Not Looking For

- Native mobile apps — responsive web is fine for now
- Multi-vendor / marketplace features — we're the only seller
- B2B or wholesale pricing
- Heavy analytics — basic reporting is enough for this stage
- Social login — email and password is fine for now

---

## Users

**Customers** — tech buyers. They'll be comparing specs, reading descriptions carefully, probably browsing on mobile. They're not going to be patient with a slow or confusing experience.

**Admins (us)** — primarily me, eventually whoever we hire for fulfillment. Not highly technical. The admin interface needs to be straightforward enough that I'm not googling how to do basic things.

---

## Technical Notes

I don't have strong opinions here and I'll mostly defer to the team. A developer friend told me to make sure we're not building something that'll fall apart the moment we want to add a feature — he called it a "big ball of mud." I don't know the technical term but I know I've seen the result and I don't want it.

**Performance matters.** The site needs to feel fast. I've bounced off slow e-commerce sites myself and I won't accept that for ours.

**Payments.** I'd like to integrate İyzico as the primary payment gateway — it's the most practical choice for the Turkish market. I know Stripe also supports Turkish merchants now and it might be worth looking at as a secondary option or for future international expansion. Either way, this needs to be real from day one — not a placeholder.

**Hosting.** I don't know much about this side. I want something reliable that doesn't cost a fortune at our current scale but can handle growth. I'll take the team's recommendation.

---

## Timeline and Budget

I'm self-funding this so I need to be realistic. My goal is to have something live within 4-6 months. I know that might be tight. If scope needs to be trimmed I'd rather cut admin features than touch the customer-facing store or the promotions system.

Priority order:
1. The core store — non-negotiable
2. Promotions engine — non-negotiable, this is a key part of how we compete
3. Stock reservation — important, but we could launch with a simpler approach if needed
4. Admin features beyond the basics — can be added post-launch

---

## Long-Term

I'm not looking for a one-off engagement. If this goes well I'd want to keep working with the same team as we grow. Things on the horizon: mobile app, logistics integrations, maybe an ERP connection if we get to that scale.

Happy to jump on a call to go through any of this. I'm usually available evenings.

— Kemal
