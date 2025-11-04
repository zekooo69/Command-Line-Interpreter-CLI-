import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;   
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

class Parser {
    private String commandName;
    private String[] args;

    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("\"([^\"]*)\"|'([^']*)'|(\\S+)");

    public boolean parse(String input) {
        commandName = null;
        args = new String[0];

        if (input == null) return false;
        input = input.trim();
        if (input.isEmpty()) return false;

        List<String> tokens = new ArrayList<>();
        Matcher m = TOKEN_PATTERN.matcher(input);
        while (m.find()) {
            if (m.group(1) != null) tokens.add(m.group(1));
            else if (m.group(2) != null) tokens.add(m.group(2));
            else tokens.add(m.group(3));
        }

        if (tokens.isEmpty()) return false;

        commandName = tokens.get(0);
        if (tokens.size() > 1)
            args = tokens.subList(1, tokens.size()).toArray(new String[0]);
        else
            args = new String[0];

        return true;
    }

    public String getCommandName() {
        return commandName;
    }

    public String[] getArgs() {
        return args == null ? new String[0] : args;
    }
}

public class Terminal {
    private Parser parser;
    private Path currentDir;

    public Terminal() {
        this.parser = new Parser();
        this.currentDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

    public String pwd() {
        return currentDir.toString();
    }

    public void cd(String[] args) {
        Path target;
        if (args == null || args.length == 0) {
            target = Paths.get(System.getProperty("user.home"));
        } else {
            String p = args[0];
            if (p.equals("..")) {
                Path parent = currentDir.getParent();
                if (parent == null) {
                    System.out.println("cd: already at filesystem root");
                    return;
                } else {
                    currentDir = parent.toAbsolutePath().normalize();
                    return;
                }
            } else if (p.equals(".")) {
                return;
            } else {
                Path candidate = Paths.get(p);
                if (!candidate.isAbsolute()) candidate = currentDir.resolve(candidate);
                target = candidate.normalize();
            }
        }

        if (!Files.exists(target)) {
            System.out.println("cd: no such file or directory: " + target);
            return;
        }
        if (!Files.isDirectory(target)) {
            System.out.println("cd: not a directory: " + target);
            return;
        }
        currentDir = target.toAbsolutePath().normalize();
    }

        public void ls() {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(currentDir)) {
                List<String> names = new ArrayList<>();
                for (Path p : ds) names.add(p.getFileName().toString());
                Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
                for (String name : names) System.out.println(name);
            } catch (IOException e) {
                System.out.println("ls: error reading directory: " + e.getMessage());
            }
        }

