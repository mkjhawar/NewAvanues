# Phase 7: App Templates - Implementation Plan

**Version**: 1.0.0
**Date**: 2025-11-06
**Status**: Planning
**Estimated Duration**: 4-5 weeks
**Dependencies**: Phases 1-6 Complete ✅

---

## Overview

Phase 7 delivers **production-ready app templates** that leverage the complete IDEAMagic framework (UI Components, Forms, Workflows) to generate functional applications in **under 10 minutes**.

**Goal**: Enable developers to generate complete KMP applications with a single DSL configuration, reducing time-to-market from weeks to minutes.

---

## Architecture

### Template System Components

```
AppTemplate (Sealed Class)
├── metadata: TemplateMetadata
├── ui: UITemplate
├── database: DatabaseTemplate
├── workflows: List<WorkflowTemplate>
├── forms: List<FormTemplate>
└── features: Set<Feature>

TemplateGenerator
├── generateProject(config: AppConfig): ProjectStructure
├── generateDatabase(schema: DatabaseTemplate): SQLSchema
├── generateUI(template: UITemplate): ComponentTree
└── generateWorkflows(flows: List<WorkflowTemplate>): WorkflowEngine
```

### 5 Core Templates

#### 1. **E-Commerce Template** 🛒
```kotlin
object ECommerceTemplate : AppTemplate {
    override val name = "E-Commerce Store"
    override val forms = listOf(
        ProductForm,      // Product management
        CheckoutForm,     // Payment + shipping
        OrderForm         // Order tracking
    )
    override val workflows = listOf(
        CheckoutWorkflow,      // Cart → Payment → Confirmation
        OrderTrackingWorkflow  // Order status updates
    )
    override val features = setOf(
        Feature.PRODUCT_CATALOG,
        Feature.SHOPPING_CART,
        Feature.PAYMENT_PROCESSING,
        Feature.ORDER_MANAGEMENT,
        Feature.USER_REVIEWS
    )
}
```

**Generated App Includes**:
- Product catalog with filtering/search
- Shopping cart with persistence
- Multi-step checkout workflow
- Payment integration (Stripe/PayPal)
- Order tracking dashboard
- User authentication
- Admin panel (product/order management)

**Database Schema** (Auto-generated):
```sql
CREATE TABLE products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    price REAL NOT NULL CHECK(price >= 0),
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    category TEXT NOT NULL,
    image_url TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    status TEXT NOT NULL CHECK(status IN ('pending', 'paid', 'shipped', 'delivered', 'cancelled')),
    total_amount REAL NOT NULL,
    shipping_address TEXT NOT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE order_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    product_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL CHECK(quantity > 0),
    price_per_unit REAL NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);
```

**Estimated LOC**: ~8,000 lines (generated)

---

#### 2. **Task Management Template** ✅
```kotlin
object TaskManagementTemplate : AppTemplate {
    override val name = "Task & Project Manager"
    override val forms = listOf(
        TaskForm,         // Task creation/editing
        ProjectForm,      // Project setup
        TeamMemberForm    // User management
    )
    override val workflows = listOf(
        TaskWorkflow,         // New → In Progress → Review → Done
        ProjectWorkflow       // Planning → Active → Archived
    )
    override val features = setOf(
        Feature.TASK_BOARDS,
        Feature.KANBAN_VIEW,
        Feature.GANTT_CHART,
        Feature.TIME_TRACKING,
        Feature.TEAM_COLLABORATION
    )
}
```

**Generated App Includes**:
- Kanban board (drag-and-drop)
- Task lists with filtering
- Project timeline (Gantt chart)
- Time tracking
- Team collaboration (comments, assignments)
- Notifications
- Dashboard with analytics

