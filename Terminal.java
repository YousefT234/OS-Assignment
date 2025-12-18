/*
Notes
    You can use built-in functions and predefined classes in Java. Do not use "exec" to implement any of these commands
    Make a function for each command and call it in the switch case
    The redirection operators (>, >>) is handled already in the parser class. You just need to call handleOutput function if your command returns an output
    Wrong commands case is handled, You need to handle wrong arguments cases based on the behavior of the command
*/


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.io.*;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '\\' && i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                if (next == '"' || next == '\\') {
                    cur.append(next);
                    i++; 
                    continue;
                }
                cur.append(c);
                continue;
            }

            if (c == '"') {
                inQuotes = !inQuotes;
                // don't include the quote character in the token
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (cur.length() > 0) {
                    parts.add(cur.toString());
                    cur.setLength(0);
                }
            } else {
                cur.append(c);
            }
        }

        // If still inside quotes, report error (unbalanced quote) and refuse to parse
        if (inQuotes) {
            System.err.println("Parse error: missing closing double quote (\") in input");
            return false;
        }

        if (cur.length() > 0) parts.add(cur.toString());

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
        try {
            return new File(System.getProperty("user.dir")).getCanonicalPath();
        } catch (IOException e) {
            // Fall back to the raw property if canonicalization fails
            return System.getProperty("user.dir");
        }
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
            try {
                String home = new File(System.getProperty("user.home")).getCanonicalPath();
                System.setProperty("user.dir", home);
            } catch (IOException e) {
                System.setProperty("user.dir", System.getProperty("user.home"));
            }
        } else if (args.length == 1) {
            String path = args[0];
            if (path.equals("..")) {
                // Go one level up
                File parent = currentDir.getParentFile();
                if (parent != null) {
                    try {
                        System.setProperty("user.dir", parent.getCanonicalPath());
                    } catch (IOException e) {
                        System.setProperty("user.dir", parent.getAbsolutePath());
                    }
                }
            } else {
                // Handle relative or absolute path
                File newDir = new File(path);
                if (!newDir.isAbsolute()) {
                    newDir = new File(currentDir, path);
                }

                if (newDir.exists() && newDir.isDirectory()) {
                    try {
                        System.setProperty("user.dir", newDir.getCanonicalPath());
                    } catch (IOException e) {
                        System.setProperty("user.dir", newDir.getAbsolutePath());
                    }
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
            System.err.println("Invalid number of arguments.");
            return "";
        }
        String arg = args[0].trim();

        File targetDir = new File(arg);
        if (!targetDir.isAbsolute()) {
            targetDir = new File(pwd(), arg);
        }

        if (!targetDir.exists() || !targetDir.isFile()) {
            System.err.println("No such file.");
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
            System.err.println("Invalid number of arguments.");
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
            try (FileOutputStream fos = new FileOutputStream(zipFile.toString());
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
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
                        Path zipPath = zipFile.toPath().toAbsolutePath().normalize();
                        Files.walk(sourcePath).forEach(path -> {
                            try {
                                Path absPath = path.toAbsolutePath().normalize();
                                // Skip adding the zip file into itself if it's inside the source tree
                                if (absPath.equals(zipPath)) return;

                                String zipEntryName = sourcePath.getParent().relativize(path).toString().replace("\\", "/");
                                if (Files.isDirectory(path)) {
                                    // ensure directory entries end with /
                                    if (!zipEntryName.endsWith("/")) zipEntryName = zipEntryName + "/";
                                    ZipEntry dirEntry = new ZipEntry(zipEntryName);
                                    zos.putNextEntry(dirEntry);
                                    zos.closeEntry();
                                } else {
                                    try (FileInputStream fis = new FileInputStream(path.toString())) {
                                        ZipEntry zipEntry = new ZipEntry(zipEntryName);
                                        zos.putNextEntry(zipEntry);
                                        int length;
                                        while ((length = fis.read(buffer)) > 0) {
                                            zos.write(buffer, 0, length);
                                        }
                                        zos.closeEntry();
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });

                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void unzip(String[] args) {
        if (args.length != 1 && args.length != 3) {
            System.err.println("Invalid number of arguments.");
            return;
        }
        if (args.length == 3 && !Objects.equals(args[1], "-d")) {
            System.err.println("Invalid arguments.");
            return;
        }
        File fileToUnzip = new File(args[0]);
        if (!fileToUnzip.isAbsolute())
            fileToUnzip = new File(pwd(), args[0]);
        File destination = new File(pwd());
        if (args.length == 3) {
            destination = new File(args[2]);
            if (!destination.isAbsolute())
                destination = new File(pwd(), args[2]);
            if (!destination.exists())
                destination.mkdirs();

        }

        try {
            FileInputStream fis = new FileInputStream(fileToUnzip.toString());
            ZipInputStream zis = new ZipInputStream(fis);
            try {
                ZipEntry entry = zis.getNextEntry();
                while (entry != null) {
                    File newFile = new File(destination, entry.getName());
                    if (entry.isDirectory())
                        newFile.mkdirs();
                    else if (newFile.getParentFile() != null && !newFile.getParentFile().exists())
                        newFile.getParentFile().mkdirs();
                    else {
                        FileOutputStream fos = new FileOutputStream(newFile);
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                        fos.close();
                    }
                    zis.closeEntry();
                    entry = zis.getNextEntry();
                }
            } finally {
                zis.close();
            }
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    // Command: touch
    public void touch(String[] args) {
        if (args.length != 1) {
            System.out.println("Error: touch requires exactly one file path argument.");
            return;
        }

        File file = new File(args[0]);
        if (!file.isAbsolute()) {
            file = new File(pwd(), args[0]);
        }

        try {
            if (file.createNewFile()) {
                // file created
            } else {
                // File exists
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
        if (!file.isAbsolute()) {
            file = new File(pwd(), args[0]);
        }

        if (!file.exists()) {
            System.out.println("Error: File not found: " + args[0]);
        } else if (file.isDirectory()) {
            System.out.println("Error: Cannot remove a directory with 'rm'. Use 'rmdir' or 'cp -r'.");
        } else if (file.delete()) {
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
            if (!file.isAbsolute()) {
                file = new File(pwd(), filename);
            }

            if (!file.exists() || file.isDirectory()) {
                return "Error: File not found or is a directory: " + filename;
            }
            else if (file.length() == 0){
                return "File is empty: " + filename;
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

    // Helper function for cp -r
    private void copyRecursive(Path source, Path destination) throws IOException {
        if (!Files.exists(source)) return;


        if (Files.isDirectory(destination)) {
            destination = destination.resolve(source.getFileName());
        }

        final Path target = destination;

        Files.walk(source)
                .forEach(sourcePath -> {
                    try {
                        Path destPath = target.resolve(source.relativize(sourcePath));
                        if (Files.isDirectory(sourcePath)) {
                            Files.createDirectories(destPath);
                        } else {
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


        Function<String, File> resolveFile = (pathName) -> {
            File file = new File(pathName);
            if (!file.isAbsolute()) {
                return new File(pwd(), pathName);
            }
            return file;
        };

        // Case: cp -r dir1 dir2
        if (args.length == 3 && args[0].equals("-r")) {
            File sourceDir = resolveFile.apply(args[1]);
            File destDir = resolveFile.apply(args[2]);

            if (!sourceDir.isDirectory() || !sourceDir.exists() || !destDir.isDirectory() || !destDir.exists()) {
                System.out.println("Error: 'cp -r' requires both arguments to be existing directories.");
                return;
            }
            try {
                copyRecursive(sourceDir.toPath(), destDir.toPath());
            } catch (IOException e) {
                System.out.println("Error during recursive copy: " + e.getMessage());
            }
            return;
        }
        else if (args[0].equals("-r")){
            System.out.println("Error: 'cp -r' requires both arguments to be existing directories.");
            return;
        }

        // Case: cp file1 file2
        if (args.length == 2) {
            File sourceFile = resolveFile.apply(args[0]);
            File destFile = resolveFile.apply(args[1]);

            if (!sourceFile.isFile() || !sourceFile.exists()) {
                System.out.println("Error: Source file not found or is a directory: " + args[0]);
                return;
            }

            try {
                Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.out.println("Error copying file: " + e.getMessage());
            }
            return;
        }

        System.out.println("Error: Invalid arguments for cp command.");
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
                rm(args);
                break;
            case "rmdir":
                rmdir(args);
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
                handleOutput(wc(args));
                break;
            case "zip":
                zip(args);
                break;
            case "unzip":
                unzip(args);
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
