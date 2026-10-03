# Utsav — Every celebration, one app

**Utsav** is a marketplace for booking event vendors for celebrations — Indian, American, and everything in between. Customers browse verified vendors, see portfolio photos tagged with what they *actually cost*, read real reviews, and book time slots directly. Vendors — especially newcomers and upcoming entrepreneurs — list their businesses, prove themselves with budget-tagged past work, and grow.

## The idea

Finding vendors for a sangeet, mehendi, birthday, wedding, or Halloween party today means scrolling Instagram or chasing word-of-mouth. Utsav puts it in one app, with a **budget-first** differentiator:

- **❄ Budget Freeze (two-sided)** — customers freeze a total budget once; every search, filter, and AI-built team stays strictly inside it. Vendors tag every portfolio photo with the budget it was executed at ("This mandap: $1,800 · 120 guests") — proof over promises.
- **AI event concierge** — type a plain sentence ("sangeet for 80 people in Houston next month, budget $3000") and get a matched vendor team with an estimated total.
- **Build your team** — bundle decorator + photographer + makeup artist, see one combined price and the dates when *everyone* is free.
- **Vendor compare** — side-by-side comparison of up to 3 vendors.
- **Slot-based booking** — pick a real date & time, confirm instantly. No quote-chasing.
- **Vendor-to-vendor collaboration graph** — vendors recommend each other; book the whole trusted crew.
- **Diaspora + India** — 8 cities (Boston, New York, Houston, Bay Area, Dallas, Hyderabad, Chennai, Bangalore) with $ / ₹ pricing.

## What's inside

- `index.html` — the entire app: 8 vendor categories, 14 occasions, 19 seed vendors, hash-routed views (Home, Browse, Vendor profile, Compare, Team builder, Onboarding, My bookings, Occasions), booking flow with confetti, AI concierge, Budget Freeze.
- `images/` — 16 photorealistic images (hero, 8 category shots, 6 portfolio shots), warm tones, no watermarks.

## How to run

No build step. Just open `index.html` in a browser — or serve it:

```bash
cd ~/workspace/utsav
python3 -m http.server 8000
# open http://localhost:8000
```

Data (bookings, vendor signups, reviews, frozen budget, team) persists in `localStorage`. Demo data is illustrative — business names, reviews, and Instagram handles are fictional.

## Tech

Tailwind CSS via CDN, vanilla JS, Google Fonts (Fraunces + Inter). Single self-contained file.
