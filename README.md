## Cympl: A Simple Language

A toy language implementation to learn how to build a compiler and interpreter.

### Language highlights
- Statically typed
- Strongly typed
- C-like syntax
- First-class functions
- Support Closure
- Support Recursion
- Support Lambda expression
- Support varargs function

### Types
- Integer 32 bits
- Float 64 bit
- String Unicode characters, support concatenation and length property
- Boolean
- Array 1-dimensional and multidimensional. can be initialized with
  - literal form like `[]` or `[1, 2, 3]` 
  - new form like `new int[10]` or `new int[10, 10]`
- Function 
  - `() -> int` means a function that takes no arguments and returns an integer
  - `(int) -> int` means a function that takes one integer and returns an integer
  - `(int, int) -> int` means a function that takes two integers and returns an integer
- Void
- Any is a super type of any other type

### C-like Syntax

#### C-like syntax for variable declaration and function declaration
- `int x = 1`
- `float y = 2.0`
- `string z = "hello"`
- `boolean b = true`
- `int[] a = [1, 2, 3]`
- `int[][] b = [[1, 2, 3], [4, 5, 6]]`
- `int[] c = new int[10]`
- `int[][] d = new int[10][10]`
- `int add(int x, int y) { return x + y; }`

#### C-like syntax for control flow
- if, else, while, for, break, continue, return, switch, case, default

#### C-like syntax for expression
- arithmetic: `+`, `-`, `*`, `/`, `%`. `+` is also defined on String, performs String concatenation
- logical: `&&`, `||`, `!`
  - `&&` and `||` have short-circuit semantics
- relational: `==`, `!=`, `>`, `<`, `>=`, `<=`
- array indexing: `a[0]`, `b[0][1]`
- function call: `add(1, 2)`
- variable assignment: `x = 1`
- lambda expression: `(int x) -> x + 1`
- closure: `int add(int x) { return (int y) -> x + y; }`

#### Built-in functions
- `println(any x)`
- `String readln(String prompt)`

## How to build and run

### Pre-requisites
- [Install GraalVM](https://www.graalvm.org/docs/getting-started/#install-graalvm)
The cli module needs graalvm compiler to be built into native executable.
To install GraalVM on macOS or Linux, SDKMAN! is recommended. 
Get SDKMAN! from [sdkman.io](https://sdkman.io/) and install the Liberica GraalVM distribution by using the following commands:
```shell
$ sdk install java 22.3.r17-nik
$ sdk use java 22.3.r17-nik
```

- [Install Gradle](https://gradle.org/install/)
SDKMAN! can also be used to install Gradle. 
```shell
$ sdk install gradle 8.0.2
```

### Build and run
- build and run as a jar file
```shell
$ gradle build && java -jar cli/build/libs/cli.jar
```

- build and run as a native executable
```shell
$ gradle nativeCompile && ./cli/build/bin/cympl
```
