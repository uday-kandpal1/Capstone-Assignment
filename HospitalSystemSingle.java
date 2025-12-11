import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
public class HospitalSystemSingle {

    /* ----------------------------- ADTs ----------------------------- */
    public static class Patient {
        public final int id;
        public String name;
        public int age;
        public int severity;

        public Patient(int id, String name, int age, int severity) {
            this.id = id; this.name = name; this.age = age; this.severity = severity;
        }
        public String toString() {
            return String.format("Patient[id=%d,name=%s,age=%d,severity=%d]", id, name, age, severity);
        }
    }
    public static class Token {
        public final int tokenId;
        public final int patientId;
        public final int doctorId;
        public final int slotId;
        public final TokenType type;
        public Token(int tokenId, int patientId, int doctorId, int slotId, TokenType type) {
            this.tokenId = tokenId; this.patientId = patientId; this.doctorId = doctorId; this.slotId = slotId; 
            this.type = type;
        }
        public String toString() {
            return String.format("Token[id=%d,pid=%d,doc=%d,slot=%d,type=%s]", 
            tokenId, patientId, doctorId, slotId, type);
        }
        public enum TokenType { ROUTINE, EMERGENCY }
    }

    public static class Slot {
        public final int slotId;
        public final String startTime;
        public final String endTime;
        public boolean booked;
        public Slot(int slotId, String startTime, String endTime) {
            this.slotId = slotId; this.startTime = startTime; this.endTime = endTime; this.booked = false;
        }
        public String toString() {
            return String.format("Slot[id=%d,%s-%s,booked=%b]", slotId, startTime, endTime, booked);
        }
    }
    /* Doctor implemented with an internal singly linked list for slots */
    public static class Doctor {
        public final int id;
        public final String name;
        public final String specialization;
        private SlotNode head;
        private SlotNode tail; // maintain tail for O(1) append
        private int slotCount = 0;
        private static class SlotNode { Slot slot; SlotNode next; SlotNode(Slot s){ slot = s; next = null; } }

        public Doctor(int id, String name, String specialization) {
            this.id = id; this.name = name; this.specialization = specialization; head = null; tail = null;
        }
        // add slot at tail — O(1) thanks to tail
        public void addSlot(Slot s) {
            SlotNode node = new SlotNode(s);
            if (head == null) { head = tail = node; }
            else { tail.next = node; tail = node; }
            slotCount++;
        }
        // cancel (remove) a slot by slotId — O(k) worst-case
        public boolean cancelSlot(int slotId) {
            SlotNode cur = head, prev = null;
            while (cur != null) {
                if (cur.slot.slotId == slotId) {
                    if (prev == null) head = cur.next;
                    else prev.next = cur.next;
                    if (cur == tail) tail = prev;
                    slotCount--;
                    return true;
                }
                prev = cur; cur = cur.next;
            }
            return false;
        }

        // find next free slot — O(k)
        public Slot findNextFreeSlot() {
            SlotNode cur = head;
            while (cur != null) {
                if (!cur.slot.booked) return cur.slot;
                cur = cur.next;
            }
            return null;
        }

        // mark slot as booked by slotId
        public boolean bookSlot(int slotId) {
            SlotNode cur = head;
            while (cur != null) {
                if (cur.slot.slotId == slotId && !cur.slot.booked) {
                    cur.slot.booked = true; return true;
                }
                cur = cur.next;
            }
            return false;
        }

        public List<Slot> getAllSlots() {
            List<Slot> out = new ArrayList<>();
            SlotNode cur = head;
            while (cur != null) { out.add(cur.slot); cur = cur.next; }
            return out;
        }

        public int pendingCount() {
            int c = 0; SlotNode cur = head;
            while (cur != null) { if (!cur.slot.booked) c++; cur = cur.next; }
            return c;
        }

        public String toString() {
            return String.format("Doctor[id=%d,name=%s,spec=%s,pendingSlots=%d]", id, name, specialization, pendingCount());
        }
    }

