import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Hospital Appointment Management System
 *
 * LinkedList<Appointment> -> ordered appointment schedule
 * HashMap<String,Patient> -> O(1) patient lookup by ID
 * Comparator -> sort by time / priority
 * Iterator, ListIterator -> traverse, remove, update, insert
 * Serialization -> persist patients + appointments
 *
 * Compile: javac HospitalAppointmentSystem.java
 * Run: java HospitalAppointmentSystem
 */
public class HospitalAppointmentSystem {

    static Date t(int day, int hour, int min) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2026, Calendar.NOVEMBER, day, hour, min, 0);
        return c.getTime();
    }

    public static void main(String[] args) throws Exception {
        AppointmentManager mgr = new AppointmentManager();

        // 1. Register patients (HashMap)
        mgr.registerPatient(new Patient("P001", "Ravi Kumar", 45, "9876500001"));
        mgr.registerPatient(new Patient("P002", "Anita Rao", 32, "9876500002"));
        mgr.registerPatient(new Patient("P003", "Suresh Babu", 67, "9876500003"));
        mgr.registerPatient(new Patient("P004", "Meena Iyer", 28, "9876500004"));
        System.out.println("Lookup P003 -> " + mgr.findPatient("P003"));

        // 2. Book appointments (inserted in time order via ListIterator)
        mgr.book("P001", "Sharma", t(16, 11, 0), Priority.ROUTINE);
        mgr.book("P002", "Sharma", t(16, 9, 30), Priority.URGENT);
        mgr.book("P003", "Reddy", t(16, 10, 0), Priority.EMERGENCY);
        mgr.book("P004", "Reddy", t(16, 9, 0), Priority.ROUTINE);
        mgr.book("P001", "Sharma", t(16, 9, 30), Priority.ROUTINE); // conflict -> rejected
        mgr.book("P999", "Reddy", t(16, 12, 0), Priority.ROUTINE); // unknown patient -> rejected

        mgr.printAll("Schedule (insertion keeps time order)", mgr.getAppointments());

        // 3. Comparator-based sorting
        mgr.printAll("Sorted by TIME", mgr.sorted(AppointmentComparators.BY_TIME));
        mgr.printAll("Sorted by PRIORITY then TIME",
                mgr.sorted(AppointmentComparators.BY_PRIORITY_THEN_TIME));

        // 4. Iterator / ListIterator updates
        mgr.escalatePatient("P004", Priority.URGENT); // ListIterator.set
        mgr.reschedule(1, t(16, 8, 30)); // remove + sorted re-insert
        mgr.completeConsultation(3, "Chest pain; ECG normal. Prescribed rest.");
        mgr.completeConsultation(2, "Viral fever. Paracetamol 500mg.");
        mgr.printAll("After escalate / reschedule / consultations", mgr.getAppointments());
        mgr.printReverse("Reverse traversal (ListIterator.hasPrevious)");

        System.out.println("\nCancelling #4 -> " + mgr.cancel(4)); // Iterator.remove
        System.out.println("Purged completed/cancelled: " + mgr.purgeInactive());
        mgr.printAll("Active schedule", mgr.getAppointments());

        // 5. Patient history lookup
        System.out.println("\nHistory of P003: " + mgr.findPatient("P003").getHistory());

        // 6. Serialization round-trip
        String file = "hospital_data.ser";
        mgr.save(file);
        System.out.println("\nSaved to " + file);

        AppointmentManager restored = new AppointmentManager();
        restored.load(file);
        restored.printAll("Restored from disk", restored.getAppointments());
        System.out.println("Restored patient P002 -> " + restored.findPatient("P002"));

        // 7. Collection evaluation
        printEvaluation();
    }

    static void printEvaluation() {
        System.out.println("\n================ COLLECTION SUITABILITY EVALUATION ================");
        String[][] rows = {
                { "Collection", "Strengths", "Weaknesses", "Verdict for this app" },
                { "LinkedList", "O(1) insert/remove at iterator position;",
                        "O(n) get(i) / search; more memory per node", "GOOD: schedule is traversed & edited" },
                { "", "ListIterator add/set/remove; deque ops", "poor cache locality",
                        "via iterators (cancel, reschedule)" },
                { "ArrayList", "O(1) random access; compact; fast sort", "O(n) mid insert/remove (shifting)",
                        "OK for read-heavy / reporting views" },
                { "HashMap", "O(1) avg lookup by key", "unordered; not thread-safe", "BEST for patient ID -> Patient" },
                { "TreeMap/TreeSet", "Always sorted, O(log n) ops", "Needs unique ordering key; no duplicates",
                        "Strong alt. if sorted-by-time only" },
                { "PriorityQueue", "O(log n) poll of most urgent", "No ordered iteration; O(n) remove(x)",
                        "Great for 'next emergency' triage queue" },
                { "LinkedHashMap", "HashMap + insertion order", "extra memory",
                        "Useful if patient listing order matters" },
                { "Vector/Hashtable", "Synchronized (legacy)", "Slow, obsolete",
                        "AVOID; use Collections.synchronizedXxx" },
        };
        for (int i = 0; i < rows.length; i++) {
            String[] r = rows[i];
            System.out.println(pad(r[0], 17) + "| " + pad(r[1], 44) + "| "
                    + pad(r[2], 42) + "| " + r[3]);
        }
        System.out.println();
    }

    static String pad(String s, int width) {
        StringBuffer sb = new StringBuffer(s);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }
}

