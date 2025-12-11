Overview

This project implements a Hospital Appointment & Triage System using multiple core data structures, as required in Capstone Assignment 5.

The system models a realistic OPD (Outpatient Department) workflow, handling:

 Routine appointment bookings

 Emergency triage (severity-based priority)

 Doctor schedule management

 Patient registration & indexing

 Undo/rollback of recent actions

 Reports & analytics (served patients, pending counts, Top-K frequent patients)

All components are implemented from scratch using custom data structures (no Java Collections Queue/PriorityQueue for required structures).

 Key Data Structures Used
Feature	Data Structure	Purpose
Routine Appointments	Circular Queue	O(1) enqueue/dequeue
Emergency Triage	Min Heap	Severity-based priority
Doctor Schedules	Singly Linked List	Slot management
Patient Records	Hash Table (Chaining)	Fast lookup & updates
Undo Log	Stack	Rollback actions
Reports	Traversals/Count	Analytics
 System Architecture
Entities

Patient: {id, name, age, severity}

Token: {tokenId, patientId, doctorId, slotId, type}

Doctor: Has linked list of schedule slots

Slot: {slotId, startTime, endTime, booked}

Core Modules

Circular Queue — For routine tokens

MinHeapTriage — Emergency severity ordering

PatientHashTable — O(1) average lookup

Doctor LinkedList — Slot insertion, deletion

UndoStack — Undo "register", "book", "triage", "serve"

HospitalSystemSingle — Integrates all modules + CLI

] Running the Program
Compile
javac HospitalSystemSingle.java

Run
java HospitalSystemSingle

Optional: Enable Assertions for Manual Tests
java -ea HospitalSystemSingle

Testing

Two testing methods provided:

 JUnit Tests (Recommended)

File: HospitalSystemSingleTest.java

Tests cover:

patient insert/get

routine booking

emergency triage preemption

undo operations

serve logic

slot unbooking



 Time & Space Complexity
Circular Queue
Operation	Time	Space
enqueue / dequeue	O(1)	O(Q)
Min Heap
Operation	Time	Space
insert	O(log n)	O(n)
extract-min	O(log n)	O(n)
peek	O(1)	—
Hash Table (Chaining)
Operation	Avg Time	Worst	Space
search	O(1)	O(n)	O(m)
insert	O(1)	O(n)	O(m)
delete	O(1)	O(n)	O(m)
Linked List (Doctor Schedule)
Operation	Time
add slot	O(1)
cancel slot	O(k)
find next free slot	O(k)
Undo Stack

push(): O(1)

pop(): O(1)
