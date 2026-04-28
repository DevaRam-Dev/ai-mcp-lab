---
name: legacy-monolith-analysis
description: Skill for analyzing and understanding legacy monolithic Java applications
---

# Legacy Monolith Analysis Skill

## Purpose

Guide systematic analysis of the EFFORT-Web monolith to understand its structure, identify technical debt, and plan modernization.

## When to Use

- Onboarding to understand the system
- Planning modernization efforts
- Identifying candidates for extraction or refactoring
- Assessing risk areas before major changes

## Analysis Framework

### 1. Dependency Analysis

Identify high-coupling areas:

```bash
# Count imports from other packages to find tightly coupled classes
grep -c "import com.ayansys.effort" src/main/java/com/ayansys/effort/manager/WebManager.java

# Find classes with the most dependencies
find src/main/java -name "*.java" -exec grep -l "import com.ayansys" {} \; | \
    xargs -I{} sh -c 'echo "$(grep -c "import com.ayansys" {}) {}"' | sort -rn | head -20
```

### 2. Complexity Hotspots

Large files are often complexity hotspots:

```bash
# Find largest Java files (likely need decomposition)
find src/main/java -name "*.java" -exec wc -l {} \; | sort -rn | head -20
```

### 3. Multi-Tenant Configuration Analysis

EFFORT uses per-customer configuration via XML constants files:

```
constants.xml                 → Shared defaults
all-constants.xml             → Cross-environment settings
siemens-uat-constants.xml     → Siemens UAT environment
siemens-live-constants.xml    → Siemens Production
nd-constants.xml              → ND environment
new-test-constants.xml        → Test environment (currently active)
```

**Risk**: Environment switching via XML comments in `root-context.xml` is error-prone.

### 4. Technical Debt Indicators

| Indicator | What to Look For |
|---|---|
| God Classes | Manager/DAO classes > 1000 lines |
| Long Methods | Methods > 100 lines |
| Duplicate Code | Similar SQL queries across DAOs |
| Dead Code | Unused methods, commented-out blocks |
| Naming Inconsistency | Mixed naming patterns (e.g., `schedulars` vs `schedulers`) |
| Package Confusion | `util/` vs `util1/`, `service/` vs `manager/` |
| Configuration Sprawl | 40+ XML config files |

### 5. Risk Assessment Matrix

| Area | Risk Level | Reason |
|---|---|---|
| Controllers | Medium | Many endpoints, but thin layer |
| Managers | High | Core business logic, high complexity |
| DAOs | Medium | SQL-heavy, but isolated |
| XML Config | High | Environment switching is manual and error-prone |
| Filters | Low | Stable, rarely changed |
| Schedulers | Medium | Customer-specific, multiple overlapping configs |

### Modernization Opportunities

1. **Externalize configuration** — Replace XML constant switching with environment variables or Spring profiles
2. **Add test coverage** — Start with Manager layer unit tests
3. **Extract shared utilities** — Consolidate `util/` and `util1/`
4. **Standardize naming** — Align package and class naming conventions
5. **Reduce XML config** — Migrate beans to Java `@Configuration` classes incrementally
