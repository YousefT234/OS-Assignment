package os_assignment;

import java.util.List;

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
    }

    public String getName() { return name; }
    public int getArrivalTime() { return arrivalTime; }
    public int getInitialBurstTime() { return initialBurstTime; }
    public int getBurstTime() { return burstTime; }
    public int getPriority() { return priority; }
    public int getQuantum() { return quantum; }
    public int getWaitingTime() { return waitingTime; }
    public int getTurnaroundTime() { return turnaroundTime; }
    public int getCompletionTime() { return completionTime; }
    public int getStartTime() { return startTime; }
    public int getLastExecutionTime() { return lastExecutionTime; }

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