**Key Forms**:
```kotlin
val TaskForm = form("task") {
    textField("title") {
        label("Task Title")
        required()
        maxLength(200)
    }

    textAreaField("description") {
        label("Description")
        maxLength(2000)
    }

    selectField("priority") {
        label("Priority")
        options(listOf("Low", "Medium", "High", "Critical"))
        defaultValue("Medium")
    }

    selectField("status") {
        label("Status")
        options(listOf("New", "In Progress", "Review", "Done", "Blocked"))
        defaultValue("New")
    }

    dateField("due_date") {
        label("Due Date")
        minDate(LocalDate.now())
    }

    selectField("assigned_to") {
        label("Assigned To")
        // Dynamic: Load team members from DB
        dynamicOptions { db -> db.getAllUsers() }
    }
}
```

**Estimated LOC**: ~7,500 lines

---

#### 3. **Social Media Template** 📱
```kotlin
object SocialMediaTemplate : AppTemplate {
    override val name = "Social Network"
    override val forms = listOf(
        PostForm,         // Create posts
        ProfileForm,      // User profile
        CommentForm       // Comments
    )
    override val workflows = listOf(
        ContentModerationWorkflow,  // Submit → Review → Publish
        UserOnboardingWorkflow      // Register → Profile → Interests
    )
    override val features = setOf(
        Feature.NEWS_FEED,
        Feature.USER_PROFILES,
        Feature.FOLLOW_SYSTEM,
        Feature.LIKES_COMMENTS,
        Feature.MEDIA_UPLOAD,
        Feature.NOTIFICATIONS
    )
}
```

**Generated App Includes**:
- News feed with infinite scroll
- User profiles with followers/following
- Post creation (text, images, videos)
- Like/comment system
- Real-time notifications
- Search (users, posts, hashtags)
- Moderation tools

**Estimated LOC**: ~9,000 lines

---

#### 4. **Learning Management System (LMS) Template** 🎓
```kotlin
object LMSTemplate : AppTemplate {
    override val name = "Learning Platform"
    override val forms = listOf(
        CourseForm,       // Course creation
        LessonForm,       // Lesson content
        QuizForm,         // Assessments
        EnrollmentForm    // Student enrollment
    )
    override val workflows = listOf(
        CourseCompletionWorkflow,  // Enroll → Progress → Complete → Certificate
        QuizWorkflow              // Start → Answer → Submit → Grade
    )
    override val features = setOf(
        Feature.COURSE_CATALOG,
        Feature.VIDEO_PLAYER,
        Feature.QUIZZES,
        Feature.PROGRESS_TRACKING,
        Feature.CERTIFICATES,
        Feature.DISCUSSION_FORUMS
    )
}
```

**Generated App Includes**:
- Course catalog with categories
- Video lessons with progress tracking
- Quiz engine with auto-grading
- Certificate generation
- Discussion forums
- Student dashboard
- Instructor admin panel

**Estimated LOC**: ~10,000 lines

---

#### 5. **Healthcare Appointment Template** 🏥
```kotlin
object HealthcareTemplate : AppTemplate {
    override val name = "Appointment Booking System"
    override val forms = listOf(
        PatientForm,          // Patient registration
        AppointmentForm,      // Book appointment
        MedicalRecordForm     // Health records
    )
    override val workflows = listOf(
        AppointmentWorkflow,      // Request → Confirm → Reminder → Complete
        PatientOnboardingWorkflow // Register → Insurance → Medical History
    )
    override val features = setOf(
        Feature.APPOINTMENT_SCHEDULING,
        Feature.CALENDAR_VIEW,
        Feature.PATIENT_RECORDS,
        Feature.PRESCRIPTION_MANAGEMENT,
        Feature.NOTIFICATIONS,
        Feature.TELEMEDICINE
    )
}
```

**Generated App Includes**:
- Appointment booking calendar
- Patient records management
- Doctor availability scheduling
- SMS/email reminders
- Prescription tracking
- Telemedicine video integration
- HIPAA-compliant data encryption

**Estimated LOC**: ~9,500 lines

---

## DSL Configuration API

### Example: Generate E-Commerce App in 10 Minutes

