/*
Notes
    You can use built-in functions and predefined classes in Java. Do not use "exec" to implement any of these commands
    Make a function for each command and call it in the switch case
    The redirection operators (>, >>) are handled already in the parser class. You just need to call handleOutput function if your command returns an output
    Wrong commands case is handled, You need to handle wrong arguments cases based on the behavior of the command
*/


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.io.*;

class Parser {
    String commandName;
    String[] args;
    String outputFile;
    boolean appendMode;

    public boolean parse(String input) {
        input = input.trim();
        if (input.isEmpty()) return false;

        if (input.contains(">>")) {
            String[] temp = input.split(">>", 2);
            input = temp[0].trim();
            outputFile = temp[1].trim();
            appendMode = true;
        } else if (input.contains(">")) {
            String[] temp = input.split(">", 2);
            input = temp[0].trim();
            outputFile = temp[1].trim();
            appendMode = false;
        } else outputFile = null;


        String[] temp = input.split("\\s+");
        if (temp.length == 0) return false;
        commandName = temp[0];
        args = new String[temp.length - 1];
        System.arraycopy(temp, 1, args, 0, args.length);
        return true;
    }

    public String getCommandName() {
        return commandName;
    }

    public String[] getArgs() {
        return args;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public boolean AppendMode() {
        return appendMode;
    }

}

public class Terminal {
    Parser parser = new Parser();

    private void handleOutput(String text) {
        String file = parser.getOutputFile();
        if (file == null) {
            System.out.println(text);
        } else {
            try (FileWriter writer = new java.io.FileWriter(file, parser.AppendMode())) {
                writer.write(text + System.lineSeparator());
            } catch (IOException e) {
                System.err.println("Error writing to file: " + e.getMessage());
            }
        }
    }

    // Command: pwd
    public String pwd() {
        return System.getProperty("user.dir");
    }

    // Command: ls
    public String ls() {
        File currentDir = new File(System.getProperty("user.dir"));
        File[] files = currentDir.listFiles();
        if (files == null || files.length == 0) return "Directory is empty.";

        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        StringBuilder sb = new StringBuilder();
        for (File f : files) {
            sb.append(f.getName()).append("\n");
        }
        return sb.toString().trim();
    }

    // Command: cd → Change directory (3 cases)
    public void cd(String[] args) {
        File currentDir = new File(System.getProperty("user.dir"));

        if (args.length == 0) {
            // No args → Go to home directory
            System.setProperty("user.dir", System.getProperty("user.home"));
        } else if (args.length == 1) {
            String path = args[0];
            if (path.equals("..")) {
                // Go one level up
                File parent = currentDir.getParentFile();
                if (parent != null) {
                    System.setProperty("user.dir", parent.getAbsolutePath());
                }
            } else {
                // Handle relative or absolute path
                File newDir = new File(path);
                if (!newDir.isAbsolute()) {
                    newDir = new File(currentDir, path);
                }

                if (newDir.exists() && newDir.isDirectory()) {
                    System.setProperty("user.dir", newDir.getAbsolutePath());
                } else {
                    System.out.println("Invalid path or directory does not exist.");
                }
            }
        } else {
            System.out.println("cd command takes at most one argument.");
        }
    }


    // Command: touch
    public void touch(String[] args) {
        if (args.length != 1) {
            System.out.println("Error: touch requires exactly one file path argument.");
            return;
        }

        File file = new File(args[0]);
        try {
            if (file.createNewFile()) {
                // Success - file created
            } else {
                // File exists, update timestamp (standard touch behavior)
                file.setLastModified(System.currentTimeMillis());
            }
        } catch (IOException e) {
            System.out.println("Error: Could not create file " + args[0] + ". Check path and permissions.");
        }
    }

    // Command: rm
    public void rm(String[] args) {
        if (args.length != 1) {
            System.out.println("Error: rm requires exactly one file name argument.");
            return;
        }

        File file = new File(args[0]);

        if (!file.exists()) {
            System.out.println("Error: File not found: " + args[0]);
        } else if (file.isDirectory()) {
            System.out.println("Error: Cannot remove a directory with 'rm'. Use 'rmdir' or 'cp -r'.");
        } else if (file.delete()) {
            // Success
        } else {
            System.out.println("Error: Could not delete file: " + args[0]);
        }
    }

