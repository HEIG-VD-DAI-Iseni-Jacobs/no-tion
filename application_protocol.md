# The "No-Tion" Application Protocol

## Overview
Le protocole de l'application "No-Tion" est un protocole de communication qui permet à un client de gérer ses notes en communiquant avec un serveur.

## Transport protocol
Il utilise le protocole de transport TCP pour assurer la fiabilité de la transmission des données.

Le port qu'il utilise est le numéro de port 16447.

Le protocole de l'application "No-Tion" est un protocole de transport de texte où chaque message doit être encodé en UTF-8 et délimité par un caractère de nouvelle ligne (`\n`).

Les messages sont traités comme des messages textuels.

La connexion initiale doit être établie par le client.

Une fois la connexion établie, le client peut envoyer des commandes au serveur pour gérer ses notes.

Le serveur doit vérifier si les commandes reçues sont valides et les exécuter.

Si une commande est invalide, le serveur doit renvoyer un message d'erreur au client.

Sur un message inconnu, le serveur doit renvoyer un message d'erreur au client.

// TODO : relire et corriger
## Messages

### Connexion
Le client envoie une commande de connexion au serveur pour s'authentifier.

#### Requête
```text
CONNECT
```

#### Réponse
```text
OK
```

### Déconnexion
Le client envoie une commande de déconnexion au serveur pour se déconnecter.

#### Requête
```text
DISCONNECT
```

#### Réponse
```text
OK
```

### Création d'une note
Le client envoie une commande de création de note au serveur.

#### Requête
```text
CREATE_NOTE
Titre de la note
Contenu de la note
```

#### Réponse
```text
OK
```

### Suppression d'une note
Le client envoie une commande de suppression de note au serveur.

#### Requête
```text
DELETE_NOTE
Titre de la note
```

#### Réponse
```text
OK
```

### Liste des notes
Le client envoie une commande pour obtenir la liste des notes au serveur.

#### Requête
```text
LIST_NOTES
```

#### Réponse
```text
Titre de la note 1
Contenu de la note 1
Titre de la note 2
Contenu de la note 2
...
```

### Récupération d'une note
Le client envoie une commande pour récupérer une note au serveur.

#### Requête
```text
GEt_NOTE
Titre de la note
```

#### Réponse
```text
Contenu de la note
```

### Modification d'une note
Le client envoie une commande pour modifier une note au serveur.

#### Requête
```text
UPDATE_NOTE
Titre de la note
Nouveau contenu de la note
```

#### Réponse
```text
OK
```

### Erreur
Le serveur envoie un message d'erreur au client.

#### Réponse
```text
ERROR <code>
```

// TODO : réduire les erreurs
Les codes d'erreur sont les suivants :
- 1 : la note n'existe pas
- 2 : la note existe déjà
- 3 : erreur inconnue
- 4 : commande inconnue
- 5 : erreur de syntaxe
- 6 : erreur de connexion
- 7 : erreur de déconnexion
- 8 : erreur de création de note
- 9 : erreur de suppression de note
- 10 : erreur de récupération de note
- 11 : erreur de modification de note
- 12 : erreur de liste des notes

// TODO : implémenter
## Exemples

