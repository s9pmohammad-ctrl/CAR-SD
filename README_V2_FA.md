# CAR SD v2 — MCU Matcher
هدف: Helix/T440 با F52L + S212A70 + MCU V6.0-UP01 + CAN0017.
این نسخه ابتدا به صورت Read-Only مچ می‌کند، پکیج‌ها، Binder serviceها، device nodeها، دما، Fan PWM/RPM و LED را اسکن می‌کند و گزارش می‌سازد. نوشتن ناشناخته روی MCU عمداً قفل است تا پروتکل واقعی از گزارش همان دستگاه استخراج شود.
محل گزارش: Android/data/com.carsd.app/files/reports/
