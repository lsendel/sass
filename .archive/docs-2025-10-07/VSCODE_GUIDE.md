# VS Code - Java Development Guide

**Project**: SASS Platform
**Date**: 2025-10-01
**Status**: ✅ Compilation Clean (0 errors, 0 warnings)

---

## 🎯 Quick Fix for VS Code Errors

If you see errors in VS Code but Gradle builds successfully:

### Run the Fix Script

```bash
./scripts/fix-vscode-java.sh
```

Then follow the 4 steps printed by the script.

---

## 📋 Manual Fix Steps

If the script doesn't work, follow these steps manually:

### Step 1: Clean Java Language Server Workspace

1. Press: `Cmd+Shift+P` (Mac) or `Ctrl+Shift+P` (Windows/Linux)
2. Type: `Java: Clean Java Language Server Workspace`
3. Press: `Enter`
4. Click: `Reload and delete` when prompted
5. Wait for VS Code to reload and re-index

### Step 2: Reload VS Code Window

1. Press: `Cmd+Shift+P`
2. Type: `Reload Window`
3. Press: `Enter`

### Step 3: Rebuild from Gradle

1. Press: `Cmd+Shift+B` (Mac) or `Ctrl+Shift+B` (Windows/Linux)
2. Select: `Gradle: Compile Java`
3. Wait for build to complete

### Step 4: Check Problems Panel

1. Press: `Cmd+Shift+M` (Mac) or `Ctrl+Shift+M` (Windows/Linux)
2. Should show: `0 errors, 0 warnings`

---

## ⚙️ VS Code Java Extension Settings

The project includes optimized settings in [.vscode/settings.json](.vscode/settings.json):

```json
{
  "java.configuration.updateBuildConfiguration": "automatic",
  "java.compile.nullAnalysis.mode": "disabled",
  "java.import.gradle.enabled": true,
  "java.import.gradle.wrapper.enabled": true,
  "java.project.sourcePaths": ["backend/src/main/java"]
}
```

---

## 🔍 Verify Build Status

### From Terminal

```bash
cd backend
./gradlew clean compileJava compileTestJava
```

**Expected Output**:

```
BUILD SUCCESSFUL in 1s
✅ 0 errors
✅ 0 warnings
```

### From VS Code Tasks

1. Press: `Cmd+Shift+P`
2. Type: `Tasks: Run Task`
3. Select: `Gradle: Compile Java`

---

## 🐛 Troubleshooting

### Problem: VS Code shows errors but Gradle succeeds

**Cause**: Java Language Server cache is stale

**Solution**:

1. Run: `./scripts/fix-vscode-java.sh`
2. Follow on-screen instructions
3. Restart VS Code if needed

### Problem: "Cannot resolve symbol" errors

**Cause**: Gradle dependencies not imported

**Solution**:

1. Open: `backend/build.gradle`
2. Right-click in editor
3. Select: `Gradle: Import Project`
4. Wait for import to complete

### Problem: "Method does not override" errors

**Cause**: Java version mismatch

**Solution**:

1. Check Java version: `java -version`
2. Should be: Java 21
3. If wrong version, update in VS Code settings:
   ```json
   {
     "java.configuration.runtimes": [
       {
         "name": "JavaSE-21",
         "path": "/path/to/jdk-21",
         "default": true
       }
     ]
   }
   ```

### Problem: Build tasks not showing

**Cause**: Tasks.json not loaded

**Solution**:

1. Check: `.vscode/tasks.json` exists
2. Reload window: `Cmd+Shift+P` → `Reload Window`

---

## 📊 Expected State

After following the fixes:

| Panel                            | Expected State                      |
| -------------------------------- | ----------------------------------- |
| **Problems** (Cmd+Shift+M)       | 0 errors, 0 warnings                |
| **Terminal** (`./gradlew build`) | BUILD SUCCESSFUL                    |
| **Output** (Java)                | No error messages                   |
| **Explorer**                     | No red squiggly lines in Java files |

---

## 🔧 Recommended VS Code Extensions

### Essential

1. **Extension Pack for Java** (Microsoft)
   - Language Support for Java
   - Debugger for Java
   - Test Runner for Java
   - Maven for Java
   - Gradle for Java

2. **Spring Boot Extension Pack** (VMware)
   - Spring Boot Tools
   - Spring Initializr

### Optional but Helpful

3. **SonarLint** - Code quality analysis
4. **Checkstyle** - Code style checking
5. **Error Lens** - Inline error display

---

## 📁 Project Structure

```
sass/
├── backend/                    # Java/Spring Boot backend
│   ├── src/
│   │   ├── main/java/         # Main source code
│   │   └── test/java/         # Test source code
│   ├── build.gradle           # Gradle build configuration
│   └── gradlew               # Gradle wrapper script
├── .vscode/
│   ├── settings.json         # VS Code Java settings
│   └── tasks.json            # Build tasks
└── scripts/
    └── fix-vscode-java.sh    # VS Code fix script
```

---

## 🚀 Quick Commands Reference

| Task                | Command                            |
| ------------------- | ---------------------------------- |
| **Clean & Compile** | `./gradlew clean compileJava`      |
| **Run Tests**       | `./gradlew test`                   |
| **Build All**       | `./gradlew build`                  |
| **Fix VS Code**     | `./scripts/fix-vscode-java.sh`     |
| **Verify Build**    | `./scripts/verify-build-status.sh` |

---

## ✅ Success Criteria

You'll know everything is working when:

1. ✅ Problems panel shows 0 errors
2. ✅ Terminal `./gradlew build` succeeds
3. ✅ No red squiggly lines in Java files
4. ✅ Auto-completion works for Java classes
5. ✅ Go to Definition (F12) works
6. ✅ Hover tooltips show documentation

---

## 💡 Tips

### Faster Reload

Instead of restarting VS Code, just reload the window:

```
Cmd+Shift+P → "Reload Window"
```

### View Java Output

To see what the Language Server is doing:

```
View → Output → Select "Java" from dropdown
```

### Increase Memory (if needed)

Add to `.vscode/settings.json`:

```json
{
  "java.jdt.ls.vmargs": "-Xmx2G"
}
```

---

## 📞 Getting Help

If problems persist after trying all fixes:

1. **Check Java Output**: View → Output → Java
2. **Check Language Server Status**: Look for errors in Java output
3. **Restart VS Code**: Completely quit and reopen
4. **Reinstall Java Extension**: Uninstall Extension Pack for Java, then reinstall
5. **Check Gradle**: Ensure `./gradlew build` works in terminal

---

## 🎓 Understanding the Disconnect

**Why does Gradle show different results than VS Code?**

- **Gradle**: Uses the build configuration in `build.gradle` directly
- **VS Code Java Extension**: Uses its own Language Server that analyzes code independently
- **The Language Server**:
  - Maintains its own cache of project structure
  - Can get out of sync after code changes
  - Needs to be refreshed when dependencies change

**The fix**: Clean the Language Server workspace to force it to re-analyze from scratch.

---

**Last Updated**: 2025-10-01
**Script Location**: `./scripts/fix-vscode-java.sh`
**Settings Location**: `.vscode/settings.json`
