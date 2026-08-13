# Python数据分析入门指南

> 本文档适用对象：想学习数据分析的学习者、数据科学入门者

## 一、数据分析工作流

数据分析的完整工作流程：

数据采集 → 数据清洗 → 探索性分析 → 可视化 → 建模 → 报告

## 二、核心工具库

### 2.1 NumPy - 数值计算
```python
import numpy as np

# 创建数组
arr = np.array([1, 2, 3, 4, 5])
zeros = np.zeros((3, 4))
ones = np.ones((2, 3))
range_arr = np.arange(0, 10, 2)

# 数学运算
arr + 10          # 广播
arr * 2
np.sqrt(arr)
np.mean(arr)
np.std(arr)
```

### 2.2 Pandas - 数据处理
```python
import pandas as pd

# 读取数据
df = pd.read_csv('data.csv')
df = pd.read_excel('data.xlsx')

# 数据探索
df.head()          # 查看前5行
df.info()          # 数据信息
df.describe()      # 统计描述

# 数据筛选
df[df['age'] > 25]
df.query('age > 25 and city == "Beijing"')

# 分组聚合
df.groupby('category')['value'].sum()
df.groupby('category').agg({'value': ['sum', 'mean', 'count']})

# 缺失值处理
df.isnull().sum()
df.dropna()
df.fillna(value)
```

### 2.3 Matplotlib - 数据可视化
```python
import matplotlib.pyplot as plt

# 折线图
plt.plot(x, y)
plt.xlabel('X轴标签')
plt.ylabel('Y轴标签')
plt.title('图表标题')
plt.show()

# 柱状图
plt.bar(categories, values)

# 散点图
plt.scatter(x, y)

# 子图
fig, axes = plt.subplots(2, 2)
```

### 2.4 Seaborn - 高级可视化
```python
import seaborn as sns

# 热力图
sns.heatmap(correlation_matrix)

# 分布图
sns.distplot(data)
sns.kdeplot(data)

# 分类图
sns.barplot(x='category', y='value', data=df)
sns.boxplot(x='category', y='value', data=df)
```

## 三、实战项目示例

### 3.1 泰坦尼克数据分析
```python
# 加载数据
df = pd.read_csv('titanic.csv')

# 数据探索
df.info()
df.isnull().sum()

# 年龄缺失值填充
df['Age'].fillna(df['Age'].median(), inplace=True)

# 性别与存活率
df.groupby('Sex')['Survived'].mean()

# 可视化
sns.barplot(x='Pclass', y='Survived', data=df)
plt.title('乘客等级与存活率关系')
plt.show()
```

## 四、学习资源推荐

- Kaggle：实战数据集与竞赛平台
- UCI机器学习库：经典数据集
- Pandas官方文档：最权威的参考
- DataCamp：互动式数据分析课程
