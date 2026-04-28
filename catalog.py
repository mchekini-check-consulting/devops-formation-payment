#!/usr/bin/env python3

import json
import random
import time
import urllib.request
import urllib.error
from typing import List, Dict

BASE_URL = "http://localhost:80/api/catalog"

# Catégories disponibles
CATEGORIES = [
    "Informatique",
    "Audio",
    "Tablettes",
    "Smartphones",
    "Moniteurs",
    "Périphériques",
    "Stockage"
]

# Bibliothèque de noms de produits par catégorie
PRODUCTS_BY_CATEGORY = {
    "Informatique": [
        "Ordinateur Portable", "PC Desktop", "Station de Travail", 
        "Mini PC", "Carte Mère", "Processeur Intel i9", "Processeur AMD Ryzen",
        "Carte Graphique RTX", "Mémoire RAM 32Go", "SSD NVMe 1To"
    ],
    "Audio": [
        "Casque Bluetooth", "Enceinte Sans Fil", "Microphone USB",
        "Casque Studio", "Barre de Son", "Écouteurs Intra-Auriculaires",
        "Enceinte Portable", "Casque Gaming"
    ],
    "Tablettes": [
        "Tablette 10 pouces", "Tablette Graphique", "Tablette Enfant",
        "Tablette Pro", "Tablette Amazon Fire", "iPad Air", "Samsung Galaxy Tab"
    ],
    "Smartphones": [
        "Smartphone Android", "iPhone 15", "Samsung Galaxy S24",
        "Google Pixel 8", "Xiaomi Mi 14", "OnePlus 12", "Smartphone Pliable"
    ],
    "Moniteurs": [
        "Moniteur 24 pouces", "Moniteur 27 pouces", "Moniteur 4K",
        "Moniteur Gaming 144Hz", "Moniteur Ultrawide", "Écran Tactile",
        "Moniteur Portable", "Moniteur 32 pouces"
    ],
    "Périphériques": [
        "Souris Gaming", "Clavier Mécanique", "Webcam HD",
        "Tapis Souris", "Support Ordinateur", "Hub USB-C",
        "Adaptateur HDMI", "Station d'Accueil"
    ],
    "Stockage": [
        "Disque Dur Externe 2To", "SSD Interne 1To", "Clé USB 128Go",
        "Carte SD 256Go", "NAS Personnel", "Boîtier Disque Dur",
        "SSD Portable 500Go", "Disque Dur 4To"
    ]
}

def create_product(name: str, price: float, category: str, description: str) -> bool:
    """Crée un produit dans le catalogue"""
    product_data = {
        "name": name,
        "price": price,
        "category": category,
        "description": description
    }
    
    json_data = json.dumps(product_data).encode('utf-8')
    
    try:
        req = urllib.request.Request(
            BASE_URL,
            data=json_data,
            headers={'Content-Type': 'application/json'},
            method='POST'
        )
        
        with urllib.request.urlopen(req, timeout=10) as resp:
            if resp.status in [200, 201]:
                response_data = json.loads(resp.read().decode('utf-8'))
                print(f"  ✓ Produit créé - ID: {response_data.get('id', 'N/A')}")
                return True
            else:
                print(f"  ✗ Erreur HTTP {resp.status}")
                return False
    except urllib.error.HTTPError as e:
        error_msg = e.read().decode('utf-8')
        print(f"  ✗ Erreur HTTP {e.code}: {error_msg}")
        return False
    except Exception as e:
        print(f"  ✗ Erreur: {e}")
        return False

def generate_random_product(index: int) -> Dict:
    """Génère un produit aléatoire"""
    # Choisir une catégorie aléatoire
    category = random.choice(CATEGORIES)
    
    # Choisir un nom depuis la liste de la catégorie ou en générer un
    if category in PRODUCTS_BY_CATEGORY and random.random() > 0.3:
        name = random.choice(PRODUCTS_BY_CATEGORY[category])
        # Ajouter un suffixe unique parfois
        if random.random() > 0.7:
            name = f"{name} {random.choice(['Pro', 'Plus', 'Max', 'Ultra', 'Lite', random.choice(['2024', '2025']), f'V{random.randint(1,3)}'])}"
    else:
        adjectives = ["Premium", "Ultra", "Super", "Pro", "Max", "Essential", "Advanced", "Professional"]
        name = f"{random.choice(adjectives)} {category} {random.randint(100, 999)}"
    
    # Prix : entre 10 et 2000 euros
    if "ordinateur" in name.lower() or "pc" in name.lower() or "station" in name.lower():
        price = round(random.uniform(500, 2500), 2)
    elif "smartphone" in name.lower() or "iphone" in name.lower():
        price = round(random.uniform(300, 1500), 2)
    elif "carte" in name.lower() or "processeur" in name.lower():
        price = round(random.uniform(150, 800), 2)
    else:
        price = round(random.uniform(15, 500), 2)
    
    # Description
    descriptions = [
        f"Produit de haute qualité dans la catégorie {category}",
        f"{name} - Performance et fiabilité garanties",
        f"Dernière technologie en matière de {category.lower()}",
        "Livré avec garantie constructeur 2 ans",
        "Idéal pour les professionnels et particuliers",
        f"Le meilleur rapport qualité-prix dans les {category.lower()}",
        "Certifié et testé par nos experts"
    ]
    description = random.choice(descriptions)
    
    return {
        "name": name,
        "price": price,
        "category": category,
        "description": description
    }

def main():
    print("=" * 60)
    print("Création de 20 produits dans le catalogue")
    print("=" * 60)
    print()
    
    # Vérifier si le service est accessible
    print("[1/3] Vérification du service catalogue...")
    try:
        req = urllib.request.Request(BASE_URL, method='HEAD')
        with urllib.request.urlopen(req, timeout=5) as resp:
            print(f"✓ Service catalogue accessible (HTTP {resp.status})")
    except Exception as e:
        print(f"✗ Service catalogue non accessible: {e}")
        print("Vérifiez que docker-compose est démarré")
        return
    
    print()
    print("[2/3] Création des 20 produits...")
    print("-" * 60)
    
    products_created = 0
    failed_products = []
    
    for i in range(1, 21):
        product = generate_random_product(i)
        
        print(f"\n[{i}/20] Création du produit:")
        print(f"  Nom: {product['name']}")
        print(f"  Prix: {product['price']} €")
        print(f"  Catégorie: {product['category']}")
        print(f"  Description: {product['description'][:50]}...")
        
        if create_product(product['name'], product['price'], product['category'], product['description']):
            products_created += 1
        else:
            failed_products.append(i)
        
        # Petit délai pour ne pas surcharger l'API
        time.sleep(0.3)
    
    print("\n" + "=" * 60)
    print("[3/3] Résumé final")
    print("=" * 60)
    print(f"✓ Produits créés avec succès: {products_created}/20")
    
    if failed_products:
        print(f"✗ Échec pour les produits: {failed_products}")
    else:
        print("✓ Tous les produits ont été créés avec succès !")
    
    print("\n✅ Terminé !")
    input("\nAppuyez sur Entrée pour continuer...")

if __name__ == "__main__":
    main()