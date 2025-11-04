# Mini-Command-Line-Interpreter
mini-cli is a lightweight, cross-platform command-line interface implemented in Java. It mimics core Unix shell functionalities while remaining fully portable on Windows, macOS, and Linux.
# mini-cli

A minimal, cross-platform command-line interface written in Java.  
`mini-cli` provides basic shell functionality such as navigating directories, listing files, copying data, zipping/unzipping, and more — all implemented from scratch without relying on system shell commands.

---

## 🚀 Features

### ✅ Core Shell Commands
| Command | Description |
|--------|-------------|
| `pwd` | Show current working directory |
| `cd <path>` | Change directory (supports absolute & relative paths) |
| `ls` | List directory contents |
| `cat <file>` | Print file contents |
| `wc <file>` | Count lines, words, characters |
| `cp <src> <dest>` | Copy files |
| `rm <file>` | Delete a file |
| `mkdir <dir>` | Create directory |
| `rmdir <dir>` | Remove directory |
| `touch <file>` | Create an empty file |
| `zip <zipname> <files...>` | Create a zip archive |
| `unzip <zipfile>` | Extract a zip file |
| `help` | Show help menu |

---

## 🧱 Architecture

The project is organized into modular components:
src/
├── Terminal.java # main loop, command dispatcher
├── Parser.java # tokenizing & argument parsing
├── Commands/ # commands implementation
└── Utils/ # helpers (path resolver, zip utils, IO utils)

### ✅ Clear separation of concerns
- **Parser** handles tokenizing commands (supports quoting `"text with spaces"`).
- **Executor** maps commands → functions.
- **FileSystemUtils** ensures safe path handling for all OS environments.
- **ZipUtils** wraps Java zip APIs with safe and simple usage.

---

## 📦 Installation

### 1. Clone the repository
```sh
git clone https://github.com/<your-username>/mini-cli.git
cd mini-cli

2. Compile

Using Gradle:

./gradlew build


Or using javac:

javac -d out src/**/*.java

3. Run
java -cp out Terminal

🖥️ Usage Example
> pwd
C:/Users/Zaki

> ls
src  README.md  build.gradle

> mkdir test
> cd test
> touch hello.txt
> cat hello.txt
