# Coffeetaria – Phase 1 Deliverable

**Course:** Desenvolvimento de Software Seguro (DESOFS) 2025/2026  
**Class:** wed_ffs | **Team:** 3 | **Repository:** desofs2026_wed_ffs_3  

**Team Members:**
- Leonardo Costa, 1250532
- Henrique Dias, 1201816
- Luís Santos, 1250534
- Lourenço Mendes, 1201270
- Nuno Oliveira, 1210939

---

## System Description

Coffeetaria is a cafeteria management backend system (REST API + relational database) that enables clients to browse daily menus and place orders, employees to fulfill orders, and administrators to manage the full system. The system also performs OS-level operations such as generating reports and writing audit logs to the filesystem.

---

## Document Index

| Document | Description |
|----------|-------------|
| [Analysis & Requirements](./analysis.md) | Functional requirements, non-functional requirements, secure development requirements, and abuse cases |
| [Design](./design.md) | System architecture, domain model, DFDs (Level 0 & 1), secure design decisions |
| [Threat Analysis](./threat_analysis.md) | STRIDE threat identification per DFD element, risk assessment, and mitigations |
| [Security Testing Plan](./security_testing.md) | Security testing methodology, test cases linked to threats and requirements, ASVS checklist |

---

## Phase 1 Checklist

### Analysis / Requirements
- [x] Functional and non-functional requirements → [`analysis.md`](./analysis.md)
- [x] Secure development requirements → [`analysis.md §3`](./analysis.md#3-secure-development-requirements)
- [x] Abuse cases → [`analysis.md §4`](./analysis.md#4-abuse-cases)

### Design
- [x] System overview & domain model → [`design.md §1-2`](./design.md)
- [x] Data Flow Diagram – Level 0 → [`design.md §3.1`](./design.md)
- [x] Data Flow Diagram – Level 1 → [`design.md §3.2`](./design.md)
- [x] Data Flow Diagram – Level 2 (P5 Reporting) → [`design.md §3.3`](./design.md)
- [x] Secure architecture decisions → [`design.md §4`](./design.md)
- [x] Threat modeling (STRIDE per DFD element) → [`threat_analysis.md §1`](./threat_analysis.md)
- [x] Risk assessment → [`threat_analysis.md §2`](./threat_analysis.md)
- [x] Mitigations → [`threat_analysis.md §3`](./threat_analysis.md)
- [x] Security testing plan → [`security_testing.md`](./security_testing.md)
- [x] ASVS checklist → [`security_testing.md §3`](./security_testing.md)
