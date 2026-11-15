# Hospital Appointment Management System

A console-based Java application for managing patient records, outpatient appointment schedules, clinical triage prioritization, and persistent consultation histories using the **Java Collections Framework** and **Object Serialization**.

## Key Features & Data Structures

* **Patient Lookup (`HashMap<String, Patient>`):** Stores patient demographics and consultation histories (`ArrayList<String>`) with average $O(1)$ retrieval by patient ID.
* **Chronological Schedule (`LinkedList<Appointment>`):** Maintains bookings in sorted time order with $O(1)$ node insertion/deletion at iterator positions.
* **Triage Sorting (`Comparator`):** Sorts schedule views by timestamp (`BY_TIME`), urgency (`BY_PRIORITY`), or combined triage order (`BY_PRIORITY_THEN_TIME`) without altering the master schedule.
* **Safe Updates & Traversal (`Iterator` & `ListIterator`):** Performs doctor conflict validation, chronological insertion (`add`), priority escalation and consultation logging (`set`), reverse traversal (`hasPrevious`), and cancellation/purging (`remove`).
* **Disk Persistence (`Serializable`):** Saves and restores the appointment schedule, patient map, and ID counter via `hospital_data.ser`.

## Project Structure

```text
.
├── HospitalAppointmentSystem.java   # Driver, domain models, comparators, and AppointmentManager
├── hospital_data.ser                # Serialized binary state (generated at runtime)
└── README.md                        # Project documentation
```

## Quick Start

**Prerequisites:** JDK 8 or higher.

```bash
# Clone the repository
git clone https://github.com/username/hospital-appointment-system.git
cd hospital-appointment-system

# Compile and run
javac HospitalAppointmentSystem.java
java HospitalAppointmentSystem
```

## Collection Suitability Overview

| Collection | Key Complexity | Role / Verdict |
| :--- | :--- | :--- |
| **`LinkedList`** | $O(1)$ iterator add/remove | **Schedule:** Ideal for frequent mid-list chronological insertion and cancellation. |
| **`HashMap`** | $O(1)$ avg lookup | **Patients:** Optimal for instant ID-based patient and history retrieval. |
| **`ArrayList`** | $O(1)$ random access | **Consultation Notes:** Compact append-only storage per patient. |
| **`TreeMap`** | $O(\log n)$ sorted ops | Good alternative if indexed by a unique `(Date, Doctor)` composite key. |
| **`PriorityQueue`** | $O(\log n)$ `poll()` | Best suited for a dedicated emergency walk-in triage queue. |
| **`Vector` / `Hashtable`** | Synchronized overhead | Avoided; legacy collections. |

## Academic Details

* **Student(s):** `[Your Name(s) & Roll Number(s)]`
* **Faculty Guide:** `[Faculty Name]`
* **Course:** `[Course Code & Name]`