```kotlin
fun main() {
    val app = generateApp {
        // 1. Choose template
        template = AppTemplate.ECOMMERCE

        // 2. Configure branding
        branding {
            name = "TechGadgets Shop"
            package = "com.techgadgets.shop"
            colors {
                primary = Color(0xFF1976D2)
                secondary = Color(0xFFFFA726)
                accent = Color(0xFF4CAF50)
            }
            logo = "assets/logo.png"
        }

        // 3. Database configuration
        database {
            dialect = SQLDialect.POSTGRESQL  // or SQLITE, MYSQL, SQLSERVER
            host = "localhost"
            port = 5432
            name = "techgadgets_db"
            migrations = true  // Auto-generate Flyway migrations
        }

        // 4. Feature toggles
        features {
            enable(Feature.PRODUCT_CATALOG)
            enable(Feature.SHOPPING_CART)
            enable(Feature.CHECKOUT)
            enable(Feature.ORDER_TRACKING)
            enable(Feature.USER_REVIEWS)
            disable(Feature.WISHLIST)  // Optional feature
        }

        // 5. Payment provider
        payments {
            provider = PaymentProvider.STRIPE
            apiKey = System.getenv("STRIPE_API_KEY")
            currency = "USD"
        }

        // 6. Platform targets
        platforms {
            android {
                minSdk = 26
                targetSdk = 34
            }
            ios {
                minVersion = "15.0"
            }
            desktop {
                targets = setOf(JVMTarget.WINDOWS, JVMTarget.MAC, JVMTarget.LINUX)
            }
        }
    }

    // 7. Generate project
    app.generate(outputPath = "/path/to/output")

    // Output:
    // ✅ Project structure created
    // ✅ Database schema generated (10 tables)
    // ✅ 45 UI components generated
    // ✅ 8 workflows configured
    // ✅ 12 forms created
    // ✅ Gradle build files configured
    // ✅ Ready to run: ./gradlew run
}
```

**Time Required**: 5-10 minutes (configuration + generation)
**Generated Code**: 8,000+ lines, production-ready

---

## Implementation Phases

### Week 1: Template System Foundation
**Files to Create**:
```
Universal/IDEAMagic/Templates/
├── Core/
│   ├── src/commonMain/kotlin/
│   │   ├── AppTemplate.kt
│   │   ├── TemplateMetadata.kt
│   │   ├── TemplateGenerator.kt
│   │   ├── AppConfig.kt
│   │   ├── BrandingConfig.kt
│   │   ├── DatabaseConfig.kt
│   │   └── Feature.kt
│   └── build.gradle.kts
```

**Tasks**:
1. Define `AppTemplate` sealed interface
2. Create `TemplateGenerator` engine
3. Implement `AppConfig` DSL
4. Build project structure generator
5. Add Gradle/build file generation

**Deliverables**: Template system architecture (8 files, ~1,200 LOC)

---

### Week 2: E-Commerce Template
**Files to Create**:
```
Universal/IDEAMagic/Templates/ECommerce/
├── src/commonMain/kotlin/
│   ├── ECommerceTemplate.kt
│   ├── forms/
│   │   ├── ProductForm.kt
│   │   ├── CheckoutForm.kt
│   │   └── OrderForm.kt
│   ├── workflows/
│   │   ├── CheckoutWorkflow.kt
│   │   └── OrderTrackingWorkflow.kt
│   ├── database/
│   │   └── ECommerceSchema.kt
│   └── ui/
│       ├── ProductCatalog.kt
│       ├── ShoppingCart.kt
│       └── CheckoutScreen.kt
```

**Tasks**:
1. Define product/order forms
2. Implement checkout workflow
3. Generate database schema
4. Create UI templates
5. Add payment integration hooks

**Deliverables**: Complete e-commerce template (15 files, ~3,500 LOC)

---

### Week 3: Task Management + Social Media Templates
**Similar structure for both templates**

**Tasks**:
1. Task Management:
   - Task/project forms
   - Kanban board workflow
   - Time tracking integration

2. Social Media:
   - Post/profile forms
   - Content moderation workflow
   - News feed generation

