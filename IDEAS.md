# Idées d'ajouts pour Random Block Placement

Classées par effort. Priorités recommandées : **1**, **5** et **6**.

---

## Rapides à faire

### 1. Continuer quand un bloc manque ⭐
Aujourd'hui, dès qu'un bloc sélectionné n'est plus dans l'inventaire, le mixin coupe tout le mode (`RandomBlockSelector.missingBlocks`, puis `BlockPlacer.disable()` dans `MultiPlayerGameModeMixin`). C'est frustrant quand un des blocs est épuisé en pleine construction.

- Ajouter un réglage « Si un bloc manque : Désactiver / Continuer avec les autres », en une seule ligne `SettingRow` dans `SettingsScreen`.
- En mode « Continuer », les pourcentages des blocs restants sont recalculés automatiquement, et un avertissement s'affiche une seule fois.

### 2. Un HUD plus informatif
Le HUD (`HudRndBlockPlacer`) affiche seulement « ENABLE » (« ON » ou « Enabled » serait plus naturel en anglais).

- Afficher le nom du preset actif.
- Afficher les petites icônes des blocs sélectionnés avec leur pourcentage.
- Ajouter une option pour choisir la position du HUD.

### 3. Une traduction française
Toutes les chaînes sont déjà des clés de traduction : il suffit d'ajouter `lang/fr_fr.json`.

### 4. L'intégration avec Mod Menu
Ouvrir `SettingsScreen` depuis la liste des mods. Il ne faut presque rien ajouter, l'écran existe déjà.

---

## Nouvelles fonctionnalités

### 5. Changer de preset sans ouvrir le menu ⭐
- Une touche pour passer au preset suivant ou précédent (ou Alt + molette).
- Un message au-dessus de la hotbar confirme le changement : « Preset : Mur de pierre ».
- Très pratique en pleine construction.

### 6. Plusieurs modes de placement ⭐
En plus du hasard pur :

| Mode | Comportement |
|------|--------------|
| **Aléatoire** | Le comportement actuel |
| **Séquentiel** | Les blocs sont posés dans l'ordre : A, B, C, A, B, C… |
| **Pas deux fois de suite** | Le même bloc n'est jamais posé deux fois d'affilée, pour éviter les amas |
| **Selon la position** | Le hasard dépend des coordonnées x, y, z : replacer un bloc au même endroit redonne le même bloc, et on peut faire des damiers ou des motifs |

### 7. Partager des presets
- Un bouton **Export** copie le preset dans le presse-papier sous forme de texte.
- Un bouton **Import** le colle.
- Ça permet d'échanger ses palettes avec d'autres joueurs.

### 8. Une alerte de stock bas
Un avertissement discret quand un des blocs sélectionnés passe sous un certain nombre, avant qu'il ne manque vraiment.

### 9. Créer un preset à partir de la hotbar
Un bouton qui sélectionne d'un coup tous les blocs de la hotbar, avec des parts égales.

---

## Qualité du code

### 10. Des tests unitaires
Le projet n'en a aucun. Deux cibles faciles, car ce sont des fonctions de logique pure :

- `RandomBlockSelector.select` : la répartition selon les poids.
- `BlockSelectionScreenState.setPercent` : les arrondis des pourcentages et le maintien des proportions.

### 11. Une vraie démo dans le README
- Ajouter des captures de la nouvelle interface.
- Retirer du dépôt la vidéo `Random Block Placement DEMO.mp4` (73 Mo), puisque le lien YouTube existe déjà.
