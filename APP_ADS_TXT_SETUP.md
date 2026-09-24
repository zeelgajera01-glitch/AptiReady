# Google AdMob Authorized Digital Sellers (`app-ads.txt`) Setup Guide for AptiRise

This document provides instructions for setting up the `app-ads.txt` file to protect AptiRise against ad fraud and ensure authorized seller verification by Google AdMob.

---

## 1. File Details
- **Filename**: `app-ads.txt`
- **File Location in Project**: Root directory (`E:/Android Project/AptiReady/app-ads.txt`)

---

## 2. Required File Content
The file contains exactly one authorized seller entry line:

```text
google.com, pub-4263244815223132, DIRECT, f08c47fec0942fa0
```

* **System Domain**: `google.com`
* **Publisher ID**: `pub-4263244815223132`
* **Account Type**: `DIRECT`
* **Certification Authority ID**: `f08c47fec0942fa0`

---

## 3. Required Future Hosting Location
When you obtain a developer website domain, host this plain-text file at the root directory of your website:

```text
https://YOUR-DOMAIN/app-ads.txt
```

> **IMPORTANT**: Replace `YOUR-DOMAIN` with your actual registered domain name (for example, `https://example.com/app-ads.txt`). Do not use subdirectories (e.g. `https://example.com/sub/app-ads.txt` is not valid).

---

## 4. Google Play Console Contact Information Requirement
Google AdMob's automated crawler discovers your `app-ads.txt` file via your app's listing on the Google Play Store.

1. Open **Google Play Console**.
2. Select the **AptiRise** app listing.
3. Navigate to **Store presence** $\rightarrow$ **Store listing** $\rightarrow$ **Contact Details**.
4. In the **Website** field, enter your developer website domain (e.g., `https://YOUR-DOMAIN`).
5. Save and publish your store listing changes.

---

## 5. AdMob Verification Workflow
1. **Prepare File**: The `app-ads.txt` file is prepared in this repository.
2. **Host File**: Upload `app-ads.txt` to `https://YOUR-DOMAIN/app-ads.txt`.
3. **Publish App**: Publish AptiRise on the Google Play Store with the website URL in contact details.
4. **Link App in AdMob**:
   - Go to **AdMob Console** $\rightarrow$ **Apps** $\rightarrow$ **All Apps** $\rightarrow$ **AptiRise** $\rightarrow$ **App Settings**.
   - Link your Google Play Store listing.
5. **Crawler Verification**:
   - AdMob periodically crawls `https://YOUR-DOMAIN/app-ads.txt`.
   - In AdMob Console under **`app-ads.txt`**, status will update to **Authorized** once verified.

---

## 6. Important Notes
- **External Web File**: The `app-ads.txt` file is hosted on a web server, **NOT inside the Android app APK or assets**.
- **No Early Claim**: Storing `app-ads.txt` in the Android source repository prepares the file for hosting, but AdMob verification occurs only after the web server hosts it and Google Play Console links the developer website.