    // Command: cat
    public String cat(String[] args) {
        if (args.length == 0 || args.length > 2) {
            return "Error: cat requires one or two file name arguments.";
        }

        StringBuilder content = new StringBuilder();
        for (String filename : args) {
            File file = new File(filename);
            if (!file.exists() || file.isDirectory()) {
                return "Error: File not found or is a directory: " + filename;
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append(System.lineSeparator());
                }
            } catch (IOException e) {
                return "Error reading file " + filename + ": " + e.getMessage();
            }
        }
        return content.toString().trim();
    }

    // Helper function for cp -r: Recursively copies a directory and its contents
    private void copyRecursive(Path source, Path destination) throws IOException {
        if (!Files.exists(source)) return;

        // Check if destination is a directory, if so, append source folder name
        if (Files.isDirectory(destination)) {
            destination = destination.resolve(source.getFileName());
        }

        final Path target = destination;

        Files.walk(source)
                .forEach(sourcePath -> {
                    try {
                        Path destPath = target.resolve(source.relativize(sourcePath));
                        if (Files.isDirectory(sourcePath)) {
                            Files.createDirectories(destPath); // Create subdirectories
                        } else {
                            // Copy the file, overwriting if it exists
                            Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        System.err.println("Error copying " + sourcePath + ": " + e.getMessage());
                    }
                });
    }

    // Command: cp
    public void cp(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.out.println("Error: cp requires two arguments (source and destination) or three arguments (cp -r source destination).");
            return;
        }

        // Case: cp -r dir1 dir2 (Recursive directory copy)
        if (args.length == 3 && args[0].equals("-r")) {
            File sourceDir = new File(args[1]);
            File destDir = new File(args[2]);

            if (!sourceDir.isDirectory() || !sourceDir.exists() || !destDir.isDirectory() || !destDir.exists()) {
                System.out.println("Error: 'cp -r' requires both arguments to be existing directories.");
                return;
            }
            try {
                // Call the recursive helper function
                copyRecursive(sourceDir.toPath(), destDir.toPath());
            } catch (IOException e) {
                System.out.println("Error during recursive copy: " + e.getMessage());
            }
            return;
        }

        // Case: cp file1 file2 (File copy)
        if (args.length == 2) {
            File sourceFile = new File(args[0]);
            File destFile = new File(args[1]);

            if (!sourceFile.isFile() || !sourceFile.exists()) {
                System.out.println("Error: Source file not found or is a directory: " + args[0]);
                return;
            }

            try {
                // Copy the file, replacing the destination if it exists
                Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.out.println("Error copying file: " + e.getMessage());
            }
            return;
        }

        // Handle bad arguments that slipped through
        System.out.println("Error: Invalid arguments for cp command.");
    }


    public void chooseCommandAction() {
        String command = parser.getCommandName();
        String[] args = parser.getArgs();
        switch (command) {
            case "pwd":
                handleOutput(pwd());
                break;
            case "ls":
                handleOutput(ls());
                break;
            case "cd":
                cd(args);
                break;
            case "mkdir":
                //put ur function here
                break;
            case "rm":
                rm(args);
                break;
            case "rmdir":
                //put ur function here
                break;
            case "touch":
                touch(args);
                break;
            case "cat":
                handleOutput(cat(args));
                break;
            case "cp":
                cp(args);
                break;
            case "wc":
                //put ur function here
                break;
            case "zip":
                //put ur function here
                break;
            case "unzip":
                //put ur function here
                break;
            default:
                System.out.println("Unknown command");
                break;
        }

    }

    public static void main(String[] args) {
        System.out.println("Welcome to the terminal");
        Terminal terminal = new Terminal();
        String input;
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            input = scanner.nextLine();
            if (input.equals("exit")) {
                System.out.println("Exiting the terminal.");
                break;
            }
            if (!terminal.parser.parse(input)) continue;
            terminal.chooseCommandAction();
        }
        scanner.close();
    }
}
