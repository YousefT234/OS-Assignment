// import java.util.*;


// class Process {
//     private final String name;
//     private final int arrivalTime;
//     private final int initialBurstTime;
//     private int burstTime;
//     private int priority;
//     private int quantum;
//     private List<Integer> quantumHistory;

//     private int waitingTime;
//     private int turnaroundTime;
//     private int completionTime;
//     private int startTime;
//     private int lastExecutionTime;

//     public Process(String name, int arrivalTime, int burstTime, int priority, int quantum) {
//         this.name = name;
//         this.arrivalTime = arrivalTime;
//         this.initialBurstTime = burstTime;
//         this.burstTime = burstTime;
//         this.priority = priority;
//         this.quantum = quantum;
//         this.quantumHistory = new ArrayList<>();
//         quantumHistory.add(quantum);
//         this.startTime = -1;
//         this.lastExecutionTime = 0;
//     }

//     public String getName() {
//         return name;
//     }

//     public int getArrivalTime() {
//         return arrivalTime;
//     }

//     public int getInitialBurstTime() {
//         return initialBurstTime;
//     }

//     public int getBurstTime() {
//         return burstTime;
//     }

//     public int getPriority() {
//         return priority;
//     }

//     public int getQuantum() {
//         return quantum;
//     }

//     public List<Integer> getQuantumHistory() {
//         return quantumHistory;
//     }

//     public int getWaitingTime() {
//         return waitingTime;
//     }

//     public int getTurnaroundTime() {
//         return turnaroundTime;
//     }

//     public int getCompletionTime() {
//         return completionTime;
//     }

//     public int getStartTime() {
//         return startTime;
//     }

//     public int getLastExecutionTime() {
//         return lastExecutionTime;
//     }

//     public void execute(int duration) {
//         if (startTime == -1) {
//             startTime = lastExecutionTime;
//         }
//         burstTime -= duration;
//         lastExecutionTime += duration;
//     }

//     public void setTurnaroundTime(int turnaroundTime) {
//         this.turnaroundTime = turnaroundTime;
//     }

//     public void setWaitingTime(int waitingTime) {
//         this.waitingTime = waitingTime;
//     }

//     public void setCompletionTime(int completionTime) {
//         this.completionTime = completionTime;
//     }

//     public void setLastExecutionTime(int lastExecutionTime) {
//         this.lastExecutionTime = lastExecutionTime;
//     }

//     public void setPriority(int priority) {
//         this.priority = priority;
//     }

//     public void setQuantum(int quantum) {
//         this.quantum = quantum;
//         quantumHistory.add(quantum);
//     }

//     public boolean isCompleted() {
//         return burstTime <= 0;
//     }

//     public void setStartTime(int startTime) {
//         this.startTime = startTime;
//     }

// }

// abstract class AbstractScheduler {

//     protected List<Process> initialProcesses;

//     public abstract void schedule();

//     protected final int contextSwitchTime;

//     public AbstractScheduler(List<Process> processes, int csTime) {
//         this.initialProcesses = deepCopyProcesses(processes);
//         this.contextSwitchTime = csTime;
//     }

//     protected int performContextSwitch(int currentTime) {
//         return currentTime + contextSwitchTime;
//     }

//     private List<Process> deepCopyProcesses(List<Process> original) {
//         List<Process> copy = new java.util.ArrayList<>();
//         for (Process p : original) {
//             copy.add(new Process(
//                     p.getName(),
//                     p.getArrivalTime(),
//                     p.getInitialBurstTime(),
//                     p.getPriority(),
//                     p.getQuantum()
//             ));
//         }
//         return copy;
//     }

//     public static void calculateMetrics(Process p, int completionTime) {
//         p.setCompletionTime(completionTime);
//         int turnaroundTime = completionTime - p.getArrivalTime();
//         p.setTurnaroundTime(turnaroundTime);
//         int waitingTime = turnaroundTime - p.getInitialBurstTime();
//         p.setWaitingTime(waitingTime);
//     }

//     public static double calculateAverageWaitingTime(List<Process> processes) {
//         double totalWaitingTime = 0;
//         for (Process p : processes) {
//             totalWaitingTime += p.getWaitingTime();
//         }
//         return totalWaitingTime / processes.size();
//     }

//     public static double calculateAverageTurnaroundTime(List<Process> processes) {
//         double totalTurnaroundTime = 0;
//         for (Process p : processes) {
//             totalTurnaroundTime += p.getTurnaroundTime();
//         }
//         return totalTurnaroundTime / processes.size();
//     }
// }

