# 🚀 SimuLabSec - 半导体工艺仿真平台（增强版）

## 📌 项目简介

SimuLabSec 是一个面向半导体工艺优化的仿真平台，支持对晶圆（Wafer）在光刻过程中的对准（Overlay）进行模拟分析。  
用户可以通过配置不同参数（如波长、对准策略、工艺条件等），生成仿真结果并进行可视化分析，从而辅助工艺优化决策。

该项目为 SimuLab 的增强版本（Sec = Secure / Scalable），在原有基础上进一步优化了：

- 系统性能
- 并发计算能力
- 架构可扩展性

---

## 🧠 核心功能

### 🔬 1. 仿真计算

- 支持 wafer 数据生成
- 支持 overlay 套刻误差计算
- 多参数组合仿真（波长、级次等）

### ⚙️ 2. 并发计算优化

- 使用线程池 + 并行计算模型
- 支持任务级并发 + 任务内并行（两层并发）
- 提升计算效率

### 📊 3. 数据可视化

- Wafer 色温图（Heatmap）
- 折线图 / 散点图 / 直方图
- 表格数据展示

---

## 🏗️ 技术架构

### 后端

- Java
- Spring Boot
- Spring Data JPA
- MySQL
- Redis（缓存 / 分布式锁）

### 前端

- Vue 3
- ECharts（数据可视化）

### 部署

- Docker / Docker Compose
- Nginx

---

## ⚡ 性能优化（重点亮点🔥）

### 1️⃣ 并发模型设计

```text
线程池（任务并发） + Parallelism（任务内部并行）
```
<img width="842" height="418" alt="image" src="https://github.com/user-attachments/assets/2c304f14-6466-4628-ae84-3dfbe41a4d62" />
缓存命中耗时
<img width="941" height="363" alt="image" src="https://github.com/user-attachments/assets/f4992af2-8853-4d06-9e52-60e22230ba8b" />


