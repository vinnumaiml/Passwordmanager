# Elvarix — Transactional Email Delivery & Supabase Auth Setup Guide

## 1. Problem Diagnosis: Why Verification Emails Do Not Arrive

In Supabase Auth, email verification delivery failures occur due to three main causes:

1. **Default Rate Limits (3 emails/hour)**:
   The built-in Supabase email service is strictly throttled to **3 emails per hour per project**. If more than 3 signup attempts or resends occur within 60 minutes, Supabase silently rejects or drops subsequent messages with rate-limit errors.
2. **Shared IP Reputation & Spam Filters**:
   The built-in service sends from shared domains (`noreply@mail.app.supabase.io` or `noreply@supabase.co`) without customer-specific SPF/DKIM alignment. Major providers (Gmail, Outlook, Yahoo) frequently quarantine these messages directly into the **Spam/Junk** folder or block them entirely.
3. **Missing Redirect URL Configuration**:
   When confirmation emails are sent, the token link targets the project `SITE_URL` (defaulting to `http://localhost:3000`). If mobile deep-links (`elvarix://auth/callback`) are not whitelisted in **Authentication → URL Configuration → Redirect URLs**, mobile devices cannot complete authentication.

---

## 2. Setting Up a Custom SMTP Provider (Resend / SendGrid / Postmark / AWS SES)

To achieve 99.9% inbox deliverability, configure a custom SMTP provider in your Supabase project dashboard.

### Recommended Provider: Resend (Free 3,000 emails/month)
1. Sign up at [https://resend.com](https://resend.com) and add your domain (e.g., `elvarix.com` or your personal domain).
2. Add the required DNS records in your DNS host (Cloudflare, Namecheap, GoDaddy):
   - **SPF**: `TXT` record `v=spf1 include:amazonses.com ~all`
   - **DKIM**: 3 `CNAME` records provided by Resend.
   - **DMARC**: `TXT` record `_dmarc.yourdomain.com` with `v=DMARC1; p=none; rua=mailto:dmarc@yourdomain.com`
3. Generate an API Key under **API Keys** in Resend (name: `Elvarix-Supabase-Auth`).
4. In your **Supabase Dashboard**:
   - Navigate to **Authentication → Email → Custom SMTP**.
   - Toggle **Enable Custom SMTP** to `ON`.
   - **Sender email**: `noreply@yourdomain.com` (or `auth@yourdomain.com`)
   - **Sender name**: `Elvarix Security`
   - **Host**: `smtp.resend.com`
   - **Port**: `465` (SSL) or `587` (TLS)
   - **Username**: `resend`
   - **Password**: `<YOUR_RESEND_API_KEY>`
   - Click **Save**.

---

## 3. Configuring Supabase Auth Redirect URLs for Android Deep Linking

In your **Supabase Dashboard**:
1. Go to **Authentication → URL Configuration**.
2. Set **Site URL** to:
   ```
   https://vinnumaiml.github.io/Passwordmanager/callback
   ```
3. Add the following to **Redirect URLs**:
   ```
   elvarix://auth/callback
   https://vinnumaiml.github.io/Passwordmanager/callback
   ```
4. Click **Save Changes**.

---

## 4. Email Template Configuration (Confirmation & Reset)

In **Authentication → Email Templates → Confirm signup**:
- **Subject**: `Confirm your Elvarix Vault Account`
- **Body**:
```html
<div style="background-color: #0B0D12; color: #F4F5F8; padding: 32px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">
  <div style="max-width: 480px; margin: 0 auto; background-color: #11151D; border-radius: 16px; border: 1px solid #181D27; padding: 32px;">
    <h2 style="color: #67E8D1; margin-top: 0; font-size: 22px; letter-spacing: 1.5px;">ELVARIX</h2>
    <h1 style="color: #F4F5F8; font-size: 20px; font-weight: 600;">Confirm your email address</h1>
    <p style="color: #A5ADBC; font-size: 14px; line-height: 22px;">
      Welcome to Elvarix. Please confirm your email address to activate your zero-knowledge encrypted vault.
    </p>
    <div style="margin: 28px 0;">
      <a href="{{ .ConfirmationURL }}" style="display: block; width: 100%; text-align: center; background-color: #A78BFA; color: #0B0D12; font-weight: 700; text-decoration: none; padding: 14px 0; border-radius: 10px; font-size: 15px;">
        Verify Email & Unlock Vault
      </a>
    </div>
    <p style="color: #778296; font-size: 12px; line-height: 18px;">
      If you did not sign up for Elvarix, please ignore this email. This link will expire in 24 hours.
    </p>
  </div>
</div>
```

---

## 5. End-to-End Verification Test Procedure

1. **Local Configuration**:
   Add your real Supabase credentials into `local.properties`:
   ```properties
   SUPABASE_URL=https://your-project-id.supabase.co
   SUPABASE_ANON_KEY=your-real-anon-key
   ```
2. **App Registration**:
   Open Elvarix, navigate to **Sign Up**, enter name, email, and password.
3. **Verification**:
   - The app securely calls Supabase Auth `signUpWith(Email)`.
   - The app transitions to the **Verification Pending** screen displaying the masked email.
   - The transactional email is received in the recipient's inbox (not Spam).
   - Tapping the confirmation button opens the deep link `elvarix://auth/callback`.
   - Elvarix validates the cryptographic token, switches `AuthState` to `Authenticated`, and smoothly enters the Master Password setup.
4. **Resend & Status Check**:
   - Tapping **Check Status** performs an active server call (`auth.refreshCurrentSession()`), rejecting unverified accounts.
   - Tapping **Resend Email** triggers an authenticated resend with a mandatory 60-second cooldown.
