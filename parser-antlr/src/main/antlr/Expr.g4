grammar Expr;

prog: statement+EOF             #Program
;

statement
    : varDecl ';'                   #VariableDeclaration
    | funcDecl                      #FunctionDeclaration
    | assign ';'                    #Assignment
    | expr';'                       #Expression
    | returnStat                    #ReturnStatement
    | ifStat                        #IfStatement
    | whileStat                     #WhileStatement
    | forStat                       #ForStatement
    | breakStat                     #BreakStatement
    | continueStat                  #ContinueStatement
    | block                         #BlockStatement
;

varDecl: ID ':' type=(FLOAT_TYPE | INT_TYPE | STRING_TYPE) '=' expr;

funcDecl: 'func' ID '(' paramDecls? ')' (':' type=(FLOAT_TYPE | INT_TYPE | STRING_TYPE | VOID_TYPE))? block;

paramDecls: paramDecl (',' paramDecl)*;

paramDecl: ID ':' type=(FLOAT_TYPE | INT_TYPE | STRING_TYPE);

returnStat: 'return' expr? ';';

block: '{' statement* '}';

ifStat: IF '(' expr ')' thenBranch=statement (ELSE elseBranch=statement )?;

whileStat: WHILE '(' expr ')' statement;

forInit: varDecl | assign;
forStat: FOR '(' forInit? ';' cond=expr? ';' update=assign? ')' statement;

breakStat: BREAK ';';

continueStat: CONTINUE ';';

assign: ID '=' expr;

expr: ID '(' exprlist? ')'              # FunctionCall
    | MINUS expr                        # Negation
    | NOT expr                          # LogicalNot
    | '(' expr ')'                      # ParenthesizedExpression
    | <assoc=right> expr '^' expr       # Power
    | expr op=(TIMES | DIV | REM) expr  # MulDiv
    | expr op=(PLUS | MINUS) expr       # AddSub
    | expr op=(EQ | NEQ | GT | GTE | LT | LTE) expr           # Comparison
    | expr AND expr                     # LogicalAnd
    | expr OR expr                      # LogicalOr
    | bool=(TRUE | FALSE)               # BOOL
    | ID                # Variable
    | FLOAT             # FLOAT
    | INT               # INT
    | STRING            # STRING
    ;

exprlist: expr (',' expr)*;

PLUS: '+';
MINUS: '-';
TIMES: '*';
DIV: '/';
REM: '%';
EQ: '==';
NEQ: '!=';
GT: '>';
GTE: '>=';
LT: '<';
LTE: '<=';
NOT: '!';
AND: '&&';
OR: '||';

INT_TYPE: 'INT';
FLOAT_TYPE: 'FLOAT';
STRING_TYPE: 'STRING';
BOOL_TYPE: 'BOOL';
VOID_TYPE: 'VOID';

TRUE: 'true';
FALSE: 'false';
INT: DIGIT+;
FLOAT: DIGIT '.' DIGIT* | '.' DIGIT+;
STRING: '"' (ESC|.)*? '"';

IF: 'if';
ELSE: 'else';
WHILE: 'while';
FOR: 'for';
BREAK: 'break';
CONTINUE: 'continue';

ID: [a-zA-Z_][a-zA-Z0-9_]*;
COMMENT: '/*' .*? '*/' -> skip;
LINECOMMENT: ('//'|'#')~[\r\n]* -> skip;
WS: [ \t\r\n]+ -> skip;

fragment DIGIT: [0-9];
fragment ESC: '\\'[btnr"\\];