**Deliverables**: 2 complete templates (25 files, ~6,000 LOC)

---

### Week 4: LMS + Healthcare Templates
**Tasks**:
1. LMS:
   - Course/quiz forms
   - Course completion workflow
   - Video player integration

2. Healthcare:
   - Patient/appointment forms
   - Appointment scheduling workflow
   - HIPAA compliance features

**Deliverables**: 2 complete templates (30 files, ~7,500 LOC)

---

### Week 5: Testing, Documentation, CLI Tool
**Tasks**:
1. Write comprehensive tests for all templates
2. Create template selection CLI
3. Write template developer guide
4. Add template customization docs
5. Create video demos

**Deliverables**:
- Test suite (422+ tests)
- CLI tool (`avamagic create-app --template ecommerce`)
- Documentation (80+ pages)
- Demo videos (5 templates)

---

## Generated Project Structure

```
TechGadgetsShop/
├── gradle/
├── android/
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── kotlin/com/techgadgets/shop/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── ProductCatalogScreen.kt
│   │   │   │   │   ├── ShoppingCartScreen.kt
│   │   │   │   │   └── CheckoutScreen.kt
│   │   │   │   ├── viewmodels/
│   │   │   │   └── data/
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle.kts
├── ios/
│   ├── TechGadgetsShop/
│   │   ├── ContentView.swift
│   │   ├── ProductCatalogView.swift
│   │   └── Info.plist
│   └── TechGadgetsShop.xcodeproj
├── shared/
│   ├── src/
│   │   ├── commonMain/
│   │   │   ├── kotlin/com/techgadgets/shop/
│   │   │   │   ├── App.kt
│   │   │   │   ├── database/
│   │   │   │   │   ├── ProductRepository.kt
│   │   │   │   │   └── OrderRepository.kt
│   │   │   │   ├── forms/
│   │   │   │   │   ├── ProductForm.kt
│   │   │   │   │   └── CheckoutForm.kt
│   │   │   │   └── workflows/
│   │   │   │       └── CheckoutWorkflow.kt
│   │   ├── androidMain/
│   │   └── iosMain/
│   └── build.gradle.kts
├── settings.gradle.kts
├── build.gradle.kts
└── README.md
```

**Ready to run**:
```bash
# Android
./gradlew :android:app:installDebug

# iOS
cd ios && xcodebuild

# Desktop
./gradlew :desktop:run
```

---

## CLI Tool Design

### Installation
```bash
npm install -g @avamagic/cli
# or
brew install avamagic
```

### Usage
```bash
# Interactive template selection
avamagic create

# Direct template
avamagic create --template ecommerce --name "My Shop"

# List templates
avamagic list-templates

# Customize template
avamagic create --template ecommerce --config custom-config.yml
```

**Example `custom-config.yml`**:
```yaml
template: ecommerce
branding:
  name: "TechGadgets Shop"
  package: "com.techgadgets.shop"
  colors:
    primary: "#1976D2"
    secondary: "#FFA726"
database:
  dialect: postgresql
  host: localhost
  port: 5432
features:
  - product_catalog
  - shopping_cart
  - checkout
  - order_tracking
payments:
  provider: stripe
  currency: USD
platforms:
  android:
    minSdk: 26
  ios:
    minVersion: "15.0"
```

---

## Testing Strategy