// class RRScheduler extends AbstractScheduler {

//     private final int timeQuantum;

//     public RRScheduler(List<Process> processes, int contextSwitchTime, int timeQuantum) {
//         super(processes, contextSwitchTime);
//         this.timeQuantum = timeQuantum;
//     }

//     @Override
//     public void schedule() {

//         List<Process> processes = initialProcesses;
//         processes.sort(Comparator.comparingInt(Process::getArrivalTime));

//         Queue<Process> readyQueue = new LinkedList<>();
//         List<String> executionOrder = new ArrayList<>();

//         int currentTime = 0;
//         int index = 0;
//         Process currentProcess = null;
//         Process lastProcess = null;

//         while (true) {


//             while (index < processes.size() &&
//                     processes.get(index).getArrivalTime() <= currentTime) {
//                 readyQueue.add(processes.get(index));
//                 index++;
//             }

//             if (currentProcess == null) {
//                 if (readyQueue.isEmpty()) {
//                     if (index == processes.size())
//                         break;
//                     currentTime = processes.get(index).getArrivalTime();
//                     continue;
//                 }

//                 currentProcess = readyQueue.poll();


//                 if (lastProcess != null) {
//                     currentTime = performContextSwitch(currentTime);
//                 }

//                 if (currentProcess.getStartTime() == -1)
//                     currentProcess.setStartTime(currentTime);
//             }

//             executionOrder.add(currentProcess.getName());

//             int execTime = Math.min(timeQuantum, currentProcess.getBurstTime());
//             currentProcess.execute(execTime);
//             currentTime += execTime;


//             while (index < processes.size() &&
//                     processes.get(index).getArrivalTime() <= currentTime) {
//                 readyQueue.add(processes.get(index));
//                 index++;
//             }


//             if (currentProcess.isCompleted()) {
//                 calculateMetrics(currentProcess, currentTime);
//                 lastProcess = currentProcess;
//                 currentProcess = null;
//             } else {
//                 readyQueue.add(currentProcess);
//                 lastProcess = currentProcess;
//                 currentProcess = null;
//             }
//         }

//         printResults(executionOrder);
//     }

//     private void printResults(List<String> executionOrder) {
//         System.out.println("\n--- Round Robin Scheduling Results ---");
//         System.out.println("Execution Order: " + executionOrder);
//         System.out.println("Process\tWaiting Time\t\tTurnaround Time");

//         List<Process> sorted = new ArrayList<>(initialProcesses);
//         sorted.sort(Comparator.comparing(Process::getName));

//         for (Process p : sorted) {
//             System.out.println(p.getName() + "\t\t" +
//                     p.getWaitingTime() + "\t\t\t\t\t\t" +
//                     p.getTurnaroundTime());
//         }

//         System.out.println("Average Waiting Time: " +
//                 String.format("%.2f", calculateAverageWaitingTime(initialProcesses)));

//         System.out.println("Average Turnaround Time: " +
//                 String.format("%.2f", calculateAverageTurnaroundTime(initialProcesses)));

//     }
// }



// class SJFScheduler extends AbstractScheduler {

//     public SJFScheduler(List<Process> processes, int contextSwitchTime) {
//         super(processes, contextSwitchTime);
//     }

//     @Override
//     public void schedule() {
//         int currentTime = 0;
//         int completedProcessesCount = 0;
//         int totalProcesses = initialProcesses.size();

//         PriorityQueue<Process> readyQueue = new PriorityQueue<>(
//                 Comparator.comparingInt(Process::getBurstTime)
//                         .thenComparingInt(Process::getArrivalTime)
//         );

//         List<Process> activeProcesses = new ArrayList<>(initialProcesses);
//         activeProcesses.sort(Comparator.comparingInt(Process::getArrivalTime));

//         Process currentProcess = null;
//         Process lastProcess = null;
//         List<String> executionOrder = new ArrayList<>();

//         while (completedProcessesCount < totalProcesses) {

//             while (!activeProcesses.isEmpty() && activeProcesses.get(0).getArrivalTime() <= currentTime) {
//                 readyQueue.add(activeProcesses.remove(0));
//             }

//             if (!readyQueue.isEmpty()) {
//                 Process bestCandidate = readyQueue.peek();

