# Python基础语法速查手册

> 本文档适用对象：Python初学者、需要快速查阅语法的学习者

## 一、变量与数据类型

### 1.1 变量定义
```python
# 变量命名规则：字母/下划线开头，区分大小写
name = "Python"           # 字符串
age = 30                  # 整数
pi = 3.14                 # 浮点数
is_valid = True           # 布尔值
```

### 1.2 字符串操作
```python
# 字符串方法
text = "Hello, Python"
len(text)                 # 获取长度
text.upper()              # 转大写
text.lower()              # 转小写
text.replace("Python", "World")  # 替换
text.split(",")           # 分割

# 字符串格式化
name = "Alice"
age = 25
# f-string（推荐）
print(f"姓名：{name}，年龄：{age}")
# format 方法
print("姓名：{}，年龄：{}".format(name, age))
```

## 二、列表

### 2.1 列表操作
```python
# 创建列表
numbers = [1, 2, 3, 4, 5]
mixed = [1, "hello", 3.14, True]

# 索引与切片
numbers[0]       # 第一个元素：1
numbers[-1]      # 最后一个元素：5
numbers[1:3]     # 切片：[2, 3]

# 常用方法
numbers.append(6)        # 添加元素
numbers.insert(0, 0)     # 指定位置插入
numbers.remove(3)        # 删除指定值
numbers.pop()            # 删除最后一个
```

### 2.2 列表推导式
```python
# 基本语法：[表达式 for 变量 in 序列]
squares = [x**2 for x in range(10)]
# 带条件
even = [x for x in range(20) if x % 2 == 0]
```

## 三、字典

### 3.1 字典操作
```python
# 创建字典
person = {
    "name": "Alice",
    "age": 30,
    "city": "Beijing"
}

# 访问与修改
person["name"]           # 获取值
person["age"] = 31       # 修改值
person["email"] = "alice@email.com"  # 添加新键值对

# 遍历字典
for key, value in person.items():
    print(f"{key}: {value}")
```

## 四、控制流

### 4.1 条件语句
```python
# if-elif-else
score = 85
if score >= 90:
    grade = "A"
elif score >= 80:
    grade = "B"
elif score >= 60:
    grade = "C"
else:
    grade = "D"
```

### 4.2 循环
```python
# for 循环
for i in range(5):
    print(i)  # 0, 1, 2, 3, 4

# for 循环遍历列表
fruits = ["apple", "banana", "cherry"]
for fruit in fruits:
    print(fruit)

# while 循环
count = 0
while count < 5:
    print(count)
    count += 1
```

## 五、函数

### 5.1 函数定义与调用
```python
# 基本函数
def greet(name):
    return f"Hello, {name}!"

# 默认参数
def power(base, exp=2):
    return base ** exp

# 可变参数
def sum_all(*args):
    return sum(args)

# 关键字参数
def introduce(**kwargs):
    for key, value in kwargs.items():
        print(f"{key}: {value}")
```

### 5.2 Lambda 表达式
```python
# 基本语法
square = lambda x: x ** 2
# 用于排序
users = [{"name": "Alice", "age": 30}, {"name": "Bob", "age": 25}]
sorted_users = sorted(users, key=lambda x: x["age"])
```

## 六、常用内置函数

| 函数 | 作用 | 示例 |
|-----|------|-----|
| len() | 获取长度 | len("Python") → 6 |
| type() | 获取类型 | type(10) → int |
| int() | 转整数 | int("10") → 10 |
| str() | 转字符串 | str(10) → "10" |
| sum() | 求和 | sum([1, 2, 3]) → 6 |
| max() | 最大值 | max([1, 2, 3]) → 3 |
| min() | 最小值 | min([1, 2, 3]) → 1 |
| sorted() | 排序 | sorted([3, 1, 2]) → [1, 2, 3] |
