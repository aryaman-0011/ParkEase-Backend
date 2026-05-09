# ParkEase — Jenkins CI/CD Setup Guide

## Prerequisites
- **Docker Desktop** installed and running
- **Git** configured with your repository

---

## Quick Start (3 steps)

### Step 1: Start Jenkins + SonarQube
```bash
cd ParkingLot-Backend
docker compose -f docker-compose-jenkins.yml up -d
```

Wait ~2 minutes for both services to boot up.

### Step 2: Get Jenkins Initial Password
```bash
docker exec parkease-jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```
Copy the password that appears.

### Step 3: Open Jenkins
1. Go to **http://localhost:9090**
2. Paste the initial admin password
3. Click **"Install suggested plugins"** (wait for installation)
4. Create your admin user

---

## Configure Jenkins (one-time setup)

### A) Install Required Plugins
Go to: **Manage Jenkins → Plugins → Available plugins**

Search and install:
- ✅ Pipeline
- ✅ Git
- ✅ Maven Integration
- ✅ JaCoCo (optional — for coverage reports in Jenkins UI)

### B) Add JDK 17
Go to: **Manage Jenkins → Tools → JDK installations**
- Name: `JDK-17`
- Check "Install automatically" → Select JDK 17

### C) Add Maven
Go to: **Manage Jenkins → Tools → Maven installations**
- Name: `Maven-3`
- Check "Install automatically" → Select latest Maven 3.x

### D) Add SonarQube Token
1. Open **http://localhost:9000** → Login (admin/admin)
2. Go to **My Account → Security → Generate Token** → Copy it
3. In Jenkins: **Manage Jenkins → Credentials → System → Global credentials**
4. Add → **Secret text**
   - ID: `sonarqube-token`
   - Secret: (paste the SonarQube token)

---

## Create the Pipeline Job

1. Jenkins Dashboard → **New Item**
2. Name: `ParkEase-Pipeline`
3. Type: **Pipeline** → OK
4. Under **Pipeline** section:
   - Definition: **Pipeline script from SCM**
   - SCM: **Git**
   - Repository URL: `https://github.com/aryaman-0011/ParkEase-Backend.git`
   - Branch: `*/main`
   - Script Path: `Jenkinsfile`
5. Click **Save**
6. Click **"Build Now"** 🚀

---

## Pipeline Stages

```
┌─────────────┐    ┌─────────┐    ┌──────┐    ┌────────────┐    ┌─────────┐
│  Checkout    │───>│  Build  │───>│ Test │───>│ SonarQube  │───>│ Package │
│  (Git Pull)  │    │ (Compile)│   │(JUnit)│   │ (Analysis) │    │  (JAR)  │
└─────────────┘    └─────────┘    └──────┘    └────────────┘    └─────────┘
```

| Stage | What it does | When it runs |
|-------|-------------|--------------|
| Checkout | Pulls latest code from GitHub | Every build |
| Build | Compiles all 10 services | Every build |
| Test | Runs 373+ unit tests + JaCoCo | Every build |
| SonarQube | Code quality + coverage analysis | main/develop only |
| Package | Creates deployable JAR files | main only |

---

## Useful URLs
| Service | URL |
|---------|-----|
| Jenkins | http://localhost:9090 |
| SonarQube | http://localhost:9000 |

## Shutting Down
```bash
docker compose -f docker-compose-jenkins.yml down
```

## Important Notes
- The `Jenkinsfile` is a **read-only configuration file** — it does NOT modify any application code
- Jenkins runs in its own Docker container, completely isolated from your project
- All build artifacts are stored inside Jenkins, not in your project directory