//                 if (bestCandidate != lastProcess && lastProcess != null) {
//                     currentTime += contextSwitchTime;
//                 }

//                 currentProcess = bestCandidate;

//                 if (currentProcess.getStartTime() == -1) {
//                     currentProcess.setLastExecutionTime(currentTime);
//                 }

//                 currentProcess.execute(1);
//                 currentTime++;

//                 if (currentProcess != lastProcess) executionOrder.add(currentProcess.getName());

//                 if (currentProcess.isCompleted()) {
//                     completedProcessesCount++;
//                     readyQueue.poll();

//                     calculateMetrics(currentProcess, currentTime);
//                 }

//                 lastProcess = currentProcess;

//             } else {
//                 currentTime++;
//             }
//         }

//     }
// }


// class PreemptivePriorityScheduler extends AbstractScheduler {

//     private static final int AGING_FACTOR = 10;

//     public PreemptivePriorityScheduler(List<Process> processes, int csTime) {
//         super(processes, csTime);
//     }

//     @Override
//     public void schedule() {
//         List<Process> processes = initialProcesses;
//         processes.sort(Comparator.comparingInt(Process::getArrivalTime));

//         List<Process> readyList = new ArrayList<>();
//         Map<String, Integer> waitingTimes = new HashMap<>();
//         for (Process p : processes) {
//             waitingTimes.put(p.getName(), 0);
//         }

//         List<String> executionHistory = new ArrayList<>();
//         int currentTime = 0;
//         Process currentProcess = null;
//         int nextArrivalIndex = 0;

//         while (true) {
//             // Add arrived processes
//             while (nextArrivalIndex < processes.size() && processes.get(nextArrivalIndex).getArrivalTime() <= currentTime) {
//                 Process p = processes.get(nextArrivalIndex);
//                 readyList.add(p);
//                 nextArrivalIndex++;
//             }

//             // Check if all done
//             boolean allDone = (nextArrivalIndex == processes.size()) && readyList.isEmpty() && (currentProcess == null || currentProcess.isCompleted());
//             if (allDone) {
//                 break;
//             }

//             // Handle selection or preemption
//             if (currentProcess == null || currentProcess.isCompleted()) {
//                 if (readyList.isEmpty()) {
//                     if (nextArrivalIndex < processes.size()) {
//                         currentTime = processes.get(nextArrivalIndex).getArrivalTime();
//                         continue;
//                     } else {
//                         break;
//                     }
//                 }
//                 // Select new process
//                 currentProcess = selectAndRemoveBest(readyList, waitingTimes);
//                 currentTime = performContextSwitch(currentTime);
//                 for (Process p : readyList) {
//                     incrementWaiting(p, waitingTimes, contextSwitchTime);
//                 }
//                 if (currentProcess.getStartTime() == -1) {
//                     currentProcess.setStartTime(currentTime);
//                 }
//             } else {
//                 // Check for preemption
//                 Process potential = getBest(readyList, waitingTimes);
//                 if (potential != null && getEffective(potential, waitingTimes) < getEffective(currentProcess, waitingTimes)) {
//                     // Preempt
//                     readyList.add(currentProcess);
//                     Process newCurrent = selectAndRemoveBest(readyList, waitingTimes);
//                     currentTime = performContextSwitch(currentTime);
//                     for (Process p : readyList) {
//                         incrementWaiting(p, waitingTimes, contextSwitchTime);
//                     }
//                     currentProcess = newCurrent;
//                     if (currentProcess.getStartTime() == -1) {
//                         currentProcess.setStartTime(currentTime);
//                     }
//                 }
//             }

//             // Execute
//             int remaining = currentProcess.getBurstTime();
//             if (remaining <= 0) {
//                 currentProcess = null;
//                 continue;
//             }
//             int nextArrTime = (nextArrivalIndex < processes.size()) ? processes.get(nextArrivalIndex).getArrivalTime() : Integer.MAX_VALUE;
//             int dt = Math.min(remaining, nextArrTime - currentTime);
//             if (dt > 0) {
//                 int start = currentTime;
//                 currentProcess.execute(dt);
//                 currentTime += dt;
//                 executionHistory.add(currentProcess.getName() + " from " + start + " to " + currentTime);
//                 for (Process p : readyList) {
//                     incrementWaiting(p, waitingTimes, dt);
//                 }
//             }

//             // Check completion
//             if (currentProcess.isCompleted()) {
//                 AbstractScheduler.calculateMetrics(currentProcess, currentTime);
//                 currentProcess = null;
//             }
//         }