    /* ----------------------------- Circular Queue ----------------------------- */
    public static class CircularQueue {
        private final Token[] data;
        private int head = 0, tail = 0, size = 0, capacity;

        public CircularQueue(int capacity) {
            this.capacity = capacity;
            this.data = new Token[capacity];
        }
        public boolean isEmpty() { return size == 0; }
        public boolean isFull() { return size == capacity; }
        public int size() { return size; }
        public boolean enqueue(Token t) {
            if (isFull()) return false;
            data[tail] = t; tail = (tail + 1) % capacity; size++; return true;
        }

        public Token dequeue() {
            if (isEmpty()) return null;
            Token t = data[head]; data[head] = null; head = (head + 1) % capacity; size--; return t;
        }

        public Token peek() { return isEmpty() ? null : data[head]; }
    }

    /* ----------------------------- Min Heap (Triage) ----------------------------- */
    public static class MinHeapTriage {
        private final ArrayList<Token> heap = new ArrayList<>();
        private void swap(int i, int j) {
            Token t = heap.get(i); heap.set(i, heap.get(j)); heap.set(j, t);
        }
        private void heapifyUp(int idx, Comparator<Token> cmp) {
            while (idx > 0) {
                int parent = (idx - 1) / 2;
                if (cmp.compare(heap.get(idx), heap.get(parent)) < 0) { swap(idx, parent); idx = parent; }
                else break;
            }
        }
        private void heapifyDown(int idx, Comparator<Token> cmp) {
            int n = heap.size();
            while (true) {
                int left = 2 * idx + 1, right = left + 1, smallest = idx;
                if (left < n && cmp.compare(heap.get(left), heap.get(smallest)) < 0) smallest = left;
                if (right < n && cmp.compare(heap.get(right), heap.get(smallest)) < 0) smallest = right;
                if (smallest != idx) { swap(idx, smallest); idx = smallest; }
                else break;
            }
        }
        public boolean isEmpty() { return heap.isEmpty(); }
        public int size() { return heap.size(); }
        public void insert(Token t, Comparator<Token> cmp) {
            heap.add(t); heapifyUp(heap.size() - 1, cmp);
        }
        public Token extractMin(Comparator<Token> cmp) {
            if (heap.isEmpty()) return null;
            Token root = heap.get(0);
            Token last = heap.remove(heap.size() - 1);
            if (!heap.isEmpty()) { heap.set(0, last); heapifyDown(0, cmp); }
            return root;
        }

        public Token peek(Comparator<Token> cmp) { return heap.isEmpty() ? null : heap.get(0); }
    }

    /* ----------------------------- Patient Hash Table (chaining) ----------------------------- */
    public static class PatientHashTable {
        private static class Entry { final int key; Patient value; Entry(int k, Patient v){ key=k; value=v; } }
        private final LinkedList<Entry>[] buckets;
        private final int capacity;

        public PatientHashTable(int capacity) {
            this.capacity = capacity;
            buckets = new LinkedList[capacity];
            for (int i=0;i<capacity;i++) buckets[i] = new LinkedList<>();
        }

        private int hash(int key) { return Math.abs(key) % capacity; }

        public void upsert(Patient p) {
            int idx = hash(p.id);
            for (Entry e : buckets[idx]) if (e.key == p.id) { e.value = p; return; }
            buckets[idx].add(new Entry(p.id, p));
        }

        public Patient get(int patientId) {
            int idx = hash(patientId);
            for (Entry e : buckets[idx]) if (e.key == patientId) return e.value;
            return null;
        }