// ===================== MODEL CLASSES =====================

enum Priority {
    EMERGENCY(1), URGENT(2), ROUTINE(3);

    private final int level;

    Priority(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}

enum Status {
    SCHEDULED, COMPLETED, CANCELLED
}

class Patient implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private String name;
    private int age;
    private String phone;
    private final List<String> history = new ArrayList<String>(); // consultation records

    Patient(String id, String name, int age, String phone) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.phone = phone;
    }

    String getId() {
        return id;
    }

    String getName() {
        return name;
    }

    int getAge() {
        return age;
    }

    String getPhone() {
        return phone;
    }

    List<String> getHistory() {
        return history;
    }

    void addHistory(String record) {
        history.add(record);
    }

    public String toString() {
        return "Patient[" + id + ", " + name + ", age " + age + ", ph " + phone + "]";
    }
}

class Appointment implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int appointmentId;
    private final String patientId;
    private final String doctor;
    private Date time;
    private Priority priority;
    private Status status = Status.SCHEDULED;
    private String consultationNotes = "";

    Appointment(int appointmentId, String patientId, String doctor,
            Date time, Priority priority) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.doctor = doctor;
        this.time = time;
        this.priority = priority;
    }

    int getAppointmentId() {
        return appointmentId;
    }

    String getPatientId() {
        return patientId;
    }

    String getDoctor() {
        return doctor;
    }

    Date getTime() {
        return time;
    }

    Priority getPriority() {
        return priority;
    }

    Status getStatus() {
        return status;
    }

    String getConsultationNotes() {
        return consultationNotes;
    }

    void setTime(Date time) {
        this.time = time;
    }

    void setPriority(Priority priority) {
        this.priority = priority;
    }

    void setStatus(Status status) {
        this.status = status;
    }

    void setConsultationNotes(String notes) {
        this.consultationNotes = notes;
    }

    public String toString() {
        SimpleDateFormat fmt = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
        return "#" + appointmentId + " | " + patientId + " | Dr." + doctor
                + " | " + fmt.format(time) + " | " + priority + " | " + status
                + (consultationNotes.length() == 0 ? "" : " | Notes: " + consultationNotes);
    }
}

// ===================== COMPARATORS =====================

class AppointmentComparators {

    /** Earliest appointment first. */
    static final Comparator<Appointment> BY_TIME = new Comparator<Appointment>() {
        public int compare(Appointment a, Appointment b) {
            return a.getTime().compareTo(b.getTime());
        }
    };

    /** Most urgent first (EMERGENCY, URGENT, ROUTINE). */
    static final Comparator<Appointment> BY_PRIORITY = new Comparator<Appointment>() {
        public int compare(Appointment a, Appointment b) {
            return a.getPriority().getLevel() - b.getPriority().getLevel();
        }
    };

    /** Priority first; ties broken by time. */
    static final Comparator<Appointment> BY_PRIORITY_THEN_TIME = new Comparator<Appointment>() {
        public int compare(Appointment a, Appointment b) {
            int result = BY_PRIORITY.compare(a, b);
            if (result != 0) {
                return result;
            }
            return BY_TIME.compare(a, b);
        }
    };
}

// ===================== MANAGER =====================

class AppointmentManager {
    private LinkedList<Appointment> appointments = new LinkedList<Appointment>();
    private HashMap<String, Patient> patients = new HashMap<String, Patient>();
    private int nextAppointmentId = 1;

    // ---------- Patients (HashMap) ----------

    boolean registerPatient(Patient p) {
        if (patients.containsKey(p.getId())) {
            return false;
        }
        patients.put(p.getId(), p);
        return true;
    }

    Patient findPatient(String id) {
        return patients.get(id); // O(1)
    }

    // ---------- Booking (LinkedList + ListIterator) ----------