//         // Output
//         System.out.println("Preemptive Priority Scheduling with Aging:");
//         System.out.println("Processes execution order:");
//         for (String entry : executionHistory) {
//             System.out.println(entry);
//         }
//         System.out.println("Waiting Time for each process:");
//         List<Process> sortedProcesses = new ArrayList<>(initialProcesses);
//         sortedProcesses.sort(Comparator.comparing(Process::getName));
//         for (Process p : sortedProcesses) {
//             System.out.println(p.getName() + ": " + p.getWaitingTime());
//         }
//         System.out.println("Turnaround Time for each process:");
//         for (Process p : sortedProcesses) {
//             System.out.println(p.getName() + ": " + p.getTurnaroundTime());
//         }
//         System.out.println("Average Waiting Time: " + String.format("%.2f", AbstractScheduler.calculateAverageWaitingTime(initialProcesses)));
//         System.out.println("Average Turnaround Time: " + String.format("%.2f", AbstractScheduler.calculateAverageTurnaroundTime(initialProcesses)));
//     }

//     private int getEffective(Process p, Map<String, Integer> waitingTimes) {
//         return p.getPriority() - (waitingTimes.get(p.getName()) / AGING_FACTOR);
//     }

//     private void incrementWaiting(Process p, Map<String, Integer> waitingTimes, int time) {
//         String name = p.getName();
//         waitingTimes.put(name, waitingTimes.get(name) + time);
//     }

//     private Process getBest(List<Process> ready, Map<String, Integer> waitingTimes) {
//         if (ready.isEmpty()) return null;
//         Process best = ready.get(0);
//         int bestEff = getEffective(best, waitingTimes);
//         int bestArr = best.getArrivalTime();
//         for (int i = 1; i < ready.size(); i++) {
//             Process cand = ready.get(i);
//             int eff = getEffective(cand, waitingTimes);
//             int arr = cand.getArrivalTime();
//             if (eff < bestEff || (eff == bestEff && arr < bestArr)) {
//                 bestEff = eff;
//                 bestArr = arr;
//                 best = cand;
//             }
//         }
//         return best;
//     }

//     private Process selectAndRemoveBest(List<Process> ready, Map<String, Integer> waitingTimes) {
//         Process best = getBest(ready, waitingTimes);
//         if (best != null) {
//             ready.remove(best);
//         }
//         return best;
//     }
// }

// class AGScheduler extends AbstractScheduler {
//     public AGScheduler(List<Process> processes, int csTime) {
//         super(processes, csTime);
//     }

//     @Override
//     public void schedule() {
//         List<Process> processes = initialProcesses;
//         processes.sort(Comparator.comparingInt(Process::getArrivalTime));

//         Queue<Process> readyQueue = new LinkedList<>();
//         List<String> executionHistory = new ArrayList<>();

//         int currentTime = 0;
//         int nextProcessIndex = 0;
//         Process currentProcess = null;
//         int phase = 1;
//         while (true) {
//             // Add arrived processes
//             while (nextProcessIndex < processes.size() && processes.get(nextProcessIndex).getArrivalTime() <= currentTime) {
//                 Process p = processes.get(nextProcessIndex++);
//                 readyQueue.add(p);
//             }
//             if (readyQueue.isEmpty() && currentProcess == null) {
//                 if (nextProcessIndex == processes.size())
//                     break;
//                 currentTime = processes.get(nextProcessIndex).getArrivalTime();
//                 continue;
//             }

//             if (currentProcess == null) {
//                 currentProcess = readyQueue.poll();

//             }

//             // Phase 1
//             executionHistory.add(currentProcess.getName());
//             int time = (currentProcess.getQuantum() + 3) / 4;
//             time = Math.min(time, currentProcess.getBurstTime());
//             currentProcess.execute(time);
//             int rem = currentProcess.getQuantum() - time;
//             currentTime += time;

//             if (currentProcess.getBurstTime() == 0) {
//                 calculateMetrics(currentProcess, currentTime);
//                 currentProcess.setQuantum(0); // scenario iv
//                 currentProcess = null;
//                 continue;
//             }
//             while (nextProcessIndex < processes.size() && processes.get(nextProcessIndex).getArrivalTime() <= currentTime) {
//                 Process p = processes.get(nextProcessIndex++);
//                 readyQueue.add(p);
//             }

