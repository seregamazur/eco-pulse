# eco-pulse

# The Guardian Environment News Enrichment & Search Pipeline

A cloud-native data pipeline designed to collect, enrich (via AI), and archive news articles. The architecture follows an **S3-first** approach to minimize costs associated with AWS OpenSearch.

## Tech Stack
* **Java (Quarkus Native):** Core logic for Daily and Backfill Lambdas, and the Historical processor.
* **Python:** Utility scripts for data fetching and S3 orchestration.
* **AWS Lambda:** Serverless execution of daily enrichment and data restoration.
* **AWS S3:** The "Single Source of Truth" (SSoT) storing both raw CSVs and enriched JSONs.
* **AWS OpenSearch:** Search engine for front-end queries (managed as an on-demand resource).
* **OpenAI API:** GPT models used for content summarization and sentiment analysis.

---

## Data Flow & Workflows

### 1. Initial Bootstrapping (Historical Data)
To process past news from scratch:
1.  **Python Script 1:** Pulls news from *The Guardian API* for a specific date range and saves them to local `.csv` files.
2.  **Python Script 2:** Pushes these CSV files to the S3 bucket (`raw-data/` prefix).
3.  **Historical Java Class:** A local/standalone runner that pulls CSVs from S3, sends content to **ChatGPT** for enrichment, and generates JSON files.
4.  **Python Script 3:** Pushes the final JSON files to S3 (`enriched-news/` prefix).

### 2. Backfill Procedure [`BackfillIndexingLambdaHandler`](https://github.com/seregamazur/eco-pulse/blob/main/ingest-service/src/main/java/com/seregamazur/pulse/pipeline/backfill/BackfillIndexingLambdaHandler.java)
When OpenSearch is re-enabled after a period of being offline:
1.  Deploy/Start the OpenSearch Domain.
2.  Invoke the **Backfill Lambda**.
3.  The Lambda scans `enriched-news/`, reads all JSON files, and performs a **Bulk Indexing** operation to restore the search database.

### 3. Daily Pipeline (Automation)[`ScheduledDailyLambdaHandler`](https://github.com/seregamazur/eco-pulse/blob/main/ingest-service/src/main/java/com/seregamazur/pulse/pipeline/daily/ScheduledDailyLambdaHandler.java)
The automated daily cycle:
* **Trigger:** Triggered nightly via AWS EventBridge.
* **Fetch:** Downloads yesterday's news from The Guardian.
* **Enrich:** Processes text through the OpenAI API.
* **S3 Archive:** Saves the result to `enriched-news/YYYY-MM-DD.json`. **This step is critical and never skipped.**
* **Indexing:** Attempts to push data to OpenSearch. If the domain is missing/off, it fails gracefully without re-triggering ChatGPT (saving API credits).

### 4. Analytics API Lambda (The "Server") [`DashboardController`](https://github.com/seregamazur/eco-pulse/blob/main/query-service/src/main/java/com/seregamazur/pulse/controller/DashboardController.java)
The front-end doesn't talk to OpenSearch directly. Instead, there is a dedicated **Server Lambda** that acts as the analytics engine:
* **Purpose:** Provides REST endpoints for the UI to fetch aggregated data and charts.
* **Date Range:** Supports full historical analysis from **January 1st, 2024, to the present day**.
* **Efficiency:** Optimized to query OpenSearch and format data specifically for visualization libraries (charts/graphs).
---

### Security & Access Control
The API is not just protected by identity; it’s hardened against unauthorized clients:
* **Firebase App Check:** Integrated to ensure only requests from my verified web application can access the analytics endpoints. This prevents scraping and unauthorized API usage (even if someone has a token).
* **Identity:** Works alongside Firebase Auth to provide a multi-layered security model.
* **Logic:** Implemented in [`FirebaseAuthFilter`](https://github.com/seregamazur/eco-pulse/blob/main/query-service/src/main/java/com/seregamazur/pulse/auth/FirebaseAuthFilter.java) to validate the `X-Firebase-AppCheck` tokens on the server side.

## Maintenance & Lessons Learned
* **Storage Hierarchy:**
    * `raw/` -> Original API responses (CSV).
    * `enriched-news/` -> Final AI-processed data (JSON). One file per day.
* **OpenSearch Domain Rules Creation:**
    * Domain Level Access with IP and IAM Role

---

## Python Utility Scripts
1.  `guardian_news_scrap.py`: API interaction and CSV generation.
2.  `upload_to_s3_csv.py`: Generic S3 uploader for local raw news (csv) files.
3.  `upload_to_s3_json.py`: Generic S3 uploader for local enrich news (json) files.