    /**
     * Books an appointment and inserts it at the correct chronological position
     * using ListIterator.add(). Rejects unknown patients and doctor double-booking.
     */
    Appointment book(String patientId, String doctor, Date time, Priority priority) {
        if (!patients.containsKey(patientId)) {
            System.out.println("  ! Unknown patient: " + patientId);
            return null;
        }
        // conflict check via Iterator
        Iterator<Appointment> it = appointments.iterator();
        while (it.hasNext()) {
            Appointment a = it.next();
            if (a.getStatus() == Status.SCHEDULED
                    && a.getDoctor().equals(doctor) && a.getTime().equals(time)) {
                System.out.println("  ! Dr." + doctor + " already booked at " + time);
                return null;
            }
        }
        Appointment appt = new Appointment(nextAppointmentId++, patientId, doctor, time, priority);
        insertSorted(appt);
        return appt;
    }

    /**
     * Inserts into the list at the chronological position using ListIterator.add().
     */
    private void insertSorted(Appointment appt) {
        ListIterator<Appointment> li = appointments.listIterator();
        while (li.hasNext()) {
            if (li.next().getTime().after(appt.getTime())) {
                li.previous(); // step back so we insert BEFORE the later one
                break;
            }
        }
        li.add(appt);
    }

    // ---------- Update / delete using Iterator & ListIterator ----------

    /** Cancel via Iterator.remove() - the safe way to remove while traversing. */
    boolean cancel(int appointmentId) {
        Iterator<Appointment> it = appointments.iterator();
        while (it.hasNext()) {
            if (it.next().getAppointmentId() == appointmentId) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    /** Remove all completed/cancelled records (bulk cleanup with Iterator). */
    int purgeInactive() {
        int count = 0;
        Iterator<Appointment> it = appointments.iterator();
        while (it.hasNext()) {
            if (it.next().getStatus() != Status.SCHEDULED) {
                it.remove();
                count++;
            }
        }
        return count;
    }

    /**
     * Record consultation: updates the appointment (ListIterator.set) and patient
     * history.
     */
    boolean completeConsultation(int appointmentId, String notes) {
        ListIterator<Appointment> li = appointments.listIterator();
        while (li.hasNext()) {
            Appointment a = li.next();
            if (a.getAppointmentId() == appointmentId) {
                a.setStatus(Status.COMPLETED);
                a.setConsultationNotes(notes);
                li.set(a);
                Patient p = patients.get(a.getPatientId());
                if (p != null) {
                    SimpleDateFormat fmt = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
                    p.addHistory(fmt.format(a.getTime()) + " (Dr." + a.getDoctor() + "): " + notes);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Reschedule: remove the old entry, re-insert at the right chronological spot.
     */
    boolean reschedule(int appointmentId, Date newTime) {
        Appointment target = null;
        ListIterator<Appointment> li = appointments.listIterator();
        while (li.hasNext()) {
            Appointment a = li.next();
            if (a.getAppointmentId() == appointmentId) {
                target = a;
                li.remove();
                break;
            }
        }
        if (target == null) {
            return false;
        }
        target.setTime(newTime);
        insertSorted(target);
        return true;
    }

    /** Escalate priority of every SCHEDULED appointment of a patient. */
    void escalatePatient(String patientId, Priority newPriority) {
        ListIterator<Appointment> li = appointments.listIterator();
        while (li.hasNext()) {
            Appointment a = li.next();
            if (a.getPatientId().equals(patientId) && a.getStatus() == Status.SCHEDULED) {
                a.setPriority(newPriority);
                li.set(a);
            }
        }
    }

    // ---------- Sorting (Comparator) ----------

    List<Appointment> sorted(Comparator<Appointment> cmp) {
        List<Appointment> copy = new LinkedList<Appointment>(appointments); // master order intact
        Collections.sort(copy, cmp);
        return copy;
    }

    // ---------- Display ----------

    void printAll(String title, Collection<Appointment> list) {
        System.out.println("\n--- " + title + " ---");
        if (list.isEmpty()) {
            System.out.println("  (none)");
            return;
        }
        Iterator<Appointment> it = list.iterator();
        while (it.hasNext()) {
            Appointment a = it.next();
            Patient p = patients.get(a.getPatientId());
            System.out.println("  " + a + "  [" + (p == null ? "?" : p.getName()) + "]");
        }
    }

    /** Reverse traversal - a ListIterator-only capability. */
    void printReverse(String title) {
        System.out.println("\n--- " + title + " ---");
        ListIterator<Appointment> li = appointments.listIterator(appointments.size());
        while (li.hasPrevious()) {
            System.out.println("  " + li.previous());
        }
    }

    List<Appointment> getAppointments() {
        return appointments;
    }

    // ---------- Serialization ----------

    void save(String file) throws IOException {
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(file));
            out.writeObject(appointments);
            out.writeObject(patients);
            out.writeInt(nextAppointmentId);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    void load(String file) throws IOException, ClassNotFoundException {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream(file));
            appointments = (LinkedList<Appointment>) in.readObject();
            patients = (HashMap<String, Patient>) in.readObject();
            nextAppointmentId = in.readInt();
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