//             //go to second phase (Priority)
//             Process nextProcess = null;
//             if (!readyQueue.isEmpty())
//                 nextProcess = Collections.min(readyQueue, Comparator.comparingInt(Process::getPriority));
//             if (nextProcess == null || currentProcess.getPriority() <= nextProcess.getPriority())
//                 nextProcess = currentProcess;
//             if (nextProcess != currentProcess) {
//                 currentProcess.setQuantum(currentProcess.getQuantum() + (rem + 1) / 2);  // scenario ii
//                 readyQueue.remove(nextProcess);
//                 readyQueue.add(currentProcess);
//                 currentProcess = nextProcess;
//                 continue;
//             }
//             time = (currentProcess.getQuantum() + 3) / 4;
//             time = Math.min(time, currentProcess.getBurstTime());
//             currentProcess.execute(time);
//             rem -= time;
//             currentTime += time;

//             if (currentProcess.getBurstTime() == 0) {
//                 calculateMetrics(currentProcess, currentTime);
//                 currentProcess.setQuantum(0); // scenario iv
//                 currentProcess = null;
//                 continue;
//             }
//             while (nextProcessIndex < processes.size() && processes.get(nextProcessIndex).getArrivalTime() <= currentTime) {
//                 Process p = processes.get(nextProcessIndex++);
//                 readyQueue.add(p);
//             }

//             // Third phase (Shortest remaining time)
//             nextProcess = null;
//             if (!readyQueue.isEmpty())
//                 nextProcess = Collections.min(readyQueue, Comparator.comparingInt(Process::getBurstTime));

//             if (nextProcess == null || currentProcess.getBurstTime() <= nextProcess.getBurstTime())
//                 nextProcess = currentProcess;
//             if (nextProcess != currentProcess) {
//                 currentProcess.setQuantum(currentProcess.getQuantum() + rem); // scenario iii
//                 readyQueue.remove(nextProcess);
//                 readyQueue.add(currentProcess);
//                 currentProcess = nextProcess;
//                 continue;
//             }
//             time = rem;
//             time = Math.min(time, currentProcess.getBurstTime());
//             currentProcess.execute(time);
//             currentTime += time;

//             if (currentProcess.getBurstTime() == 0) {
//                 calculateMetrics(currentProcess, currentTime);
//                 currentProcess.setQuantum(0); // scenario iv
//                 currentProcess = null;
//                 continue;
//             }
//             currentProcess.setQuantum(currentProcess.getQuantum() + 2);  // scenario i
//             readyQueue.add(currentProcess);
//             currentProcess = null;

//         }

//         // output
//         processes.sort(Comparator.comparingInt(Process::getStartTime));
//         System.out.println("AGScheduler");
//         System.out.println("Processes execution order: " + executionHistory);
//         for (Process p : processes) {
//             System.out.print(p.getName() + " ");
//             System.out.print(" started at " + p.getStartTime());
//             System.out.print(" waited for " + p.getWaitingTime());
//             System.out.print(" turned around for " + p.getTurnaroundTime());
//             System.out.print(" quantum history: " + p.getQuantumHistory());
//             System.out.println();
//         }

//         System.out.println("Average Waiting Time: " + calculateAverageWaitingTime(processes));
//         System.out.println("Average Turnaround Time: " + calculateAverageTurnaroundTime(processes));

//     }
// }


// public class CPU_Schedulers_Simulator {

//     public static void main(String[] args) {
//         System.out.println("... CPU Schedulers Simulator ...");
//         Scanner sc = new Scanner(System.in);
//         int numProcesses = sc.nextInt();
//         int rrQuantum = sc.nextInt();
//         int contextSwitchTime = sc.nextInt();
//         List<Process> processes = new ArrayList<>();
//         for (int i = 0; i < numProcesses; i++) {
//             String name = sc.next();
//             int arrival = sc.nextInt();
//             int burst = sc.nextInt();
//             int priority = sc.nextInt();
//             int quantum = sc.nextInt();
//             processes.add(new Process(name, arrival, burst, priority, quantum));
//         }

//          new RRScheduler(processes, contextSwitchTime, rrQuantum).schedule();
// //        PreemptivePriorityScheduler scheduler = new PreemptivePriorityScheduler(processes, contextSwitchTime);
//         AGScheduler scheduler = new AGScheduler(processes, contextSwitchTime);
//         scheduler.schedule();
//         sc.close();
//     }
// }
