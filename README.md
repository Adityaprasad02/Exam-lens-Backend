# FileHandler - AI-Powered Exam Paper Analysis Platform

FileHandler is a full-stack application that enables automated analysis of exam question papers. Users upload PDF files containing exam papers, and the system extracts content, applies AI-driven question analysis, and produces structured insights including topic distribution, marks allocation, and importance ranking.

Built with **Spring Boot**, it demonstrates modern backend development practices: RESTful APIs, asynchronous processing, external AI integration, PDF handling, and report generation.

This project serves as a strong portfolio piece for backend engineering roles, showcasing AI integration, non-blocking I/O, and clean architecture.

## Features

- **PDF Upload & Text Extraction**  
  Secure upload of exam papers in PDF format with reliable text extraction.

- **AI-Powered Question Analysis**  
  Leverages large language models (via OpenRouter or Hugging Face APIs) to identify:  
  - Main topics and subtopics  
  - Marks distribution per question/topic  
  - Question difficulty/importance indicators

- **Data Visualization**  
  Interactive charts displaying:  
  - Topic-wise marks distribution  
  - Subtopic importance breakdown

- **Downloadable Analysis Report**  
  Structured PDF report containing summary, detailed analysis, and visualizations — ready for download and sharing.

- **High-Performance Backend**  
  - Non-blocking HTTP client (WebClient)  
  - Asynchronous processing with `CompletableFuture`  
  - Clean REST API design suitable for React, Angular, or mobile frontends

## System Architecture
User → Frontend → REST API (Spring Boot)
↓
PDF Upload & Storage
↓
Text Extraction (PDFBox)
↓
AI Analysis (LLM via WebClient)
↓
Structured Data Processing & Aggregation
↓
Chart Data Preparation + PDF Report Generation
↓
Response to Frontend
text## Workflow

1. User uploads an exam paper (PDF) via the frontend  
2. Backend receives and stores the file temporarily  
3. Text is extracted using Apache PDFBox  
4. Extracted content is sent to an LLM API for structured analysis  
5. Backend processes AI output into domain models  
6. Generates chart-ready data and a formatted PDF report  
7. Returns analysis results and download link to the client

## Tech Stack

### Backend
- Java 17+  
- Spring Boot 3.x  
- Spring WebFlux (WebClient for non-blocking calls)  
- CompletableFuture (async orchestration)  
- Jackson (JSON processing)  
- Apache PDFBox (PDF text extraction)  

### AI Integration
- OpenRouter API / Hugging Face Inference API  

### Reporting & Visualization (API-ready)
- Chart data in JSON format (compatible with Chart.js, Recharts, etc.)  
- PDF generation library (e.g. iText, OpenPDF, or Flying Saucer)

### Recommended Frontend
- React + Vite  
- Axios / TanStack Query  
- Chart.js or Recharts  

### Optional Enhancements
- Python-based OCR service (for scanned/handwritten papers)  
- Database persistence (analysis history)

## Project Structure


FileHandler/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/filehandler/
│   │   │       ├── FileHandlerApplication.java
│   │   │       ├── config/
│   │   │       │   └── WebClientConfig.java
│   │   │       ├── controller/
│   │   │       │   └── FileUploadController.java
│   │   │       ├── model/
│   │   │       │   ├── dto/
│   │   │       │   └── entity/
│   │   │       └── service/
│   │   │           ├── AIAnalysisService.java
│   │   │           ├── FileProcessingService.java
│   │   │           └── ReportGenerationService.java
│   │   └── resources/
│   │       └── application.yml (or application.properties)
│   └── test/                  # (optional – add when you have tests)
└── (other files: .gitignore, README.md, etc.)


## Screenshots

### Topic-wise Marks Distribution Chart
![Topic-wise Marks Distribution](images/Screenshot%202026-03-12%20020239.png)

### AI-Generated Summary & Marks Breakdown
![AI Analysis Summary](images/Screenshot%202026-03-12%20020259.png)

### Interactive Chart Detail (Zoom)
![Chart Zoom View](images/Screenshot%202026-03-12%20020319.png)

### Generated Downloadable PDF Report
![PDF Report Example](images/Screenshot%202026-03-12%20020355.png)



