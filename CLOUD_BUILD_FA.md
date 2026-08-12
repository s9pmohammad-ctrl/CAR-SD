# ساخت آنلاین APK برنامه CAR SD

## روش پیشنهادی: GitHub Actions

این پروژه طوری آماده شده که بدون Android Studio و فقط با مرورگر ساخته شود.

### مراحل

1. به https://github.com بروید و وارد حساب شوید.
2. یک Repository جدید با نام `CAR-SD` بسازید.
3. فایل‌های داخل این پوشه را در ریشه Repository آپلود کنید.
   مهم: پوشه مخفی `.github` نیز باید آپلود شود.
4. پس از آپلود، GitHub Actions به صورت خودکار Build را شروع می‌کند.
5. وارد تب `Actions` شوید و Workflow با نام `Build CAR SD APK` را باز کنید.
6. اگر Build خودکار اجرا نشد، `Run workflow` را بزنید.
7. بعد از سبز شدن Build، پایین صفحه در بخش Artifacts روی `CAR-SD-APK` بزنید.
8. ZIP خروجی GitHub را باز کنید؛ داخل آن فایل `CAR-SD.apk` قرار دارد.
9. `CAR-SD.apk` را به مانیتور اندرویدی منتقل و نصب کنید.

## روش جایگزین: Codemagic

فایل `codemagic.yaml` نیز در ریشه پروژه وجود دارد. Repository را به Codemagic وصل کنید و Workflow
`CAR SD Android APK` را اجرا کنید. خروجی `CAR-SD.apk` به عنوان Artifact نمایش داده می‌شود.

## نکته مهم درباره T440

Build شدن APK به معنی شناسایی قطعی MCU فن و RGB نیست. برنامه فقط مسیرهای استاندارد Android/Linux را
به صورت امن شناسایی می‌کند. اگر Fan/RGB توسط MCU اختصاصی Helix کنترل شود، برای تکمیل آن باید API/فرمان MCU
از APK کارخانه‌ای یا لاگ دستگاه استخراج شود.
