```markdown
# Random-Block-Placement Development Patterns

> Auto-generated skill from repository analysis

## Overview
This skill teaches you the core development patterns and conventions used in the `Random-Block-Placement` Java codebase. You'll learn how to structure files, write and organize code, follow commit message guidelines, and understand the project's testing approach. This guide is ideal for contributors who want to maintain consistency and quality in this repository.

## Coding Conventions

### File Naming
- Use **PascalCase** for all file names.
  - **Example:** `BlockManager.java`, `RandomBlockGenerator.java`

### Import Style
- Use **relative imports** within the project.
  - **Example:**
    ```java
    import mypackage.BlockManager;
    ```

### Export Style
- Use **named exports** (Java's `public` classes and methods).
  - **Example:**
    ```java
    public class BlockManager {
        public void placeBlock() { ... }
    }
    ```

### Commit Messages
- Follow the **conventional commit** format.
- Use **type** prefixes, such as `refactor`.
- Keep commit messages descriptive (average ~90 characters).
  - **Example:**
    ```
    refactor: optimize block placement algorithm for better performance
    ```

## Workflows

### Refactoring Code
**Trigger:** When improving code structure or performance without changing external behavior  
**Command:** `/refactor`

1. Identify code that can be improved (e.g., cleaner logic, better naming).
2. Make the necessary changes in the codebase.
3. Write a commit message starting with `refactor:`, describing the change.
4. Ensure all tests pass before pushing.

## Testing Patterns

- **Test files** use the `*.test.*` naming pattern.
  - **Example:** `BlockManager.test.java`
- The specific testing framework is **unknown**, so check existing test files for structure and assertions.
- Place tests alongside or within a dedicated test directory as per existing patterns.

## Commands
| Command    | Purpose                                      |
|------------|----------------------------------------------|
| /refactor  | Start a code refactoring workflow            |

```