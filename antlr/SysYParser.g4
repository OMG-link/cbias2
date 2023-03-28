parser grammar SysYParser;

options { tokenVocab = SysYLexer; }

compUnit: (decl | funcDef)*;

decl: constDecl | varDecl;

constDecl: CONST bType constDef (COMMA constDef)* SEMI;

bType: INT | FLOAT;

constDef: Ident (LBRACKET constExp RBRACKET)* ASSIGN constInitVal;

constInitVal: constExp
            | LBRACE (constInitVal (COMMA constInitVal)* )? RBRACE;

varDecl: bType varDef (COMMA varDef)* SEMI;

varDef: Ident (LBRACKET constExp RBRACKET)*
      | Ident (LBRACKET constExp RBRACKET)* ASSIGN initVal;

initVal: expr
       | LBRACE (initVal (COMMA initVal)* )? RBRACE;

funcDef: funcType Ident LPAREN funcFParams? RPAREN block;

funcType: VOID | INT | FLOAT;

funcFParams: funcFParam (COMMA funcFParam)*;

funcFParam: bType Ident (LBRACKET RBRACKET (LBRACKET expr RBRACKET)*)?;

block: LBRACE blockItem* RBRACE;

blockItem: decl | stmt;

stmt: lVal ASSIGN expr SEMI
    | expr? SEMI
    | block
    | IF LPAREN cond RPAREN stmt (ELSE stmt)?
    | WHILE LPAREN cond RPAREN stmt
    | BREAK SEMI
    | CONTINUE SEMI
    | RETURN expr? SEMI;

expr: addExp;

cond: lOrExp;

lVal: Ident (LBRACKET expr RBRACKET)*;

primaryExp: LPAREN expr RPAREN
          | lVal
          | number;

number: IntConst | FloatConst;

unaryExp: primaryExp
        | Ident LPAREN funcRParams? RPAREN
        | unaryOp unaryExp;

unaryOp: ADD | SUB | LNOT;

funcRParams: expr (COMMA expr)*;

mulExp: unaryExp ((MUL | DIV | MOD) unaryExp)*;

addExp: mulExp ((ADD | SUB) mulExp)*;

relExp: addExp ((LT | GT | LE | GE) addExp)*;

eqExp: relExp ((EQ | NE) relExp)*;

lAndExp: eqExp (LAND eqExp)*;

lOrExp: lAndExp (LOR lAndExp)*;

constExp: addExp;
