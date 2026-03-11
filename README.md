# 🚀 FileHandler — AI Powered Exam Analysis Platform

## 📌 Overview

**FileHandler** is a full-stack **AI-powered exam paper analysis platform** built using **Java Spring Boot**.  
The system automatically extracts and analyzes question papers from uploaded PDF files and generates **topic-wise insights, marks distribution, and structured reports**.

This project demonstrates **backend engineering, AI integration, asynchronous processing, and data visualization**, making it a strong portfolio project for internships or backend roles.

---

## ⚡ Features

### 📄 PDF Upload
- Upload exam papers in **PDF format**
- Automatic text extraction from question papers

### 🤖 AI-Powered Analysis
- Uses **LLM APIs** to analyze questions
- Extracts:
  - Topics
  - Subtopics
  - Marks distribution
  - Question importance

### 📊 Graphical Insights
- Topic-wise marks distribution
- Important subtopics visualization
- Interactive charts for better understanding

### 📑 Downloadable PDF Report
- Generates a structured **exam analysis report**
- Easy to download and share

### ⚡ High Performance Backend
- Built with **Spring Boot**
- **Async processing** using `CompletableFuture`
- Non-blocking API calls with **WebClient**

### 🔌 Frontend Ready APIs
- Clean **REST APIs**
- Easy integration with **React / Angular / Mobile apps**

---

## 🧠 System Architecture
User Uploads PDF
|
v
Spring Boot Backend
|
v
PDF Text Extraction
|
v
AI Model (LLM API)
|
v
Structured Data Processing
|
v
Charts + PDF Report Generation


---

## 🔄 Workflow

1. User uploads an **exam paper PDF**
2. Backend extracts **text content**
3. AI model analyzes **questions and topics**
4. Backend generates:
   - Topic analysis
   - Charts
   - PDF report
5. Results are returned to the frontend

---

## 🛠 Tech Stack

### Backend
- Java
- Spring Boot
- Spring WebFlux (`WebClient`)
- CompletableFuture
- Jackson JSON Parser

### Frontend (Recommended)
- React
- Vite
- Axios
- Chart.js / Recharts

### AI Integration
- OpenRouter API
- HuggingFace models

### PDF Processing
- Apache PDFBox
- Optional Python OCR service

---

## 📂 Project Structure


FileHandler
│
├── controller
│ └── FileUploadController
│
├── service
│ ├── FileProcessingService
│ ├── AIAnalysisService
│ └── ReportGenerationService
│
├── model
│ ├── ResponseModel
│ └── TopicDetails
│
├── config
│ └── WebClientConfig
│
└── resources
└── application.properties


📸 Screenshots
## Landing Page
![Landing Page](images\Screenshot 2026-03-12 020239.png)

## Upload Page
![Upload Page](images\Screenshot 2026-03-12 020259.png)