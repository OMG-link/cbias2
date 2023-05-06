# RISC-V 汇编语言建模

## 静态变量

注：float和int的标记格式基本相同，区别仅为存储数值的格式，因此以下不再区分

### 基本结构

+ .globl tag
+ 段描述符（见下）
+ .align k -> 按照2^k对齐
+ .type tag, @object
+ .size tag, size
+ tag:
+ 数据内容

### 未初始化普通变量

+ .section .sbss,"aw",@nobits
+ sbss->small data bss section
+ aw->readable & writable
+ @nobits 未初始化变量不占用内存空间
+ 对齐为数据大小，通常是2^2

### 已初始化普通变量

+ .section .sdata,"aw"
+ 对齐为数据大小，通常为2^2

### 未初始化数组变量

+ .bss
+ .align 3

### 已初始化数组变量

+ .data
+ .align 3

### 普通常量

+ .section .srodata,"a"
+ 对齐为数据大小，通常为2^2

### 数组常量

+ .section .rodata
+ .align 3

## 函数

### 函数参数规范

前8个参数使用寄存器a0~a7(x10~x17)与浮点参数直接传输，剩余参数直接倒序压入栈中，靠近栈顶的为靠前的参数。

注意，参数均占据8个字节（视为64位）

### 函数头

+ .text
+ .align 1
+ .globl functionName
+ .type functionName, @function
+ functionName:
+ 函数体内容
+ .size functionName, .-functionName

### 栈帧规范

栈顶寄存器sp减去栈帧大小，将返回寄存器ra的值存储在栈的最深处，栈帧寄存器的值存储在第二位，随后将其赋值为原栈顶位置。