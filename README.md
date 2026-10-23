# Task Scheduler: Insertion Sort Algorithm

A Java Swing desktop application whose main purpose is to **demonstrate and implement the Insertion Sort algorithm**. Tasks are added to a scheduler and can then be sorted by deadline, priority, or task ID, with every sort performed by a hand-written insertion sort (no `Collections.sort` or `Comparator`).

## Features

- Add, edit, search, and delete tasks (name, description, deadline, priority)
- Mark tasks as completed with a checkbox and delete all completed tasks at once
- Sort the task list with Insertion Sort by:
  - **Deadline**: earliest to latest
  - **Priority**: High → Medium → Low
  - **Task ID**: lowest to highest (#10 → #99)
- Stable sorting: tasks that tie are kept in the order they were added
- Deadline validation in `MM/DD/YY` format, including leap years
- Tasks are saved in a local SQLite database, so they persist between runs

## The Insertion Sort Algorithm

Insertion Sort builds the sorted list one item at a time. For each task, starting at the second one, it is compared with the tasks before it. Larger tasks are shifted one position to the right, and the task is inserted into the gap.

From [InsertionSort.java](InsertionSort.java):

```java
public static void sortByTaskID(ArrayList<Task> list) {
    for (int i = 1; i < list.size(); i++) {
        Task currentTask = list.get(i);
        int j = i - 1;

        while (j >= 0 && list.get(j).getTaskID() > currentTask.getTaskID()) {
            list.set(j + 1, list.get(j));   // shift right
            j--;
        }

        list.set(j + 1, currentTask);       // insert in place
    }
}
```

| Case    | Time complexity | Notes                          |
|---------|-----------------|--------------------------------|
| Best    | O(n)            | List is already sorted         |
| Average | O(n²)           |                                |
| Worst   | O(n²)           | List is in reverse order       |
| Space   | O(1)            | Sorts in place                 |

Insertion Sort is **stable**, and it works well for small lists like this one (task IDs are limited to 10–99).

## Project Structure

| File                                         | Description                                                         |
|----------------------------------------------|---------------------------------------------------------------------|
| [Main.java](Main.java)                       | Entry point; sets the Nimbus look and feel and launches the GUI     |
| [TaskSchedulerGUI.java](TaskSchedulerGUI.java) | Swing user interface (table, input form, sort dropdown)           |
| [TaskManager.java](TaskManager.java)         | Task logic: add, edit, delete, search, date validation, sort calls  |
| [InsertionSort.java](InsertionSort.java)     | **The Insertion Sort implementations**                              |
| [Task.java](Task.java)                       | Task model (ID, name, description, deadline, priority, status)      |
| [TaskDatabase.java](TaskDatabase.java)       | SQLite persistence through JDBC                                     |
| `lib/sqlite-jdbc-3.36.0.3.jar`               | SQLite JDBC driver                                                  |
| `task_scheduler.db`                          | SQLite database file (created automatically)                        |

## Requirements

- Java Development Kit (JDK) 8 or later
- The SQLite JDBC jar included in the `lib` folder

## How to Run

From the project folder:

**Windows**
```bash
javac -cp ".;lib/sqlite-jdbc-3.36.0.3.jar" *.java
java -cp ".;lib/sqlite-jdbc-3.36.0.3.jar" Main
```

**macOS / Linux**
```bash
javac -cp ".:lib/sqlite-jdbc-3.36.0.3.jar" *.java
java -cp ".:lib/sqlite-jdbc-3.36.0.3.jar" Main
```

## How to Use

1. Enter a task name, description, deadline (`MM/DD/YY`, e.g. `12/25/26`), and priority, then click **Add**.
2. Add several tasks in no particular order.
3. Use the **Sort By** dropdown at the bottom to choose *Deadline*, *Priority*, or *INSERTION SORT ALGORITHM* (sort by Task ID). The table is re-sorted using Insertion Sort.
4. Use **Search** to find a task by name or ID (e.g. `#42`), **Edit** to change a task, and the checkbox plus **Delete** to remove completed tasks.

## Author

GEMS #3
