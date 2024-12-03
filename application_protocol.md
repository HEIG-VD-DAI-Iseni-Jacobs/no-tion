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
Le client envoie une commande de connexion au serveur pour s'authentifier avec son nom d'utilisateur.

#### Requête
```text
CONNECT <name>
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
None.

### Création d'une note
Le client envoie une commande de création de note au serveur.
Pour simplifier le code, le client devra d'abord créer une note, puis la modifier pour ajouter le contenu.

#### Requête
```text
CREATE_NOTE <titre>
```

#### Réponse
```text
OK
```

### Suppression d'une note
Le client envoie une commande de suppression de note au serveur.

#### Requête
```text
DELETE_NOTE <titre>
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
1 <titre de la note 1>
2 <titre de la note 2>
...
n <titre de la note n>
```

### Récupération d'une note
Le client envoie une commande pour récupérer une note au serveur.

#### Requête
```text
GET_NOTE <index>
```

#### Réponse
```text
NOTE <contenu>
```

### Modification du contenu d'une note
Le client envoie une commande pour modifier une note au serveur.

#### Requête
```text
UPDATE_CONTENT <index> <nouveau contenu>
```

#### Réponse
```text
OK
```

### Modification du titre d'une note
Le client envoie une commande pour modifier le titre d'une note au serveur.

#### Requête
```text
UPDATE_TITLE <index> <nouveau titre>
```

#### Réponse
```text
OK
```


### Erreur
Le serveur envoie un message d'erreur au client si une des requêtes est invalide.

#### Réponse
```text
ERROR <code>
```

Les codes d'erreur sont les suivants :
- -1 : not found (Note inexistante)
- -2 : conflict (Note déjà existante)
- -3 : syntax error (Commande inconnue ou incorrecte)

// TODO : implémenter
## Exemples

