# Java LangChain4j 从零到实战

微信公众号 [小貔貅技术日记](https://mp.weixin.qq.com/s/Wq27p5Eiq-CC2cQWoPrQng) 专栏「Java LangChain4j 从零到实战」配套示例代码，涵盖从入门到进阶的完整知识体系。

## 项目概述

基于 **LangChain4j 1.0.0** 的实战教程项目，通过 19 个章节、67 个 Java 源文件，系统讲解如何使用 Java 构建大模型应用。

## 技术栈

| 项目 | 版本/说明 |
|------|-----------|
| Java | 17 |
| LangChain4j | 1.0.0 |
| 构建工具 | Maven |
| Chat 模型 | MiniMax-M2.5 |
| Embedding 模型 | 智谱 embedding-3 |

## 环境配置

### 1. 克隆项目

```bash
git clone https://github.com/x-PiXiu/LangChain4jTestDemo.git
cd ExampleDemo
```

### 2. 配置 API Key

复制环境变量模板并填入真实密钥：

```bash
cp .env.example .env
```

需要在 `.env` 文件中配置：

| 环境变量 | 说明 | 获取方式 |
|----------|------|----------|
| `MINIMAX_API_KEY` | MiniMax 平台 API Key | [MiniMax 开放平台](https://platform.minimaxi.com/) |
| `ZHIPU_API_KEY` | 智谱平台 API Key | [智谱开放平台](https://open.bigmodel.cn/) |

或者在 IDE 运行配置中设置环境变量：

```
MINIMAX_API_KEY=your_key_here
ZHIPU_API_KEY=your_key_here
```

### 3. 运行示例

使用 IDE 直接运行各章节的 `main` 方法即可，无需额外启动服务（Redis 相关示例除外）。

## 章节目录

### 入门篇

| 章节 | 目录 | 内容 | 核心类 |
|------|------|------|--------|
| 第1篇 | `the_1` | Hello AI：第一个 LangChain4j 程序 | `HelloAI` |
| 第3篇 | `the_3` | Prompt 工程：代码审查、翻译、评分、意图分类、多轮对话 | `JavaCodeReviewer` `TranslationAssistant` `ScoreAnalysis` `IntentClassifier` `MultiTurnChat` |

### 核心能力篇

| 章节 | 目录 | 内容 | 核心类 |
|------|------|------|--------|
| 第4篇 | `the_4` | 记忆管理：窗口记忆、Redis 持久化、客服场景 | `MemoryChatExample` `PersistentMemoryChat` `MemoryCustomerService` `RedisChatMemoryStore` |
| 第5篇 | `the_5` | 模型配置：多模型切换、流式输出、模型对比 | `MiniMaxConfig` `GLMConfig` `ModelSelector` `ModelComparison` `StreamingDemo` |
| 第6篇 | `the_6` | Chain 模式：对话链、单步/多步代码审查链 | `ConversationalChainDemo` `SmartCodeReviewAssistant` `MultiStepCodeReviewChain` |
| 第7篇 | `the_7` | AI Service：简单/多用户/智能客服、MySQL 存储 | `SimpleCustomerService` `MultiUserCustomerService` `SmartCustomerService` `MySQLChatMemoryStore` |

### RAG 篇

| 章节 | 目录 | 内容 | 核心类 |
|------|------|------|--------|
| 第8篇 | `the_8` | RAG 基础：Embedding + 向量存储 + 检索增强生成 | `SimpleRAG` |
| 第9篇 | `the_9` | 语义搜索引擎 | `SemanticSearchEngine` |
| 第10篇 | `the_10` | 文档分割策略 | `DocumentSplitDemo` |
| 第11篇 | `the_11` | 完整 RAG 管道：索引、检索、生成 | `RAGFullPipelineDemo` |

### Agent 篇

| 章节 | 目录 | 内容 | 核心类 |
|------|------|------|--------|
| 第13篇 | `the_13` | 多工具助手：数据库、日历、邮件工具 | `MultiToolAssistant` |
| 第14篇 | `the_14` | 天气 Agent：工具调用 + Agent 模式 | `WeatherAgentDemo` |
| 第15篇 | `the_15` | 超级助手：多工具编排 + 链路追踪 | `SuperAssistantDemo` `CalendarTool` `EmailTool` `KnowledgeTool` |

### 工程化篇

| 章节 | 目录 | 内容 | 核心类 |
|------|------|------|--------|
| 第17篇 | `the_17` | 可观测性与评估：指标采集、A/B 测试、测试套件、质量评判 | `MetricsCollectingListener` `StructuredLoggingListener` `DebugListener` `ABTestFramework` `AgentTestSuite` `RetrievalEvaluator` `QualityJudge` `FullEvaluationPipeline` |
| 第18篇 | `the_18` | 状态机与 LangGraph：谈判 Agent、人机协作 | `NegotiationStateMachine` `NegotiationAgent` `NegotiationContext` `NegotiationState` `HumanInTheLoop` `LlmDecisionMaker` |
| 第19篇 | `the_19` | Guardrails 与输出解析：PII 脱敏、内容审核、约束输出 | `PIIRedactor` `PIIPattern` `ContentModerator` `GuardrailedCustomerServiceAgent` `GuardrailAuditLog` `EnumConstrainedOutputParser` `LengthBoundedOutputParser` `RobustWeatherOutputParser` |

## 项目结构

```
ExampleDemo/
├── .env.example              # 环境变量模板
├── .gitignore
├── pom.xml                   # Maven 配置
├── README.md
└── src/main/java/
    ├── the_1/                # 第1篇：Hello AI
    ├── the_3/                # 第3篇：Prompt 工程
    ├── the_4/                # 第4篇：记忆管理
    ├── the_5/                # 第5篇：模型配置
    ├── the_6/                # 第6篇：Chain 模式
    ├── the_7/                # 第7篇：AI Service
    ├── the_8/                # 第8篇：RAG 基础
    ├── the_9/                # 第9篇：语义搜索
    ├── the_10/               # 第10篇：文档分割
    ├── the_11/               # 第11篇：完整 RAG 管道
    ├── the_13/               # 第13篇：多工具助手
    ├── the_14/               # 第14篇：天气 Agent
    ├── the_15/               # 第15篇：超级助手
    ├── the_17/               # 第17篇：可观测性与评估
    ├── the_18/               # 第18篇：状态机与 LangGraph
    └── the_19/               # 第19篇：Guardrails
```

## 学习路线

```
入门
 │
 ├── the_1  Hello AI
 ├── the_3  Prompt 工程
 │
核心能力
 │
 ├── the_4  记忆管理
 ├── the_5  模型配置
 ├── the_6  Chain 模式
 ├── the_7  AI Service
 │
RAG
 │
 ├── the_8  RAG 基础
 ├── the_9  语义搜索
 ├── the_10 文档分割
 ├── the_11 完整 RAG 管道
 │
Agent
 │
 ├── the_13 多工具助手
 ├── the_14 天气 Agent
 ├── the_15 超级助手
 │
工程化
 │
 ├── the_17 可观测性与评估
 ├── the_18 状态机与 LangGraph
 └── the_19 Guardrails
```

