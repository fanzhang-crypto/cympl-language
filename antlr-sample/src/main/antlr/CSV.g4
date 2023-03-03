grammar CSV;

file: hdr (row)*;

hdr: row;

row: field (',' field)*'\r'?'\n';

field
    : TEXT          # TEXT
    | STRING        # STRING
    |               # EMPTY
    ;

TEXT: ~[,\r\n"]+;
STRING: '"' ('""'|~'"')* '"';
WS: [ ] -> skip;


