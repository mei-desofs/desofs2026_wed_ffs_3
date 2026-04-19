# Threat Analysis – Cafeteria Management System

---

## 1. Threat Identification (STRIDE per DFD Element)

### How to use this template
For each component of the system, identify threats using **STRIDE**. Not all STRIDE categories apply to all element types:

| Element Type    | S | T | R | I | D | E |
|-----------------|---|---|---|---|---|---|
| External Entity | ✅ |   | ✅ |   |   |   |
| Process         | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Data Store      |   | ✅ |   | ✅ | ✅ |   |
| Data Flow       |   | ✅ |   | ✅ | ✅ |   |

**Legend:**
- **S** – Spoofing (fake identity)
- **T** – Tampering (modify data)
- **R** – Repudiation (deny actions)
- **I** – Information Disclosure (leak data)
- **D** – Denial of Service (crash/overload)
- **E** – Elevation of Privilege (gain unauthorized access)

---

### 1.1 Authentication (Process)

| ID | STRIDE | Threat Description | Abuse Case |
|----|--------|--------------------|------------|
| T01 | S | _[describe how identity can be faked]_ | _[scenario: "An attacker..."]_ |
| T02 | T | _[describe how data can be modified]_ | _[scenario]_ |
| T03 | R | _[describe how actions can be denied]_ | _[scenario]_ |
| T04 | I | _[describe how info can leak]_ | _[scenario]_ |
| T05 | D | _[describe how service can be disrupted]_ | _[scenario]_ |
| T06 | E | _[describe how privileges can be escalated]_ | _[scenario]_ |

---

### 1.2 User Management (Process)

| ID | STRIDE | Threat Description | Abuse Case |
|----|--------|--------------------|------------|
| T07 | S | _[describe]_ | _[scenario]_ |
| T08 | T | _[describe]_ | _[scenario]_ |
| T09 | R | _[describe]_ | _[scenario]_ |
| T10 | I | _[describe]_ | _[scenario]_ |
| T11 | D | _[describe]_ | _[scenario]_ |
| T12 | E | _[describe]_ | _[scenario]_ |

---

### 1.3 Purchase Flow (Process)

| ID | STRIDE | Threat Description | Abuse Case |
|----|--------|--------------------|------------|
| T13 | S | _[describe]_ | _[scenario]_ |
| T14 | T | _[describe]_ | _[scenario]_ |
| T15 | R | _[describe]_ | _[scenario]_ |
| T16 | I | _[describe]_ | _[scenario]_ |
| T17 | D | _[describe]_ | _[scenario]_ |
| T18 | E | _[describe]_ | _[scenario]_ |

---

### 1.4 Ingredient / Dish / Menu Management (Process)

| ID | STRIDE | Threat Description | Abuse Case |
|----|--------|--------------------|------------|
| T19 | S | _[describe]_ | _[scenario]_ |
| T20 | T | _[describe]_ | _[scenario]_ |
| T21 | R | _[describe]_ | _[scenario]_ |
| T22 | I | _[describe]_ | _[scenario]_ |
| T23 | D | _[describe]_ | _[scenario]_ |
| T24 | E | _[describe]_ | _[scenario]_ |

---

### 1.5 Database (Data Store)

| ID | STRIDE | Threat Description | Abuse Case |
|----|--------|--------------------|------------|
| T25 | T | _[describe]_ | _[scenario]_ |
| T26 | I | _[describe]_ | _[scenario]_ |
| T27 | D | _[describe]_ | _[scenario]_ |

---

### 1.6 API Data Flows (Data Flow)

| ID | STRIDE | Threat Description | Abuse Case |
|----|--------|--------------------|------------|
| T28 | T | _[describe]_ | _[scenario]_ |
| T29 | I | _[describe]_ | _[scenario]_ |
| T30 | D | _[describe]_ | _[scenario]_ |

---

### 1.7 External Entities – Client / Employee / Admin (External Entity)

| ID | STRIDE | Threat Description | Abuse Case |
|----|--------|--------------------|------------|
| T31 | S | _[describe]_ | _[scenario]_ |
| T32 | R | _[describe]_ | _[scenario]_ |

---

## 2. Risk Assessment

### Methodology: Likelihood × Impact Matrix

```
                    IMPACT
              Low    Medium    High
         ┌────────┬─────────┬────────┐
  High   │ Medium │  High   │Critical│
L        ├────────┼─────────┼────────┤
I Medium │  Low   │ Medium  │  High  │
K        ├────────┼─────────┼────────┤
E Low    │  Low   │  Low    │ Medium │
         └────────┴─────────┴────────┘
```

### Risk Rating Table

| Threat ID | Threat (short name) | Likelihood (L/M/H) | Impact (L/M/H) | Risk Level |
|-----------|---------------------|---------------------|-----------------|------------|
| T01 | _[name]_ | _[L/M/H]_ | _[L/M/H]_ | _[Low/Medium/High/Critical]_ |
| T02 | _[name]_ | _[L/M/H]_ | _[L/M/H]_ | _[Low/Medium/High/Critical]_ |
| T03 | _[name]_ | _[L/M/H]_ | _[L/M/H]_ | _[Low/Medium/High/Critical]_ |
| ... | ... | ... | ... | ... |

---

## 3. Mitigations

_Focus on **High** and **Critical** threats first._

| Threat ID | Risk Level | Mitigation | Status |
|-----------|------------|------------|--------|
| _[id]_ | 🔴 Critical | _[specific fix to apply]_ | ⬜ To Do / ✅ Already Implemented |
| _[id]_ | 🟠 High | _[specific fix]_ | ⬜ / ✅ |
| _[id]_ | 🟠 High | _[specific fix]_ | ⬜ / ✅ |
| _[id]_ | 🟡 Medium | _[specific fix]_ | ⬜ / ✅ |
| ... | ... | ... | ... |
