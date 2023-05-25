grammar Cympl;

@header {
package cympl.parser.antlr;
}

prog: statement+EOF             #Program
;

statement
    : varDecl ';'                   #VariableDeclaration
    | funcDecl                      #FunctionDeclaration
    | assign ';'                    #Assignment
    | expr ';'                      #Expression
    | returnStat                    #ReturnStatement
    | ifStat                        #IfStatement
    | whileStat                     #WhileStatement
    | forStat                       #ForStatement
    | breakStat                     #BreakStatement
    | continueStat                  #ContinueStatement
    | switchStat                    #SwitchStatement
    | block                         #BlockStatement
;

type
    : VOID_TYPE                     #VoidType
    | INT_TYPE                      #IntType
    | FLOAT_TYPE                    #FloatType
    | STRING_TYPE                   #StringType
    | BOOL_TYPE                     #BoolType
    | funcType                      #FunctionType
    | '(' funcType ')' '[' ']'      #FunctionArrayType //to distinguish cases like (() -> int)[] and () -> int[]
    | type '[' ']'                  #ArrayType
    ;

funcType: '(' paramTypes=typeList? ')' ARROW_RIGHT retType=type;
typeList
    : type (',' type)* (',' varargType)?
    | varargType
    ;
varargType: type '...';

varDecl: type ID '=' expr;

funcDecl:  type ID '(' paramDecls? ')' block;

paramDecls
    : paramDecl (',' paramDecl)* (',' variableParamDecl)?
    | variableParamDecl
    ;

paramDecl: type ID;

variableParamDecl: type '...' ID;

returnStat: RETURN expr? ';';

block: '{' statement* '}';

ifStat: IF '(' cond=expr ')' thenBranch=statement (ELSE elseBranch=statement )?;

whileStat: WHILE '(' cond=expr ')' body=statement;

forStat: FOR '(' forInit? ';' cond=expr? ';' forUpdate? ')' body=statement;
forInit: varDecl | assign;
forUpdate: expr | assign;

breakStat: BREAK ';';

continueStat: CONTINUE ';';

switchStat: SWITCH '(' expr ')' '{' caseStat* defaultCase? '}';

caseStat: CASE expr ':' statement? breakStat?;
defaultCase: DEFAULT ':' statement;

assign: (lvalue=expr) '=' (rvalue=expr);

idList: ID (',' ID)*;

expr: funcExpr=expr '(' paramList=exprlist? ')'         # FunctionCall
    | NEW type ('[' expr ']')+                          # NewArray
    | arrayExpr=expr '[' indexExpr=expr ']'             # ArrayAccess
    | '(' idList? ')' ARROW_RIGHT (expr | statement)    # Lambda
    | MINUS expr                                        # Negation
    | NOT expr                                          # LogicalNot
    | '(' expr ')'                                      # ParenthesizedExpression
    | expr '.' ID                                       # Property
    | op=(INC | DEC) expr                               # PreIncDec
    | expr op=(INC | DEC)                               # PostIncDec
    | <assoc=right> expr '^' expr                       # Power
    | expr op=(TIMES | DIV | REM) expr                  # MulDiv
    | expr op=(PLUS | MINUS) expr                       # AddSub
    | expr op=(EQ | NEQ | GT | GTE | LT | LTE) expr     # Comparison
    | expr AND expr                                     # LogicalAnd
    | expr OR expr                                      # LogicalOr
    | ID                                                # Variable
    | bool=(TRUE | FALSE)                               # BoolLiteral
    | INT                                               # IntLiteral
    | FLOAT                                             # FloatLiteral
    | STRING                                            # StringLiteral
    | '[' elements=exprlist? ']'                        # ArrayLiteral
    ;

exprlist: expr (',' expr)*;

ARROW_RIGHT: '->';
INC: '++';
DEC: '--';
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

INT_TYPE: 'int';
FLOAT_TYPE: 'float';
STRING_TYPE: 'String';
BOOL_TYPE: 'bool';
VOID_TYPE: 'void';

TRUE: 'true';
FALSE: 'false';
INT: DIGIT+;
FLOAT: DIGIT '.' DIGIT* | '.' DIGIT+;
STRING: '"' (ESC|.)*? '"';

NEW: 'new';
IF: 'if';
ELSE: 'else';
WHILE: 'while';
FOR: 'for';
SWITCH: 'switch';
CASE: 'case';
DEFAULT: 'default';
BREAK: 'break';
CONTINUE: 'continue';
RETURN: 'return';

ID: [a-zA-Z_][a-zA-Z0-9_]*;
COMMENT: '/*' .*? '*/' -> skip;
LINECOMMENT: ('//'|'#')~[\r\n]* -> skip;
WS: [ \t\r\n]+ -> skip;

fragment DIGIT: [0-9];
fragment ESC: '\\'[btnr"\\];

