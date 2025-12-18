import java.util.*;

class Process {
    private final String name;
    private final int arrivalTime;
    private final int initialBurstTime;
    private int burstTime;
    private int priority;
    private int quantum;

    private int waitingTime;
    private int turnaroundTime;
    private int completionTime;
    private int startTime;
    private int lastExecutionTime;

    public Process(String name, int arrivalTime, int burstTime, int priority, int quantum) {
        this.name = name;
        this.arrivalTime = arrivalTime;
        this.initialBurstTime = burstTime;
        this.burstTime = burstTime;
        this.priority = priority;
        this.quantum = quantum;
        this.startTime = -1;
        this.lastExecutionTime = 0;
    }

    public String getName() {
        return name;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public int getInitialBurstTime() {
        return initialBurstTime;
    }

    public int getBurstTime() {
        return burstTime;
    }

    public int getPriority() {
        return priority;
    }

    public int getQuantum() {
        return quantum;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    public int getTurnaroundTime() {
        return turnaroundTime;
    }

    public int getCompletionTime() {
        return completionTime;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getLastExecutionTime() {
        return lastExecutionTime;
    }

    public void execute(int duration) {
        if (startTime == -1) {
            startTime = lastExecutionTime;
        }
        burstTime -= duration;
        lastExecutionTime += duration;
    }

    public void setTurnaroundTime(int turnaroundTime) {
        this.turnaroundTime = turnaroundTime;
    }

    public void setWaitingTime(int waitingTime) {
        this.waitingTime = waitingTime;
    }

    public void setCompletionTime(int completionTime) {
        this.completionTime = completionTime;
    }

    public void setLastExecutionTime(int lastExecutionTime) {
        this.lastExecutionTime = lastExecutionTime;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setQuantum(int quantum) {
        this.quantum = quantum;
    }

    public boolean isCompleted() {
        return burstTime <= 0;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

}

abstract class AbstractScheduler {

    protected List<Process> initialProcesses;

    public abstract void schedule();

    protected final int contextSwitchTime;

    public AbstractScheduler(List<Process> processes, int csTime) {
        this.initialProcesses = deepCopyProcesses(processes);
        this.contextSwitchTime = csTime;
    }

    protected int performContextSwitch(int currentTime) {
        return currentTime + contextSwitchTime;
    }

    private List<Process> deepCopyProcesses(List<Process> original) {
        List<Process> copy = new java.util.ArrayList<>();
        for (Process p : original) {
            copy.add(new Process(
                    p.getName(),
                    p.getArrivalTime(),
                    p.getInitialBurstTime(),
                    p.getPriority(),
                    p.getQuantum()
            ));
        }
        return copy;
    }

    public static void calculateMetrics(Process p, int completionTime) {
        p.setCompletionTime(completionTime);
        int turnaroundTime = completionTime - p.getArrivalTime();
        p.setTurnaroundTime(turnaroundTime);
        int waitingTime = turnaroundTime - p.getInitialBurstTime();
        p.setWaitingTime(waitingTime);
    }

    public static double calculateAverageWaitingTime(List<Process> processes) {
        double totalWaitingTime = 0;
        for (Process p : processes) {
            totalWaitingTime += p.getWaitingTime();
        }
        return totalWaitingTime / processes.size();
    }

    public static double calculateAverageTurnaroundTime(List<Process> processes) {
        double totalTurnaroundTime = 0;
        for (Process p : processes) {
            totalTurnaroundTime += p.getTurnaroundTime();
        }
        return totalTurnaroundTime / processes.size();
    }
}

class PreemptivePriorityScheduler extends AbstractScheduler {

    private static final int AGING_FACTOR = 10;

    public PreemptivePriorityScheduler(List<Process> processes, int csTime) {
        super(processes, csTime);
    }

    @Override
    public void schedule() {
        List<Process> processes = initialProcesses;
        processes.sort(Comparator.comparingInt(Process::getArrivalTime));

        List<Process> readyList = new ArrayList<>();
        Map<String, Integer> waitingTimes = new HashMap<>();
        for (Process p : processes) {
            waitingTimes.put(p.getName(), 0);
        }

        List<String> executionHistory = new ArrayList<>();
        int currentTime = 0;
        Process currentProcess = null;
        int nextArrivalIndex = 0;

        while (true) {
            // Add arrived processes
            while (nextArrivalIndex < processes.size() && processes.get(nextArrivalIndex).getArrivalTime() <= currentTime) {
                Process p = processes.get(nextArrivalIndex);
                readyList.add(p);
                nextArrivalIndex++;
            }

            // Check if all done
            boolean allDone = (nextArrivalIndex == processes.size()) && readyList.isEmpty() && (currentProcess == null || currentProcess.isCompleted());
            if (allDone) {
                break;
            }

            // Handle selection or preemption
            if (currentProcess == null || currentProcess.isCompleted()) {
                if (readyList.isEmpty()) {
                    if (nextArrivalIndex < processes.size()) {
                        currentTime = processes.get(nextArrivalIndex).getArrivalTime();
                        continue;
                    } else {
                        break;
                    }
                }
                // Select new process
                currentProcess = selectAndRemoveBest(readyList, waitingTimes);
                currentTime = performContextSwitch(currentTime);
                for (Process p : readyList) {
                    incrementWaiting(p, waitingTimes, contextSwitchTime);
                }
                if (currentProcess.getStartTime() == -1) {
                    currentProcess.setStartTime(currentTime);
                }
            } else {
                // Check for preemption
                Process potential = getBest(readyList, waitingTimes);
                if (potential != null && getEffective(potential, waitingTimes) < getEffective(currentProcess, waitingTimes)) {
                    // Preempt
                    readyList.add(currentProcess);
                    Process newCurrent = selectAndRemoveBest(readyList, waitingTimes);
                    currentTime = performContextSwitch(currentTime);
                    for (Process p : readyList) {
                        incrementWaiting(p, waitingTimes, contextSwitchTime);
                    }
                    currentProcess = newCurrent;
                    if (currentProcess.getStartTime() == -1) {
                        currentProcess.setStartTime(currentTime);
                    }
                }
            }

            // Execute
            int remaining = currentProcess.getBurstTime();
            if (remaining <= 0) {
                currentProcess = null;
                continue;
            }
            int nextArrTime = (nextArrivalIndex < processes.size()) ? processes.get(nextArrivalIndex).getArrivalTime() : Integer.MAX_VALUE;
            int dt = Math.min(remaining, nextArrTime - currentTime);
            if (dt > 0) {
                int start = currentTime;
                currentProcess.execute(dt);
                currentTime += dt;
                executionHistory.add(currentProcess.getName() + " from " + start + " to " + currentTime);
                for (Process p : readyList) {
                    incrementWaiting(p, waitingTimes, dt);
                }
            }

            // Check completion
            if (currentProcess.isCompleted()) {
                AbstractScheduler.calculateMetrics(currentProcess, currentTime);
                currentProcess = null;
            }
        }

        // Output
        System.out.println("Preemptive Priority Scheduling with Aging:");
        System.out.println("Processes execution order:");
        for (String entry : executionHistory) {
            System.out.println(entry);
        }
        System.out.println("Waiting Time for each process:");
        List<Process> sortedProcesses = new ArrayList<>(initialProcesses);
        sortedProcesses.sort(Comparator.comparing(Process::getName));
        for (Process p : sortedProcesses) {
            System.out.println(p.getName() + ": " + p.getWaitingTime());
        }
        System.out.println("Turnaround Time for each process:");
        for (Process p : sortedProcesses) {
            System.out.println(p.getName() + ": " + p.getTurnaroundTime());
        }
        System.out.println("Average Waiting Time: " + String.format("%.2f", AbstractScheduler.calculateAverageWaitingTime(initialProcesses)));
        System.out.println("Average Turnaround Time: " + String.format("%.2f", AbstractScheduler.calculateAverageTurnaroundTime(initialProcesses)));
    }

    private int getEffective(Process p, Map<String, Integer> waitingTimes) {
        return p.getPriority() - (waitingTimes.get(p.getName()) / AGING_FACTOR);
    }

    private void incrementWaiting(Process p, Map<String, Integer> waitingTimes, int time) {
        String name = p.getName();
        waitingTimes.put(name, waitingTimes.get(name) + time);
    }

    private Process getBest(List<Process> ready, Map<String, Integer> waitingTimes) {
        if (ready.isEmpty()) return null;
        Process best = ready.get(0);
        int bestEff = getEffective(best, waitingTimes);
        int bestArr = best.getArrivalTime();
        for (int i = 1; i < ready.size(); i++) {
            Process cand = ready.get(i);
            int eff = getEffective(cand, waitingTimes);
            int arr = cand.getArrivalTime();
            if (eff < bestEff || (eff == bestEff && arr < bestArr)) {
                bestEff = eff;
                bestArr = arr;
                best = cand;
            }
        }
        return best;
    }

    private Process selectAndRemoveBest(List<Process> ready, Map<String, Integer> waitingTimes) {
        Process best = getBest(ready, waitingTimes);
        if (best != null) {
            ready.remove(best);
        }
        return best;
    }
}


class AGScheduler extends AbstractScheduler {
    public AGScheduler(List<Process> processes, int csTime) {
        super(processes, csTime);
    }

    @Override
    public void schedule() {
        List<Process> processes = initialProcesses;
        processes.sort(Comparator.comparingInt(Process::getArrivalTime));

        Queue<Process> readyQueue = new LinkedList<>();

        int currentTime = 0;
        int nextProcessIndex = 0;
        int phase = 0;
        Process currentProcess = null;
        while (true) {
            // Add arrived processes
            while (nextProcessIndex < processes.size() && processes.get(nextProcessIndex).getArrivalTime() <= currentTime) {
                Process p = processes.get(nextProcessIndex++);
                readyQueue.add(p);
            }
            if (readyQueue.isEmpty() && currentProcess == null) {
                if(nextProcessIndex == processes.size())
                    break;
                currentTime = processes.get(nextProcessIndex).getArrivalTime();
                continue;
            }

            if (currentProcess == null) {
                currentProcess = readyQueue.poll();
                phase = 0;
            }
            if(currentProcess.getQuantum() == 0) {
                currentProcess.setQuantum(2);
                readyQueue.add(currentProcess);
                currentProcess = null;
                continue;
            }
            int time = (currentProcess.getQuantum() + 3) / 4;
            time = Math.min(time, currentProcess.getBurstTime());
            currentProcess.execute(time);
            currentTime += time;
            if (currentProcess.getBurstTime() > 0) {
                if (phase == 0) {
                    //go to second phase (Priority)
                    Process nextProcess = Collections.min(readyQueue, Comparator.comparingInt(Process::getPriority));
                    if (nextProcess != currentProcess) {
                        int rem = currentProcess.getQuantum() - time;
                        currentProcess.setQuantum(currentProcess.getQuantum() + (rem + 1) / 2);
                        readyQueue.add(currentProcess);
                        currentProcess = nextProcess;
                        currentTime = performContextSwitch(currentTime);
                    }
                    phase = 1;
                } else {
                    // go to third phase (Shortest remaining time)
                    Process nextProcess = Collections.min(readyQueue, Comparator.comparingInt(Process::getBurstTime));
                    if (nextProcess != currentProcess) {
                        int rem = currentProcess.getQuantum() - time;
                        currentProcess.setQuantum(currentProcess.getQuantum() + rem);
                        readyQueue.add(currentProcess);
                        currentProcess = nextProcess;
                        currentTime = performContextSwitch(currentTime);
                    }
                    phase = 0;
                }
            } else {
                currentProcess.setCompletionTime(currentTime);
                currentProcess.setQuantum(0); // scenario iv
                currentProcess = null;
                phase = 0;
            }
        }
    }
}

public class CPU_Schedulers_Simulator {

    public static void main(String[] args) {
        System.out.println("... CPU Schedulers Simulator ...");
        Scanner sc = new Scanner(System.in);
        int numProcesses = sc.nextInt();
        int rrQuantum = sc.nextInt(); // 
        int contextSwitchTime = sc.nextInt();
        List<Process> processes = new ArrayList<>();
        for (int i = 0; i < numProcesses; i++) {
            String name = sc.next();
            int arrival = sc.nextInt();
            int burst = sc.nextInt();
            int priority = sc.nextInt();
            int quantum = sc.nextInt();
            processes.add(new Process(name, arrival, burst, priority, quantum));
        }
        PreemptivePriorityScheduler scheduler = new PreemptivePriorityScheduler(processes, contextSwitchTime);
        scheduler.schedule();
        sc.close();
    }
}
