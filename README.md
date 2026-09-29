# AIHM — Data Preparation Pipeline (Kotlin)

> مهمة هذا الأسبوع: **تحضير البيانات** (التحليل الاستكشافي EDA + التنظيف + استخراج الميزات) — قبل اختيار الخوارزمية وتدريب النموذج (T6 + T8).
> اللغة: **Kotlin/JVM** • الإدارة: **Git & GitHub** • بدون أي مكتبات خارجية (يبني ويعمل دون اتصال بالإنترنت).

## ما الذي يفعله المشروع؟

يأخذ ملف `data/raw/sleep_diary.csv` وينتج في مجلد `output/`:

| الملف | الوصف |
|---|---|
| `eda_report.md` | تقرير التحليل الاستكشافي الكامل (نظرة عامة، قيم مفقودة، مكررات، إحصاءات وصفية، قيم شاذة، مشاكل الجودة، سجل التنظيف) |
| `cleaned_sleep_diary.csv` | البيانات بعد التنظيف (حذف المكررات، إعادة حساب الكفاءة، تعويض القيم المفقودة بمتوسط كل مستخدم، تقييد القيم) |
| `features_per_night.csv` | ميزات لكل ليلة (جاهزة للنموذج): يوم الأسبوع، عطلة نهاية الأسبوع، ساعة النوم/الاستيقاظ، دين النوم، منتصف النوم… |
| `features_per_user.csv` | ميزات مجمعة لكل مشارك: متوسط/انحراف مدة النوم، كفاءة النوم، نسبة الاستيقاظ ليلاً، فرق نهاية الأسبوع، انتظام موعد النوم… |

> نفس خط الأنابيب (profiling → cleaning → feature extraction) يُعاد استخدامها لاحقًا مع بيانات معدل ضربات القلب (Galaxy Watch Active2 / GalaxyPPG / Galaxy Watch7) — فقط استبدل قارئ CSV ونموذج السجل.

## المتطلبات

- **JDK 11 أو أحدث** (تأكد: `java -version`)
- إما **IntelliJ IDEA / Android Studio** (الأسهل)، أو **Gradle**، أو مترجم **kotlinc** فقط.

## الطريقة 1: التشغيل من IntelliJ IDEA / Android Studio (موصى بها)

1. افتح البرنامج → `File` → `Open` → اختر مجلد المشروع `aihm-data-prep`.
2. انتظر حتى يكتشف Gradle المشروع ويحمّل إعداداته (شريط تقدم أسفل الشاشة).
3. من شجرة الملفات افتح: `src/main/kotlin/com/aihm/dataprep/Main.kt`.
4. اضغط على السهم الأخضر بجانب `fun main(...)` أو `Shift + F10`.
5. ستظهر النتائج في نافذة **Run** أسفل الشاشة، وتُكتب الملفات الأربعة في مجلد `output/`.

## الطريقة 2: سطر الأوامر بـ Gradle

```bash
cd aihm-data-prep
./gradlew run            # Linux/macOS
gradlew.bat run          # Windows
```

لبناء ملف JAR قابل للنقل:

```bash
./gradlew jar
java -jar build/libs/aihm-data-prep-1.0.jar data/raw/sleep_diary.csv output
```

## الطريقة 3: مترجم kotlinc فقط (بدون Gradle)

```bash
cd aihm-data-prep
find src/main/kotlin -name "*.kt" > sources.txt
kotlinc @sources.txt -include-runtime -d aihm-dataprep.jar
java -jar aihm-dataprep.jar data/raw/sleep_diary.csv output
```

## الوسائط (اختيارية)

```
java -jar aihm-dataprep.jar <مسار_ملف_الإدخال.csv> <مجلد_الإخراج>
```

الافتراضي: `data/raw/sleep_diary.csv` → `output/`.

## رفع المشروع على GitHub

```bash
cd aihm-data-prep
git init
git add .
git commit -m "Data prep pipeline: EDA + cleaning + feature extraction (sleep diary)"
# أنشئ مستودعًا فارغًا على GitHub ثم:
git remote add origin https://github.com/<حسابك>/aihm-data-prep.git
git branch -M main
git push -u origin main
```

## هيكل المشروع

```
aihm-data-prep/
├── build.gradle.kts / settings.gradle.kts
├── data/raw/sleep_diary.csv        ← البيانات الخام
├── output/                         ← المخرجات (تتجدد في كل تشغيل)
└── src/main/kotlin/com/aihm/dataprep/
    ├── Main.kt                     ← نقطة التشغيل
    ├── model/SleepRecord.kt        ← نموذج البيانات + التحقق
    ├── io/CsvReader.kt             ← القراءة والتحليل الأولي
    ├── eda/Stats.kt + Profiler.kt  ← الإحصاء والتحليل الاستكشافي
    ├── cleaning/Cleaner.kt         ← خطوات التنظيف (مسجّلة بالكامل)
    ├── features/FeatureExtractor.kt← استخراج الميزات
    └── report/ReportGenerator.kt   ← كتابة التقرير وملفات CSV
```

## ملاحظات للمرحلة القادمة (اختيار الخوارزمية)

- بيانات سجل النوم هذه هي **بيانات سياق** تُستخدم لاحقًا كـ context features مع معدل ضربات القلب.
- عند توفر بيانات معدل ضربات القلب: أضف `HeartRateRecord` وقارئًا مشابهًا، واستخدم نفس `Stats`/`Profiler`/`Cleaner`، وفي `FeatureExtractor` أضف نافذات زمنية (5 دقائق): متوسط HR، الانحراف المعياري، الانحراف عن baseline الشخصي، مستوى النشاط/الخطوات، وقت اليوم، مدة الحالة الشاذة — كما هو محدد في T8.
