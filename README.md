# Shiro Keyboard — prototype Android 0.1

Clavier natif Java, sans bibliothèque tierce à l’exécution, sans publicité ni SDK de statistiques. Projet indépendant, sans code ou assets de Gboard. Android 8 minimum.

## Fonctions implémentées

- AZERTY français, chiffres et symboles, accents, majuscules, suppression Unicode par point de code, actions Entrée de l’application.
- Trois thèmes et trois polices de touches. La police du texte dans l’application destinataire reste contrôlée par cette application.
- Panneau Web : saisie séparée ou import explicite du texte sélectionné, ouverture de DuckDuckGo dans le navigateur. Ce n’est pas un moteur de résultats embarqué.
- Panneau IA : correction, reformulation et traduction en anglais via OpenAI Chat Completions. Brouillon visible, envoi explicite et insertion manuelle de la réponse. Clé API personnelle requise, coûts et disponibilité selon compte et modèle ; un abonnement ChatGPT ne fournit pas automatiquement de crédit API.
- Clé chiffrée AES-GCM avec Android Keystore ; sauvegardes de l’application désactivées. Pas de journal de frappes ni d’accès au presse-papiers.
- Web/IA désactivés pour les types standard de champs de mot de passe et les champs marqués sans apprentissage personnalisé. Une application qui indique mal le type de ses champs peut empêcher cette détection.
- Annulation et effacement du brouillon lors du changement de champ. Une requête déjà envoyée ne peut pas être retirée du serveur par l’annulation locale.

## Construire un APK

Ce projet n’a PAS été compilé ou exécuté sur Android dans l’environnement de création : le SDK et Gradle sont absents et le téléchargement réseau était indisponible. Les contrôles de structure ne remplacent pas une compilation. Des corrections peuvent être nécessaires.

### Android Studio / ordinateur

Ouvrir ce dossier dans Android Studio avec JDK 17, installer SDK Android 35, laisser la synchronisation Gradle se terminer et utiliser Build APK(s). Ou avec Gradle 8.9 installé et ANDROID_HOME configuré :

```sh
gradle --no-daemon :app:assembleDebug :app:lintDebug
```

Sortie : `app/build/outputs/apk/debug/app-debug.apk`.

Le projet ne contient pas de Gradle Wrapper binaire, car il n’a pas été possible de le télécharger ici. Gradle 8.9 doit être installé ou fourni par l’IDE.

### Depuis Android avec GitHub Actions

Dans un dépôt GitHub de ton choix, placer le CONTENU de ce dossier à la racine, y compris `.github/workflows/android.yml`. Lancer Actions → Construire APK Android → Run workflow. Si build et lint réussissent, télécharger l’artefact Shiro-Keyboard-debug et extraire l’APK. Cette procédure exige un compte GitHub et une connexion Internet ; aucun dépôt n’est créé ou publié par ce projet.

L’APK de debug est signé avec la clé du runner. La clé peut changer entre deux builds GitHub : pour des mises à jour installables sans désinstallation, configurer ultérieurement une signature de release stable. Ne jamais publier une clé privée ou un token API dans le dépôt.

## Utilisation

Installer l’APK compilé. Ouvrir Shiro Keyboard → Activer le clavier → Choisir le clavier. Configurer apparence, modèle et clé si nécessaire. Dans une application, Web ou IA ouvre un brouillon dans lequel les touches écrivent séparément du champ courant. « Sélection » importe uniquement la sélection à la demande. L’IA envoie exclusivement le brouillon au point d’API fixe `https://api.openai.com/v1/chat/completions` ; le navigateur reçoit la requête Web. Ne pas envoyer de données sensibles à un service distant sans le souhaiter.

## Validation nécessaire sur appareil

1. Saisie, accents, emojis déjà présents, texte sélectionné, suppression et action Entrée dans plusieurs applications.
2. Rotation, changement de champ/application, portrait/paysage, petits écrans et taille de police système élevée.
3. Champs texte et numériques de mot de passe : pas de panneau IA/Web.
4. Mode avion : saisie locale disponible, erreur IA compréhensible ; absence de clé ne bloque pas le clavier.
5. Requête IA : vérifier le brouillon avant envoi, résultat jamais inséré automatiquement, changement de champ ignorant une ancienne réponse.
6. Activation/changement de clavier, thèmes, effacement et conservation de la clé.

## Limites de cette première version

Pas d’autocorrection, suggestions, saisie glissée, dictée, clavier numérique dédié, ligne emoji, adaptation complète aux insets/bords Android 15 ou équivalent de la maturité Gboard. La suppression est par point de code, pas par groupe de graphèmes : un emoji composé peut demander plusieurs suppressions. Le modèle par défaut est configurable et son accès dépend du compte. La compatibilité API doit être validée en ligne.
