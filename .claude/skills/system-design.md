---
name: system-design
description: Skill for designing new features and modules within the existing enterprise architecture
---

# System Design Skill

## Purpose

Guide the design of new features and modules that fit within the EFFORT-Web architecture.

## When to Use

- Planning new feature implementation
- Designing new modules or integrations
- Evaluating architectural trade-offs for new requirements
- Creating technical design documents

## Design Process

### 1. Requirements Analysis

Before designing, clarify:
- What business problem does this solve?
- Who are the users (web portal users, mobile app, API consumers, schedulers)?
- What data entities are involved?
- What existing code can be reused?
- What are the performance requirements?

### 2. Component Design (Following Existing Architecture)

For a new feature "X", design these components:

```
XController.java          → HTTP endpoint handling
    ↓
XManager.java             → Business logic
    ↓
XDao.java                 → Database queries
    ↓
Database tables            → Schema changes
    ↓
x.jsp                     → UI (if web-facing)
tiles-definitions.xml     → Tiles registration (if new page)
```

### 3. Design Template

```markdown
## Feature: [Name]

### Overview
Brief description of what the feature does.

### Components

**Controller**: `XController.java`
- Endpoint: `/web/x/list` (GET) — List page
- Endpoint: `/web/x/save` (POST, @ResponseBody) — AJAX save

**Manager**: `XManager.java`
- `listItems(customerId, page, size)` — Business logic for listing
- `saveItem(item)` — Validation and persistence

**DAO**: `XDao.java`
- `getItemsByCustomer(customerId, offset, limit)` — Paginated query
- `insertItem(item)` — Insert with generated key

**View**: `WEB-INF/views/xList.jsp`
- Uses Tiles layout
- jQuery AJAX for save operations

**Database**:
- Table: `x_items` (id, name, customer_id, status, created_date, updated_date)
- Index: `idx_x_items_customer` on (customer_id, status)

### Configuration Changes
- Add Tiles definition in `tiles-definitions.xml`
- Add URL pattern to Spring Security if needed

### Dependencies
- Existing: CustomerDao, NotificationManager
- New: XDao, XManager
```

### 4. Integration Points

When designing, consider how the new feature interacts with:

| Integration | Consideration |
|---|---|
| Spring Security | Does the URL need authentication? Role-based access? |
| Multi-tenancy | Is data scoped by customer_id? |
| Scheduling | Does it need background processing? |
| JMS/Messaging | Does it need async communication? |
| MongoDB | Is there unstructured data to store? |
| Redis | Is there data to cache? |
| JasperReports | Does it need PDF/report generation? |
| Mobile API | Does it need endpoints under `/service/*`? |

### 5. Database Design Principles

- Always include `customer_id` for multi-tenant isolation
- Include `created_date`, `updated_date` audit columns
- Use `is_active` (TINYINT 1/0) for soft deletes
- Foreign keys should match existing naming conventions
- Index all columns used in WHERE clauses and JOINs

### 6. Checklist Before Implementation

- [ ] Components follow Controller → Manager → DAO pattern
- [ ] Database schema includes customer_id for multi-tenancy
- [ ] Endpoints secured with Spring Security
- [ ] Input validation at controller level
- [ ] Error handling in all layers
- [ ] Logging at appropriate levels
- [ ] SQL queries parameterized
- [ ] JSP output encoded