        public boolean delete(int patientId) {
            int idx = hash(patientId);
            Entry toRemove = null;
            for (Entry e : buckets[idx]) if (e.key == patientId) { toRemove = e; break; }
            if (toRemove != null) { buckets[idx].remove(toRemove); return true; }
            return false;
        }
    }
    /* ----------------------------- Undo Stack ----------------------------- */
    public static class UndoStack {
        public static class Action {
            public final String actionType;
            public final Object payload;
            public Action(String actionType, Object payload) { this.actionType = actionType; this.payload = payload; }
        }private final Stack<Action> st = new Stack<>();
        public void push(String type, Object payload) { st.push(new Action(type, payload)); }
        public Action pop() { return st.isEmpty() ? null : st.pop(); }
        public boolean isEmpty() { return st.isEmpty(); }
    }
    /* ----------------------------- HospitalSystem (glue) ----------------------------- */
    private final Map<Integer, Doctor> doctors = new HashMap<>();
    private final PatientHashTable patients;
    private final CircularQueue routineQueue;
    private final MinHeapTriage triage;
    private final UndoStack undo;
    private final AtomicInteger tokenCounter = new AtomicInteger(1);
    private int servedCount = 0;
    private int pendingCount = 0;
    public HospitalSystemSingle(int patientTableSize, int routineQueueCapacity) {
        patients = new PatientHashTable(patientTableSize);
        routineQueue = new CircularQueue(routineQueueCapacity);
        triage = new MinHeapTriage();
        undo = new UndoStack();
    }

    public void upsertPatient(Patient p) {
        patients.upsert(p);
        undo.push("register", p.id); // payload patient id
    }
    public Patient getPatient(int id) { return patients.get(id); }
    public void addDoctor(Doctor d) { doctors.put(d.id, d); }

    /* Booking routine appointment: find next free slot and enqueue token */
    public boolean bookRoutine(int patientId, int doctorId) {
        Doctor doc = doctors.get(doctorId);
        if (doc == null) return false;
        Slot s = doc.findNextFreeSlot();
        if (s == null) return false;
        boolean ok = doc.bookSlot(s.slotId);
        if (!ok) return false;
        Token t = new Token(tokenCounter.getAndIncrement(), patientId, doctorId, s.slotId, Token.TokenType.ROUTINE);
        boolean enq = routineQueue.enqueue(t);
        if (!enq) { s.booked = false; return false; } // rollback if queue full
        pendingCount++;
        undo.push("book", t);
        return true;
    }
    /* Emergency triage insertion */
    public void triageInsert(int patientId) {
        Patient p = patients.get(patientId);
        if (p == null) { System.out.println("Patient not found."); return; }
        Token t = new Token(tokenCounter.getAndIncrement(), patientId, -1, -1, Token.TokenType.EMERGENCY);
        Comparator<Token> cmp = this::cmpByPatientSeverity;
        triage.insert(t, cmp);
        pendingCount++;
        undo.push("triage", t);
    }

    /* Serve next: emergency (triage) has priority over routine */
    public Token serveNext() {
        Comparator<Token> cmp = this::cmpByPatientSeverity;
        Token next = triage.peek(cmp) != null ? triage.extractMin(cmp) : routineQueue.dequeue();
        if (next == null) return null;
        servedCount++; pendingCount = Math.max(0, pendingCount - 1);
        undo.push("serve", next);
        return next;
    }
    private int cmpByPatientSeverity(Token a, Token b) {
        Patient pa = patients.get(a.patientId); Patient pb = patients.get(b.patientId);
        int sa = pa == null ? Integer.MAX_VALUE : pa.severity;
        int sb = pb == null ? Integer.MAX_VALUE : pb.severity;
        return Integer.compare(sa, sb);
    }
    /* Undo: best-effort reversals */
    public String undo() {
        UndoStack.Action act = undo.pop();
        if (act == null) return "Nothing to undo";
        switch (act.actionType) {
            case "book": {
                Token t = (Token) act.payload;
                rebuildQueueWithoutToken(t.tokenId);
                Doctor d = doctors.get(t.doctorId);
                if (d != null) for (Slot s : d.getAllSlots()) if (s.slotId == t.slotId) s.booked = false;
                pendingCount = Math.max(0, pendingCount - 1);
                return "Undid booking " + t.tokenId;
            }
            case "triage": {
                Token tk = (Token) act.payload;
                rebuildTriageWithoutToken(tk.tokenId);
                pendingCount = Math.max(0, pendingCount - 1);
                return "Undid triage " + tk.tokenId;
            }
            case "serve": {
                Token served = (Token) act.payload;
                if (served.type == Token.TokenType.ROUTINE) rebuildQueueWithFront(served);
                else { Comparator<Token> cmp = this::cmpByPatientSeverity; triage.insert(served, cmp); }
                servedCount = Math.max(0, servedCount - 1);
                pendingCount++;
                return "Undid serve " + served.tokenId;
            }
            case "register": {
                int pid = (int) act.payload;
                patients.delete(pid);
                return "Undid patient register " + pid;
            }
            default: return "Unknown action to undo";
        }}
    /* rebuild helpers (simple, destructive reads + rebuild) */
    private void rebuildQueueWithoutToken(int tokenId) {
        List<Token> items = new ArrayList<>();
        while (!routineQueue.isEmpty()) {
            Token t = routineQueue.dequeue();
            if (t.tokenId != tokenId) items.add(t);
        }
        for (Token t : items) routineQueue.enqueue(t);
    }
    private void rebuildTriageWithoutToken(int tokenId) {
        List<Token> items = new ArrayList<>();
        Comparator<Token> cmp = this::cmpByPatientSeverity;
        Token cur;
        while ((cur = triage.extractMin(cmp)) != null) {
            if (cur.tokenId != tokenId) items.add(cur);
        }
        for (Token t : items) triage.insert(t, cmp);
    }

