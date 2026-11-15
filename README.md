# Hospital Appointment Management System

A console-based Java application for managing outpatient appointments, patient records, clinical triage prioritization, and persistent consultation histories using the **Java Collections Framework** and **Object Serialization**.

---

## Overview

This project models core hospital scheduling workflows without external database dependencies by pairing complementary Java data structures with custom comparators and fail-safe iterators:

- **Patient Directory (`HashMap<String, Patient>`):** Provides average $O(1)$ patient lookup by ID, demographic management, and chronological consultation history tracking.
- **Appointment Schedule (`LinkedList<Appointment>`):** Maintains bookings in chronological order with $O(1)$ node insertion/deletion at iterator positions.
- **Triage & Multi-Criteria Sorting (`Comparator`):** Supports sorting schedules by appointment timestamp (`BY_TIME`), clinical urgency (`BY_PRIORITY`), or combined triage order (`BY_PRIORITY_THEN_TIME`) on defensive copies without disturbing the master schedule.
- **Safe Traversal & In-Place Mutations (`Iterator` & `ListIterator`):** Handles doctor double-booking checks, chronological mid-list insertions (`ListIterator.add`), priority escalations (`ListIterator.set`), consultation logging, reverse schedule traversal (`ListIterator.previous`), and cancellation/purging (`Iterator.remove`).
- **Disk Persistence (`Serializable`):** Saves and restores the entire appointment schedule, patient map, and ID sequence counter via binary object streams (`hospital_data.ser`).

---

## Project Structure

```text
.
├── HospitalAppointmentSystem.java   # Main driver, domain models, comparators, and manager
├── hospital_data.ser                # Generated binary file storing serialized state (created at runtime)
└── README.md                        # Project documentation
```

### Key Classes in `HospitalAppointmentSystem.java`

| Class / Enum | Responsibility |
| :--- | :--- |
| `HospitalAppointmentSystem` | Entry point demonstrating patient registration, conflict validation, sorting, iterator updates, serialization round-trip, and collection evaluation. |
| `AppointmentManager` | Core controller managing the `LinkedList` schedule, `HashMap` patient registry, iterator operations, and file I/O. |
| `Patient` | Serializable entity storing patient ID, name, age, phone number, and an `ArrayList<String>` of consultation records. |
| `Appointment` | Serializable entity tracking appointment ID, patient ID, doctor name, scheduled `Date`, `Priority`, `Status`, and consultation notes. |
| `AppointmentComparators` | Defines `BY_TIME`, `BY_PRIORITY`, and `BY_PRIORITY_THEN_TIME` comparators. |
| `Priority` & `Status` | Enums representing clinical triage levels (`EMERGENCY`, `URGENT`, `ROUTINE`) and booking states (`SCHEDULED`, `COMPLETED`, `CANCELLED`). |

---

## Getting Started

### Prerequisites
- **Java Development Kit (JDK):** JDK 8 or higher installed and configured on your system `PATH`.

### Compilation & Execution

1. **Clone the repository:**
   ```bash
   git clone https://github.com/username/hospital-appointment-system.git
   cd hospital-appointment-system
   ```

2. **Compile the source code:**
   ```bash
   javac HospitalAppointmentSystem.java
   ```

3. **Run the application:**
   ```bash
   java HospitalAppointmentSystem
   ```

---

## Features Demonstrated at Runtime

1. **Patient Lookup (`HashMap`):** Registers sample patients (`P001`–`P004`) and retrieves patient records in $O(1)$ time.
2. **Conflict Detection & Chronological Insertion:** Rejects double-booked doctor time slots and unknown patient IDs, inserting valid bookings into sorted time order using `ListIterator`.
3. **Comparator Sorting:** Displays schedules sorted strictly by time as well as prioritized by clinical severity (`EMERGENCY` $\rightarrow$ `URGENT` $\rightarrow$ `ROUTINE`).
4. **Iterator Mutations:**
   - Escalates patient priority (`ListIterator.set`).
   - Reschedules existing appointments by removing and re-inserting in sorted order.
   - Records consultation notes and appends summaries to the patient's medical history.
   - Traverses the schedule in reverse using `ListIterator.hasPrevious()`.
   - Cancels individual appointments and purges inactive records via `Iterator.remove()`.
5. **Serialization Round-Trip:** Writes application state to `hospital_data.ser` using `ObjectOutputStream` and verifies full restoration using `ObjectInputStream`.

---

## Collection Suitability Summary

| Collection | Time Complexity (Key Ops) | Suitability in This System |
| :--- | :--- | :--- |
| **`LinkedList`** | $O(1)$ iterator add/remove; $O(n)$ search | **Selected for Schedule:** Efficient mid-list chronological insertion, rescheduling, and bulk purging without element shifting. |
| **`HashMap`** | $O(1)$ avg get/put | **Selected for Patients:** Fast ID-based retrieval of patient profiles and consultation histories. |
| **`ArrayList`** | $O(1)$ random access; $O(n)$ mid-insert | **Used for Patient History:** Compact append-only storage for per-patient consultation notes. |
| **`TreeMap` / `TreeSet`** | $O(\log n)$ sorted operations | Strong alternative if appointments are indexed by a unique composite `(Date, Doctor)` key. |
| **`PriorityQueue`** | $O(\log n)$ `poll()`; $O(n)$ arbitrary remove | Best suited for a dedicated emergency walk-in triage queue rather than a full timetable. |
| **`Vector` / `Hashtable`** | Synchronized (legacy overhead) | Avoided in favor of modern collections. |

---