### Template Generation Tests
```kotlin
class ECommerceTemplateTest {
    @Test
    fun `generates valid project structure`() {
        val config = AppConfig {
            template = AppTemplate.ECOMMERCE
            branding { name = "Test Shop" }
        }

        val project = TemplateGenerator.generate(config)

        assertTrue(project.hasFile("settings.gradle.kts"))
        assertTrue(project.hasModule("android"))
        assertTrue(project.hasModule("ios"))
        assertTrue(project.hasModule("shared"))
    }

    @Test
    fun `generates valid database schema`() {
        val schema = ECommerceTemplate.database.toSQL(SQLDialect.SQLITE)

        assertTrue(schema.contains("CREATE TABLE products"))
        assertTrue(schema.contains("CREATE TABLE orders"))
        assertTrue(schema.contains("CREATE TABLE order_items"))
    }

    @Test
    fun `generates valid forms`() {
        val forms = ECommerceTemplate.forms

        assertEquals(3, forms.size)
        assertTrue(forms.any { it.name == "product" })
        assertTrue(forms.any { it.name == "checkout" })
    }

    @Test
    fun `generates valid workflows`() {
        val workflows = ECommerceTemplate.workflows

        assertEquals(2, workflows.size)
        val checkout = workflows.find { it.name == "checkout" }
        assertNotNull(checkout)
        assertEquals(5, checkout?.steps?.size)
    }
}
```

### Integration Tests
```kotlin
@Test
fun `generated app compiles and runs`() {
    val config = AppConfig {
        template = AppTemplate.ECOMMERCE
        branding { name = "Test Shop"; package = "com.test.shop" }
        database { dialect = SQLDialect.SQLITE }
    }

    val outputPath = Files.createTempDirectory("test-app")
    TemplateGenerator.generate(config, outputPath)

    // Run Gradle build
    val result = ProcessBuilder("./gradlew", "build")
        .directory(outputPath.toFile())
        .start()
        .waitFor()

    assertEquals(0, result)  // Build succeeded
}
```

**Total Tests**: 120+ tests across all templates

---

## Documentation Deliverables

### 1. Template User Guide (40 pages)
- Quick start (10 minutes to first app)
- Template selection guide
- Configuration DSL reference
- Feature customization
- Deployment guides

### 2. Template Developer Guide (30 pages)
- Creating custom templates
- Template API reference
- Form/workflow integration
- UI component generation
- Database schema generation

### 3. API Reference (25 pages)
- `AppTemplate` interface
- `TemplateGenerator` class
- `AppConfig` DSL
- All 5 template APIs

---

## Success Metrics

**Quantitative**:
- ✅ 5 production-ready templates
- ✅ <10 minutes to generate functional app
- ✅ 8,000+ LOC generated per template
- ✅ 120+ tests passing
- ✅ CLI tool with interactive mode
- ✅ 95+ pages documentation

**Qualitative**:
- ✅ Generated apps compile without errors
- ✅ Apps run on Android, iOS, Desktop
- ✅ Database migrations work correctly
- ✅ Forms validate data properly
- ✅ Workflows manage state correctly
- ✅ UI follows platform guidelines

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Generated code doesn't compile | High | Automated testing of each template |
| Platform-specific bugs | Medium | Test on all platforms (Android/iOS/Desktop) |
| Database migration failures | High | Comprehensive migration testing |
| Form validation edge cases | Medium | Extensive validation rule testing |
| Workflow state corruption | High | State machine property-based testing |

---

## Budget Estimate

**Development Time**: 4-5 weeks (1 developer)
**Cost**: ~$25,000 - $30,000 (@ $150/hour)

**Breakdown**:
- Week 1: Template system foundation ($6,000)
- Week 2: E-commerce template ($6,000)
- Week 3: Task + Social templates ($6,000)
- Week 4: LMS + Healthcare templates ($6,000)
- Week 5: Testing + docs + CLI ($6,000)

---

## Timeline

**Start Date**: 2025-11-06
**End Date**: 2025-12-06

**Milestones**:
- Week 1 (Nov 13): Template system ready
- Week 2 (Nov 20): E-commerce template complete
- Week 3 (Nov 27): Task + Social templates complete
- Week 4 (Dec 4): LMS + Healthcare templates complete
- Week 5 (Dec 6): Testing, docs, CLI ready

---

## Next Steps

1. ✅ Review and approve Phase 7 plan
2. Start Week 1: Template system foundation
3. Create `AppTemplate.kt` interface
4. Implement `TemplateGenerator` engine
5. Build `AppConfig` DSL

---

**Plan Status**: Ready for Implementation
**Awaiting**: User approval to proceed
