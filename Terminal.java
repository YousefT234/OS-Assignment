/*
Notes
    You can use built-in functions and predefined classes in Java. Do not use "exec" to implement any of these commands
    Make a function for each command and call it in the switch case
    The redirection operators (>, >>) is handled already in the parser class. You just need to call handleOutput function if your command returns an output
    Wrong commands case is handled, You need to handle wrong arguments cases based on the behavior of the command
*/


import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
            outputFile = temp[1].trim().replaceAll("^\"|\"$", "");
            appendMode = true;
        } else if (input.contains(">")) {
            String[] temp = input.split(">", 2);
            input = temp[0].trim();
            outputFile = temp[1].trim().replaceAll("^\"|\"$", "");
            appendMode = false;
        } else outputFile = null;


        List<String> parts = new ArrayList<>();
        String cur = "";
        boolean inQuotes = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (cur.length() > 0) {
                    parts.add(cur.toString());
                    cur = "";
                }
            } else {
                cur += c;
            }
        }
        if (cur.length() > 0) parts.add(cur);

        if (parts.isEmpty()) return false;

        commandName = parts.get(0);
        args = parts.size() > 1 ? parts.subList(1, parts.size()).toArray(new String[0]) : new String[0];
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
        String path = parser.getOutputFile();
        if (path == null) {
            System.out.println(text);
        } else {
            File f = new File(path);
            if (!f.isAbsolute()) {
                path = pwd() + File.separator + path;
            }
            try (FileWriter writer = new java.io.FileWriter(path, parser.AppendMode())) {
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

    //Command: mkdir -> takes one or more arg(a dir name or a full\relative path)
    public void mkdir(String[] args) {
        if (args.length == 0) {
            System.out.println("argument needed");
            return;
        }
        File currentDir = new File(System.getProperty("user.dir"));
        for (String arg : args) {
            if (arg.trim().isEmpty()) {
                continue;
            }
            File targetDir = new File(arg);
            if (!targetDir.isAbsolute()) {
                targetDir = new File(currentDir, arg);
            }
            if (targetDir.exists()) {
                System.out.println("error: file exists");
                continue;
            }
            if (targetDir.mkdirs()) {
                System.out.println("Dir'" + arg + "'created");
            } else {
                System.out.println("failed");
            }
        }
    }

    //command: rmdir -> takes * or full/relative path and removes only if empty
    public void rmdir(String[] args) {
        if (args.length != 1) {
            System.out.println("invalid number of arguments");
            return;
        }
        String arg = args[0].trim();
        if (arg.isEmpty()) {
            System.out.println("missing operand");
            return;
        }
        File currentDir = new File(System.getProperty("user.dir"));
        //case one: *
        if (arg.equals("*")) {
            File[] files = currentDir.listFiles();
            if (files == null) {
                System.out.println("cannot access current dir");
                return;
            }
            for (File file : files) {
                if (file.isDirectory() && file.listFiles().length == 0) {
                    if (file.delete()) {
                        System.out.println("success");
                    } else {
                        System.out.println("error");
                    }
                }
            }
            return;
        }
        //case 2
        File targetDir = new File(arg);
        if (!targetDir.isAbsolute()) {
            targetDir = new File(currentDir, arg);
        }
        if (!targetDir.exists()) {
            System.out.println("no such file or dir");
            return;
        }
        if (!targetDir.isDirectory()) {
            System.out.println("not a directory");
            return;
        }
        if (targetDir.listFiles().length > 0) {
            System.out.println("directory not empty");
            return;
        }
        if (targetDir.delete()) {
            System.out.println("success");
        } else {
            System.out.println("failed");
        }
    }

    // command: wc -> takes a file and counts the number of lines, words, and characters.
    public String wc(String[] args) {
        if (args.length != 1) {
            System.out.println("invalid number of arguments");
            return "";
        }
        String arg = args[0].trim();

        File targetDir = new File(arg);
        if (!targetDir.isAbsolute()) {
            targetDir = new File(pwd(), arg);
        }

        if (!targetDir.exists() || !targetDir.isFile()) {
            System.out.println("No such file.");
            return "";
        }

        int lines = 0, words = 0, chars = 0;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(targetDir));
            String curLine;
            while ((curLine = reader.readLine()) != null) {
                lines++;
                chars += curLine.length();
                if (!curLine.isEmpty()) words += curLine.trim().split("\\s+").length;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lines + " " + words + " " + chars + " " + arg;
    }

    public void zip(String[] args) {
        if (args.length == 0) {
            System.out.println("invalid number of arguments");
            return;
        }

        int i = 0;
        boolean subDirectories = false;

        if (Objects.equals(args[0], "-r")) {
            subDirectories = true;
            i = 1;
        }

        File zipFile = new File(args[i]);
        if (!zipFile.isAbsolute()) zipFile = new File(pwd(), args[i]);
        try {
            byte[] buffer = new byte[1024];
            FileOutputStream fos = new FileOutputStream(zipFile.toString());
            ZipOutputStream zos = new ZipOutputStream(fos);
            for (i++; i < args.length; i++) {
                File fileToZip = new File(args[i]);
                if (!fileToZip.isAbsolute()) fileToZip = new File(pwd(), args[i]);

                if (!subDirectories) {
                    if (!fileToZip.isFile()) continue;
                    FileInputStream fis = new FileInputStream(fileToZip.toString());
                    zos.putNextEntry(new ZipEntry(fileToZip.getName()));
                    int length;
                    while ((length = fis.read(buffer)) > 0)
                        zos.write(buffer, 0, length);

                    zos.closeEntry();
                    fis.close();
                } else {

                    Path sourcePath = fileToZip.toPath();
                    Files.walk(sourcePath).filter(path -> !Files.isDirectory(path)).forEach(path -> {
                        String zipEntryName = sourcePath.getParent().relativize(path).toString().replace("\\", "/");
                        try {
                            FileInputStream fis = new FileInputStream(path.toString());
                            ZipEntry zipEntry = new ZipEntry(zipEntryName);
                            zos.putNextEntry(zipEntry);

                            int length;
                            while ((length = fis.read(buffer)) > 0)
                                zos.write(buffer, 0, length);

                            zos.closeEntry();
                            fis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

                }
            }
            zos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void unzip(String[] args) {

    }

    public void chooseCommandAction() {
        String command = parser.getCommandName();
        String[] args = parser.getArgs();
        switch (command) {
            case "pwd":
                if (args.length != 0) {
                    System.out.println("This command takes no arguments");
                    break;
                }
                handleOutput(pwd());
                break;
            case "ls":
                if (args.length != 0) {
                    System.out.println("This command takes no arguments");
                    break;
                }
                handleOutput(ls());
                break;
            case "cd":
                cd(args);
                break;
            case "mkdir":
                mkdir(args);
                break;
            case "rm":
                //put ur function here
                break;
            case "rmdir":
                rmdir(args);
                break;
            case "touch":
                //put ur function here
                break;
            case "cat":
                //put ur function here
                break;
            case "cp":
                //put ur function here
                break;
            case "wc":
                handleOutput(wc(args));
                break;
            case "zip":
                zip(args);
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
        System.out.println("Make sure to enclose arguments with spaces in double quotes.");
        Terminal terminal = new Terminal();
        String input;
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print(terminal.pwd());
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
