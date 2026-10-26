# 🛡️ Ecosistema Guardian: Plataforma IoMT de Monitoreo Multimodal Continuo para la Prevención del Riesgo Suicida

[![Licencia: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Nivel de Madurez](https://img.shields.io/badge/TRL-4%20(Validación%20en%20Laboratorio)-green.svg)]()
[![Hardware](https://img.shields.io/badge/Hardware-ESP32--C3%20%7C%20RISC--V-red.svg)](Guante/)
[![Android](https://img.shields.io/badge/Gateway-Android%20%7C%20Kotlin%20%7C%20Compose-brightgreen.svg)](APP_GUARDIAN/)
[![Cloud](https://img.shields.io/badge/Cloud-Spring%20Boot%203%20%7C%20React-blueviolet.svg)](WEB_GUARDIAN/)

Plataforma abierta de **Internet de las Cosas Médicas (IoMT)** diseñada para cerrar la brecha temporal entre la manifestación biológica de una crisis emocional y la respuesta oportuna en salud pública. El sistema acopla en tiempo real la respuesta autonómica (*Variabilidad de la Frecuencia Cardíaca - HRV y Conductancia Electrodérmica - GSR*) con evaluaciones ecológicas momentáneas (EMA), ofreciendo una alternativa abierta de bajo costo (**$52 USD**) frente a equipos propietarios cerrados de investigación clínica (**Empatica E4 > $1.800 USD**).

---

## 📌 Tabla de Contenidos
- [Contexto y Justificación Epidemiológica](#-contexto-y-justificación-epidemiológica)
- [Arquitectura Global de 3 Capas](#-arquitectura-global-de-3-capas)
- [Componentes del Repositorio](#-componentes-del-repositorio)
- [Validación Experimental y Resultados Clave](#-validación-experimental-y-resultados-clave)
- [Comparativa Económica (BOM Reales)](#-comparativa-económica-bom-reales)
- [Marco Regulatorio y Salud Pública](#-marco-regulatorio-y-salud-pública)
- [Consideraciones Éticas](#-consideraciones-éticas)
- [Autores y Reconocimientos](#-autores-y-reconocimientos)

---

## 🌍 Contexto y Justificación Epidemiológica

* **Mortalidad Global en Adolescentes (OMS):** De acuerdo con la Organización Mundial de la Salud (*WHO Global Health Estimates, 2024*), el suicidio cobra más de **700.000 vidas anuales** y constituye la **4.ª causa principal de muerte a nivel global en adolescentes y jóvenes de 15 a 19 años**. Más del 77% de los casos se concentran en países de ingresos bajos y medianos.
* **El Iceberg del Subregistro:** El 85% del sufrimiento ocurre de forma silenciosa en la vida cotidiana debido al estigma social y al temor a la hospitalización involuntaria.
* **Dinámica Temporal Intradiaria:** La ideación suicida no es un estado estático; se comporta como **picos agudos de desregulación autonómica** que escalan en minutos u horas. Las consultas psiquiátricas periódicas actúan como «fotos fijas» desconectadas de las fluctuaciones en tiempo real (estudios *JAMA Network Open*, Wang et al., 2021; Czyz et al., 2023).

---

## 🏛️ Arquitectura Global de 3 Capas

El ecosistema desacopla la adquisición física, la agregación móvil y el análisis bioestadístico en tres niveles de alta resiliencia:

```mermaid
graph TD
    subgraph CapaEdge["1. Capa Edge (Wearable Guante)"]
        GLOVE["ESP32-C3 RISC-V (BLE 5.0)<br>FreeRTOS · Batería LiPo 500 mAh"]
        S_PPG["MAX30102: PPG Óptico (100 Hz)"]
        S_GSR["GSR Palmar: Conductancia (10 Hz)"]
        S_TMP["MLX90614: Termometría IR (4 Hz)"]
        S_IMU["MPU-6050: Actigrafía 6-DOF (50 Hz)"]
        MUX["Multiplexor I²C TCA9548A (400 kHz)"]
        
        S_PPG --> MUX
        S_TMP --> MUX
        S_IMU --> MUX
        MUX --> GLOVE
        S_GSR --> GLOVE
    end

    subgraph CapaGateway["2. Capa Gateway (App Android)"]
        APP["Aplicación Móvil Kotlin + Compose<br>(GloveForegroundService · MTU 512)"]
        ROOM[("Room SQLite (Offline-First)<br>Buffer con 0% pérdida")]
        EMA["Módulo EMA (AlarmManager)<br>4 ventanas intradiarias (< 60 s)"]
        
        APP <--> ROOM
        APP <--> EMA
    end

    subgraph CapaCloud["3. Capa Cloud (Backend & Frontend)"]
        API["Spring Boot 3 API REST<br>(JWT · TLS 1.3 · AES-256)"]
        DB[("PostgreSQL 15<br>Series Temporales e Índices")]
        WEB["Panel Web React 18 + Vite<br>(Supervisión Clínica y Filtros)"]
        EXPORT["Exportador Bioestadístico<br>(Matrices .xlsx para R / SPSS)"]
        
        API <--> DB
        WEB <--> API
        API --> EXPORT
    end

    GLOVE -->|Tramas BLE Notificadas (18,4 ms)| APP
    APP -->|Sincronización HTTPS Oportunista (WorkManager)| API
```

---

## 📦 Componentes del Repositorio

El ecosistema se distribuye en repositorios y módulos especializados:

| Repositorio / Módulo | Rol Arquitectural | Tecnologías Principales | Enlace |
| :--- | :--- | :--- | :---: |
| **[`Guante/`](Guante/)** | **Capa Edge** (Hardware & Firmware) | ESP32-C3 (RISC-V), FreeRTOS, BLE 5.0, I²C TCA9548A, MAX30102, GSR, MLX90614, MPU-6050. | [Ver README](Guante/README.md) |
| **[`APP_GUARDIAN/`](APP_GUARDIAN/)** | **Capa Gateway** (Móvil Android) | Kotlin 2.0, Jetpack Compose, Room SQLite, Foreground Services, AlarmManager, Retrofit 2, OkHttp. | [Ver README](APP_GUARDIAN/README.md) |
| **[`WEB_GUARDIAN/`](WEB_GUARDIAN/)** | **Capa Cloud** (Servidor & Panel) | Spring Boot 3, Java 17, PostgreSQL 15, Spring Security (JWT), React 18, Vite, Tailwind CSS, Docker. | [Ver README](WEB_GUARDIAN/README.md) |
| **[`baner/`](baner/)** | **Divulgación Científica** | Póster académico oficial para **ECICIENCIA 2026** (70 cm × 100 cm, PowerPoint vectorial a 300 DPI). | [`baner/`](baner/) |
| **[`ieee/`](ieee/)** | **Publicación Científica** | Manuscrito formal de investigación en formato IEEE LaTeX (`main.tex`). | [`ieee/`](ieee/) |
| **[`articulo tecnova/`](articulo%20tecnova/)** | **Artículo Extendido** | Artículo para revista científica indexada y congreso internacional TECNOVA. | [`articulo tecnova/`](articulo%20tecnova/) |
| **[`datos_extraidos_sabio/`](datos_extraidos_sabio/)** | **Datos Bioestadísticos** | Telemetría real del piloto ambulatorio (20.877 tramas fisiológicas y 27 micro-encuestas EMA). | [`datos_extraidos_sabio/`](datos_extraidos_sabio/) |
| **[`presentacion/`](presentacion/)** | **Sustentación Oral** | Diapositivas web interactivas y guion verbal cronometrado para jurados de grado. | [`presentacion/`](presentacion/) |

---

## 📊 Validación Experimental y Resultados Clave

El prototipo fue sometido a una doble validación (en banco instrumental y ambulatoria longitudinal $N = 1$ durante 8 días continuos):

### 1. Desempeño en Banco Instrumental (60 min continuos)
* **Pérdida de Paquetes BLE:** **0,0 %** durante 4 horas continuas de transmisión ininterrumpida.
* **Latencia Media BLE:** **18,4 ms** por paquete empaquetado.
* **Autonomía Energética:** **11,5 horas** a un consumo medio de 42,3 mA con batería LiPo de 500 mAh.
* **Especificidad del Filtro Cinemático:** **98,0 %** en la discriminación de falsos artefactos mecánicos frente a descargas simpáticas genuinas en reposo ($|a_{\text{din}}| \ge 0,35\text{ g}$).

### 2. Validación Ambulatoria Longitudinal ($N = 1$, 8 días, 20.877 tramas, 27 EMA)
* **Adherencia al Protocolo EMA:** **96,4 %** (27 de 28 evaluaciones completadas en menos de 60 segundos por sesión).
* **Acoplamiento Fisiológico-Conductual:** Durante la jornada de máxima tensión afectiva reportada (29 de septiembre):
  * El **RMSSD (HRV)** cayó de **$48,2\text{ ms}$ a $24,8\text{ ms}$** (**$-48,5\,\%$**, supresión marcada del tono vagal parasimpático).
  * El **GSR (conductancia)** se elevó de **$2,14\ \mu\text{S}$ a $4,85\ \mu\text{S}$** (**$+126,6\,\%$**, descarga adrenérgica simpática aguda).
  * Coincidencia temporal exacta con el pico de sobrecarga subjetiva EMA de **82 / 100**.
* **Factor Protector Identificado:** Fuerte correlación inversa entre el Soporte Social percibido y la Tensión reportada (**$r = -0,69$, $p < 0,001$** en correlación no paramétrica de Spearman).

---

## 💰 Comparativa Económica (BOM Reales)

| Dispositivo / Ecosistema | Tipo de Tecnología | Acceso a Datos | Sensor GSR | Suscripción Mensual | Costo Estimado (USD) | Costo en Pesos (COP) |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| **Empatica E4 / EmbracePlus** | Smartband Médica Cerrada | Propietario / API | Sí | Opcional según plan | **$1.800 USD** | ~$7.200.000 COP |
| **Biopac MP160 (PPG+EDA)** | Sistema Estacionario de Laboratorio | Licencia Propietaria | Sí | No aplica | **>$15.000 USD** | >$60.000.000 COP |
| **Wearables Comerciales** *(Oura/Apple/WHOOP)* | Relojes y Anillos de Consumo | Algoritmos de Caja Negra | No | **$6 – $30 USD / mes** | **$300 – $500 USD** | ~$1.200.000 – $2.000.000 COP |
| **Ecosistema Guardian (Wearable)** | **Plataforma Abierta IoMT (MIT)** | **Bioseñales Crudas (Raw Data)** | **Sí** | **$0 USD (Sin cuotas)** | **$52 USD** | **$220.000 COP** |

> **Ahorro demostrado:** Reducción de costos de más del **97%** frente a la pulsera clínica Empatica E4 y más del **99%** frente a estaciones fijas de laboratorio.

---

## 📜 Marco Regulatorio y Salud Pública

La arquitectura de Guardian se alinea estratégicamente con las directrices de salud pública de Colombia:
1. **Resolución 000347 de 2026 (Ministerio de Salud y Protección Social):** Implementación de mecanismos continuos y nominales de tamizaje tecnológico y seguimiento ambulatorio en salud mental.
2. **Articulación con Código Dorado y Línea 106:** Transformación del modelo reactivo de emergencias hospitalarias hacia una detección temprana subclínica en el hogar.

---

## ⚖️ Consideraciones Éticas y Seguridad

* **Herramienta de Soporte y Tamizaje:** Guardian **no es un dispositivo autónomo de diagnóstico psiquiátrico** ni prescribe intervenciones farmacológicas automáticas; opera como un vector de telemetría objetiva para el profesional tratante.
* **Protección de Datos Sensibles:** Encriptación de datos en reposo (**AES-256**) y en tránsito (**TLS 1.3**).
* **Seudonimización Estricta:** Cumplimiento de la **Ley Estatutaria 1581 de 2012 de Colombia** y los principios éticos de la **Declaración de Helsinki**.

---

## 👥 Autores y Reconocimientos

* **Investigador Principal:** Juan Pablo Nieto Cortés (`juan.nieto-co@mail.escuelaing.edu.co`)
* **Profesor Director / Asesor:** Mario Julián Cañón Ayala
* **Grupo y Semillero:** **Semillero de Bases de Datos** · Centro de Estudios Informáticos (CEI) · Programa de Ingeniería de Sistemas.
* **Institución:** Escuela Colombiana de Ingeniería Julio Garavito, Bogotá D.C., Colombia.
* **Evento de Divulgación:** **ECICIENCIA 2026**.
* **Licencia:** [MIT License](https://opensource.org/licenses/MIT) · Código y Hardware Abierto para la Democratización de la Salud Mental Digital.
