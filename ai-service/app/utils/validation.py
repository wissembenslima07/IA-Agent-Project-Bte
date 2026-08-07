"""Validation des données entrantes"""

class ValidationError(Exception):
    pass

def valider_analyse_request(dossierId: int, documents: list):
    """Valide une demande d'analyse"""
    if not isinstance(dossierId, int) or dossierId <= 0:
        raise ValidationError("dossierId doit être un entier positif")
    
    if not documents or len(documents) == 0:
        raise ValidationError("Au moins un document est requis")
    
    if len(documents) > 20:
        raise ValidationError("Maximum 20 documents par analyse")
    
    for i, doc in enumerate(documents):
        if not isinstance(doc.get('typeDocument'), str) or not doc['typeDocument'].strip():
            raise ValidationError(f"Document {i}: typeDocument requis et non vide")
        
        if not isinstance(doc.get('contenu'), str) or not doc['contenu'].strip():
            raise ValidationError(f"Document {i}: contenu requis et non vide")
        
        if len(doc['contenu']) > 5000:
            raise ValidationError(f"Document {i}: contenu trop long (max 5000 chars)")