    public void mkdir(String[] args) {
        if (args == null || args.length == 0) {
            System.out.println("mkdir: missing operand");
            return;
        }
        for (String s : args) {
            Path path = Paths.get(s);
            if (!path.isAbsolute()) path = currentDir.resolve(path);
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                System.out.println("mkdir: cannot create directory '" + path + "': " + e.getMessage());
            }
        }
    }

    public void rmdir(String[] args) {
        if (args == null || args.length == 0) {
            System.out.println("rmdir: missing operand");
            return;
        }
        if (args.length == 1 && args[0].equals("*")) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(currentDir)) {
                for (Path p : ds) {
                    if (Files.isDirectory(p) && isDirectoryEmpty(p)) {
                        try {
                            Files.delete(p);
                            System.out.println("rmdir: removed empty directory " + p.getFileName());
                        } catch (IOException e) {
                            System.out.println("rmdir: cannot remove " + p.getFileName() + ": " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("rmdir: error scanning current directory: " + e.getMessage());
            }
            return;
        }

        String s = args[0];
        Path path = Paths.get(s);
        if (!path.isAbsolute()) path = currentDir.resolve(path);
        path = path.normalize();
        if (!Files.exists(path)) {
            System.out.println("rmdir: failed to remove '" + path + "': No such file or directory");
            return;
        }
        if (!Files.isDirectory(path)) {
            System.out.println("rmdir: failed to remove '" + path + "': Not a directory");
            return;
        }
        if (!isDirectoryEmpty(path)) {
            System.out.println("rmdir: failed to remove '" + path + "': Directory not empty");
            return;
        }
        try {
            Files.delete(path);
        } catch (IOException e) {
            System.out.println("rmdir: failed to remove '" + path + "': " + e.getMessage());
        }
    }

    private boolean isDirectoryEmpty(Path dir) {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            return !ds.iterator().hasNext();
        } catch (IOException e) {
            return false;
        }
    }

    public void touch(String[] args) {
        if (args == null || args.length == 0) {
            System.out.println("touch: missing file operand");
            return;
        }
        String s = args[0];
        Path p = Paths.get(s);
        if (!p.isAbsolute()) p = currentDir.resolve(p);
        p = p.normalize();
        try {
            Path parent = p.getParent();
            if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
            if (!Files.exists(p)) {
                Files.createFile(p);
            } else {
                Files.setLastModifiedTime(p, FileTime.fromMillis(System.currentTimeMillis()));
            }
        } catch (IOException e) {
            System.out.println("touch: cannot touch '" + p + "': " + e.getMessage());
        }
    }

    public void rm(String[] args) {
        if (args == null || args.length != 1) {
            System.out.println("rm: usage: rm <file>");
            return;
        }
        Path p = Paths.get(args[0]);
        if (!p.isAbsolute()) p = currentDir.resolve(p);
        p = p.normalize();
        if (!Files.exists(p)) {
            System.out.println("rm: cannot remove '" + p.getFileName() + "': No such file");
            return;
        }
        if (Files.isDirectory(p)) {
            System.out.println("rm: cannot remove '" + p.getFileName() + "': Is a directory");
            return;
        }
        try {
            Files.delete(p);
        } catch (IOException e) {
            System.out.println("rm: error removing '" + p.getFileName() + "': " + e.getMessage());
        }
    }

    public void cat(String[] args) {
        if (args == null || (args.length != 1 && args.length != 2)) {
            System.out.println("cat: usage: cat <file>  OR  cat <file1> <file2>");
            return;
        }
        for (String fname : args) {
            Path p = Paths.get(fname);
            if (!p.isAbsolute()) p = currentDir.resolve(p);
            p = p.normalize();
            if (!Files.exists(p)) {
                System.out.println("cat: " + p + ": No such file or directory");
                return;
            }
            if (Files.isDirectory(p)) {
                System.out.println("cat: " + p + ": Is a directory");
                return;
            }
            try (BufferedReader br = Files.newBufferedReader(p)) {
                String line;
                while ((line = br.readLine()) != null) System.out.println(line);
            } catch (IOException e) {
                System.out.println("cat: error reading '" + p + "': " + e.getMessage());
                return;
            }
        }
    }

    public void wc(String[] args) {
        if (args == null || args.length != 1) {
            System.out.println("wc: usage: wc <file>");
            return;
        }
        Path p = Paths.get(args[0]);
        if (!p.isAbsolute()) p = currentDir.resolve(p);
        p = p.normalize();
        if (!Files.exists(p) || Files.isDirectory(p)) {
            System.out.println("wc: " + p + ": No such file or is a directory");
            return;
        }

        long lines = 0, words = 0, chars = 0;
        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line;
            while ((line = br.readLine()) != null) {
                lines++;
                String[] ws = line.trim().isEmpty() ? new String[0] : line.trim().split("\\s+");
                words += ws.length;
                chars += line.length() + 1;
            }
        } catch (IOException e) {
            System.out.println("wc: error reading '" + p + "': " + e.getMessage());
            return;
        }
        System.out.println(lines + " " + words + " " + chars + " " + p.getFileName());
    }

    public void cp(String[] args) {
        if (args == null || args.length == 0) {
            System.out.println("cp: missing operand");
            return;
        }

        if (args.length >= 1 && args[0].equals("-r")) {
            if (args.length != 3) {
                System.out.println("cp: usage: cp -r <srcDir> <dstDir>");
                return;
            }
            Path src = Paths.get(args[1]);
            if (!src.isAbsolute()) src = currentDir.resolve(src);
            src = src.normalize();

            Path dst = Paths.get(args[2]);
            if (!dst.isAbsolute()) dst = currentDir.resolve(dst);
            dst = dst.normalize();

            if (!Files.exists(src) || !Files.isDirectory(src)) {
                System.out.println("cp: source directory does not exist or is not a directory: " + src);
                return;
            }

            try {
                if (!Files.exists(dst)) Files.createDirectories(dst);
                else if (!Files.isDirectory(dst)) {
                    System.out.println("cp: destination exists and is not a directory: " + dst);
                    return;
                }
                Path dstTarget = dst.resolve(src.getFileName());
                copyDirectoryRecursively(src, dstTarget);
            } catch (IOException e) {
                System.out.println("cp: error copying directories: " + e.getMessage());
            }
            return;
        }

        if (args.length != 2) {
            System.out.println("cp: usage: cp <source-file> <target-file>");
            return;
        }
        Path src = Paths.get(args[0]);
        if (!src.isAbsolute()) src = currentDir.resolve(src);
        src = src.normalize();

        Path dst = Paths.get(args[1]);
        if (!dst.isAbsolute()) dst = currentDir.resolve(dst);
        dst = dst.normalize();

        if (!Files.exists(src) || Files.isDirectory(src)) {
            System.out.println("cp: source does not exist or is a directory: " + src);
            return;
        }
        try {
            Path dstParent = dst.getParent();
            if (dstParent != null && !Files.exists(dstParent)) Files.createDirectories(dstParent);
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.out.println("cp: error copying file: " + e.getMessage());
        }
    }

    private void copyDirectoryRecursively(Path src, Path dst) throws IOException {
        if (Files.exists(dst) && !Files.isDirectory(dst))
            throw new IOException("Target exists and is not a directory: " + dst);
        Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path rel = src.relativize(dir);
                Path targetDir = dst.resolve(rel);
                if (!Files.exists(targetDir)) Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path rel = src.relativize(file);
                Path targetFile = dst.resolve(rel);
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void zipCmd(String[] args) {
        if (args == null || args.length < 2) {
            System.out.println("zip: usage: zip <archive.zip> <file1> [file2 ...]   OR   zip -r <archive.zip> <directory>");
            return;
        }
        boolean recursive = false;
        int idx = 0;
        if (args[0].equals("-r")) {
            recursive = true;
            idx = 1;
        }

        if (idx + 1 >= args.length) {
            System.out.println("zip: missing arguments");
            return;
        }

        Path archive = Paths.get(args[idx]);
        if (!archive.isAbsolute()) archive = currentDir.resolve(archive);
        archive = archive.normalize();

        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(archive)))) {
            if (recursive) {
                Path src = Paths.get(args[idx + 1]);
                if (!src.isAbsolute()) src = currentDir.resolve(src);
                src = src.normalize();
                if (!Files.exists(src) || !Files.isDirectory(src)) {
                    System.out.println("zip: source directory does not exist or is not a directory: " + src);
                    return;
                }
                Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Path rel = src.relativize(file);
                        String entryName = src.getFileName().resolve(rel).toString().replace("\\", "/");
                        zos.putNextEntry(new ZipEntry(entryName));
                        Files.copy(file, zos);
                        zos.closeEntry();
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                for (int i = idx + 1; i < args.length; i++) {
                    Path f = Paths.get(args[i]);
                    if (!f.isAbsolute()) f = currentDir.resolve(f);
                    f = f.normalize();
                    if (!Files.exists(f) || Files.isDirectory(f)) {
                        System.out.println("zip: skipping (not a file): " + f);
                        continue;
                    }
                    String entryName = f.getFileName().toString();
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(f, zos);
                    zos.closeEntry();
                }
            }
            System.out.println("zip: created " + archive);
        } catch (IOException e) {
            System.out.println("zip: error creating archive: " + e.getMessage());
        }
    }

    public void unzipCmd(String[] args) {
        if (args == null || args.length == 0) {
            System.out.println("unzip: usage: unzip <archive.zip> [-d <destDir>]");
            return;
        }
        Path archive = null;
        Path dest = currentDir;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-d")) {
                if (i + 1 < args.length) {
                    Path d = Paths.get(args[i + 1]);
                    if (!d.isAbsolute()) d = currentDir.resolve(d);
                    dest = d.normalize();
                } else {
                    System.out.println("unzip: -d requires a destination directory");
                    return;
                }
            } else if (args[i].toLowerCase().endsWith(".zip")) {
                if (archive == null) {
                    Path a = Paths.get(args[i]);
                    if (!a.isAbsolute()) a = currentDir.resolve(a);
                    archive = a.normalize();
                }
            } else if (archive == null && args.length == 1) {
                Path a = Paths.get(args[i]);
                if (!a.isAbsolute()) a = currentDir.resolve(a);
                archive = a.normalize();
            }
        }

        if (archive == null) {
            System.out.println("unzip: archive .zip not specified");
            return;
        }
        if (!Files.exists(archive)) {
            System.out.println("unzip: cannot find zipfile: " + archive);
            return;
        }

        try {
            if (!Files.exists(dest)) Files.createDirectories(dest);
        } catch (IOException e) {
            System.out.println("unzip: cannot create destination: " + dest + " : " + e.getMessage());
            return;
        }

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(archive)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path outPath = dest.resolve(entry.getName()).normalize();
                if (!outPath.startsWith(dest)) {
                    System.out.println("unzip: skipping unsafe entry " + entry.getName());
                    zis.closeEntry();
                    continue;
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    Path parent = outPath.getParent();
                    if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
                    try (OutputStream os = Files.newOutputStream(outPath)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) os.write(buffer, 0, len);
                    }
                }
                zis.closeEntry();
            }
            System.out.println("unzip: extracted to " + dest);
        } catch (IOException e) {
            System.out.println("unzip: error extracting: " + e.getMessage());
        }
    }

    public void help() {
        System.out.println("Supported commands:");
        System.out.println("  pwd");
        System.out.println("  cd [dir]");
        System.out.println("  ls");
        System.out.println("  mkdir <paths>");
        System.out.println("  rmdir * | rmdir <dir>");
        System.out.println("  touch <file>");
        System.out.println("  rm <file>");
        System.out.println("  cat <file> OR cat <file1> <file2>");
        System.out.println("  wc <file>");
        System.out.println("  cp <src> <dst>");
        System.out.println("  cp -r <srcDir> <dstDir>");
        System.out.println("  zip <archive.zip> <file1> [file2 ...]");
        System.out.println("  zip -r <archive.zip> <directory>");
        System.out.println("  unzip <archive.zip> [-d <destDir>]");
        System.out.println("  Redirection: use '>' to overwrite or '>>' to append.");
        System.out.println("  help");
        System.out.println("  exit");
    }

    public boolean chooseCommandAction(String input) {
        boolean parsed = parser.parse(input);
        if (!parsed) return true;

        String cmd = parser.getCommandName();
        String[] fullArgs = parser.getArgs();

        boolean redirect = false;
        boolean append = false;
        Path redirectPath = null;
        List<String> actualArgs = new ArrayList<>();
        for (int i = 0; i < fullArgs.length; i++) {
            if (fullArgs[i].equals(">") || fullArgs[i].equals(">>")) {
                redirect = true;
                append = fullArgs[i].equals(">>");
                if (i + 1 < fullArgs.length) {
                    Path p = Paths.get(fullArgs[i + 1]);
                    if (!p.isAbsolute()) p = currentDir.resolve(p);
                    redirectPath = p.normalize();
                } else {
                    System.out.println("Redirection operator requires a filename");
                    return true;
                }
                break;
            } else {
                actualArgs.add(fullArgs[i]);
            }
        }

        PrintStream originalOut = System.out;
        ByteArrayOutputStream baos = null;
        if (redirect) {
            baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            System.setOut(ps);
        }

        try {
            switch (cmd) {
                case "pwd":
                    System.out.println(pwd());
                    break;
                case "cd":
                    cd(actualArgs.toArray(new String[0]));
                    break;
                case "ls":
                    ls();
                    break;
                case "mkdir":
                    mkdir(actualArgs.toArray(new String[0]));
                    break;
                case "rmdir":
                    rmdir(actualArgs.toArray(new String[0]));
                    break;
                case "touch":
                    touch(actualArgs.toArray(new String[0]));
                    break;
                case "rm":
                    rm(actualArgs.toArray(new String[0]));
                    break;
                case "cat":
                    cat(actualArgs.toArray(new String[0]));
                    break;
                case "wc":
                    wc(actualArgs.toArray(new String[0]));
                    break;
                case "cp":
                    cp(actualArgs.toArray(new String[0]));
                    break;
                case "zip":
                    zipCmd(actualArgs.toArray(new String[0]));
                    break;
                case "unzip":
                    unzipCmd(actualArgs.toArray(new String[0]));
                    break;
                case "help":
                    help();
                    break;
                case "exit":
                    return false;
                default:
                    System.out.println(cmd + " ");
            }
        } finally {
            if (redirect) {
                System.out.flush();
                System.setOut(originalOut);
                if (redirectPath == null) {
                    System.out.println("Redirection failed: no target file specified");
                } else {
                    try {
                        Path parent = redirectPath.getParent();
                        if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
                        if (!append) {
                            Files.write(redirectPath, baos.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                        } else {
                            Files.write(redirectPath, baos.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        }
                    } catch (IOException e) {
                        System.out.println("Redirection error: " + e.getMessage());
                    }
                }
            }
        }

        return true;
    }

    public static void main(String[] args) {
        Terminal term = new Terminal();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Mini-CLI ready. Type 'help' for available commands. 'exit' to quit.");
        while (true) {
            try {
                System.out.print(term.pwd() + " $ ");
                String line = reader.readLine();
                if (line == null) break;
                boolean cont = term.chooseCommandAction(line);
                if (!cont) break;
            } catch (IOException e) {
                System.out.println("I/O error: " + e.getMessage());
            }
        }
        System.out.println("Goodbye.");
    }
}
