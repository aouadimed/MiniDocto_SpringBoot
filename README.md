# 🏥 Mini Docto+ Backend

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-Atlas-green.svg)](https://www.mongodb.com/atlas)
[![JWT](https://img.shields.io/badge/JWT-0.11.5-blue.svg)](https://github.com/jwtk/jjwt)

## 📝 Présentation

**Mini Docto+** est une API REST pour la mise en relation entre **patients** et **médecins**. 

**Fonctionnalités principales :**
- 🔐 Authentification JWT sécurisée (portails séparés médecins/patients)
- � Gestion des utilisateurs et profils
- 📅 Système de disponibilités médecins
- 🩺 Réservation et gestion de rendez-vous

---

## 🧰 Technologies

- **Java 17** + **Spring Boot 3.5.3**
- **MongoDB Atlas** (base NoSQL)
- **JWT** pour l'authentification
- **Spring Security** + **BCrypt**
- **Lombok** + **Maven**

---

## ⚙️ Installation Rapide

### Prérequis
- Java 17+, Maven 3.6+
- Compte MongoDB Atlas

### Configuration
```properties
# src/main/resources/application.properties
spring.data.mongodb.uri=mongodb+srv://user:pass@cluster.mongodb.net/minidoctoplus
jwt.secret=VotreCleSecrete56Caracteres
jwt.expirationMs=900000          # 15 minutes
jwt.refreshExpirationMs=604800000 # 7 jours
server.port=8081
```

### Démarrage
```bash
mvn clean compile
mvn spring-boot:run
# API disponible sur http://localhost:8081
```

---

## � Sécurité

### JWT & Authentification
- **Tokens séparés** : Access (15min) + Refresh (7 jours)
- **Portails distincts** : `/auth/doctor/login` et `/auth/patient/login`
- **Rôles** : USER (patients) et PRO (médecins)

### Protection
- Mots de passe **hachés BCrypt**
- **Filtrage JWT** automatique sur tous les endpoints
- **Validation des rôles** côté backend
- **CORS configuré** pour les apps frontend

```java
// Exemple de sécurisation
@PreAuthorize("hasRole('PRO')")  // Médecins uniquement
@GetMapping("/appointments/doctor/me")
```

---

## 🚀 Performance

### Optimisations Appliquées
- **Pagination** sur toutes les listes (médecins, rendez-vous)
- **Requêtes MongoDB filtrées** par date/statut
- **Indexes composés** sur doctorId+startTime
- **Lazy loading** des relations
- **DTO Pattern** pour séparer modèles/réponses

### Exemple Pagination
```java
@GetMapping("/available-doctors")
public ResponseEntity<...> getAvailableDoctors(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size) {
    // Pagination automatique Spring Data
}
```

---

## 🔗 API Endpoints Principaux

### Authentification
```bash
POST /auth/doctor/login     # Connexion médecin
POST /auth/patient/login    # Connexion patient  
POST /auth/refresh          # Renouveler token
```

### Disponibilités
```bash
GET  /availability/available-doctors        # Liste médecins
GET  /availability/doctors/{id}/schedule-groups  # Planning médecin
POST /availability/update                   # MAJ créneaux (médecin)
```

### Rendez-vous
```bash
POST /appointments/book       # Réserver RDV (patient)
GET  /appointments/me         # Mes RDV (patient)
GET  /appointments/doctor/me  # Mes RDV (médecin)
DELETE /appointments/{id}     # Annuler RDV
```

---

## 📎 Exemple d'Usage

### 1. Connexion Médecin
```bash
POST /auth/doctor/login
{
  "email": "dr.martin@hospital.fr",
  "password": "password123"
}
# → Retourne JWT access/refresh tokens
```

### 2. Réservation Patient
```bash
POST /appointments/book
Authorization: Bearer <jwt_token>
{
  "doctorId": "64f123...",
  "slotId": "64f789..."
}
# → Réservation confirmée avec détails
```

---

## 🏗️ Architecture

```
com.minidocto/
├── auth/          # Authentification JWT
├── user/          # Gestion utilisateurs  
├── availability/  # Créneaux médecins
├── appointment/   # Rendez-vous
└── shared/        # Config sécurité, utils
```

**Pattern utilisé :** Controller → Service → Repository → MongoDB
