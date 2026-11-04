Output "assets/demo.gif"

Set Theme "Catppuccin Mocha"
Set Width 1050
Set Height 680
Set FontSize 15
Set LineHeight 1.25
Set Padding 18
Set WindowBar "Colorful"
Set BorderRadius 8
Set TypingSpeed 35ms

# Setup clean environment & prompt
Hide
Type "cd demo"
Enter
Type "export PS1='\[\033[01;36m\]annovibe-demo\[\033[00m\]:\[\033[01;34m\]~\[\033[00m\]$ '"
Enter
Type "clear"
Enter
Show
Sleep 1s

# 1. View clean source code: formatBytes stub returns null, main calls it
Type "cat src/main/java/ByteUtils.java"
Enter
Sleep 3.5s

# 2. Compile with javac + Annovibe (synthesizes bytecode at compile time)
Type "mvn compile -Dstyle.color=always"
Enter
Sleep 5s

# 3. Run the compiled class: verifies the vibe works live!
Type "java -cp target/classes ByteUtils"
Enter
Sleep 3.5s

# 4. Decompile target/classes/ByteUtils.class to inspect the generated code!
Type "cfr target/classes/ByteUtils.class"
Enter
Sleep 8.5s
