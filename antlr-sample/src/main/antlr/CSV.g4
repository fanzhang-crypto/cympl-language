grammar CSV;

file: hdr (row)*;

hdr: row;

row: field (',' field)*'\r'?'\n';

field
    : TEXT          # TEXT
    | STRING        # STRING
    |               # EMPTY
    ;

WS: [ ] -> skip;
TEXT: ~[,\r\n"]+;
STRING: '"' ('""'|~'"')* '"';


