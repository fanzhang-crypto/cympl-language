## Cympl: A Simple Language

Simple and clean implementation just to practice the theory of Compilers and Interpreters.

### Language highlights
- Statically typed
- Strongly typed
- C-like syntax
- First-class functions
- Support Closure
- Support Recursion
- Support Lambda expression

### Types
- Integer 32 bits
- Float 64 bit
- String Unicode characters, support concatenation and length property
- Boolean true or false
- Array 1-dimensional and multidimensional. can be initialized with
  - literal form like `[]` or `[1, 2, 3]` 
  - new form like `new int[10]` or `new int[10, 10]`
- Function type. 
  - `() -> int` means a function that takes no arguments and returns an integer
  - `(int) -> int` means a function that takes one integer and returns an integer
  - `(int, int) -> int` means a function that takes two integers and returns an integer
- Null is both a type and a value
- Void is a type, but not a value
- Any is a type which is a super type of any other type

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
- if, else, while, for, break, continue, return

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