    private void rebuildQueueWithFront(Token token) {
        List<Token> items = new ArrayList<>();
        while (!routineQueue.isEmpty()) items.add(routineQueue.dequeue());
        routineQueue.enqueue(token);
        for (Token t : items) routineQueue.enqueue(t);
    }

    /* Reports */
    public String reportSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== SUMMARY ===\n");
        sb.append("Served: ").append(servedCount).append("\n");
        sb.append("Pending: ").append(pendingCount).append("\n");
        sb.append("Doctors:\n");
        for (Doctor d : doctors.values()) {
            sb.append(d).append("\n");
            sb.append("  NextSlot: ").append(d.findNextFreeSlot()).append("\n");
            sb.append("  PendingSlots: ").append(d.pendingCount()).append("\n");
        }
        return sb.toString();
    }

    public List<Integer> topKFrequentPatients(int K) {
        Map<Integer,Integer> freq = new HashMap<>();
        // scan routine queue destructively then rebuild
        List<Token> tmp = new ArrayList<>();
        while (!routineQueue.isEmpty()) {
            Token t = routineQueue.dequeue();
            freq.put(t.patientId, freq.getOrDefault(t.patientId, 0) + 1);
            tmp.add(t);
        }
        for (Token t : tmp) routineQueue.enqueue(t);

        // triage: drain and rebuild
        Comparator<Token> cmp = this::cmpByPatientSeverity;
        List<Token> tr = new ArrayList<>();
        Token cur;
        while ((cur = triage.extractMin(cmp)) != null) {
            freq.put(cur.patientId, freq.getOrDefault(cur.patientId, 0) + 1);
            tr.add(cur);
        }
        for (Token t : tr) triage.insert(t, cmp);

        PriorityQueue<Map.Entry<Integer,Integer>> pq = new PriorityQueue<>((a,b) -> Integer.compare(b.getValue(),
         a.getValue()));
        pq.addAll(freq.entrySet());
        List<Integer> out = new ArrayList<>();
        for (int i=0;i<K && !pq.isEmpty();i++) out.add(pq.poll().getKey());
        return out;
    }

    /* ----------------------------- CLI & demo ----------------------------- */
    private static void printMenu() {
        System.out.println("=== Hospital CLI ===");
        System.out.println("1. Register/Update Patient");
        System.out.println("2. Book Slot (Routine)");
        System.out.println("3. Serve Next");
        System.out.println("4. Emergency In (Triage)");
        System.out.println("5. Undo");
        System.out.println("6. Reports");
        System.out.println("7. Exit");
        System.out.print("Choose: ");
    }
    public static void main(String[] args) {
        HospitalSystemSingle hs = new HospitalSystemSingle(31, 20);
        // Seed doctors & slots
        Doctor d1 = new Doctor(1, "Dr. Rao", "General");
        d1.addSlot(new Slot(101, "09:00", "09:15"));
        d1.addSlot(new Slot(102, "09:15", "09:30"));
        d1.addSlot(new Slot(103, "09:30", "09:45"));
        hs.addDoctor(d1);
        Doctor d2 = new Doctor(2, "Dr. Mehta", "Pediatrics");
        d2.addSlot(new Slot(201, "09:00", "09:20"));
        d2.addSlot(new Slot(202, "09:20", "09:40"));
        hs.addDoctor(d2);
        // Seed patients
        hs.upsertPatient(new Patient(1, "Alice", 30, 5));
        hs.upsertPatient(new Patient(2, "Bob", 45, 2));
        hs.upsertPatient(new Patient(3, "Charlie", 10, 1));
        // Demo booking & triage
        System.out.println("Demo: Book Alice with Dr. Rao");
        boolean b1 = hs.bookRoutine(1, 1); System.out.println("Book success: " + b1);
        System.out.println("Demo: Emergency triage for Bob");
        hs.triageInsert(2);
        System.out.println("Serve next (should serve Bob due to triage): " + hs.serveNext());
        System.out.println("Serve next (should serve Alice): " + hs.serveNext());
        System.out.println(hs.reportSummary());
        // Interactive CLI
        Scanner sc = new Scanner(System.in);
        while (true) {
            printMenu();
            int choice = -1;
            try { choice = Integer.parseInt(sc.nextLine().trim()); } catch (Exception e) { choice = -1; }
            if (choice == 7) { System.out.println("Bye"); break; }
            switch (choice) {
                case 1:
                    try {
                        System.out.print("Patient id: "); int pid = Integer.parseInt(sc.nextLine().trim());
                        System.out.print("Name: "); String name = sc.nextLine().trim();
                        System.out.print("Age: "); int age = Integer.parseInt(sc.nextLine().trim());
                        System.out.print("Severity (lower => more urgent): "); int sev = Integer.parseInt(sc.nextLine().trim());
                        hs.upsertPatient(new Patient(pid, name, age, sev));
                        System.out.println("Registered/Updated: " + pid);
                    } catch (Exception ex) { System.out.println("Invalid input."); }
                    break;
                case 2:
                    try {
                        System.out.print("Patient id: "); int pid = Integer.parseInt(sc.nextLine().trim());
                        System.out.print("Doctor id: "); int did = Integer.parseInt(sc.nextLine().trim());
                        boolean ok = hs.bookRoutine(pid, did);
                        System.out.println(ok ? "Booked" : "Booking failed (no slot / queue full)");
                    } catch (Exception ex) { System.out.println("Invalid input."); }
                    break;
                case 3:
                    Token served = hs.serveNext();
                    System.out.println(served == null ? "No patients to serve" : "Served: " + served);
                    break;
                case 4:
                    try {
                        System.out.print("Patient id for triage: "); int pid = Integer.parseInt(sc.nextLine().trim());
                        hs.triageInsert(pid);
                        System.out.println("Inserted in emergency triage");
                    } catch (Exception ex) { System.out.println("Invalid input."); }
                    break;
                case 5:
                    System.out.println(hs.undo());
                    break;
                case 6:
                    System.out.println(hs.reportSummary());
                    System.out.println("Top 3 frequent patients: " + hs.topKFrequentPatients(3));
                    break;
                default:
                    System.out.println("Invalid choice");
            }
        }
        sc.close();
    }
}

    /* ----------------------------- Complexity Notes (summary) -----------------------------
     * Queue enqueue/dequeue: O(1) time, O(1) space per op
     * Heap insert/extract-min: O(log n) time, O(1) space per op; O(n) total space
     * Hash search/insert (average): O(1) time, O(1) space per op (O(m) table)
     * Linked list insert at tail: O(1) (tail maintained); delete: O(k)
     * Stack push/pop (undo): O(1)
     * Find next free slot: O(k)
     * Reports: O(k) per doctor; Top-K naive: O(n log K)
     * ----------------------------------------------------------------------------------*/
